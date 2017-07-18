package com.aceattorneyonline.master.verticles;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.aceattorneyonline.master.Client;
import com.aceattorneyonline.master.Player;

import io.vertx.core.AbstractVerticle;

/**
 * Parent class for all verticles that require a client list context.
 * 
 * <p>
 * XXX: this configuration sucks
 */
public abstract class ClientListVerticle extends AbstractVerticle {

	private Map<UUID, Client> clientList;
	private Map<UUID, Player> playerList;

	public ClientListVerticle(Map<UUID, Client> clientList) {
		this.clientList = clientList;
	}

	protected Client getClientById(UUID id) {
		return clientList.get(id);
	}

	protected Player getPlayerById(UUID id) {
		return playerList.get(id);
	}

	private void addClient(UUID id, Client client) {
		// TODO: determine performance/necessity of synchronized
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

	/** Retrieves a list of all connected clients. */
	public Collection<Client> getClientsList() {
		return clientList.values();
	}

	/** Retrieves a list of all connected players. */
	public Collection<Player> getPlayersList() {
		return playerList.values();
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
