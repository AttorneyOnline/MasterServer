package com.aceattorneyonline.master.verticles;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.Client;
import com.aceattorneyonline.master.events.Events;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class Motd extends ClientListVerticle {

	private static final Logger logger = LoggerFactory.getLogger(Motd.class);

	public Motd(Map<UUID, Client> clientList) {
		super(clientList);
	}

	@Override
	public void start() {
		logger.info("MOTD verticle starting");

		EventBus eventBus = getVertx().eventBus();
		eventBus.consumer(Events.SET_MOTD.toString(), this::handleSetMotd);
		eventBus.consumer(Events.RELOAD_MOTD.toString(), this::handleReloadMotd);
	}

	@Override
	public void stop() {
		logger.info("MOTD verticle stopping");
	}

	public void handleSetMotd(Message<String> event) {
		event.fail(0, "not implemented"); // TODO handleSetMotd
	}

	public void handleReloadMotd(Message<String> event) {
		event.fail(0, "not implemented"); // TODO handleReloadMotd
	}

}
