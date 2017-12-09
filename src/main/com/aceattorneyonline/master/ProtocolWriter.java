package com.aceattorneyonline.master;

import java.util.Collection;

import io.vertx.core.buffer.Buffer;

/**
 * Defines the protocol-specific methods on how commands will be sent back to a
 * client.
 * 
 * <p>
 * Calls to commands not specifically supported by a protocol should throw an
 * {@link UnsupportedOperationException}.
 */
public interface ProtocolWriter {

	/** Writes a buffer to the client. */
	void write(Buffer buffer);

	/** Writes a UTF-8-encoded string to the client. */
	default void write(String string) {
		write(Buffer.buffer(string));
	}

	/** Sends a single server entry being advertised. */
	void sendServerEntry(int number, AdvertisedServer advertiser);

	/** Sends multiple server entries being advertised. */
	void sendServerEntries(Collection<AdvertisedServer> serverList);

	/**
	 * Sends a message as an official communication between the master server and
	 * the client.
	 */
	void sendSystemMessage(String message);

	/** Sends a regular chat message. */
	void sendChatMessage(String author, String message);

	/** Sends the master server version. */
	void sendVersion(String version);

	/** Sends a pong to a server. */
	void sendPong();

	/** Sends a pong error to non-servers. */
	void sendPongError();

	/** Sends a heartbeat success for new advertisers. */
	void sendNewHeartbeatSuccess();

	/** Sends a periodic check to verify that the connection is still open. */
	void sendConnectionCheck();
	
	/** Sends a ban notification. */
	void sendBanNotification(String message);

}
