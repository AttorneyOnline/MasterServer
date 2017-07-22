package com.aceattorneyonline.master.protocol;

import java.util.Collection;

import com.aceattorneyonline.master.AdvertisedServer;
import com.aceattorneyonline.master.ProtocolWriter;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

public class AOProtocolWriter implements ProtocolWriter {

	protected final WriteStream<Buffer> writer;

	public AOProtocolWriter(WriteStream<Buffer> writer) {
		this.writer = writer;
	}

	@Override
	public void sendServerEntry(AdvertisedServer advertiser) {
		String ip = advertiser.address().host();
		String port = Integer.toString(advertiser.address().port());
		String name = advertiser.name();
		String version = advertiser.version();

		StringBuilder packet = new StringBuilder();
		packet.append("SN#")
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
			String port = Integer.toString(advertiser.address().port());
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
		writer.write(Buffer.buffer("CT#AOMS#" + message + "#%"));
	}

	@Override
	public void sendChatMessage(String author, String message) {
		if (author == "") {
			// This method was found in ms.py
			writer.write(Buffer.buffer("CT#" + message + "\b00##%"));
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

	public void sendClientConnectSuccess() {
		writer.write(Buffer.buffer("servercheok#1.7.5#%"));
	}

	private String sanitize(String str) {
		return str.replaceAll("%", "<percent>")
				.replaceAll("#", "<num>")
				.replaceAll("$", "<dollar>")
				.replaceAll("&", "<and>");
	}

}
