package com.aceattorneyonline.master;

import java.lang.reflect.Method;
import java.util.List;

import com.aceattorneyonline.master.events.UuidProto.Uuid;

public interface ChatCommand {

	/**
	 * Serializes a chat command into a protobuf object. However, since protobufs
	 * don't have a common class, it returns a type of Object instead. The protobuf
	 * is then to be passed to the event bus by the chat command parser.
	 * 
	 * @param args
	 *            a list of arguments passed from the chat command
	 * @return protobuf object.
	 * @throws IllegalArgumentException
	 */
	public Object serializeCommand(Uuid invoker, List<String> args) throws IllegalArgumentException;

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
