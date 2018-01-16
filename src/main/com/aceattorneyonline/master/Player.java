package com.aceattorneyonline.master;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.events.SharedEventProtos.AnalyticsEvent;

import io.vertx.core.net.NetSocket;

/**
 * Represents a player (non-server) that is currently connected to the master
 * server.
 */
public class Player extends Client {
	
	private static final Logger logger = LoggerFactory.getLogger(Player.class);
	
	private String name = "";
	private boolean admin;
	private String version;
	private String hardwareId;

	// This chat receiver is kept here as a strong reference.
	private final ChatReceiver chatReceiver;
	
	public Player(NetSocket socket)  {
		super(socket);
		chatReceiver = new ChatReceiver(this);
	}
	
	public void setName(String name) {
		logger.info("{}: Set name to {}", this, name);
		this.name = name;
	}
	
	public void setVersion(String version) {
		logger.debug("{}: Set version to {}", this, version);
		this.version = version;
	}
	
	public void setHardwareId(String hardwareId) {
		logger.debug("{}: Set hardware ID to {}", this, hardwareId);
		this.hardwareId = hardwareId;
	}

	/**
	 * Returns the name used in chat. May be null, as players are only required to
	 * enter their names before using the chat functionality.
	 */
	public String name() {
		return name;
	}
	
	/** Returns the version of the client. May be null. */
	public String version() {
		return version;
	}
	
	/** Returns the hardware ID of the client (often the hard drive ID). May be null. */
	public String hardwareId() {
		return hardwareId;
	}

	public String toString() {
		String name = name();
		if (name == null || name.isEmpty()) {
			name = "(unnamed)";
		}
		return String.format("%s - Player %s%s", id(), name, admin ? " (a)" : "");
	}

	/** Sets the admin status of a player. <em>Use with caution!</em> */
	public void setAdmin(boolean admin) {
		this.admin = admin;
		logger.debug("{}: Set admin status: {}", address(), admin);
	}

	/** Returns whether or not player has full admin rights. */
	public boolean hasAdmin() {
		return admin;
	}

	/** Called when a player has disconnected from the server. */
	public void onDisconnect() {
		if (chatReceiver != null)
			chatReceiver.close();
	}
	
	/** Creates a protocol buffer containing this player's analytics data. */
	public AnalyticsEvent getAnalyticsData() {
		AnalyticsEvent.Builder analyticsEventBuilder = AnalyticsEvent.newBuilder()
				.setAddress(address().host());
		if (version() != null)
			analyticsEventBuilder.setVersion(version());
		if (hardwareId() != null)
			analyticsEventBuilder.setId(hardwareId());
		
		return analyticsEventBuilder.build();
	}
}
