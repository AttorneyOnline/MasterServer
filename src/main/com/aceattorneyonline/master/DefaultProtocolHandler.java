package com.aceattorneyonline.master;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;

public class DefaultProtocolHandler implements Handler<NetSocket> {

	private static final Logger logger = LoggerFactory.getLogger(DefaultProtocolHandler.class);

	/**
	 * A list of handlers to check protocols for. As the handshake message can be
	 * one of many protocols, we use this list to track each possible protocol. If a
	 * compatible one is found, then we hand off the socket to a more specific
	 * handler.
	 */
	private final List<ProtocolHandler> handlerList = new ArrayList<ProtocolHandler>();

	/** Registers a protocol handler to the handler list. */
	public void register(ProtocolHandler handler) {
		handlerList.add(handler);
		logger.debug("Registered {} to handler list", handler);
	}

	/** Handles a new connection with a compatible protocol. */
	@Override
	public void handle(NetSocket socket) {
		socket.handler(buffer -> {
			logger.info("Handling socket {}", socket.remoteAddress());
			boolean wait = false;
			for (ProtocolHandler handler : handlerList) {
				switch (handler.isCompatible(buffer)) {
				case COMPATIBLE:
					Client client = new UnconnectedClient(socket);
					socket.handler(handler.registerClient(client));
					logger.debug("Client found compatible with {}", handler);
					return;
				case WAIT:
					wait = true;
					logger.debug("Will wait on {}", handler);
					break;
				default:
				case FAIL:
					logger.trace("Failed compatibility check with {}", handler);
					break;
				}
			}
			if (!wait) {
				logger.warn("Client {} was disconnected due to invalid protocol", socket.remoteAddress());
				socket.end(Buffer.buffer("Invalid protocol"));
			}
		});
	}

}
