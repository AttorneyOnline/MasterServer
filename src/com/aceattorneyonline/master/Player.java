package com.aceattorneyonline.master;

import io.netty.channel.Channel;

/**
 * Represents a player (non-server) that is currently connected to the master
 * server.
 */
public class Player extends User {
	/** Name used in chat. May be null. */
	private String name;

	public Player(Channel channel) {
		super(channel);
	}
}
