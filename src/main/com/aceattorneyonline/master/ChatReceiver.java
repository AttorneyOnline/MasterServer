package com.aceattorneyonline.master;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.events.Events;
import com.aceattorneyonline.master.events.PlayerEventProtos;
import com.google.protobuf.InvalidProtocolBufferException;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

/**
 * Hooks into a player to receive chat message broadcasts from the event bus.
 */
public class ChatReceiver implements Handler<Message<String>> {

	private static final Logger logger = LoggerFactory.getLogger(ChatReceiver.class);

	private Client client;

	public ChatReceiver(Client client) {
		this.client = client;

		EventBus eventBus = MasterServer.vertx.eventBus();
		eventBus.consumer(Events.BROADCAST_CHAT.toString(), this);
	}

	@Override
	public void handle(Message<String> event) {
		PlayerEventProtos.SendChat chatMsg;
		try {
			chatMsg = PlayerEventProtos.SendChat.parseFrom(event.body().getBytes());
			client.protocolWriter().sendChatMessage(chatMsg.getUsername(), chatMsg.getMessage());
		} catch (NullPointerException e) {
			logger.error("Error sending chat broadcast to client", e);
		} catch (InvalidProtocolBufferException e) {
			logger.error("Error parsing chat broadcast protobuf", e);
		}
	}

}
