package com.aceattorneyonline.master.protocol;

public enum CompatibilityResult {
	/**
	 * Specifies that the packet definitively indicates compatibility with this
	 * protocol.
	 */
	COMPATIBLE,

	/**
	 * Specifies that a packet has been sent to verify compatibility with this
	 * protocol, but the default protocol handler/switcher should wait and handle
	 * the next packet.
	 */
	WAIT,

	/**
	 * Specifies that the packet does not indicate any compatibility with this
	 * protocol.
	 */
	FAIL

}
