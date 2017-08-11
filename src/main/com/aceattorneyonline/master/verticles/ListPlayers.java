package com.aceattorneyonline.master.verticles;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.events.Events;
import com.aceattorneyonline.master.events.PlayerEventProtos;
import com.aceattorneyonline.master.events.UuidProto.Uuid;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class ListPlayers extends ClientListVerticle {

	private static final Logger logger = LoggerFactory.getLogger(ListPlayers.class);

	public void start() {
		logger.info("List players verticle starting");

		EventBus eventBus = getVertx().eventBus();
		eventBus.consumer(Events.LIST_PLAYERS.getEventName(), this::handleListPlayers);
	}

	@Override
	public void stop() {
		logger.info("List players verticle stopping");
	}

	public void handleListPlayers(Message<byte[]> event) {
		ClientListVerticle clv = ClientListVerticle.getSingleton();
		String list = clv.getPlayersList().stream()
				.filter(p -> !p.name().isEmpty() && !p.isSystem()).map(p -> p.name())
				.collect(Collectors.joining(", "));
		event.reply(String.format("There are %d players online:\n%s", clv.getNamedPlayerCount(), list));
	}

	public static com.google.protobuf.Message parseListPlayers(Uuid invoker, List<String> args)
			throws IllegalArgumentException {
		return PlayerEventProtos.ListPlayers.newBuilder().build();
	}
}
