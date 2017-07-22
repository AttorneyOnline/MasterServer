package com.aceattorneyonline.master.verticles;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.Advertiser;
import com.aceattorneyonline.master.Client;
import com.aceattorneyonline.master.Player;
import com.aceattorneyonline.master.UnconnectedClient;
import com.aceattorneyonline.master.events.AdvertiserEventProtos.NewAdvertiser;
import com.aceattorneyonline.master.events.EventErrorReason;
import com.aceattorneyonline.master.events.Events;
import com.aceattorneyonline.master.events.PlayerEventProtos.NewPlayer;
import com.google.protobuf.InvalidProtocolBufferException;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class NewClientReceiver extends ClientListVerticle {

	private static final Logger logger = LoggerFactory.getLogger(NewClientReceiver.class);

	public NewClientReceiver(Map<UUID, Client> clientList) {
		super(clientList);
	}

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

	public void handleNewPlayer(Message<String> event) {
		try {
			NewPlayer newPlayer = NewPlayer.parseFrom(event.body().getBytes());
			UUID clientId = UUID.fromString(newPlayer.getId().getId());
			if (getPlayerById(clientId) == null) {
				// HACK: This cast is relatively safe, but I can't find a clean way to get it
				// off.
				// Maybe getUnconnectedClientById()?
				UnconnectedClient oldClient = (UnconnectedClient)getClientById(clientId);
				
				Player player = new Player(oldClient);
				player.context().endHandler(nil -> {
					removePlayer(clientId, player);
				});
				oldClient.setSuccessor(player);
				addPlayer(clientId, player);
			} else {
				event.fail(EventErrorReason.SECURITY_ERROR, "Player already exists");
			}
		} catch (InvalidProtocolBufferException e) {
			event.fail(EventErrorReason.INTERNAL_ERROR, "Could not parse NewPlayer protobuf");
		}
	}

	public void handleNewAdvertiser(Message<String> event) {
		try {
			NewAdvertiser newAdvertiser = NewAdvertiser.parseFrom(event.body().getBytes());
			UUID clientId = UUID.fromString(newAdvertiser.getId().getId());
			if (getAdvertiserById(clientId) == null) {
				UnconnectedClient oldClient = (UnconnectedClient)getClientById(clientId);
				
				Advertiser advertiser = new Advertiser(oldClient);
				advertiser.context().endHandler(nil -> {
					removeAdvertiser(clientId, advertiser);
				});
				oldClient.setSuccessor(advertiser);
				addAdvertiser(clientId, advertiser);
			} else {
				event.fail(EventErrorReason.SECURITY_ERROR, "Advertiser already exists");
			}
		} catch (InvalidProtocolBufferException e) {
			event.fail(EventErrorReason.INTERNAL_ERROR, "Could not parse NewAdvertiser protobuf");
		}
	}

}
