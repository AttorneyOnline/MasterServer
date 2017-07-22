package com.aceattorneyonline.master;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a player (non-server) that is currently connected to the master
 * server.
 */
public class Player extends Client {
	
	private static final Logger logger = LoggerFactory.getLogger(Player.class);
	
	private String name;
	private boolean admin;

	// This chat receiver is kept here as a strong reference.
	@SuppressWarnings("unused")
	private final ChatReceiver chatReceiver;

	/** Instantiates a connected player based from an unconnected client. */
	public Player(UnconnectedClient client) {
		super(client);
		chatReceiver = new ChatReceiver(this);
	}
	
	public void setName(String name) {
		this.name = name;
		logger.debug("User {} set their name to {}", address(), name);
	}

	/**
	 * Returns the name used in chat. May be null, as players are only required to
	 * enter their names before using the chat functionality.
	 */
	public String name() {
		return name;
	}

	public String toString() {
		String name = name();
		if (name.isEmpty()) {
			name = "(unnamed)";
		}
		return String.format("%s - Player %3s %12s", id(), admin ? "(a)" : "", name);
	}

	/** Sets the admin status of a player. <em>Use with caution!</em> */
	public void setAdmin(boolean admin) {
		this.admin = admin;
		logger.debug("User {} set themselves to admin: {}", address(), admin);
	}

	/** Returns whether or not player has full admin rights. */
	public boolean hasAdmin() {
		return admin;
	}

}
