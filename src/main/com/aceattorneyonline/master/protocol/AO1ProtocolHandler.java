package com.aceattorneyonline.master.protocol;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.Client;
import com.aceattorneyonline.master.ContextualProtocolHandler;
import com.aceattorneyonline.master.MasterServer;
import com.aceattorneyonline.master.ProtocolHandler;
import com.aceattorneyonline.master.UnconnectedClient;
import com.aceattorneyonline.master.events.EventErrorReason;
import com.aceattorneyonline.master.events.Events;
import com.aceattorneyonline.master.events.AdvertiserEventProtos.Heartbeat;
import com.aceattorneyonline.master.events.PlayerEventProtos.GetServerList;
import com.aceattorneyonline.master.events.PlayerEventProtos.GetServerListPaged;
import com.aceattorneyonline.master.events.PlayerEventProtos.NewPlayer;
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
		Uuid id = Uuid.newBuilder().setId(context().id().toString()).build();

		switch (tokens.get(0)) {
		case "ID":
			// Client version: ID#[client software]#[version]#%
			// This is an AO2 thing
			break;
		case "HI":
			// Hard drive ID (can be anything in practice): HI#[hdid]#%
			// We don't care about your hard drive ID.
			// This is an AO2 thing
			break;
		case "ALL":
			// All servers: ALL#%
			// This is an AO2 thing
			eventBus.send(Events.GET_SERVER_LIST.getEventName(), GetServerList.newBuilder().setId(id).build(),
					this::handleEventReply);
			break;
		case "askforservers":
			// All servers (paged): askforservers#% (returns first server on the list)
			// But we can hack it and return all servers, and then not accept any SR
			// requests.
			if (context() instanceof UnconnectedClient && ((UnconnectedClient) context()).getSuccessor() == null) {
				eventBus.send(Events.NEW_PLAYER.getEventName(), NewPlayer.newBuilder().setId(id).build(),
						this::handleEventReply);
			}
			eventBus.send(Events.GET_SERVER_LIST_PAGED.getEventName(),
					GetServerListPaged.newBuilder().setId(id).setPage(0).build(), this::handleEventReply);
			break;
		case "SR":
			// Page of servers: SR#[id]#%
			// Starts from 0, but returns second server!
			// But when there are no more pages, there is no response.
			eventBus.send(Events.GET_SERVER_LIST_PAGED.getEventName(),
					GetServerListPaged.newBuilder().setId(id).setPage(Integer.parseInt(tokens.get(1) + 1)).build(),
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
			// This is a server thing
			if (tokens.size() == 6) {
				eventBus.send(Events.ADVERTISER_HEARTBEAT.getEventName(),
						Heartbeat.newBuilder().setId(id).setPort(Integer.parseInt(tokens.get(1))).setName(tokens.get(2))
								.setDescription(tokens.get(3)).setVersion(tokens.get(4)).build(),
						reply -> {
							if (reply.succeeded()) {
								context().protocolWriter().sendNewHeartbeatSuccess();
							} else {
								context().context().close();
							}
						});
			}
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
		if (event.length() == 0) {
			// AO1 protocol will always wait on servercheok so we'll send that out.
			context().context().write(Buffer.buffer("servercheok#1.7.5#%"));
			return true;
		}
		return false;
	}

	@Override
	public ProtocolHandler registerClient(Client client) {
		client.setProtocolWriter(new AOProtocolWriter(client.context()));
		return new AO1ProtocolHandler(client);
	}

}
