package com.aceattorneyonline.master.protocol;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.Client;
import com.aceattorneyonline.master.ContextualProtocolHandler;
import com.aceattorneyonline.master.MasterServer;
import com.aceattorneyonline.master.ProtocolHandler;
import com.aceattorneyonline.master.events.EventErrorReason;
import com.aceattorneyonline.master.events.Events;
import com.aceattorneyonline.master.events.PlayerEventProtos.GetServerList;
import com.aceattorneyonline.master.events.UuidProto.Uuid;

import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;

public class AO1ProtocolHandler extends ContextualProtocolHandler {

	private static final Logger logger = LoggerFactory.getLogger(AO1ProtocolHandler.class);

	public AO1ProtocolHandler() {
		super();
	}

	public AO1ProtocolHandler(Client context) {
		super(context);
	}

	@Override
	public void handle(Buffer event) {
		String packet = event.toString("Windows-1251").trim();
		packet = packet.substring(0, packet.indexOf('%'));
		List<String> tokens = Arrays.asList(packet.split("#"));
		EventBus eventBus = MasterServer.vertx.eventBus();
		switch (tokens.get(0)) {
		case "ID":
			// Client version: ID#[client software]#[version]#%
			break;
		case "HI":
			// Hard drive ID (can be anything in practice): HI#[hdid]#%
			// We don't care about your hard drive ID.
			break;
		case "ALL":
			// All servers (paged): ALL#%
			eventBus.send(
					Events.GET_SERVER_LIST.getEventName(), GetServerList.newBuilder()
							.setId(Uuid.newBuilder().setId(context().id().toString()).build()).build(),
					this::handleEventReply);
			break;
		case "CT":
			// Chat: CT#[username]#[message]#%
			if (MasterServer.RESERVED_NICKS.contains(tokens.get(1).trim())) {
				context().protocolWriter().sendSystemMessage(
						"You cannot use this as your nickname, as it is reserved for the master server.");
			} else {
				eventBus.send(Events.SEND_CHAT.getEventName(), tokens.get(2), this::handleEventReply);
			}
			break;
		//// servers
		case "SCC":
			// Server heartbeat: SCC#[port]#[name]#[description]#[server software]#%
			break;
		}
	}

	private void handleEventReply(AsyncResult<Message<String>> reply) {
		if (reply.succeeded() && reply.result().body() != null) {
			context().protocolWriter().sendSystemMessage(reply.result().body());
		} else if (reply.failed()) {
			ReplyException e = (ReplyException) reply.cause();
			int errorCode = e.failureCode();
			String message = e.getMessage();
			switch (errorCode) {
			case EventErrorReason.INTERNAL_ERROR:
				context().protocolWriter().sendSystemMessage("Internal error: " + message);
				logger.error("Internal error: {}", message);
				break;
			case EventErrorReason.SECURITY_ERROR:
				context().protocolWriter()
						.sendSystemMessage("Security error: " + message + "\nThis incident has been logged.");
				logger.warn("Security error: {}", message);
				break;
			case EventErrorReason.USER_ERROR:
				context().protocolWriter().sendSystemMessage("User error: " + message);
				logger.info("User error: {}", message);
				break;
			}
		}
	}

	@Override
	public boolean isCompatible(Buffer event) {

		// TODO: read buffer and check if it's 1.7.5/retro
		logger.error("Not implemented!");
		return false;
	}

	@Override
	public ProtocolHandler registerClient(Client client) {
		client.setProtocolWriter(new AOProtocolWriter(client.context()));
		return new AO1ProtocolHandler(client);
	}

}
