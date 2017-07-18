package com.aceattorneyonline.master.verticles;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.Client;
import com.aceattorneyonline.master.events.Events;

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
		event.fail(0, "not implemented"); // TODO
	}

	public void handleNewAdvertiser(Message<String> event) {
		event.fail(0, "not implemented"); // TODO
	}

}
