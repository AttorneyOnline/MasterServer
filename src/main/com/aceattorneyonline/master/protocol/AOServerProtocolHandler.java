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
import com.aceattorneyonline.master.events.AdvertiserEventProtos.Heartbeat;
import com.aceattorneyonline.master.events.AdvertiserEventProtos.Ping;
import com.aceattorneyonline.master.events.PlayerEventProtos.NewPlayer;
import com.aceattorneyonline.master.events.UuidProto.Uuid;

import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;

public class AOServerProtocolHandler extends ContextualProtocolHandler {

	private static final Logger logger = LoggerFactory.getLogger(AOServerProtocolHandler.class);

	public AOServerProtocolHandler() {
		super();
	}

	public AOServerProtocolHandler(Client context) {
		super(context);
		// XXX: this might lead to a race condition, so supervise carefully!
		MasterServer.vertx.eventBus()
				.send(Events.NEW_ADVERTISER.getEventName(), NewPlayer.newBuilder()
						.setId(Uuid.newBuilder().setId(context.id().toString()).build()).build().toByteArray(),
						this::handleEventReply);
	}

	@Override
	public void handle(Buffer event) {
		String packet = event.toString("UTF-8").trim(); // XXX: encoding should change based on AO1/2!
		packet = packet.substring(0, packet.indexOf('%'));
		List<String> tokens = Arrays.asList(packet.split("#"));
		EventBus eventBus = MasterServer.vertx.eventBus();
		Uuid id = Uuid.newBuilder().setId(context().id().toString()).build();

		switch (tokens.get(0)) {
		case "SCC":
			// Server heartbeat: SCC#[port]#[name]#[description]#[server software]#%
			// This is a server thing
			if (tokens.size() == 6) {
				eventBus.send(Events.ADVERTISER_HEARTBEAT.getEventName(),
						Heartbeat.newBuilder().setId(id).setPort(Integer.parseInt(tokens.get(1))).setName(tokens.get(2))
								.setDescription(tokens.get(3)).setVersion(tokens.get(4)).build().toByteArray(),
						reply -> {
							if (reply.succeeded()) {
								context().protocolWriter().sendNewHeartbeatSuccess();
							} else {
								logger.warn("Advertiser dropped due to heartbeat event failure");
								context().context().close();
							}
						});
			}
			break;
		case "PING":
			// Server ping: PING#% (to check if server still exists in master list)
			eventBus.send(Events.ADVERTISER_PING.getEventName(), Ping.newBuilder().setId(id).build().toByteArray(),
					this::handleEventReply);
			break;
		}
	}

	protected void handleEventReply(AsyncResult<Message<String>> reply) {
		if (reply.failed()) {
			ReplyException e = (ReplyException) reply.cause();
			int errorCode = e.failureCode();
			String message = e.getMessage();
			switch (errorCode) {
			default: // For unhandled exceptions
			case EventErrorReason.INTERNAL_ERROR:
				logger.error("Internal error from: {}", message);
				break;
			case EventErrorReason.SECURITY_ERROR:
				logger.warn("Security error from {}: {}", message);
				break;
			case EventErrorReason.USER_ERROR:
				logger.info("User error from {}: {}", message);
				break;
			}
			context().context().close();
		}
	}

	@Override
	public CompatibilityResult isCompatible(Buffer event) {
		if (event.length() == 0) {
			// AO1 protocol will always wait on servercheok so we'll send that out.
			context().context().write(Buffer.buffer("servercheok#1.7.5#%"));
			return CompatibilityResult.WAIT;
		} else if (event.toString().startsWith("SCC")) {
			return CompatibilityResult.COMPATIBLE;
		}
		return CompatibilityResult.FAIL;
	}

	@Override
	public ProtocolHandler registerClient(Client client) {
		client.setProtocolWriter(new AOProtocolWriter(client.context()));
		return new AOServerProtocolHandler(client);
	}

}
