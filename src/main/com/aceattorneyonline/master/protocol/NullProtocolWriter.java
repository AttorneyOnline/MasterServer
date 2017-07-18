package com.aceattorneyonline.master.protocol;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.AdvertisedServer;
import com.aceattorneyonline.master.ProtocolWriter;

public class NullProtocolWriter implements ProtocolWriter {

	private static final Logger logger = LoggerFactory.getLogger(NullProtocolWriter.class);

	@Override
	public void sendServerEntry(AdvertisedServer advertiser) {
		logger.debug("Tried to send server entry to null protocol writer");
	}

	@Override
	public void sendServerEntries(Collection<AdvertisedServer> serverList) {
		logger.debug("Tried to send server entries to null protocol writer");
	}

	@Override
	public void sendSystemMessage(String message) {
		logger.debug("Tried to send system message to null protocol writer");
	}

	@Override
	public void sendChatMessage(String author, String message) {
		logger.debug("Tried to send chat message to null protocol writer");
	}

	@Override
	public void sendVersion(String version) {
		logger.debug("Tried to send master server version to null protocol writer");
	}

	@Override
	public void sendPong() {
		logger.debug("Tried to send pong to null protocol writer");
	}

	@Override
	public void sendPongError() {
		logger.debug("Tried to send pong error to null protocol writer");
	}

	@Override
	public void sendNewHeartbeatSuccess() {
		logger.debug("Tried to send heartbeat success to null protocol writer");
	}

	@Override
	public void sendConnectionCheck() {
		logger.debug("Tried to send connection check to null protocol writer");
	}

}
