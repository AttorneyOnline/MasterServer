package com.aceattorneyonline.master;

import com.aceattorneyonline.master.protocol.CompatibilityResult;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;

/**
 * Represents a protocol handler with a client that can be associated with it
 * (the "context").
 * 
 * Although an interface is not contractually required to have a constructor, it is
 * recommended for implementations to have the following constructors:
 *  - A default constructor for a generic protocol handler, for compatibility checking.
 *  - A constructor that takes a client context.
 */
public interface ContextualProtocolHandler extends ProtocolHandler {
	
	Client context();

	@Override
	void handle(Buffer event);

	/**
	 * Determines whether or not the buffer provided is compatible with this
	 * protocol.
	 */
	@Override
	CompatibilityResult isCompatible(NetSocket socket, Buffer event);

	/** Returns a new instance of this protocol handler, with a context attached. */
	@Override
	ProtocolHandler registerClient(NetSocket socket);

}
