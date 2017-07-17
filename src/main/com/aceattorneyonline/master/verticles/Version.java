package com.aceattorneyonline.master.verticles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.MasterServer;
import com.aceattorneyonline.master.events.Events;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class Version extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(Version.class);

	@Override
	public void start() {
		logger.info("Version verticle starting");
		EventBus eventBus = getVertx().eventBus();
		eventBus.consumer(Events.GET_VERSION.toString(), this::handleGetVersion);
	}

	@Override
	public void stop() {
		logger.info("Version verticle stopping");
	}

	public void handleGetVersion(Message<String> event) {
		event.reply(MasterServer.VERSION);
	}

}
