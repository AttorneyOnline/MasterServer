package com.aceattorneyonline.master.protocol;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.Advertiser;
import com.aceattorneyonline.master.Client;
import com.aceattorneyonline.master.ContextualProtocolHandler;
import com.aceattorneyonline.master.MasterServer;
import com.aceattorneyonline.master.ProtocolHandler;
import com.aceattorneyonline.master.events.AdvertiserEventProtos.Heartbeat;
import com.aceattorneyonline.master.events.AdvertiserEventProtos.Pin;
import com.aceattorneyonline.master.events.AdvertiserEventProtos.Ping;
import com.aceattorneyonline.master.events.EventErrorReason;
import com.aceattorneyonline.master.events.Events;
import com.aceattorneyonline.master.events.PlayerEventProtos.NewPlayer;
import com.aceattorneyonline.master.events.UuidProto.Uuid;
import com.aceattorneyonline.master.verticles.ClientServerList;

import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.net.NetSocket;

public class AOServerProtocolHandler implements ContextualProtocolHandler {

	private static final Logger logger = LoggerFactory.getLogger(AOServerProtocolHandler.class);
	protected Advertiser context;

	public AOServerProtocolHandler() {
		
	}

	public AOServerProtocolHandler(Advertiser context) {
		this.context = context;
		MasterServer.vertx.eventBus()
				.send(Events.NEW_ADVERTISER.getEventName(), NewPlayer.newBuilder()
						.setId(Uuid.newBuilder().setId(context.id().toString()).build()).build().toByteArray(),
						this::handleEventReply);
	}
	
	public Client context() {
		return context;
	}
	
	public void setContext(Advertiser context) {
		this.context = context;
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
			if (tokens.size() >= 4) {
				String version = tokens.size() >= 5 ? tokens.get(4) : "VANILLA";
				eventBus.send(Events.ADVERTISER_HEARTBEAT.getEventName(),
					Heartbeat.newBuilder()
						.setId(id)
						.setPort(Integer.parseInt(tokens.get(1)))
						.setName(unescape(tokens.get(2)))
						.setDescription(unescape(tokens.get(3)))
						.setVersion(version).build()
						.toByteArray(),
					reply -> {
						if (reply.succeeded()) {
							logger.debug("{}: Sent new heartbeat success", context());
							context().protocolWriter().sendNewHeartbeatSuccess();
						} else {
							logger.warn("{}: Heartbeat error: {}", context(), reply.cause());
							context().socket().close();
						}
					});
			}
			break;
		case "PING":
			// Server ping: PING#% (to check if server still exists in master list)
			eventBus.send(Events.ADVERTISER_PING.getEventName(), Ping.newBuilder().setId(id).build().toByteArray(),
					this::handleEventReply);
			break;
		case "PIN":
			// Pin: PIN#[secret]#% (pins the server as long as it is alive)
			eventBus.send(Events.PIN_SERVER.getEventName(), Pin.newBuilder().setId(id).setSecret(tokens.get(1))
					.build().toByteArray(), this::handleEventReply);
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
				logger.error("{}: Internal error: {}", context(), message);
				break;
			case EventErrorReason.SECURITY_ERROR:
				logger.warn("{}: Security error: {}", context(), message);
				break;
			case EventErrorReason.USER_ERROR:
				logger.info("{}: User error: {}", context(), message);
				break;
			}
			// Don't handle advertisers in bad states. Just let them reconnect by themselves.
			context().socket().close();
		}
	}

	protected String unescape(String str) {
		//@formatter:off
		return str.replaceAll("<percent>", "%")
				.replaceAll("<num>", "#")
				.replaceAll("\\$n", "\n")
				.replaceAll("<dollar>", "$")
				.replaceAll("<and>", "&")
				// Unescape doubly escaped symbols
				.replaceAll("<percent\\>", "<percent>")
				.replaceAll("<num\\>", "<num>")
				.replaceAll("<dollar\\>", "<dollar>")
				.replaceAll("<and\\>", "<and>");
		//@formatter:on
	}

	@Override
	public CompatibilityResult isCompatible(NetSocket socket, Buffer event) {
		if (event.length() == 0) {
			// AO1 protocol will always wait on servercheok so we'll send that out.
			// MEGA HACK: don't send buffer here because we know that
			// AO1ClientProtocolHandler will send it!
			// socket.write(Buffer.buffer("servercheok#1.7.5#%"));
			return CompatibilityResult.WAIT;
		} else if (event.toString().startsWith("SCC")) {
			return CompatibilityResult.COMPATIBLE;
		}
		return CompatibilityResult.FAIL;
	}

	@Override
	public ProtocolHandler registerClient(NetSocket socket) {
		Advertiser advertiser = new Advertiser(socket);
		ClientServerList masterList = ClientServerList.getSingleton();
		masterList.addAdvertiser(advertiser.id(), advertiser);
		advertiser.setProtocolWriter(new AOProtocolWriter(advertiser.socket()));
		return new AOServerProtocolHandler(advertiser);
	}

}
