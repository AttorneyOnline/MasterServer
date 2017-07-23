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
		long timer = MasterServer.vertx.setTimer(750, id -> {
			// If nothing received within 750 ms, take the initiative and start looking for
			// compatible handlers. For instance, AO1 doesn't take an initial packet from
			// the client.
			handle(socket, Buffer.buffer());
		});
		socket.handler(buffer -> {
			// Packet received. Cancel the timer.
			MasterServer.vertx.cancelTimer(timer);
			handle(socket, buffer);
		});
	}

	public void handle(NetSocket socket, Buffer buffer) {
		logger.info("Handling socket {}", socket.remoteAddress());
		boolean wait = false;
		for (ProtocolHandler handler : handlerList) {
			switch (handler.isCompatible(socket, buffer)) {
			case COMPATIBLE:
				logger.debug("Client found compatible with {}", handler);
				ProtocolHandler newHandler = handler.registerClient(socket);
				logger.trace("Registered new client with new protocol handler");
				newHandler.handle(buffer);
				socket.handler(newHandler);
				socket.drainHandler(nil -> {
					socket.write("Nice job trying to flood");
					socket.close();
				});
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
			logger.warn("Client {} was disconnected due to invalid protocol: {}", socket.remoteAddress(), buffer.toString());
			socket.end(Buffer.buffer("Invalid protocol"));
		}
	}

}
