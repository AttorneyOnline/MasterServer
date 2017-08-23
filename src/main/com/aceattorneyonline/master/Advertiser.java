package com.aceattorneyonline.master;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.net.NetSocket;

/**
 * Represents a server that is being advertised on the master server.
 */
public class Advertiser extends Client implements Comparable<Advertiser> {
	
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
		logger.info("Advertising server: {}", server);
		this.server = server;
	}

	@Override
	public String toString() {
		if (server != null) {
			return String.format("%s - Advertiser - %s", id(), server.toString());
		} else {
			return String.format("%s - Advertiser - (not advertising)", id());
		}
	}

	@Override
	public int compareTo(Advertiser a) {
		if (server() != null && a.server() != null) {
			return server().uptime().compareTo(a.server().uptime());
		} else {
			return id().compareTo(a.id());
		}
	}
}
