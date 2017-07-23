package com.aceattorneyonline.master;

import java.lang.reflect.Method;
import java.util.List;

import com.aceattorneyonline.master.events.Events;
import com.aceattorneyonline.master.events.UuidProto.Uuid;
import com.google.protobuf.Message;

/**
 * Represents a chat command that can be invoked by the chat command parser.
 * 
 * Each chat command may correspond to one internal event passed through the
 * event bus, each of which is registered in {@link Events}. The chat command
 * parser then receives a full list of chat commands from the events list and
 * can know what event and parser corresponds with the chat command, so that the
 * arguments can be converted into a protobuf and sent as an event.
 */
public interface ChatCommand {

	/**
	 * Serializes a chat command into a protobuf object. The protobuf is then to be
	 * passed to the event bus by the chat command parser.
	 * 
	 * @param args
	 *            a list of arguments passed from the chat command, excluding the
	 *            token of the command name
	 * @return protobuf object.
	 * @throws IllegalArgumentException
	 */
	public Message serializeCommand(Uuid invoker, List<String> args) throws IllegalArgumentException;

	/**
	 * Gets the syntax of the chat command itself. This doesn't work if the method
	 * is thrown in a method reference because it's just syntactic sugar for a
	 * lambda, and the lambda doesn't copy any of the annotations.
	 */
	public default ChatCommandSyntax getSyntax() {
		try {
			Method serializeMethod = getClass().getMethod("serializeCommand", Uuid.class, List.class);
			ChatCommandSyntax syntax = serializeMethod.getAnnotation(ChatCommandSyntax.class);
			return syntax;
		} catch (NoSuchMethodException | SecurityException e) {
			return null;
		}
	}

}
