package com.aceattorneyonline.master.verticles;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.Advertiser;
import com.aceattorneyonline.master.Client;
import com.aceattorneyonline.master.Player;
import com.aceattorneyonline.master.events.AdvertiserEventProtos.NewAdvertiser;
import com.aceattorneyonline.master.events.EventErrorReason;
import com.aceattorneyonline.master.events.Events;
import com.aceattorneyonline.master.events.PlayerEventProtos.NewPlayer;
import com.aceattorneyonline.master.events.SharedEventProtos.GetMotd;
import com.google.protobuf.InvalidProtocolBufferException;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class NewClientReceiver extends ClientListVerticle {

	private static final Logger logger = LoggerFactory.getLogger(NewClientReceiver.class);

	@Override
	public void start() {
		logger.info("New client receiver verticle starting");
		EventBus eventBus = getVertx().eventBus();
		eventBus.consumer(Events.NEW_PLAYER.toString(), this::handleNewPlayer);
		eventBus.consumer(Events.NEW_ADVERTISER.toString(), this::handleNewAdvertiser);
	}

	@Override
	public void stop() {
		logger.info("New client receiver verticle stopping (no new clients will be accepted)");
	}

	public void handleNewPlayer(Message<byte[]> event) {
		try {
			NewPlayer newPlayer = NewPlayer.parseFrom(event.body());
			UUID clientId = UUID.fromString(newPlayer.getId().getId());
			Player player = getPlayerById(clientId);
			if (player != null) {
				player.socket().endHandler(nil -> {
					logger.debug("Dropped {} from client list", player);
					player.onDisconnect();
					removePlayer(clientId, player);
				});
				getVertx().eventBus().send(Events.GET_MOTD.getEventName(), GetMotd.newBuilder().build().toByteArray(), reply -> {
					event.reply(reply.result().body() + "\n" + addUserSpecificMessage(player));
				});
			} else {
				event.fail(EventErrorReason.INTERNAL_ERROR, "Player does not exist on player table");
			}
		} catch (InvalidProtocolBufferException e) {
			event.fail(EventErrorReason.INTERNAL_ERROR, "Could not parse NewPlayer protobuf");
		}
	}

	/** Adds a user-specific message on top of the MOTD itself. */
	private String addUserSpecificMessage(Client client) {
		StringBuilder message = new StringBuilder();
		IPAddress address = new IPAddressString(client.address().host()).getAddress();
		if (address.isIPv6()) {
			message.append("You are connected to the master server via IPv6. Great!");
		}
		return message.toString();
	}

	public void handleNewAdvertiser(Message<byte[]> event) {
		try {
			NewAdvertiser newAdvertiser = NewAdvertiser.parseFrom(event.body());
			UUID clientId = UUID.fromString(newAdvertiser.getId().getId());
			Advertiser advertiser = getAdvertiserById(clientId);
			if (advertiser != null) {
				advertiser.socket().endHandler(nil -> {
					logger.info("Dropped {} from advertiser list", advertiser);
					if (advertiser.server() != null) {
						advertiser.server().delist();
					}
					removeAdvertiser(clientId, advertiser);
				});
				event.reply(null);
			} else {
				event.fail(EventErrorReason.SECURITY_ERROR, "Advertiser does not exist on advertiser table");
			}
		} catch (InvalidProtocolBufferException e) {
			event.fail(EventErrorReason.INTERNAL_ERROR, "Could not parse NewAdvertiser protobuf");
		}
	}

}
