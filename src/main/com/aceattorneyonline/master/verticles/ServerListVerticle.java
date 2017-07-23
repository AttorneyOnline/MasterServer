package com.aceattorneyonline.master.verticles;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.AdvertisedServer;

import io.vertx.core.net.SocketAddress;

public abstract class ServerListVerticle extends ClientListVerticle {

	private static final Logger logger = LoggerFactory.getLogger(ServerListVerticle.class);

	private static Comparator<AdvertisedServer> listComparator = (a, b) -> a.uptime().compareTo(b.uptime());
	private static Map<SocketAddress, AdvertisedServer> serverList = new HashMap<>();
	protected static List<AdvertisedServer> serverListCache = new ArrayList<>();
	private static boolean serverListCacheDirty = false;

	public ServerListVerticle() {
		super();
	}

	/** Caches a sorted version of the server list. */
	private void cacheServerList() {
		logger.debug("Server list cache is dirty. Rebuilding.");
		synchronized (serverListCache) {
			serverListCache = serverList.values().stream().sorted(listComparator).collect(Collectors.toList());
		}
		serverListCacheDirty = false;
	}

	/** Adds or updates an advertised server. */
	public void addServer(AdvertisedServer server) {
		synchronized (serverList) {
			serverList.put(server.address(), server);
		}
		serverListCacheDirty = true;
	}

	/** Removes an advertised server from the server list. */
	public void removeServer(AdvertisedServer server) {
		synchronized (serverList) {
			serverList.remove(server.address());
		}
		serverListCacheDirty = true;
	}

	/** Gets a sorted version of the server list from cache. */
	public List<AdvertisedServer> getSortedServerList() {
		if (serverListCacheDirty) {
			cacheServerList();
		}
		return serverListCache;
	}

}