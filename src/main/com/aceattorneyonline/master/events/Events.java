package com.aceattorneyonline.master.events;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.aceattorneyonline.master.ChatCommand;
import com.aceattorneyonline.master.ChatCommandSyntax;
import com.aceattorneyonline.master.events.UuidProto.Uuid;
import com.aceattorneyonline.master.verticles.BanList;
import com.aceattorneyonline.master.verticles.Chat;
import com.aceattorneyonline.master.verticles.Motd;
import com.aceattorneyonline.master.verticles.PrivateMessage;
import com.aceattorneyonline.master.verticles.Version;
import com.google.protobuf.Message;

/*
 * Now you ask yourself, why can't we use method references for each chat
 * command? Wouldn't that be soooo much easier?
 * 
 * Yes, it would be much easier. However, there's a tiny little problem that I
 * still can't quite understand in Java: if you anonymously declare the
 * ChatCommand as a field somewhere off in the distance, it doesn't understand
 * that it's a ChatCommand, so it returns null on getSyntax() after you declare
 * it right in from of your literal eyes. If you use a method reference, the
 * method you want to get an annotation out of is wrapped by a lambda, so all of
 * the annotations get cleared.
 * 
 * Solution: just anonymously declare the ChatCommand on the spot.
 */
public enum Events {
	NEW_PLAYER("ms.player.new"),
	NEW_ADVERTISER("ms.advertiser.new"),
	
	PLAYER_CONNECTED("ms.player.connected"),
	ADVERTISER_CONNECTED("ms.advertiser.connected"),
	PLAYER_LEFT("ms.player.left"),
	ADVERTISER_LEFT("ms.advertiser.left"),

	GET_SERVER_LIST("ms.serverlist.get"),
	GET_SERVER_LIST_PAGED("ms.serverlist.get.paged"),

	PIN_SERVER("ms.advertiser.pin"),

	SEND_CHAT("ms.chat.send"), // gets passed to event handler first
	BROADCAST_CHAT("ms.chat.broadcast"), // gets broadcast to all players
	CHAT_COMMAND_LIST("ms.chat.command.list", new ChatCommand() {

		@ChatCommandSyntax(name = "list", description = "Lists all chat commands.", arguments = "")
		@Override
		public Message serializeCommand(Uuid invoker, List<String> args) throws IllegalArgumentException {
			return Chat.parseCommandList(invoker, args);
		}

	}),
	CHAT_COMMAND_HELP("ms.chat.command.help", new ChatCommand() {

		@ChatCommandSyntax(name = "help", description = "Gets help on one chat command or lists all chat commands.", arguments = "[command]")
		public Message serializeCommand(Uuid invoker, List<String> args) throws IllegalArgumentException {
			return Chat.parseCommandHelp(invoker, args);
		}

	}),

	ADVERTISER_HEARTBEAT("ms.advertiser.heartbeat"),
	ADVERTISER_PING("ms.advertiser.ping"),

	GET_VERSION("ms.version.get", new ChatCommand() {

		@ChatCommandSyntax(name = "version", description = "Gets the master server's version.", arguments = "")
		public Message serializeCommand(Uuid invoker, List<String> args) throws IllegalArgumentException {
			return Version.parseGetVersion(invoker, args);
		}

	}),

	GET_MOTD("ms.motd.get", new ChatCommand() {
		@ChatCommandSyntax(name = "motd", description = "Gets the message of the day.", arguments = "")
		public Message serializeCommand(Uuid invoker, List<String> args) throws IllegalArgumentException {
			return Motd.parseGetMotd(invoker, args);
		}
	}),
	SET_MOTD("ms.motd.set", new ChatCommand() {
		@ChatCommandSyntax(name = "setmotd", description = "Sets the MOTD permanently.", arguments = "<message>")
		public Message serializeCommand(Uuid invoker, List<String> args) throws IllegalArgumentException {
			return Motd.parseSetMotd(invoker, args);
		}
	}),
	RELOAD_MOTD("ms.motd.reload", new ChatCommand() {
		@ChatCommandSyntax(name = "reloadmotd", description = "Reloads the MOTD to file.", arguments = "")
		public Message serializeCommand(Uuid invoker, List<String> args) throws IllegalArgumentException {
			return Motd.parseReloadMotd(invoker, args);
		}
	}),

	BAN_PLAYER("ms.admin.ban", new ChatCommand() {
		@ChatCommandSyntax(name = "ban", description = "Bans a player.", arguments = "<player/ip> <reason>")
		public Message serializeCommand(Uuid invoker, List<String> args) throws IllegalArgumentException {
			return BanList.parseBanCommand(invoker, args);
		}
	}),
	UNBAN_PLAYER("ms.admin.unban", new ChatCommand() {
		@ChatCommandSyntax(name = "unban", description = "Unbans a player.", arguments = "<ip>")
		public Message serializeCommand(Uuid invoker, List<String> args) throws IllegalArgumentException {
			return BanList.parseUnbanCommand(invoker, args);
		}
	}),

	RELOAD_BANS("ms.util.bans.reload", new ChatCommand() {
		@ChatCommandSyntax(name = "reloadBans", description = "Reloads the ban list from file.", arguments = "")
		public Message serializeCommand(Uuid invoker, List<String> args) throws IllegalArgumentException {
			return BanList.parseReloadBans(invoker, args);
		}
	}),

	PRIVATE_MESSAGE("ms.pm.send", new ChatCommand() {
		@ChatCommandSyntax(name = "pm", description = "Sends a private message to a player.", arguments = "<target> <message>")
		public Message serializeCommand(Uuid invoker, List<String> args) throws IllegalArgumentException {
			return PrivateMessage.parseSendPM(invoker, args);
		}
	}),
	
	PRIVATE_MESSAGE_REPLY("ms.pm.reply", new ChatCommand() {
		@ChatCommandSyntax(name = "r", description = "Replies to the last player who sent you a private message.", arguments = "<message>")
		public Message serializeCommand(Uuid invoker, List<String> args) throws IllegalArgumentException {
			return PrivateMessage.parseReply(invoker, args);
		}
	}),

	LIST_PLAYERS("ms.player.list", new ChatCommand() {
		@ChatCommandSyntax(name = "who", description = "Lists the players who are currently in the master server chat.", arguments = "")
		public Message serializeCommand(Uuid invoker, List<String> args) throws IllegalArgumentException {
			return PrivateMessage.parseReply(invoker, args);
		}
	});

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
