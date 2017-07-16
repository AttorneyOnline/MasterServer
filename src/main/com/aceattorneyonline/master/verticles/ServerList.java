package com.aceattorneyonline.master.verticles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.events.Events;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class ServerList extends AbstractVerticle {

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

	public void handleGetServerList(Message<String> event) {
		event.fail(0, "not implemented"); // TODO
	}

	public void handleGetServerListPaged(Message<String> event) {
		event.fail(0, "not implemented"); // TODO
	}
	
	public void handleHeartbeat(Message<String> event) {
		event.fail(0, "not implemented"); // TODO
	}
	
	public void handlePing(Message<String> event) {
		event.fail(0, "not implemented"); // TODO
	}

}
