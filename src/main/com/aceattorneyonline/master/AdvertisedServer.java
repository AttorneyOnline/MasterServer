package com.aceattorneyonline.master;

import java.time.Duration;
import java.time.Instant;

import com.aceattorneyonline.master.verticles.ServerList.DelistCallback;

import io.vertx.core.net.SocketAddress;

/** Represents a server being advertised, perhaps by an advertiser. */
public class AdvertisedServer {
	private final SocketAddress address;
	private final String hostname;
	private final int port;
	private final String name;
	private final String description;
	private final String version;
	private final Instant timeAdded;
	private DelistCallback delistCallback;

	public AdvertisedServer(String hostname, int port, String name, String description, String version) {
		this.address = new ServerAddress();
		this.hostname = hostname;
		this.port = port;
		this.name = name;
		this.description = description;
		this.version = version;
		this.timeAdded = Instant.now();
	}

	private class ServerAddress implements SocketAddress {

		@Override
		public String host() {
			return hostname;
		}

		@Override
		public int port() {
			return port;
		}

		@Override
		public boolean equals(Object o) {
			if (o != null && o.getClass().equals(getClass())) {
				SocketAddress otherAddress = (SocketAddress) o;
				return host() != null && otherAddress.host() != null
						&& host().equals(otherAddress.host())
						&& port() == otherAddress.port();
			}
			return false;
		}
	}

	public SocketAddress address() {
		return address;
	}

	public int port() {
		return port;
	}

	public String name() {
		return name;
	}

	public String description() {
		return description;
	}

	public String version() {
		return version;
	}

	public Duration uptime() {
		return Duration.between(timeAdded, Instant.now());
	}

	public void setDelistCallback(DelistCallback delist) {
		this.delistCallback = delist;
	}

	public void delist() {
		delistCallback.delist();
	}

	public String toString() {
		return String.format("Server (%s) - %s - %s:%d", version(), name(), address().host(), port());
	}

}
