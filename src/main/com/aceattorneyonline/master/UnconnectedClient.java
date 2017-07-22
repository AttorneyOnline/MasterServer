package com.aceattorneyonline.master;

import java.util.UUID;

import com.aceattorneyonline.master.protocol.NullProtocolWriter;

import io.vertx.core.net.NetSocket;

public class UnconnectedClient extends Client {
	private Client successor;

	public UnconnectedClient(NetSocket socket) {
		super(socket, UUID.randomUUID());
		setProtocolWriter(new NullProtocolWriter());
	}

	public void setSuccessor(Client successor) {
		this.successor = successor; 
	}

	public Client getSuccessor() {
		return successor;
	}

}
