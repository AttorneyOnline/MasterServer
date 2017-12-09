package com.aceattorneyonline.master.verticles;

import com.aceattorneyonline.master.Client;

import io.vertx.core.AbstractVerticle;

public class Checker extends AbstractVerticle {
	private long checkTimer;

	public void start() {
		checkTimer = vertx.setPeriodic(3500, id -> {
			for (Client client : ClientServerList.getSingleton().getClientsList()) {
				client.protocolWriter().sendConnectionCheck();
			}
		});
	}

	public void stop() {
		vertx.cancelTimer(checkTimer);
	}

}
