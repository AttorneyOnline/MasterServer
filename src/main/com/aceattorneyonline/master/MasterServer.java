package com.aceattorneyonline.master;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.protocol.AO1ClientProtocolHandler;
import com.aceattorneyonline.master.protocol.AO2ClientProtocolHandler;
import com.aceattorneyonline.master.protocol.AOServerProtocolHandler;
import com.aceattorneyonline.master.protocol.WebClientProtocolHandler;
import com.aceattorneyonline.master.verticles.Analytics;
import com.aceattorneyonline.master.verticles.BanList;
import com.aceattorneyonline.master.verticles.Chat;
import com.aceattorneyonline.master.verticles.Checker;
import com.aceattorneyonline.master.verticles.ClientServerList;
import com.aceattorneyonline.master.verticles.ListPlayers;
import com.aceattorneyonline.master.verticles.Motd;
import com.aceattorneyonline.master.verticles.NewClientReceiver;
import com.aceattorneyonline.master.verticles.PrivateMessage;
import com.aceattorneyonline.master.verticles.Servers;
import com.aceattorneyonline.master.verticles.Version;
import com.aceattorneyonline.master.verticles.shell.RemoteShell;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetServerOptions;

public class MasterServer {
	private static final Logger logger = LoggerFactory.getLogger(MasterServer.class);

	public static final String SERVER_NICK = "AOMS";
	public static final List<String> RESERVED_NICKS = Arrays.asList("AOMS", "Master Server", "System", "RCON", "Server", "MS");

	private static final int HOST_PORT = 27016;

	public static final String VERSION;
	static {
		Package pkg = MasterServer.class.getPackage();
		if (pkg == null || pkg.getImplementationVersion() == null) {
			logger.warn("No package/manifest found to get a server version from! \n"
					+ "(usually this happens when the server is not being run from a jar file)");
			VERSION = "(debug)";
		} else {
			VERSION = pkg.getImplementationVersion();
		}
	}

	public static final Instant START_TIME = Instant.now();

	private DefaultProtocolHandler defaultHandler;

	public static final Vertx vertx = Vertx.vertx();

	public static void main(String[] args) throws IOException, InterruptedException {
		logUncaughtExceptions();
		new MasterServer().run();
	}

	public void run() {
		logger.info("Server created");
		setupProtocolHandlers();
		setupVerticles();
		createVertxServer();
		shutdownHook();
	}

	private void shutdownHook() {
		final Thread mainThread = Thread.currentThread();
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
		        try {
		        	for (Player player : ClientServerList.getSingleton().getPlayersList()) {
		        		player.protocolWriter().sendSystemMessage("Master server is restarting.");
		        	}
					mainThread.join();
				} catch (InterruptedException e) {
					logger.error("Interrupted while trying to join to main thread on shutdown!", e);
				}
		    }
		});
	}

	private void setupProtocolHandlers() {
		defaultHandler = new DefaultProtocolHandler();
		defaultHandler.register(new AO1ClientProtocolHandler());
		defaultHandler.register(new AO2ClientProtocolHandler());
		defaultHandler.register(new AOServerProtocolHandler());
		defaultHandler.register(new WebClientProtocolHandler());
	}

	private void setupVerticles() {
		DeploymentOptions worker = new DeploymentOptions().setWorker(true);
		vertx.deployVerticle(NewClientReceiver.class.getName(), worker);
		vertx.deployVerticle(Servers.class.getName(), worker);
		vertx.deployVerticle(BanList.class.getName(), worker);
		vertx.deployVerticle(Chat.class.getName(), worker);
		vertx.deployVerticle(Motd.class.getName(), worker);
		vertx.deployVerticle(Version.class.getName(), worker);
		vertx.deployVerticle(PrivateMessage.class.getName(), worker);
		vertx.deployVerticle(ListPlayers.class.getName(), worker);
		vertx.deployVerticle(RemoteShell.class.getName(), worker);
		vertx.deployVerticle(Checker.class.getName(), worker);
		vertx.deployVerticle(Analytics.class.getName(), worker);
	}

	private static void logUncaughtExceptions() {
	    try {
	        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
                logger.error("Uncaught exception in thread " + thread, e);
            });
	        vertx.exceptionHandler(e -> {
	        	logger.error("Uncaught exception in Vert.x thread!", e);
	        });
	    } catch (SecurityException e) {
	        logger.error("Unable to set the default uncaught exception handler!", e);
	    }
	}

	private void createVertxServer() {
		logger.info("Creating Vert.x server");

		// @formatter:off
		NetServerOptions options = new NetServerOptions()
			.setPort(HOST_PORT)
			.setTcpKeepAlive(true)
			.setTcpNoDelay(true);
		vertx.createNetServer(options)
			.connectHandler(defaultHandler)
			.listen(result -> {
				if (result.succeeded()) {
					logger.info("Now listening at port {}", HOST_PORT);
				} else {
					logger.error("Error binding at port {}", HOST_PORT, result.cause());
				}
			});
		// @formatter:on
	}

}
