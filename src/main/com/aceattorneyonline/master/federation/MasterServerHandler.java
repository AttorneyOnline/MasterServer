package com.aceattorneyonline.master.federation;

import java.util.Collection;

import com.aceattorneyonline.master.ServerInfo;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;

/**
 * An interface for handling messages received from a master server
 * of some protocol.
 */
interface MasterServerHandler extends Handler<Buffer> {
	
	/** Queries the master server for a list of servers. */
	void query();

	/** Gets the version of the master server. */
	Future<String> version();

}
