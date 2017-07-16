package com.aceattorneyonline.master;

import java.util.List;

/**
 * Defines the protocol-specific methods on how commands will be sent back to a
 * client.
 * 
 * <p>
 * Calls to commands not specifically supported by a protocol should throw an
 * {@link UnsupportedOperationException}.
 */
public interface ProtocolWriter {

	/** Sends a single server entry being advertised. */
	public void sendServerEntry(Advertiser advertiser);

	/** Sends multiple server entries being advertised. */
	public void sendServerEntries(List<Advertiser> advertisers);

	/**
	 * Sends a message as an official communication between the master server and
	 * the client.
	 */
	public void sendSystemMessage(String message);

	/** Sends a regular chat message. */
	public void sendChatMessage(String author, String message);

	/** Sends the master server version. */
	public void sendVersion(String version);

	/** Sends a pong to a server. */
	public void sendPong();

	/** Sends a pong error to non-servers. */
	public void sendPongError();

}
