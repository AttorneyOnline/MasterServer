package com.aceattorneyonline.master;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.protocol.AO1ClientProtocolHandler;
import com.aceattorneyonline.master.protocol.AO2ClientProtocolHandler;
import com.aceattorneyonline.master.protocol.AOServerProtocolHandler;
import com.aceattorneyonline.master.verticles.BanList;
import com.aceattorneyonline.master.verticles.Chat;
import com.aceattorneyonline.master.verticles.Motd;
import com.aceattorneyonline.master.verticles.NewClientReceiver;
import com.aceattorneyonline.master.verticles.ServerList;
import com.aceattorneyonline.master.verticles.Version;

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
		if (pkg == null) {
			logger.warn("No package/manifest found to get a server version from! \n"
					+ "(usually this happens when the server is not being run from a jar file)");
			VERSION = "(debug)";
		} else {
			VERSION = pkg.getImplementationVersion();
		}
	}

	private Map<UUID, Client> clientList = new HashMap<>();

	private DefaultProtocolHandler defaultHandler;

	public static final Vertx vertx = Vertx.vertx();

	public static void main(String[] args) throws IOException, InterruptedException {
		new MasterServer().run();
	}

	public void run() {
		logger.info("Server created");
		setupProtocolHandlers();
		setupVerticles();
		createVertxServer();
	}

	private void setupProtocolHandlers() {
		defaultHandler = new DefaultProtocolHandler();
		defaultHandler.register(new AO1ClientProtocolHandler());
		defaultHandler.register(new AO2ClientProtocolHandler());
		defaultHandler.register(new AOServerProtocolHandler());
	}

	private void setupVerticles() {
		vertx.deployVerticle(new NewClientReceiver(clientList));
		vertx.deployVerticle(new ServerList(clientList));
		vertx.deployVerticle(new BanList(clientList));
		vertx.deployVerticle(new Chat(clientList));
		vertx.deployVerticle(new Motd(clientList));
		vertx.deployVerticle(new Version(clientList));
	}

	private void createVertxServer() {
		logger.info("Creating Vert.x server");

		// @formatter:off
		NetServerOptions options = new NetServerOptions()
			.setPort(HOST_PORT)
			.setTcpKeepAlive(true)
			.setIdleTimeout(10);
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
