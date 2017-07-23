package com.aceattorneyonline.master.verticles;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.AdvertisedServer;
import com.aceattorneyonline.master.Advertiser;
import com.aceattorneyonline.master.Client;
import com.aceattorneyonline.master.events.AdvertiserEventProtos.Heartbeat;
import com.aceattorneyonline.master.events.AdvertiserEventProtos.Ping;
import com.aceattorneyonline.master.events.EventErrorReason;
import com.aceattorneyonline.master.events.Events;
import com.aceattorneyonline.master.events.PlayerEventProtos.GetServerList;
import com.aceattorneyonline.master.events.PlayerEventProtos.GetServerListPaged;
import com.google.protobuf.InvalidProtocolBufferException;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.net.SocketAddress;

public class ServerList extends ClientListVerticle {

	private static final Logger logger = LoggerFactory.getLogger(ServerList.class);

	private Comparator<AdvertisedServer> listComparator = (a, b) -> a.uptime().compareTo(b.uptime());
	private Map<SocketAddress, AdvertisedServer> serverList = new HashMap<>();

	private List<AdvertisedServer> serverListCache = new ArrayList<>();
	private boolean serverListCacheDirty = false;

	@Override
	public void start() {
		logger.info("Server list verticle starting");
		EventBus eventBus = getVertx().eventBus();
		eventBus.consumer(Events.GET_SERVER_LIST.toString(), this::handleGetServerList);
		eventBus.consumer(Events.GET_SERVER_LIST_PAGED.toString(), this::handleGetServerListPaged);
		eventBus.consumer(Events.ADVERTISER_HEARTBEAT.toString(), this::handleHeartbeat);
		eventBus.consumer(Events.ADVERTISER_PING.toString(), this::handlePing);
	}

	@Override
	public void stop() {
		logger.info("Server list verticle stopping");
	}

	public void handleGetServerList(Message<byte[]> event) {
		try {
			GetServerList gsl = GetServerList.parseFrom(event.body());
			UUID id = UUID.fromString(gsl.getId().getId());
			Client client = getClientById(id);
			logger.debug("Handling get server list event from {}", client);
			client.protocolWriter().sendServerEntries(serverListCache);
			event.reply(null);
		} catch (InvalidProtocolBufferException e) {
			event.fail(EventErrorReason.INTERNAL_ERROR, "Could not parse GetServerList protobuf");
		}
	}

	public void handleGetServerListPaged(Message<byte[]> event) {
		try {
			GetServerListPaged gslp = GetServerListPaged.parseFrom(event.body());
			UUID id = UUID.fromString(gslp.getId().getId());
			int pageNo = gslp.getPage();
			AdvertisedServer server = getSortedServerList().get(pageNo);
			getClientById(id).protocolWriter().sendServerEntry(pageNo, server);
			event.reply(null);
		} catch (InvalidProtocolBufferException e) {
			event.fail(EventErrorReason.INTERNAL_ERROR, "Could not parse GetServerListPaged protobuf");
		} catch (IndexOutOfBoundsException e) {
			// event.fail(EventErrorReason.INTERNAL_ERROR, "Could not get a list at that
			// page");
			// Actually, the canonical response is nothing.
			event.reply(null);
		}
	}

	public void handleHeartbeat(Message<byte[]> event) {
		try {
			Heartbeat hb = Heartbeat.parseFrom(event.body());
			UUID id = UUID.fromString(hb.getId().getId());
			Advertiser advertiser = getAdvertiserById(id);
			if (advertiser != null) {
				AdvertisedServer server =
						new AdvertisedServer(advertiser.address(), hb.getPort(), hb.getName(), hb.getDescription(), hb.getVersion());
				advertiser.setServer(server);
				addServer(server);
				event.reply(null);
			} else {
				event.fail(EventErrorReason.SECURITY_ERROR, "Client is not an advertiser");
			}
		} catch (InvalidProtocolBufferException e) {
			event.fail(EventErrorReason.INTERNAL_ERROR, "Could not parse Heartbeat protobuf");
		}
	}

	public void handlePing(Message<byte[]> event) {
		try {
			Ping ping = Ping.parseFrom(event.body());
			UUID id = UUID.fromString(ping.getId().getId());
			Advertiser advertiser = getAdvertiserById(id);
			if (advertiser != null && advertiser.server() != null) {
				advertiser.protocolWriter().sendPong();
				event.reply(null);
			} else {
				advertiser.protocolWriter().sendPongError();
				event.reply(null);
			}
		} catch (InvalidProtocolBufferException e) {
			event.fail(EventErrorReason.INTERNAL_ERROR, "Could not parse Ping protobuf");
		}
	}

	/** Caches a sorted version of the server list. */
	private void cacheServerList() {
		synchronized (serverListCache) {
			serverListCache = serverList.values().stream().sorted(listComparator).collect(Collectors.toList());
		}
		serverListCacheDirty = false;
	}

	/** Adds or updates an advertised server. */
	public void addServer(AdvertisedServer server) {
		synchronized (serverList) {
			serverList.put(server.address(), server);
		}
		serverListCacheDirty = true;
	}

	/** Removes an advertised server from the server list. */
	public void removeServer(AdvertisedServer server) {
		synchronized (serverList) {
			serverList.remove(server.address());
		}
		serverListCacheDirty = true;
	}

	/** Gets a sorted version of the server list from cache. */
	public List<AdvertisedServer> getSortedServerList() {
		if (serverListCacheDirty) {
			cacheServerList();
		}
		return serverListCache;
	}

}
