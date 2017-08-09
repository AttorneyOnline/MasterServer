package com.aceattorneyonline.master.verticles;

import static org.fusesource.jansi.Ansi.ansi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.AdvertisedServer;
import com.aceattorneyonline.master.Advertiser;
import com.aceattorneyonline.master.MasterServer;
import com.aceattorneyonline.master.Player;
import com.aceattorneyonline.master.events.EventErrorReason;
import com.aceattorneyonline.master.events.Events;
import com.aceattorneyonline.master.events.PlayerEventProtos.SendChat;
import com.aceattorneyonline.master.events.UuidProto.Uuid;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.cli.Argument;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.Option;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.SocketAddressImpl;
import io.vertx.ext.shell.ShellService;
import io.vertx.ext.shell.ShellServiceOptions;
import io.vertx.ext.shell.command.CommandBuilder;
import io.vertx.ext.shell.command.CommandProcess;
import io.vertx.ext.shell.command.CommandRegistry;
import io.vertx.ext.shell.term.TelnetTermOptions;

public class RemoteShell extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(RemoteShell.class);
	
	private static final int PORT = 25566;
	private static final String WELCOME_FILE_NAME = "shellserver_welcome.txt";

	private static final UUID SHELL_USER_UUID = UUID.nameUUIDFromBytes("7d9a2ae2-86e9-4162-a09e-c878e937f084".getBytes());
	private Player mockPlayer;

	@Override
	public void start() {
		logger.info("Remote shell verticle starting");
	    ShellService service = ShellService.create(vertx,
	            new ShellServiceOptions()
	            .setTelnetOptions(
	                new TelnetTermOptions()
	                    .setHost("localhost")
	                    .setPort(PORT)
	            )
	            .setWelcomeMessage(getWelcomeFile(WELCOME_FILE_NAME))
	        );
	    service.start();

		// HACK: create a fake, NetSocket-less client for all remote shell actions,
	    // because default telnet server doesn't let us handle anything extra on connect/disconnect
	    mockPlayer = new ShellPlayer();
	    ClientListVerticle.getSingleton().addPlayer(mockPlayer.id(), mockPlayer);

	    createTop();
	    createSay();
	}

	private class ShellPlayer extends Player {

		public ShellPlayer() {
			super(null); // This code smells bad. Really bad.
		}

		@Override
		public boolean hasAdmin() {
			return true;
		}

		@Override
		public SocketAddress address() {
			return new SocketAddressImpl(PORT, "localhost");
		}

		@Override
		public String name() {
			return "System";
		}

		@Override
		public boolean isSystem() {
			return true;
		}
	}

	// TODO drop client on verticle stop

	private void createTop() {
		CLI cli = CLI.create("top")
				.addOption(new Option()
						.setArgName("p")
						.setLongName("show-players")
						.setFlag(true)
						.setSingleValued(true)
						)
				.addOption(new Option()
						.setArgName("s")
						.setLongName("show-servers")
						.setFlag(true)
						.setSingleValued(true)
						)
				.addOption(new Option()
						.setArgName("a")
						.setLongName("show-all")
						.setFlag(true)
						.setSingleValued(true)
						.setDefaultValue("true")
						);
		CommandBuilder builder = CommandBuilder.command(cli)
				.processHandler(this::top);
		CommandRegistry.getShared(getVertx()).registerCommand(builder.build(getVertx()));
	}

	private void top(CommandProcess process) {
		if (checkTermSizeError(process)) return;

		ClientListVerticle clv = ClientListVerticle.getSingleton();
		long id = process.vertx().setPeriodic(1000, handler -> {
			if (checkTermSizeError(process)) return;
			StringBuilder builder = new StringBuilder();
			Ansi ansi = ansi(builder);
			ansi.a(Ansi.Attribute.CONCEAL_ON).cursor(0, 0);

			// Print header
			Duration uptime = Duration.between(MasterServer.START_TIME, Instant.now());
			ansi.format("top - %tT - up %d days %02d:%02d, %2d clients, %2d advertised", Instant.now().toEpochMilli(),
					uptime.toDays(),
					uptime.toHours() % 24,
					uptime.toMinutes() % 60,
					clv.getClientsList().size(),
					clv.getAdvertisersList().size());

			// Print columns
			ansi.newline().a(Ansi.Attribute.NEGATIVE_ON)
					.format("%36s  %30s  %15s  %11s  %1s  %20s", "UUID", "Name", "IP", "Uptime", "A", "Protocol")
					.a(Ansi.Attribute.NEGATIVE_OFF).newline();
			

			boolean showAll = process.commandLine().isFlagEnabled("show-all");
			boolean showPlayers = process.commandLine().isFlagEnabled("show-players");
			boolean showServers = process.commandLine().isFlagEnabled("show-servers");
			int curRow = 3;
			if (showAll || showPlayers)
				for (Player player : clv.getPlayersList()) {
					if (curRow >= process.height() && process.height() > 0) break;
					ansi.format("%36s  %30s  %15s  %11s  %1s  %20s", player.id(), player.name(), player.address(),
							"", player.hasAdmin() ? "A" : "", player.protocolWriter().getClass().getSimpleName());
					ansi.eraseLine(Ansi.Erase.FORWARD).newline(); curRow++;
				}
			if (showAll || showServers)
				for (Advertiser advertiser: clv.getAdvertisersList()) {
					if (curRow >= process.height() && process.height() > 0) break;
					AdvertisedServer server = advertiser.server();
					if (server != null) {
						Duration advUptime = server.uptime();
						ansi.format("%36s  %30s  %15s  %02d:%02d:%02d:%02d  %1s  %20s", advertiser.id(), server.name(), server.address(),
								advUptime.toDays(), advUptime.toHours() % 24, advUptime.toMinutes() % 60, advUptime.getSeconds() % 60, "",
								advertiser.protocolWriter().getClass().getSimpleName());
						ansi.eraseLine(Ansi.Erase.FORWARD).newline(); curRow++;
					}
				}
			ansi.eraseScreen(Ansi.Erase.FORWARD);
			process.write(builder.toString());
		});

		process.interruptHandler(interrupt -> {
			process.vertx().cancelTimer(id);
			process.write(ansi().reset().toString());
			process.end();
		});
	}

	/** Check if the terminal size is too small and report an error if there is. */
	private boolean checkTermSizeError(CommandProcess process) {
		if (process.height() < 4 || process.width() < 20) {
			process.write("Sorry, but your terminal is too small to run this program.\n");
			process.end(1);
			return true;
		}
		return false;
	}

	private void createSay() {
		CLI cli = CLI.create("say")
				.addArgument(new Argument()
						.setArgName("message")
						.setMultiValued(true)
						.setDescription("The chat message to send")
						.setRequired(true)
						);
		CommandBuilder builder = CommandBuilder.command(cli)
				.processHandler(this::say);
		CommandRegistry.getShared(getVertx()).registerCommand(builder.build(getVertx()));
	}

	private void say(CommandProcess process) {
		String chatMessage = process.args().stream().collect(Collectors.joining(" "));
		getVertx().eventBus().<String>send(Events.SEND_CHAT.getEventName(),
				SendChat.newBuilder().setId(Uuid.newBuilder().setId(mockPlayer.id().toString()).build())
				.setUsername("System").setMessage(chatMessage).build().toByteArray(),
				reply -> { // Similar reply handler to those in the protocol handlers
					if (reply.succeeded() && reply.result().body() != null) {
						process.write(reply.result().body());
					} else if (reply.failed()) {
						ReplyException e = (ReplyException) reply.cause();
						int errorCode = e.failureCode();
						String message = e.getMessage();
						switch (errorCode) {
						default: // For unhandled exceptions
						case EventErrorReason.INTERNAL_ERROR:
							process.write("Internal error: " + message);
							logger.error("Shell: Internal error: {}", message);
							break;
						case EventErrorReason.SECURITY_ERROR:
							process.write("Security error: " + message);
							logger.warn("Shell: Security error: {}", message);
							break;
						case EventErrorReason.USER_ERROR:
							process.write("User error: " + message);
							logger.info("Shell: User error: {}", message);
							break;
						}
					}
					process.write("\n");
					process.end(reply.succeeded() ? 0 : 1);
				});
	}

	@Override
	public void stop() {
		logger.info("Remote shell verticle stopping");
	}

	private String getWelcomeFile(String filename) {
		try {
			logger.debug("Reading welcome file {}", filename);
			return new String(Files.readAllBytes(new File(filename).toPath()), "UTF-8");
		} catch (IOException e) {
			logger.warn("Welcome file {} was not found. Using the default one.", filename);
			return "Welcome to the Attorney Online master server shell!\n";
		}
	}

}
