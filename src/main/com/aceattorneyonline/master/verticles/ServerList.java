package com.aceattorneyonline.master.verticles;

import java.util.UUID;

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

public class ServerList extends ServerListVerticle {

	private static final Logger logger = LoggerFactory.getLogger(ServerList.class);

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
			client.protocolWriter().sendServerEntries(getSortedServerList());
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
				AdvertisedServer server = new AdvertisedServer(advertiser.address(), hb.getPort(), hb.getName(),
						hb.getDescription(), hb.getVersion());
				advertiser.setServer(server);
				server.setDelistCallback(new DelistCallback(server));
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

	public class DelistCallback {
		private AdvertisedServer server;

		public DelistCallback(AdvertisedServer server) {
			this.server = server;
		}

		public void delist() {
			logger.debug("Delisted {} from server list", server);
			removeServer(server);
		}
	}

}
