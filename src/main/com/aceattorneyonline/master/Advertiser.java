package com.aceattorneyonline.master;

/**
 * Represents a server that is being advertised on the master server.
 */
public class Advertiser extends Client {
	private String name;

	public Advertiser(UnconnectedClient client) {
		super(client);
	}

	/** Returns the name of the server. Must not be null. */
	public String name() {
		return name;
	}

	public String toString() {
		return String.format("%s - Server %30s", id(), name());
	}
}
