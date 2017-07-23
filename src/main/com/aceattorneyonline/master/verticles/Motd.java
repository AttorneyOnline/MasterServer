package com.aceattorneyonline.master.verticles;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.ChatCommandSyntax;
import com.aceattorneyonline.master.MasterServer;
import com.aceattorneyonline.master.events.AdminEventProtos.ReloadMotd;
import com.aceattorneyonline.master.events.AdminEventProtos.SetMotd;
import com.aceattorneyonline.master.events.EventErrorReason;
import com.aceattorneyonline.master.events.Events;
import com.aceattorneyonline.master.events.SharedEventProtos.GetMotd;
import com.aceattorneyonline.master.events.UuidProto.Uuid;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class Motd extends ClientListVerticle {

	private static final Logger logger = LoggerFactory.getLogger(Motd.class);
	
	private String motd = "Welcome to Attorney Online! Master Server version: " + MasterServer.VERSION;

	@Override
	public void start() {
		logger.info("MOTD verticle starting");

		EventBus eventBus = getVertx().eventBus();
		eventBus.consumer(Events.GET_MOTD.getEventName(), this::handleGetMotd);
		eventBus.consumer(Events.SET_MOTD.getEventName(), this::handleSetMotd);
		eventBus.consumer(Events.RELOAD_MOTD.getEventName(), this::handleReloadMotd);
	}

	@Override
	public void stop() {
		logger.info("MOTD verticle stopping");
	}

	public void handleSetMotd(Message<byte[]> event) {
		event.fail(EventErrorReason.INTERNAL_ERROR, "not implemented"); // FIXME handleSetMotd
	}
	
	@ChatCommandSyntax(name = "setmotd", description = "Sets the MOTD permanently.", arguments = "<message>")
	public static com.google.protobuf.Message parseSetMotd(Uuid invoker, List<String> args) throws IllegalArgumentException {
		if (args.isEmpty()) {
			throw new IllegalArgumentException();
		}
		String message = String.join(" ", args);
		return SetMotd.newBuilder().setId(invoker).setMessage(message).build();
	}

	public void handleReloadMotd(Message<byte[]> event) {
		event.fail(EventErrorReason.INTERNAL_ERROR, "not implemented"); // FIXME handleReloadMotd
	}
	
	@ChatCommandSyntax(name = "reloadmotd", description = "Reloads the MOTD to file.", arguments = "")
	public static com.google.protobuf.Message parseReloadMotd(Uuid invoker, List<String> args) throws IllegalArgumentException {
		return ReloadMotd.newBuilder().setId(invoker).build();
	}
	
	public void handleGetMotd(Message<byte[]> event) {
		event.reply(motd);
	}
	
	@ChatCommandSyntax(name = "motd", description = "Gets the message of the day.", arguments = "")
	public static com.google.protobuf.Message parseGetMotd(Uuid invoker, List<String> args) throws IllegalArgumentException {
		return GetMotd.newBuilder().build();
	}
	

}
