package com.aceattorneyonline.master;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import io.netty.channel.Channel;

/**
 * Represents any connected user currently connected to the master server.
 */
public class User {
	private final Channel channel;

	public User(Channel channel) {
		this.channel = channel;
	}

	public InetAddress getAddress() {
		return ((InetSocketAddress) channel.remoteAddress()).getAddress();
	}

}
