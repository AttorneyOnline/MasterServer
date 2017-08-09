package com.aceattorneyonline.master.verticles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.ChatCommandSyntax;
import com.aceattorneyonline.master.MasterServer;
import com.aceattorneyonline.master.Player;
import com.aceattorneyonline.master.events.AdminEventProtos.ReloadMotd;
import com.aceattorneyonline.master.events.AdminEventProtos.SetMotd;
import com.aceattorneyonline.master.events.EventErrorReason;
import com.aceattorneyonline.master.events.Events;
import com.aceattorneyonline.master.events.SharedEventProtos.GetMotd;
import com.aceattorneyonline.master.events.UuidProto.Uuid;
import com.google.protobuf.InvalidProtocolBufferException;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class Motd extends ClientListVerticle {

	private static final Logger logger = LoggerFactory.getLogger(Motd.class);

	private static final String MOTD_FILE_NAME = "motd.txt";

	private String motd = "Welcome to Attorney Online! Master Server version: " + MasterServer.VERSION;

	@Override
	public void start() {
		logger.info("MOTD verticle starting");

		EventBus eventBus = getVertx().eventBus();
		eventBus.consumer(Events.GET_MOTD.getEventName(), this::handleGetMotd);
		eventBus.consumer(Events.SET_MOTD.getEventName(), this::handleSetMotd);
		eventBus.consumer(Events.RELOAD_MOTD.getEventName(), this::handleReloadMotd);

		try {
			motd = getMotdFile(MOTD_FILE_NAME);
			logger.debug("Loaded MOTD from {}", MOTD_FILE_NAME);
		} catch (IOException e) {
			logger.error("Failed to load MOTD!", e);
		}
	}

	@Override
	public void stop() {
		logger.info("MOTD verticle stopping");
	}

	public void handleSetMotd(Message<byte[]> event) {
		try {
			SetMotd setMotd = SetMotd.parseFrom(event.body());
			UUID id = UUID.fromString(setMotd.getId().getId());
			String newMessage = setMotd.getMessage();
			Player requestingPlayer = getPlayerById(id);

			if (requestingPlayer == null) {
				event.fail(EventErrorReason.SECURITY_ERROR, "Requester is not a player.");
				return;
			} else if (!requestingPlayer.hasAdmin()) {
				event.fail(EventErrorReason.SECURITY_ERROR, "Requester does not have admin rights.");
				return;
			}

			motd = newMessage;
			event.reply("Success.");
		} catch (InvalidProtocolBufferException e) {
			event.fail(EventErrorReason.INTERNAL_ERROR, "Could not parse SetMotd protobuf");
		}
	}

	@ChatCommandSyntax(name = "setmotd", description = "Sets the MOTD permanently.", arguments = "<message>")
	public static com.google.protobuf.Message parseSetMotd(Uuid invoker, List<String> args)
			throws IllegalArgumentException {
		if (args.isEmpty()) {
			throw new IllegalArgumentException();
		}
		String message = String.join(" ", args);
		return SetMotd.newBuilder().setId(invoker).setMessage(message).build();
	}

	public void handleReloadMotd(Message<byte[]> event) {
		try {
			ReloadMotd reload = ReloadMotd.parseFrom(event.body());
			UUID id = UUID.fromString(reload.getId().getId());
			Player requestingPlayer = getPlayerById(id);

			if (requestingPlayer == null) {
				event.fail(EventErrorReason.SECURITY_ERROR, "Requester is not a player.");
				return;
			} else if (!requestingPlayer.hasAdmin()) {
				event.fail(EventErrorReason.SECURITY_ERROR, "Requester does not have admin rights.");
				return;
			}

			motd = getMotdFile(MOTD_FILE_NAME);
			event.reply("Success.");
		} catch (InvalidProtocolBufferException e) {
			event.fail(EventErrorReason.INTERNAL_ERROR, "Could not parse ReloadMotd protobuf");
		} catch (IOException e) {
			event.fail(EventErrorReason.INTERNAL_ERROR, e.getMessage());
		}
	}

	@ChatCommandSyntax(name = "reloadmotd", description = "Reloads the MOTD to file.", arguments = "")
	public static com.google.protobuf.Message parseReloadMotd(Uuid invoker, List<String> args)
			throws IllegalArgumentException {
		return ReloadMotd.newBuilder().setId(invoker).build();
	}

	public void handleGetMotd(Message<byte[]> event) {
		event.reply(Preprocessor.preprocess(motd));
	}

	@ChatCommandSyntax(name = "motd", description = "Gets the message of the day.", arguments = "")
	public static com.google.protobuf.Message parseGetMotd(Uuid invoker, List<String> args)
			throws IllegalArgumentException {
		return GetMotd.newBuilder().build();
	}

	private String getMotdFile(String filename) throws IOException {
		try {
			logger.debug("Reading welcome file {}", filename);
			return new String(Files.readAllBytes(new File(filename).toPath()), "UTF-8");
		} catch (IOException e) {
			IOException detailedE = new IOException("Could not load MOTD file " + filename + "!", e);
			logger.error(detailedE.getMessage(), detailedE);
			throw detailedE;
		}
	}

	private static class Preprocessor {
		private static final Map<String, Macro> macros = new HashMap<>();
		static {
			macros.put("version", () -> MasterServer.VERSION);
			macros.put("uptime", () -> {
				Duration uptime = Duration.between(MasterServer.START_TIME, Instant.now());
				long days = uptime.toDays(), hours = uptime.toHours() % 24, minutes = uptime.toMinutes() % 60,
						totalSeconds = uptime.getSeconds();
				Formatter formatter = new Formatter();
				if (totalSeconds < 60)
					// Don't print seconds except if the master server has only been up for a very
					// short amount of time.
					formatter.format("%d seconds", totalSeconds);
				else {
					if (days > 0)
						formatter.format("%d day%s ", days, days > 1 ? "s" : "");
					if (hours > 0)
						formatter.format("%d hour%s ", hours, hours > 1 ? "s" : "");
					if (minutes > 0)
						formatter.format("%d minute%s", minutes, minutes > 1 ? "s" : "");
				}
				String out = formatter.toString();
				formatter.close();
				return out;
			});
			macros.put("players-online", () -> String.valueOf(ClientListVerticle.getSingleton().getPlayersList()
					.stream().filter(p -> !p.name().isEmpty() && !p.isSystem()).count()));
		}

		/**
		 * Preprocesses a MOTD. Supported macros:
		 * <ul>
		 * <li><code>${version}</code></li>
		 * <li><code>${uptime}</code></li>
		 * <li><code>${players-online}</code> - the number of players with names that
		 * are currently online, excluding system clients</li>
		 * </ul>
		 */
		public static String preprocess(String motd) {
			for (Entry<String, Macro> entry : macros.entrySet()) {
				motd = motd.replaceAll("\\$\\{" + entry.getKey() + "\\}", entry.getValue().expand());
			}
			return motd;
		}

		interface Macro {
			String expand();
		}
	}

}
