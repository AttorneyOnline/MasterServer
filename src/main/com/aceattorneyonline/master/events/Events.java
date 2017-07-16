package com.aceattorneyonline.master.events;

public enum Events {
	NEW_PLAYER("ms.player.new"),
	NEW_ADVERTISER("ms.advertiser.new"),

	GET_SERVER_LIST("ms.serverlist.get"),
	GET_SERVER_LIST_PAGED("ms.serverlist.get.paged"),

	SEND_CHAT("ms.chat.send"), // gets passed to event handler first
	BROADCAST_CHAT("ms.chat.broadcast"), // gets broadcast to all players

	ADVERTISER_HEARTBEAT("ms.advertiser.heartbeat"),
	ADVERTISER_PING("ms.advertiser.ping"),

	GET_VERSION("ms.version.get"),

	SET_MOTD("ms.motd.set"),
	RELOAD_MOTD("ms.motd.reload"),

	BAN_PLAYER("ms.admin.ban"),
	UNBAN_PLAYER("ms.admin.unban"),

	RELOAD_BANS("ms.util.bans.reload");

	private final String name;

	private Events(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}
