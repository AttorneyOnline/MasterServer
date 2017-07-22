package com.aceattorneyonline.master.protocol;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.Client;
import com.aceattorneyonline.master.MasterServer;
import com.aceattorneyonline.master.ProtocolHandler;
import com.aceattorneyonline.master.events.Events;
import com.aceattorneyonline.master.events.PlayerEventProtos.GetServerList;
import com.aceattorneyonline.master.events.UuidProto.Uuid;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;

public class AO2ClientProtocolHandler extends AO1ClientProtocolHandler {

	private static final Logger logger = LoggerFactory.getLogger(AO2ClientProtocolHandler.class);

	public AO2ClientProtocolHandler() {
		super();
	}

	public AO2ClientProtocolHandler(Client context) {
		super(context);
	}

	@Override
	public void handle(Buffer event) {
		String packet = event.toString("UTF-8").trim();
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
			eventBus.send(Events.GET_SERVER_LIST.getEventName(), GetServerList.newBuilder().setId(id).build().toByteArray(),
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
		default:
			logger.warn("Unknown message from {}: {}", context(), packet);
			break;
		}
	}

	@Override
	public CompatibilityResult isCompatible(Buffer event) {
		if (event.toString().equals("ALL#%")) {
			return CompatibilityResult.COMPATIBLE;
		}
		return CompatibilityResult.FAIL;
	}

	@Override
	public ProtocolHandler registerClient(Client client) {
		client.setProtocolWriter(new AO2ProtocolWriter(client.context()));
		return new AO2ClientProtocolHandler(client);
	}

}
