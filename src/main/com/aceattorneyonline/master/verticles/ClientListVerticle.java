package com.aceattorneyonline.master.verticles;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.Advertiser;
import com.aceattorneyonline.master.Client;
import com.aceattorneyonline.master.Player;

import io.vertx.core.AbstractVerticle;

/**
 * Parent class for all verticles that require a client list context.
 * 
 * <p>
 * XXX: this design sucks
 */
public abstract class ClientListVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(ClientListVerticle.class);

	private static Map<UUID, Client> clientList = new HashMap<>();
	private static Map<UUID, Player> playerList = new HashMap<>();
	private static Map<UUID, Advertiser> advertiserList = new HashMap<>();

	public ClientListVerticle() {

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

	public static final class ClientListSingleton extends ClientListVerticle {

	}

	private static final ClientListSingleton singleton = new ClientListSingleton();

	public static ClientListSingleton getSingleton() {
		logger.debug("Client list singleton retrieved");
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

	/** Gets a list of connected players that match a name. */
	public Collection<Player> searchPlayerByName(String name) {
		return playerList.values().stream().filter(player -> player.name().toLowerCase().contains(name))
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

}
