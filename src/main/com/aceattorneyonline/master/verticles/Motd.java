package com.aceattorneyonline.master.verticles;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.ChatCommandSyntax;
import com.aceattorneyonline.master.Client;
import com.aceattorneyonline.master.events.AdminEventProtos.ReloadMotd;
import com.aceattorneyonline.master.events.AdminEventProtos.SetMotd;
import com.aceattorneyonline.master.events.Events;
import com.aceattorneyonline.master.events.UuidProto.Uuid;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class Motd extends ClientListVerticle {

	private static final Logger logger = LoggerFactory.getLogger(Motd.class);

	public Motd(Map<UUID, Client> clientList) {
		super(clientList);
	}

	@Override
	public void start() {
		logger.info("MOTD verticle starting");

		EventBus eventBus = getVertx().eventBus();
		eventBus.consumer(Events.SET_MOTD.toString(), this::handleSetMotd);
		eventBus.consumer(Events.RELOAD_MOTD.toString(), this::handleReloadMotd);
	}

	@Override
	public void stop() {
		logger.info("MOTD verticle stopping");
	}

	public void handleSetMotd(Message<String> event) {
		event.fail(0, "not implemented"); // TODO handleSetMotd
	}
	
	@ChatCommandSyntax(name = "setmotd", description = "Sets the MOTD permanently.", arguments = "<message>")
	public static com.google.protobuf.Message parseSetMotd(Uuid invoker, List<String> args) throws IllegalArgumentException {
		if (args.isEmpty()) {
			throw new IllegalArgumentException();
		}
		String message = String.join(" ", args);
		return SetMotd.newBuilder().setId(invoker).setMessage(message).build();
	}

	public void handleReloadMotd(Message<String> event) {
		event.fail(0, "not implemented"); // TODO handleReloadMotd
	}
	
	@ChatCommandSyntax(name = "reloadmotd", description = "Reloads the MOTD to file.", arguments = "")
	public static com.google.protobuf.Message parseReloadMotd(Uuid invoker, List<String> args) throws IllegalArgumentException {
		return ReloadMotd.newBuilder().setId(invoker).build();
	}

}
