package com.aceattorneyonline.master.events;

import com.aceattorneyonline.master.ChatCommand;
import com.aceattorneyonline.master.verticles.BanList;
import com.aceattorneyonline.master.verticles.Motd;
import com.aceattorneyonline.master.verticles.Version;

public enum Events {
	NEW_PLAYER("ms.player.new"),
	NEW_ADVERTISER("ms.advertiser.new"),

	GET_SERVER_LIST("ms.serverlist.get"),
	GET_SERVER_LIST_PAGED("ms.serverlist.get.paged"),

	SEND_CHAT("ms.chat.send"), // gets passed to event handler first
	BROADCAST_CHAT("ms.chat.broadcast"), // gets broadcast to all players
	CHAT_COMMAND("ms.chat.command"), // gets passed to chat command parser

	ADVERTISER_HEARTBEAT("ms.advertiser.heartbeat"),
	ADVERTISER_PING("ms.advertiser.ping"),

	GET_VERSION("ms.version.get", Version::parseGetVersion),

	SET_MOTD("ms.motd.set", Motd::parseSetMotd),
	RELOAD_MOTD("ms.motd.reload", Motd::parseReloadMotd),

	BAN_PLAYER("ms.admin.ban", BanList::parseBanCommand),
	UNBAN_PLAYER("ms.admin.unban", BanList::parseUnbanCommand),

	RELOAD_BANS("ms.util.bans.reload", BanList::parseReloadBans);

	private final String name;
	private ChatCommand command;

	private Events(String name) {
		this.name = name;
	}
	
	private Events(String name, ChatCommand command) {
		this.name = name;
		this.command = command;
	}

	@Override
	public String toString() {
		return name;
	}

	public ChatCommand getChatCommand() {
		return command;
	}

}
