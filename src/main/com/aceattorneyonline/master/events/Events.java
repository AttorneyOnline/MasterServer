package com.aceattorneyonline.master.events;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import com.aceattorneyonline.master.ChatCommand;
import com.aceattorneyonline.master.verticles.BanList;
import com.aceattorneyonline.master.verticles.Chat;
import com.aceattorneyonline.master.verticles.Motd;
import com.aceattorneyonline.master.verticles.Version;

public enum Events {
	NEW_PLAYER("ms.player.new"),
	NEW_ADVERTISER("ms.advertiser.new"),

	GET_SERVER_LIST("ms.serverlist.get"),
	GET_SERVER_LIST_PAGED("ms.serverlist.get.paged"),

	SEND_CHAT("ms.chat.send"), // gets passed to event handler first
	BROADCAST_CHAT("ms.chat.broadcast"), // gets broadcast to all players
	CHAT_COMMAND_LIST("ms.chat.command.list", Chat::parseCommandList),
	CHAT_COMMAND_HELP("ms.chat.command.help", Chat::parseCommandHelp),

	ADVERTISER_HEARTBEAT("ms.advertiser.heartbeat"),
	ADVERTISER_PING("ms.advertiser.ping"),

	GET_VERSION("ms.version.get", Version::parseGetVersion),

	GET_MOTD("ms.motd.get", Motd::parseGetMotd),
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

	// TODO: this should be changed to add the name of the chat command, if
	// applicable
	@Override
	public String toString() {
		return name;
	}

	public String getEventName() {
		return name;
	}

	public ChatCommand getChatCommand() {
		return command;
	}

	/**
	 * Returns a map of all chat commands registered as events, keyed by the
	 * command's name and valued as the event itself.
	 */
	public static Map<String, Events> getAllChatCommands() {
		return Arrays.stream(values())
				.filter(e -> e.command != null && e.command.getSyntax() != null && e.command.getSyntax().name() != null)
				.collect(Collectors.toMap(e -> e.getChatCommand().getSyntax().name(), e -> e));
	}

}
