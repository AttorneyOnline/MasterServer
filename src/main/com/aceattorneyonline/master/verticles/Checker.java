package com.aceattorneyonline.master.verticles;

import java.util.Collection;

import com.aceattorneyonline.master.Client;

import io.vertx.core.AbstractVerticle;

public class Checker extends AbstractVerticle {
	private long checkTimer;

	public void start() {
		checkTimer = vertx.setPeriodic(5000, id -> {
			Collection<Client> clients = ClientServerList.getSingleton().getClientsList();
			// Don't allow other threads to modify client list! Otherwise a
			// ConcurrentModificationException may occur.
			synchronized (clients) {
				for (Client client : clients) {
					client.protocolWriter().sendConnectionCheck();
				}
			}
		});
	}

	public void stop() {
		vertx.cancelTimer(checkTimer);
	}

}
