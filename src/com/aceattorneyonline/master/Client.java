package com.aceattorneyonline.master;

import java.util.UUID;

import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;

/**
 * Represents any user currently connected to the master server.
 * 
 * <p>
 * Client cannot be instantiated in raw form, so no dirty tricks. 
 */
public abstract class Client {
	private final NetSocket context;
	private final UUID id;

	public Client(NetSocket context, UUID id) {
		this.context = context;
		this.id = id;
	}
	
	public Client(Client client) {
		this.context = client.context;
		this.id = client.id;
	}

	/** Returns an ephemeral ID representing the session. */
	public UUID id() {
		return id;
	}

	public SocketAddress address() {
		return context.remoteAddress();
	}

}
