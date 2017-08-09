package com.aceattorneyonline.master;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.protocol.NullProtocolWriter;

import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;

/** Represents any user currently connected to the master server. */
public abstract class Client {
	
	private static final Logger logger = LoggerFactory.getLogger(Client.class);
	
	private final NetSocket socket;
	private final UUID id;
	private ProtocolWriter writer;
	
	public Client(NetSocket socket) {
		this(socket, UUID.randomUUID());
		setProtocolWriter(new NullProtocolWriter());
	} 

	public Client(NetSocket context, UUID id) {
		this.socket = context;
		this.id = id;
	}
	
	public Client(Client client) {
		this.socket = client.socket;
		this.id = client.id;
		this.writer = client.writer;
	}

	public void setProtocolWriter(ProtocolWriter writer) {
		logger.debug("{}: Set protocol writer to {}", this, writer.getClass().getSimpleName());
		this.writer = writer;
	}

	/**
	 * Returns the protocol writer that can be used to facilitate protocol-specific
	 * writes to the client.
	 */
	public ProtocolWriter protocolWriter() {
		return writer;
	}

	/** Returns the network socket of this client. */
	public NetSocket socket() {
		return socket;
	}

	/** Returns an ephemeral ID representing the session. */
	public UUID id() {
		return id;
	}

	public SocketAddress address() {
		return socket.remoteAddress();
	}

	/**
	 * Returns whether or not the client was created internally.
	 * Such clients are not counted in metrics.
	 */
	public boolean isSystem() {
		return false;
	}

	public String toString() {
		return String.format("%s - Client - %s", id(), address().toString());
	}

}
