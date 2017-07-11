package com.aceattorneyonline.master;

import io.netty.channel.Channel;

/**
 * Represents a server that is being advertised on the master server.
 */
public class Advertiser extends User {
	private String name;

	public Advertiser(Channel channel) {
		super(channel);
	}
}
