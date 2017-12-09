package com.aceattorneyonline.master;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.net.NetSocket;

/**
 * Represents a server that is being advertised on the master server.
 */
public class Advertiser extends Client {
	
	private static final Logger logger = LoggerFactory.getLogger(Advertiser.class);
	
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

	/** Called when an advertiser has disconnected from the server. */
	public void onDisconnect() {
		if (server != null) {
			server.removeAdvertiser(this);
			server = null;
		}
	}

	@Override
	public String toString() {
		if (server != null) {
			return String.format("%s - Advertiser - %s", id(), server.toString());
		} else {
			return String.format("%s - Advertiser - (not advertising)", id());
		}
	}

}
