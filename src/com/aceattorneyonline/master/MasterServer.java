package com.aceattorneyonline.master;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetServerOptions;

public class MasterServer {
	private static final int HOST_PORT = 27016;

	private static final Logger logger = LoggerFactory.getLogger(MasterServer.class.getName());

	public static void main(String[] args) throws IOException, InterruptedException {
		logger.info("Server started");
		Vertx vertx = Vertx.vertx();

		//@formatter:off
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
				.connectHandler(socket -> {
					socket.handler(buffer -> {
						
					});
				});
		//@formatter:on
	}

}
