package com.aceattorneyonline.master.verticles;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.AdvertisedServer;
import com.aceattorneyonline.master.Advertiser;
import com.aceattorneyonline.master.Client;
import com.aceattorneyonline.master.ServerInfo;
import com.aceattorneyonline.master.events.AdvertiserEventProtos.Heartbeat;
import com.aceattorneyonline.master.events.AdvertiserEventProtos.Ping;
import com.aceattorneyonline.master.events.EventErrorReason;
import com.aceattorneyonline.master.events.Events;
import com.aceattorneyonline.master.events.PlayerEventProtos.GetServerList;
import com.aceattorneyonline.master.events.PlayerEventProtos.GetServerListPaged;
import com.google.protobuf.InvalidProtocolBufferException;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class Servers extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(Servers.class);

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
			ClientServerList masterList = ClientServerList.getSingleton();
			Client client = masterList.getClientById(id);
			logger.debug("{}: Handling get server list event", client);
			client.protocolWriter().sendServerEntries(masterList.getSortedServerList());
			event.reply(null);
		} catch (InvalidProtocolBufferException e) {
			event.fail(EventErrorReason.INTERNAL_ERROR, "Could not parse GetServerList protobuf");
		}
	}

	public void handleGetServerListPaged(Message<byte[]> event) {
		try {
			GetServerListPaged gslp = GetServerListPaged.parseFrom(event.body());
			UUID id = UUID.fromString(gslp.getId().getId());
			ClientServerList masterList = ClientServerList.getSingleton();
			Client client = masterList.getClientById(id);
			int pageNo = gslp.getPage();
			logger.debug("{}: Handling paged get server list event (page {})", client, pageNo);
			if (pageNo == -1) {
				int curPage = 0;
				for (AdvertisedServer server : masterList.getSortedServerList()) {
					client.protocolWriter().sendServerEntry(curPage++, server);
				}
				event.reply(null);
			} else {
				AdvertisedServer server = masterList.getSortedServerList().get(pageNo);
				client.protocolWriter().sendServerEntry(pageNo, server);
				event.reply(null);
			}
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
			ClientServerList masterList = ClientServerList.getSingleton();
			Advertiser advertiser = masterList.getAdvertiserById(id);
			// By not passing the port by value, we end up keeping a strong reference to the heartbeat
			// protobuf, established by the SocketAddress instantiation.
			if (advertiser != null) {
				ServerInfo info = new ServerInfo(hb.getName(), hb.getDescription(), hb.getVersion());
				AdvertisedServer server = masterList.addServer(advertiser, hb.getPort(), info);
				advertiser.setServer(server);				
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
			ClientServerList masterList = ClientServerList.getSingleton();
			Advertiser advertiser = masterList.getAdvertiserById(id);
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

}
