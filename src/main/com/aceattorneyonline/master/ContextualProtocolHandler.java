package com.aceattorneyonline.master;

import io.vertx.core.buffer.Buffer;

public abstract class ContextualProtocolHandler implements ProtocolHandler {

	private Client context;

	/** Instantiates a generic protocol handler, for compatibility checking. */
	public ContextualProtocolHandler() {

	}

	/** Instantiates a protocol hander with context. */
	public ContextualProtocolHandler(Client context) {
		this.context = context;
	}

	public Client context() {
		return context;
	}

	@Override
	public abstract void handle(Buffer event);

	/**
	 * Determines whether or not the buffer provided is compatible with this
	 * protocol.
	 */
	@Override
	public abstract boolean isCompatible(Buffer event);

	/** Returns a new instance of this protocol handler, with a context attached. */
	@Override
	public abstract ProtocolHandler registerClient(Client client);

}
