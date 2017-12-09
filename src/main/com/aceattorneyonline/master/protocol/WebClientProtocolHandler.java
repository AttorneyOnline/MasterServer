package com.aceattorneyonline.master.protocol;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.java_websocket.WebSocket.Role;
import org.java_websocket.drafts.Draft.HandshakeState;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.exceptions.InvalidHandshakeException;
import org.java_websocket.framing.Framedata;
import org.java_websocket.framing.Framedata.Opcode;
import org.java_websocket.framing.PingFrame;
import org.java_websocket.framing.PongFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.HandshakeImpl1Server;
import org.java_websocket.handshake.ServerHandshakeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.Client;
import com.aceattorneyonline.master.MasterServer;
import com.aceattorneyonline.master.Player;
import com.aceattorneyonline.master.ProtocolHandler;
import com.aceattorneyonline.master.verticles.ClientServerList;

import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;

public class WebClientProtocolHandler extends AO1ClientProtocolHandler {

	private static final Logger logger = LoggerFactory.getLogger(WebClientProtocolHandler.class);

	// HACK: this essentially is serving as a "global variable" in the isCompatible
	// stage, to bridge with registerClient
	private Draft_6455 websocket = new Draft_6455();

	public WebClientProtocolHandler() {
		super();
	}

	public WebClientProtocolHandler(Client context, Draft_6455 websocket) {
		super(context);
		this.websocket = websocket;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void handle(Buffer event) {
		/*
		 * if (event.length() < 6) {
		 * logger.debug("Websocket packet is less than 6 bytes long. Ignoring.");
		 * return; } try { byte[] header = event.getBytes(0, 6); int length = header[1]
		 * & 127; byte[] body = event.getBytes(6, 6 + length); for (int i = 0; i <
		 * length; i++) { body[i] ^= header[2 + (i & 3)]; } super.handle(event); if
		 * (event.length() > length + header.length) { // If we think there are more
		 * frames inside this packet, try again handle(event.getBuffer(header.length +
		 * length, event.length())); } } catch (IndexOutOfBoundsException e) {
		 * logger.debug("Websocket packet was shorter than expected!", e); }
		 */

		// Don't parse handshakes
		if (event.toString().startsWith("GET / HTTP/1.1"))
			return;

		List<Framedata> frames = new ArrayList<>();
		try {
			frames = websocket.translateFrame(event.getByteBuf().nioBuffer());
		} catch (InvalidDataException e) {
			logger.debug("Invalid frame received from WebSocket", e);
		}
		for (Framedata frame : frames) {
			if (frame.getOpcode() == Opcode.TEXT || frame.getOpcode() == Opcode.BINARY)
				// XXX: let's hope the payload isn't incomplete...
				super.handle(Buffer.buffer(Unpooled.copiedBuffer(frame.getPayloadData())));
			else if (frame.getOpcode() == Opcode.PING)
				context().socket().write(Buffer
						.buffer(Unpooled.wrappedBuffer(websocket.createBinaryFrame(new PongFrame((PingFrame) frame)))));
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public CompatibilityResult isCompatible(NetSocket socket, Buffer event) {
		try {
			if (event.toString().contains("Sec-WebSocket-Key")) {
				websocket = new Draft_6455();
				websocket.setParseMode(Role.SERVER);
				// acceptHandshakeAsServer is not deprecated - only Draft 17 is, but
				// Draft 6455 is mostly based on Draft 17, and Draft 6455 is the
				// working WebSockets implementation. Don't listen to Eclipse.
				// Also, casting to ClientHandshake is totally fine and is actually done in
				// the reference implementation, because this may or may not be a poor library
				// to use.
				ClientHandshake handshake =
						(ClientHandshake) websocket.translateHandshake(event.getByteBuf().nioBuffer());
				if (websocket.acceptHandshakeAsServer(handshake) == HandshakeState.MATCHED) {
					ServerHandshakeBuilder response = new HandshakeImpl1Server(); // kek'd by really bad library
					websocket.postProcessHandshakeResponseAsServer(handshake, response);
					response.put("Server", "AOMS " + MasterServer.VERSION);
					for (ByteBuffer packet : websocket.createHandshake(response, Role.SERVER)) {
						// Probably safer than doing Unpooled.wrappedBuffer(packets.toArray())
						// and then writing it all as one buffer.
						socket.write(Buffer.buffer(Unpooled.wrappedBuffer(packet)));
					}
					return CompatibilityResult.COMPATIBLE;
				}
			}
		} catch (InvalidHandshakeException e) {
			logger.debug("Bad handshake with possible WebSockets client.", e);
		}
		return CompatibilityResult.FAIL;
	}

	@Override
	public ProtocolHandler registerClient(NetSocket socket) {
		Player player = new Player(socket);
		ClientServerList.getSingleton().addPlayer(player.id(), player);
		player.setProtocolWriter(new WebClientProtocolWriter(player.socket(), websocket));
		return new WebClientProtocolHandler(player, websocket);
	}

	protected boolean askForServersAllAtOnce() {
		return true;
	}
}
