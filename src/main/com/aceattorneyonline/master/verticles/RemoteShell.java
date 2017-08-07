package com.aceattorneyonline.master.verticles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.Player;
import com.aceattorneyonline.master.events.EventErrorReason;
import com.aceattorneyonline.master.events.Events;
import com.aceattorneyonline.master.events.PlayerEventProtos.SendChat;
import com.aceattorneyonline.master.events.UuidProto.Uuid;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.cli.Argument;
import io.vertx.core.cli.CLI;
import io.vertx.core.eventbus.ReplyException;
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
	            new ShellServiceOptions().setTelnetOptions(
	                new TelnetTermOptions()
	                    .setHost("localhost")
	                    .setPort(PORT)
	            )
	            .setWelcomeMessage(getWelcomeFile(WELCOME_FILE_NAME))
	        );
	    service.start();

	    mockPlayer = new Player(null); // This code smells bad. Really bad.
	    ClientListVerticle.getSingleton().addPlayer(mockPlayer.id(), mockPlayer);

	    //createInitClient();
	    createSay();
	}

	// HACK: functions to create and delete clients, because default telnet server doesn't let us handle
	// anything extra on connect/disconnect
	
	/*
	private void createInitClient() {
		CLI cli = CLI.create("init")
				.addArgument(new Argument()
						.setArgName("name")
						.setDescription("The name of the new client")
						.setRequired(true)
						);
		CommandBuilder builder = CommandBuilder.command(cli)
				.processHandler(this::initClient);
		builder.build(getVertx());
	}

	private void initClient(CommandProcess process) {
	}
	*/

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
		getVertx().eventBus().<String>send(Events.SEND_CHAT.getEventName(),
				SendChat.newBuilder().setId(Uuid.newBuilder().setId(mockPlayer.id().toString()).build())
				.setUsername("System").setMessage(process.args().get(0)).build().toByteArray(),
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
			return "Welcome to the Attorney Online master server shell!";
		}
	}

}
