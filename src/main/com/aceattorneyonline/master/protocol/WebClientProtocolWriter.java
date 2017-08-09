package com.aceattorneyonline.master.protocol;

import java.nio.ByteBuffer;

import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.framing.Framedata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

public class WebClientProtocolWriter extends AOProtocolWriter {

	private static final Logger logger = LoggerFactory.getLogger(WebClientProtocolWriter.class);
	private final Draft_6455 websocket;
	
	public WebClientProtocolWriter(WriteStream<Buffer> writer, Draft_6455 websocket) {
		super(writer);
		this.websocket = websocket;
		logger.trace("Instantiated WebClientProtocolWriter");
	}

	/** Write data to the WebSocket in text frames. */
	@SuppressWarnings("deprecation")
	public void write(String text) {
		// Don't add a mask, per spec.
		for (Framedata frame : websocket.createFrames(text, false)) {
			writer.write(Buffer.buffer(Unpooled.wrappedBuffer(websocket.createBinaryFrame(frame))));
		}
	}

	/** Write data to the WebSocket in binary frames. */
	@SuppressWarnings("deprecation")
	public void write(Buffer data) {
		ByteBuffer byteBuf = data.getByteBuf().nioBuffer();
		// Don't add a mask, per spec.
		for (Framedata frame : websocket.createFrames(byteBuf, false)) {
			writer.write(Buffer.buffer(Unpooled.wrappedBuffer(websocket.createBinaryFrame(frame))));
		}
	}
}
