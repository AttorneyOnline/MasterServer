package com.aceattorneyonline.master.verticles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.events.Events;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class Motd extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(Motd.class);

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
		event.fail(0, "not implemented"); // TODO
	}

	public void handleReloadMotd(Message<String> event) {
		event.fail(0, "not implemented"); // TODO
	}

}
