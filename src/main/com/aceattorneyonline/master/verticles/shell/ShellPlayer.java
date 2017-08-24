package com.aceattorneyonline.master.verticles.shell;

import com.aceattorneyonline.master.Player;
import com.aceattorneyonline.master.verticles.ClientListVerticle;

import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.SocketAddressImpl;

/**
 * A fake, NetSocket-less client for all remote shell actions, because the
 * default Telnet server doesn't let us handle anything extra on
 * connect/disconnect, thus designating this as a HACK.
 */
class ShellPlayer extends Player {

	public ShellPlayer() {
		super(null); // This code smells bad. Really bad.
	}

	@Override
	public boolean hasAdmin() {
		return true;
	}

	@Override
	public SocketAddress address() {
		return new SocketAddressImpl(RemoteShell.PORT, "localhost");
	}

	@Override
	public String name() {
		return "System";
	}

	@Override
	public boolean isSystem() {
		return true;
	}

	private static ShellPlayer singleton;

	/**
	 * Gets the player representing the shell if it exists; otherwise, it creates
	 * one.
	 */
	public static ShellPlayer getSingleton() {
		if (singleton == null) {
			singleton = new ShellPlayer();
			ClientListVerticle.getSingleton().addPlayer(singleton.id(), singleton);
		}
		return singleton;
	}

	/**
	 * Destroys a singleton.
	 */
	public static void destroySingleton() {
		if (singleton != null) {
			singleton.onDisconnect();
			ClientListVerticle.getSingleton().removePlayer(singleton.id(), singleton);
			singleton = null;
		}
	}
}