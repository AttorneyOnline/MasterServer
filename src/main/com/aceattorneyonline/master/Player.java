package com.aceattorneyonline.master;

/**
 * Represents a player (non-server) that is currently connected to the master
 * server.
 */
public class Player extends Client {
	private String name;
	private boolean admin;

	/** Instantiates a connected player based from an unconnected client. */
	public Player(UnconnectedClient client) {
		super(client);
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
	}

	/** Returns whether or not player has full admin rights. */
	public boolean hasAdmin() {
		return admin;
	}

}
