package com.aceattorneyonline.master.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.Client;
import com.aceattorneyonline.master.MasterServer;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;

public class WebClientProtocolHandler extends AO2ClientProtocolHandler {

	private static final Logger logger = LoggerFactory.getLogger(WebClientProtocolHandler.class);

	public WebClientProtocolHandler() {
		super();
	}

	public WebClientProtocolHandler(Client context) {
		super(context);
	}

	@Override
	public CompatibilityResult isCompatible(NetSocket socket, Buffer event) {
		if (event.toString().contains("Sec-WebSocket-Key")) {
			StringBuilder str = new StringBuilder();
			str.append("HTTP/1.1 101 Switching Protocols\n");
			str.append("Connection: Upgrade\n");
			str.append("Sec-WebSocket-Accept: {KEY} \n"); // TODO key
			str.append("Server: AOMS " + MasterServer.VERSION + "\n");
			str.append("Upgrade: websocket\n\n");
			socket.write(str.toString());
			return CompatibilityResult.COMPATIBLE;
		}
		return CompatibilityResult.FAIL;
	}
}
