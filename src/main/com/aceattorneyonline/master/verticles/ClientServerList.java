package com.aceattorneyonline.master.verticles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.AdvertisedServer;
import com.aceattorneyonline.master.Advertiser;
import com.aceattorneyonline.master.Client;
import com.aceattorneyonline.master.Player;
import com.aceattorneyonline.master.ServerInfo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.net.SocketAddress;

/**
 * Parent class for all verticles that require a client list.
 * 
 * <p>
 * XXX: this design sucks
 */
public abstract class ClientServerList {

	private static final Logger logger = LoggerFactory.getLogger(ClientServerList.class);

	private Map<UUID, Client> clientList = new HashMap<>();
	private Map<UUID, Player> playerList = new HashMap<>();
	private Map<UUID, Advertiser> advertiserList = new HashMap<>();
	
	private static final Comparator<AdvertisedServer> listComparator = (a, b) -> a.uptime().compareTo(b.uptime());
	private static Map<String, AdvertisedServer> serverList = new HashMap<>();
	private static List<AdvertisedServer> serverListCache = new ArrayList<>();
	private static boolean serverListCacheDirty = false;

	public ClientServerList() {

	}

	protected Client getClientById(UUID id) {
		return clientList.get(id);
	}

	protected Player getPlayerById(UUID id) {
		return playerList.get(id);
	}

	protected Advertiser getAdvertiserById(UUID id) {
		return advertiserList.get(id);
	}

	private void addClient(UUID id, Client client) {
		synchronized (clientList) {
			clientList.put(id, client);
		}
	}

	private void removeClient(UUID id) {
		synchronized (clientList) {
			clientList.remove(id);
		}
	}

	public void addPlayer(UUID id, Player player) {
		synchronized (playerList) {
			playerList.put(id, player);
		}
		addClient(id, player);
	}

	public void removePlayer(UUID id, Player player) {
		synchronized (playerList) {
			playerList.remove(id);
		}
		removeClient(id);
	}

	public void addAdvertiser(UUID id, Advertiser advertiser) {
		synchronized (advertiserList) {
			advertiserList.put(id, advertiser);
		}
		addClient(id, advertiser);
	}

	public void removeAdvertiser(UUID id, Advertiser advertiser) {
		synchronized (advertiserList) {
			advertiserList.remove(id);
		}
		removeClient(id);
	}

	/** Caches a sorted version of the server list. */
	private void cacheServerList() {
		logger.trace("Server list cache is dirty. Rebuilding.");
		synchronized (serverListCache) {
			serverListCache = serverList.values().stream().sorted(listComparator).collect(Collectors.toList());
		}
		serverListCacheDirty = false;
	}

	/**
	 * Adds or updates an advertised server.
	 * @return an existing server entry if found, or a new server entry if not found 
	 */
	public AdvertisedServer addServer(Advertiser advertiser, int port, ServerInfo info) {
		String hostname = advertiser.address().host();
		synchronized (serverList) {
			AdvertisedServer server = serverList.get(hostname);
			if (server != null) {
				server.setInfo(info);
				server.addAdvertiser(advertiser);
			} else {
				server = new AdvertisedServer(hostname, port, info, advertiser);
				serverList.put(hostname, server);
				serverListCacheDirty = true;
			}
			return server;
		}
	}

	/** Removes an advertised server from the server list. */
	public void removeServer(AdvertisedServer server) {
		logger.debug("Removed {} from server list", server);
		synchronized (serverList) {
			serverList.remove(server.address());
		}
		serverListCacheDirty = true;
	}

	/**
	 * Another lazy hack that is called when a client disconnects and we don't know
	 * what state he's in.
	 */
	public void onClientDisconnect(UUID id, Client disconnectedClient) {
		if (disconnectedClient instanceof Player) {
			removePlayer(id, (Player) disconnectedClient);
		} else if (disconnectedClient instanceof Advertiser) {
			removeAdvertiser(id, (Advertiser) disconnectedClient);
		}
	}

	public static ClientListSingleton getSingleton() {
		return singleton;
	}

	/** Retrieves a list of all connected clients. */
	public Collection<Client> getClientsList() {
		return clientList.values();
	}

	/** Retrieves a list of all connected players. */
	public Collection<Player> getPlayersList() {
		return playerList.values();
	}

	/** Retrieves a list of all connected advertisers. */
	public Collection<Advertiser> getAdvertisersList() {
		return advertiserList.values();
	}

	/** Gets a sorted version of the server list from cache. */
	public List<AdvertisedServer> getSortedServerList() {
		if (serverListCacheDirty) {
			cacheServerList();
		}
		return serverListCache;
	}

	/** Gets a list of connected players that match a case-insensitive search string. */
	public Collection<Player> searchPlayerByNameFuzzy(String name) {
		return playerList.values().stream().filter(player -> player.name().toLowerCase().contains(name.toLowerCase()))
				.collect(Collectors.toList());
	}
	
	/** Gets a list of connected players that exactly match a case-insensitive search string. */
	public Collection<Player> searchPlayerByNameExact(String name) {
		return playerList.values().stream().filter(player -> player.name().equalsIgnoreCase(name))
				.collect(Collectors.toList());
	}

	/** Gets a list of connected clients that match an IP address. */
	public Collection<Client> searchClientByAddress(String address) {
		return clientList.values().stream().filter(client -> client.address().host().equals(address))
				.collect(Collectors.toList());
	}

	/** Gets a list of connected players that match an IP address. */
	public Collection<Player> searchPlayerByAddress(String address) {
		return playerList.values().stream().filter(player -> player.address().host().equals(address))
				.collect(Collectors.toList());
	}

	/** Gets the number of connected non-system players who have names. */
	public long getNamedPlayerCount() {
		return getPlayersList().stream().filter(p -> !p.name().isEmpty() && !p.isSystem()).count();
	}

	/** Gets the number of connected non-system players. */
	public long getPlayerCount() {
		return getPlayersList().stream().filter(p -> !p.isSystem()).count();
	}

	public static final class ClientListSingleton extends ClientServerList {

	}

	private static final ClientListSingleton singleton = new ClientListSingleton();

}
