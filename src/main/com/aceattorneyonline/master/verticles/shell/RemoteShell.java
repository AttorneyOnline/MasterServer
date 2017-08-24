package com.aceattorneyonline.master.verticles.shell;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.shell.ShellService;
import io.vertx.ext.shell.ShellServiceOptions;
import io.vertx.ext.shell.command.CommandProcess;
import io.vertx.ext.shell.term.TelnetTermOptions;

public class RemoteShell extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(RemoteShell.class);
	
	public static final int PORT = 25566;
	private static final String WELCOME_FILE_NAME = "shellserver_welcome.txt";

	private static final UUID SHELL_USER_UUID = UUID.nameUUIDFromBytes("7d9a2ae2-86e9-4162-a09e-c878e937f084".getBytes());

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

	    service.server().registerCommandResolver(new ShellCommandPack(getVertx()));
	}

	@Override
	public void stop() {
		logger.info("Remote shell verticle stopping");
		ShellPlayer.destroySingleton();
	}
	
	/** Check if the terminal size is too small and report an error if there is. */
	static boolean checkTermSizeError(CommandProcess process) {
		if (process.height() < 4 || process.width() < 20) {
			process.write("Sorry, but your terminal is too small to run this program.\n");
			process.end(1);
			return true;
		}
		return false;
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
