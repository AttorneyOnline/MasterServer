package com.aceattorneyonline.master.protocol;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

public class AO2ProtocolWriter extends AOProtocolWriter {

	public AO2ProtocolWriter(WriteStream<Buffer> writer) {
		super(writer);
	}

	@Override
	public void sendSystemMessage(String message) {
		write("CT#AOMS#" + message + "#%");
		// Not supported in AO2 yet:
		// writer.write(Buffer.buffer("MCT#" + message + "#%"));
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
