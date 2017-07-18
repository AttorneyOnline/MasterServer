package com.aceattorneyonline.master.verticles;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.Client;
import com.aceattorneyonline.master.Player;
import com.aceattorneyonline.master.events.Events;
import com.aceattorneyonline.master.events.PlayerEventProtos.SendChat;
import com.google.protobuf.InvalidProtocolBufferException;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class Chat extends ClientListVerticle {

	private static final Logger logger = LoggerFactory.getLogger(Chat.class);

	public Chat(Map<UUID, Client> clientList) {
		super(clientList);
	}

	@Override
	public void start() {
		logger.info("Chat verticle starting");
		EventBus eventBus = getVertx().eventBus();
		eventBus.consumer(Events.SEND_CHAT.toString(), this::handleSendChat);
	}

	@Override
	public void stop() {
		logger.info("Chat verticle stopping");
	}

	public void handleSendChat(Message<String> event) {
		try {
			SendChat chat = SendChat.parseFrom(event.body().getBytes());
			UUID id = UUID.fromString(chat.getId().getId());
			Player sender = getPlayerById(id);
			String message = chat.getMessage();

			if (sender == null) {
				event.fail(1, "Requester is not a player.");
			}

			if (message.charAt(0) == '!') {
				String[] args = parseChatCommand(message);
			}

			getVertx().eventBus().publish(Events.BROADCAST_CHAT.toString(), message);

			logger.info("{}: {}", id.toString(), message);
		} catch (InvalidProtocolBufferException e) {
			event.fail(1, "Could not parse SendChat protobuf");
		}
	}

	private String[] parseChatCommand(String message) {
		return new String[0];
	}

}
