package com.aceattorneyonline.master;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.aceattorneyonline.master.verticles.ClientServerList;

/** Represents a server being advertised, perhaps by an advertiser. */
public class AdvertisedServer implements Comparable<AdvertisedServer> {
	private final String hostname;
	private final int port;

	private ServerInfo info;

	private final Instant timeAdded;

	private Set<Advertiser> advertisers = new HashSet<>();
	private boolean destroying = false;
	private long destructionTimerId;

	public AdvertisedServer(String hostname, int port, ServerInfo info, Advertiser advertiser) {
		this.hostname = hostname;
		this.port = port;
		this.info = info;
		this.timeAdded = Instant.now();
		this.advertisers.add(advertiser);
	}

	/** Adds an advertiser that is currently advertising this server. */
	public void addAdvertiser(Advertiser advertiser) {
		advertisers.add(advertiser);
		if (destroying) {
			destroying = false;
			MasterServer.vertx.cancelTimer(destructionTimerId);
			destructionTimerId = 0;
		}
	}

	/**
	 * Removes an advertiser, and if there are no advertisers left,
	 * calls the delist callback.
	 */
	public void removeAdvertiser(Advertiser advertiser) {
		advertisers.remove(advertiser);
		if (advertisers.isEmpty()) {
			destructionTimerId = MasterServer.vertx.setTimer(10000, id -> {				
				delist();
			});
			destroying = true;
		}
	}

	public String address() {
		return String.format("{}:{}", hostname, port);
	}

	public String host() {
		return hostname;
	}

	public int port() {
		return port;
	}

	public String name() {
		return info.getName();
	}

	public String description() {
		return info.getDescription();
	}

	public String version() {
		return info.getVersion();
	}

	public Duration uptime() {
		return Duration.between(timeAdded, Instant.now());
	}

	public void setInfo(ServerInfo info) {
		this.info = info;
	}

	private void delist() {
		ClientServerList masterList = ClientServerList.getSingleton();
		masterList.removeServer(this);
	}

	public String toString() {
		return String.format("%s [%s:%d][version %s][uptime %s]", name(), host(), port(), version(),
				uptime().getSeconds());
	}

	public boolean addressEquals(AdvertisedServer o) {
			return host() != null && o.host() != null
					&& host().equals(o.host())
					&& port() == o.port();
	}

	@Override
	public int compareTo(AdvertisedServer o) {
		int uptime = -uptime().compareTo(o.uptime());
		if (uptime == 0) {
			return name().compareTo(o.name());
		} else {
			return uptime;
		}
	}

}
