package com.aceattorneyonline.master.verticles;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.ChatCommandSyntax;
import com.aceattorneyonline.master.Client;
import com.aceattorneyonline.master.MasterServer;
import com.aceattorneyonline.master.events.Events;
import com.aceattorneyonline.master.events.SharedEventProtos.GetVersion;
import com.aceattorneyonline.master.events.UuidProto.Uuid;
import com.google.protobuf.InvalidProtocolBufferException;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class Version extends ClientListVerticle {

	private static final Logger logger = LoggerFactory.getLogger(Version.class);

	public Version(Map<UUID, Client> clientList) {
		super(clientList);
	}

	@Override
	public void start() {
		logger.info("Version verticle starting");
		EventBus eventBus = getVertx().eventBus();
		eventBus.consumer(Events.GET_VERSION.toString(), this::handleGetVersion);
	}

	@Override
	public void stop() {
		logger.info("Version verticle stopping");
	}

	public void handleGetVersion(Message<String> event) {
		try {
			GetVersion gv = GetVersion.parseFrom(event.body().getBytes());
			UUID id = UUID.fromString(gv.getId().getId());
			Client client = getClientById(id);
			client.protocolWriter().sendVersion(MasterServer.VERSION);
		} catch (InvalidProtocolBufferException e) {
			event.fail(1, "Could not parse GetVersion protobuf");
		}
	}

	@ChatCommandSyntax(name = "version", description = "Gets the master server's version.", arguments = "")
	public static com.google.protobuf.Message parseGetVersion(Uuid invoker, List<String> args)
			throws IllegalArgumentException {
		return GetVersion.newBuilder().setId(invoker).build();
	}

}
