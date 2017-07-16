package com.aceattorneyonline.master.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.Client;
import com.aceattorneyonline.master.ContextualProtocolHandler;
import com.aceattorneyonline.master.ProtocolHandler;

import io.vertx.core.buffer.Buffer;

public class RetroProtocolHandler extends ContextualProtocolHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(RetroProtocolHandler.class);

	public RetroProtocolHandler() {
		super();
	}

	public RetroProtocolHandler(Client context) {
		super(context);
	}

	@Override
	public void handle(Buffer event) {
	}

	@Override
	public boolean isCompatible(Buffer event) {
		return false;
	}

	@Override
	public ProtocolHandler registerClient(Client client) {
		client.setProtocolWriter(new RetroProtocolWriter(client.context()));
		return new RetroProtocolHandler(client);
	}

}
