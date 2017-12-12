package com.aceattorneyonline.master.federation;

import static com.aceattorneyonline.master.protocol.AOUtils.unescape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.ServerInfo;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;

class AOMasterServerHandler implements MasterServerHandler {

	private static final Logger logger = LoggerFactory.getLogger(AOMasterServerHandler.class);
	
	private NetSocket socket;

	private Queue<Future<String>> versionFutures = new LinkedList<>();

	private List<ServerInfo> servers = new ArrayList<>();
	private String version;

	public void connect(NetSocket socket) {
		this.socket = socket;
	}

	@Override
	public void handle(Buffer event) {
		String packet = event.toString("Windows-1251").trim();
		try {
			packet = packet.substring(0, packet.indexOf('%'));
		} catch (StringIndexOutOfBoundsException e) {
			logger.warn("{}: Packet without % delimiter! {}", socket, packet);
		}
		List<String> tokens = Arrays.asList(packet.split("#"));

		switch(tokens.get(0)) {
		case "SN":
			// Server entry
			servers.clear();
			if (tokens.size() > 6) {
				int n = Integer.parseInt(tokens.get(0));
				String ip = tokens.get(1);
				String version = tokens.get(2);
				int port = Integer.parseInt(tokens.get(3));
				String name = tokens.get(4), desc = tokens.get(5);
				ServerInfo info = new ServerInfo(name, desc, version);
				//AdvertisedServer server = new AdvertisedServer(ip, port, info, advertiser)
			}
			break;
		case "ALL":
			// All server entries
			break;
		case "CT":
			// Chat
			break;
		case "SV":
			// Version
			if (tokens.size() > 1) {
				version = unescape(tokens.get(1));
				while (!versionFutures.isEmpty()) {
					Future<String> future = versionFutures.remove();
					future.complete(version);
				}
			}
			break;
		default:
			logger.warn("{} Received unknown message: {}", socket, packet);
			break;
		}
	}

	@Override
	public void query() {
		socket.write("askforservers#%");
	}

	@Override
	public Future<String> version() {
		socket.write("VC#%");
		Future<String> future = Future.future();
		versionFutures.add(future);
		return future;
	}

}
