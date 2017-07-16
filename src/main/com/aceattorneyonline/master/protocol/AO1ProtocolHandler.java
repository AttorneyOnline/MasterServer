package com.aceattorneyonline.master.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.Client;
import com.aceattorneyonline.master.ContextualProtocolHandler;
import com.aceattorneyonline.master.ProtocolHandler;

import io.vertx.core.buffer.Buffer;

public class AO1ProtocolHandler extends ContextualProtocolHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(AO1ProtocolHandler.class);

	public AO1ProtocolHandler() {
		super();
	}

	public AO1ProtocolHandler(Client context) {
		super(context);
	}

	@Override
	public void handle(Buffer event) {
	}

	@Override
	public boolean isCompatible(Buffer event) {
		// TODO: read buffer and check if it's 1.7.5/retro
		return false;
	}

	@Override
	public ProtocolHandler registerClient(Client client) {
		client.setProtocolWriter(new AOProtocolWriter(client.context()));
		return new AO1ProtocolHandler(client);
	}

}
