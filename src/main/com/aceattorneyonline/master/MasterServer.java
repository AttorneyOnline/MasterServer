package com.aceattorneyonline.master;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.protocol.RetroProtocolHandler;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetServerOptions;

public class MasterServer {
	private static final int HOST_PORT = 27016;

	private static final Logger logger = LoggerFactory.getLogger(MasterServer.class);

	private DefaultProtocolHandler defaultHandler;
	
	public static final Vertx vertx = Vertx.vertx();

	public static void main(String[] args) throws IOException, InterruptedException {
		new MasterServer().run();
	}

	public void run() {
		logger.info("Server created");
		setupProtocolHandlers();
		createVertxServer();
	}

	private void setupProtocolHandlers() {
		defaultHandler = new DefaultProtocolHandler();
		defaultHandler.register(new RetroProtocolHandler());
	}

	private void createVertxServer() {
		logger.info("Creating Vert.x server");

		// @formatter:off
		NetServerOptions options = new NetServerOptions()
			.setPort(HOST_PORT)
			.setTcpKeepAlive(true)
			.setIdleTimeout(10);
		vertx.createNetServer(options)
			.listen(result -> {
				if (result.succeeded()) {
					logger.info("Now listening at port {}", HOST_PORT);
				} else {
					logger.error("Error binding at port {}", HOST_PORT, result.cause());
				}
			})
			.connectHandler(defaultHandler);
		// @formatter:on
	}

}
