package com.aceattorneyonline.master.protocol;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.Client;
import com.aceattorneyonline.master.ContextualProtocolHandler;
import com.aceattorneyonline.master.MasterServer;
import com.aceattorneyonline.master.Player;
import com.aceattorneyonline.master.ProtocolHandler;
import com.aceattorneyonline.master.ProtocolWriter;
import com.aceattorneyonline.master.events.EventErrorReason;
import com.aceattorneyonline.master.events.Events;
import com.aceattorneyonline.master.events.PlayerEventProtos.GetServerListPaged;
import com.aceattorneyonline.master.events.PlayerEventProtos.NewPlayer;
import com.aceattorneyonline.master.events.PlayerEventProtos.SendChat;
import com.aceattorneyonline.master.events.SharedEventProtos.GetVersion;
import com.aceattorneyonline.master.events.UuidProto.Uuid;
import com.aceattorneyonline.master.verticles.ClientServerList;

import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.net.NetSocket;

public class AO1ClientProtocolHandler extends ContextualProtocolHandler {

	private static final Logger logger = LoggerFactory.getLogger(AO1ClientProtocolHandler.class);

	public AO1ClientProtocolHandler() {
		super();
	}

	public AO1ClientProtocolHandler(Client context) {
		super(context);
		// XXX: this might lead to a race condition, so supervise carefully!
		logger.debug("{}: Sending new player event", context());
		MasterServer.vertx.eventBus().<String>send(
				Events.NEW_PLAYER.getEventName(), NewPlayer.newBuilder()
						.setId(Uuid.newBuilder().setId(context.id().toString()).build()).build().toByteArray(),
				reply -> {
					if (reply.succeeded()) {
						ProtocolWriter writer = context.protocolWriter();
						String resultBody = reply.result().body();
						writer.sendSystemMessage(resultBody);
						logger.debug("{}: New player success", context());
					} else {
						logger.error("{}: New player failed: {}", context(), reply.cause());
					}
				});
	}

	@Override
	public void handle(Buffer event) {
		String packet = event.toString("Windows-1251").trim();
		try {
			packet = packet.substring(0, packet.indexOf('%'));
		} catch (StringIndexOutOfBoundsException e) {
			logger.warn("{}: Packet without % delimiter! {}", context(), packet);
		}
		List<String> tokens = Arrays.asList(packet.split("#"));
		EventBus eventBus = MasterServer.vertx.eventBus();
		Uuid id = Uuid.newBuilder().setId(context().id().toString()).build();

		switch (tokens.get(0)) {
		case "askforservers":
			// All servers (paged): askforservers#% (returns first server on the list)
			// But we can hack it and return all servers, and then not accept any SR
			// requests.
			eventBus.send(Events.GET_SERVER_LIST_PAGED.getEventName(),
					GetServerListPaged.newBuilder().setId(id).setPage(askForServersAllAtOnce() ? -1 : 0).build().toByteArray(), this::handleEventReply);
			break;
		case "SR":
			// Page of servers: SR#[id]#%
			// Starts from 0, but returns second server!
			// But when there are no more pages, there is no response.
			eventBus.send(
					Events.GET_SERVER_LIST_PAGED.getEventName(), GetServerListPaged.newBuilder().setId(id)
							.setPage(Integer.parseInt(tokens.get(1)) + 1).build().toByteArray(),
					this::handleEventReply);
			break;
		case "CT":
			// Chat: CT#[username]#[message]#%
			if (MasterServer.RESERVED_NICKS.contains(tokens.get(1).trim())) {
				context().protocolWriter().sendSystemMessage(
						"You cannot use this as your nickname, as it is reserved for the master server.");
			} else {
				eventBus.send(Events.SEND_CHAT.getEventName(), SendChat.newBuilder().setId(id)
						.setUsername(AOUtils.unescape(tokens.get(1))).setMessage(AOUtils.unescape(tokens.get(2))).build().toByteArray(),
						this::handleEventReply);
			}
			break;
		case "VC":
			// Version check: VC#%
			eventBus.send(Events.GET_VERSION.getEventName(), GetVersion.newBuilder().setId(id).build().toByteArray(),
					this::handleEventReply);
			break;
		default:
			logger.warn("{} Received unknown message: {}", context(), packet);
			break;
		}
	}

	protected void handleEventReply(AsyncResult<Message<String>> reply) {
		if (reply.succeeded() && reply.result().body() != null) {
			context().protocolWriter().sendSystemMessage(reply.result().body());
		} else if (reply.failed()) {
			ReplyException e = (ReplyException) reply.cause();
			int errorCode = e.failureCode();
			String message = e.getMessage();
			switch (errorCode) {
			default: // For unhandled exceptions
			case EventErrorReason.INTERNAL_ERROR:
				context().protocolWriter().sendSystemMessage("Internal error: " + message);
				logger.error("{}: Internal error: {}", context(), message);
				break;
			case EventErrorReason.SECURITY_ERROR:
				context().protocolWriter()
						.sendSystemMessage("Security error: " + message + "\nThis incident has been logged.");
				logger.warn("{}: Security error: {}", context(), message);
				break;
			case EventErrorReason.USER_ERROR:
				context().protocolWriter().sendSystemMessage("User error: " + message);
				logger.info("{}: User error: {}", context(), message);
				break;
			}
		}
	}

	@Override
	public CompatibilityResult isCompatible(NetSocket socket, Buffer event) {
		if (event.length() == 0) {
			// AO1 protocol will always wait on servercheok so we'll send that out.
			socket.write(Buffer.buffer("servercheok#1.7.5#%"));
			return CompatibilityResult.WAIT;
		} else if (event.toString().equals("askforservers#%")) {
			return CompatibilityResult.COMPATIBLE;
		}
		return CompatibilityResult.FAIL;
	}

	@Override
	public ProtocolHandler registerClient(NetSocket socket) {
		Player player = new Player(socket);
		ClientServerList.getSingleton().addPlayer(player.id(), player);
		player.setProtocolWriter(new AOProtocolWriter(player.socket()));
		player.setVersion("1.7.5");
		return new AO1ClientProtocolHandler(player);
	}

	/**
	 * Returns whether or not this protocol supports receiving servers all at once
	 * on an askforservers packet.
	 */
	protected boolean askForServersAllAtOnce() {
		return false;
	}
}
