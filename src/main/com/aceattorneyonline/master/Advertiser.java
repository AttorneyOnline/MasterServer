package com.aceattorneyonline.master;

/**
 * Represents a server that is being advertised on the master server.
 */
public class Advertiser extends Client {
	private AdvertisedServer server;

	/** Promotes an unconnected client to an advertiser. */
	public Advertiser(UnconnectedClient client) {
		super(client);
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
			return String.format("%s - Advertiser - (not advertising)", id(), server.toString());
		}
	}

}
