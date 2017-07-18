package com.aceattorneyonline.master.verticles;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.Client;
import com.aceattorneyonline.master.events.Events;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class BanList extends ClientListVerticle {

	private static final Logger logger = LoggerFactory.getLogger(BanList.class);


	public BanList(Map<UUID, Client> clientList) {
		super(clientList);
	}

	@Override
	public void start() {
		logger.info("Bans list verticle starting");

		EventBus eventBus = getVertx().eventBus();
		eventBus.consumer(Events.BAN_PLAYER.toString(), this::handleBanPlayer);
		eventBus.consumer(Events.UNBAN_PLAYER.toString(), this::handleUnbanPlayer);
		eventBus.consumer(Events.RELOAD_BANS.toString(), this::handleReloadBans);
	}

	@Override
	public void stop() {
		logger.info("Bans list verticle stopping");
	}

	public void handleBanPlayer(Message<String> event) {
		event.fail(0, "not implemented"); // TODO
	}

	public void handleUnbanPlayer(Message<String> event) {
		event.fail(0, "not implemented"); // TODO
	}

	public void handleReloadBans(Message<String> event) {
		event.fail(0, "not implemented"); // TODO
	}

}
