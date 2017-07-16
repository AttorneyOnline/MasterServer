package com.aceattorneyonline.master;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;

public abstract class ContextualProtocolHandler implements ProtocolHandler {

	Client context;
	
	public ContextualProtocolHandler() {
		
	}

	public ContextualProtocolHandler(Client context) {
		this.context = context;
	}

	public Client context() {
		return context;
	}

	@Override
	public abstract void handle(Buffer event);

	@Override
	public abstract boolean isCompatible(Buffer event);

	@Override
	public abstract ProtocolHandler registerClient(Client client);

}
