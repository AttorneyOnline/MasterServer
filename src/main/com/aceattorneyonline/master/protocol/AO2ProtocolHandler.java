package com.aceattorneyonline.master.protocol;

import com.aceattorneyonline.master.Client;
import com.aceattorneyonline.master.ProtocolHandler;

import io.vertx.core.buffer.Buffer;

public class AO2ProtocolHandler extends AO1ProtocolHandler {

	public AO2ProtocolHandler() {
	}

	public AO2ProtocolHandler(Client context) {
		super(context);
	}

	@Override
	public boolean isCompatible(Buffer event) {
		// TODO: read buffer and check if it's AO2
		return false;
	}

	@Override
	public ProtocolHandler registerClient(Client client) {
		client.setProtocolWriter(new AO2ProtocolWriter(client.context()));
		return new AO2ProtocolHandler(client);
	}

}
