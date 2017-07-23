package com.aceattorneyonline.master;

import io.vertx.core.net.NetSocket;

/**
 * Represents a server that is being advertised on the master server.
 */
public class Advertiser extends Client {
	private AdvertisedServer server;
	
	public Advertiser(NetSocket socket) {
		super(socket);
	}

	/** Returns the server being advertised. */
	public AdvertisedServer server() {
		return server;
	}

	public void setServer(AdvertisedServer server) {
		this.server = server;
	}

	public String toString() {
		if (server != null) {
			return String.format("%s - Advertiser - %s", id(), server.toString());
		} else {
			return String.format("%s - Advertiser - (not advertising)", id());
		}
	}

}
