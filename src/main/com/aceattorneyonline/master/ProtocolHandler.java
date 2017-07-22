package com.aceattorneyonline.master;

import com.aceattorneyonline.master.protocol.CompatibilityResult;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;

public interface ProtocolHandler extends Handler<Buffer> {
	
	/**
	 * Attempts to determine whether or not the received message
	 * is compatible with this protocol.
	 * 
	 * @param event  the first packet received from the socket
	 * @return whether or not the handshake was successful
	 */
	public abstract CompatibilityResult isCompatible(Buffer event);

	/**
	 * Creates a new protocol handler, but with a socket actually attached to it
	 * to provide some sort of context when messages are passed through the event bus.
	 * @param client  the new client
	 * @return new instance of ProtocolHandler of identical type
	 */
	public ProtocolHandler registerClient(Client client);

}
