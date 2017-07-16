package com.aceattorneyonline.master;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;

public class ChatReceiverVerticle extends AbstractVerticle {
	
	Client client;
	
	public ChatReceiverVerticle(Client client) {
		
	}

	@Override
	public void start() throws Exception {
		EventBus eventBus = getVertx().eventBus();
		eventBus.consumer("ms.chat", this);
	}

}
