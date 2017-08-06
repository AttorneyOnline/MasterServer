package com.aceattorneyonline.master.protocol;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.AdvertisedServer;
import com.aceattorneyonline.master.ProtocolWriter;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

public class AOProtocolWriter implements ProtocolWriter {
	
	private static final Logger logger = LoggerFactory.getLogger(AOProtocolWriter.class);

	protected final WriteStream<Buffer> writer;

	public AOProtocolWriter(WriteStream<Buffer> writer) {
		this.writer = writer;
		logger.trace("Instantiated AOProtocolWriter");
	}

	@Override
	public void sendServerEntry(int number, AdvertisedServer advertiser) {
		String ip = advertiser.address().host();
		int port = advertiser.port();
		String name = advertiser.name();
		String version = advertiser.version();

		StringBuilder packet = new StringBuilder();
		packet.append("SN#")
			.append(number).append("#")
			.append(ip).append("#")
			.append(version).append("#")
			.append(port).append("#")
			.append(name).append("#%");
		writer.write(Buffer.buffer(packet.toString()));
	}

	@Override
	public void sendServerEntries(Collection<AdvertisedServer> advertisers) {
		StringBuilder packet = new StringBuilder();
		packet.append("ALL#");
		for (AdvertisedServer advertiser : advertisers) {
			String ip = advertiser.address().host();
			int port = advertiser.port();
			String name = sanitize(advertiser.name());
			String desc = sanitize(advertiser.description());
			
			packet.append(name).append("&")
				.append(desc).append("&")
				.append(ip).append("&")
				.append(port).append("#");
		}
		packet.append("%");
		writer.write(Buffer.buffer(packet.toString()));
	}

	@Override
	public void sendSystemMessage(String message) {
		for (String line : message.split("\n")) {
			writer.write(Buffer.buffer("CT#AOMS#" + sanitize(line) + "#%"));
		}
	}

	@Override
	public void sendChatMessage(String author, String message) {
		if (author.isEmpty()) {
			// This method was found in ms.py
			writer.write(Buffer.buffer("CT#" + sanitize(message) + "\b00##%"));
		} else {
			writer.write(Buffer.buffer("CT#" + sanitize(author) + "#" + sanitize(message) + "#%"));
		}
	}

	@Override
	public void sendVersion(String version) {
		writer.write(Buffer.buffer("SV#" + version + "#%"));
	}

	@Override
	public void sendPong() {
		writer.write(Buffer.buffer("PONG#%"));
	}

	@Override
	public void sendPongError() {
		writer.write(Buffer.buffer("NOSERV#%"));
	}

	@Override
	public void sendNewHeartbeatSuccess() {
		writer.write(Buffer.buffer("PSDD#0#%"));
	}
	
	@Override
	public void sendConnectionCheck() {
		writer.write(Buffer.buffer("CHECK#%"));
	}
	
	@Override
	public void sendBanNotification(String message) {
		sendSystemMessage(message);
		writer.write(Buffer.buffer("DOOM#%"));
	}

	protected String sanitize(String str) {
		//@formatter:off
		return str.replaceAll("%", "<percent>")
				.replaceAll("#", "<num>")
				.replaceAll("\\$", "<dollar>")
				.replaceAll("&", "<and>");
		//@formatter:on
	}

}
