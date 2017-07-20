package com.aceattorneyonline.master;

import java.lang.reflect.Method;
import java.util.List;

import com.aceattorneyonline.master.events.UuidProto.Uuid;
import com.google.protobuf.Message;

public interface ChatCommand {

	/**
	 * Serializes a chat command into a protobuf object. The protobuf
	 * is then to be passed to the event bus by the chat command parser.
	 * 
	 * @param args
	 *            a list of arguments passed from the chat command, excluding the
	 *            token of the command name
	 * @return protobuf object.
	 * @throws IllegalArgumentException
	 */
	public Message serializeCommand(Uuid invoker, List<String> args) throws IllegalArgumentException;

	/**
	 * Gets the name of the channel that the chat command event should be passed to
	 * in the event bus.
	 */
	// public String getEventChannelName();

	/** Gets the syntax of the chat command. */
	public default ChatCommandSyntax getSyntax() {
		try {
			Method serializeMethod = getClass().getMethod("serializeCommand", Uuid.class, List.class);
			return serializeMethod.getAnnotation(ChatCommandSyntax.class);
		} catch (NoSuchMethodException | SecurityException e) {
			return null;
		}
	}

}
