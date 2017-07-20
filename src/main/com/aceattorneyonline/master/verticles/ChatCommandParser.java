package com.aceattorneyonline.master.verticles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.ChatCommand;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;

public class ChatCommandParser extends AbstractVerticle {
	
	private static final Logger logger = LoggerFactory.getLogger(ChatCommandParser.class);
	
	private final Map<String, ChatCommand> commands = new HashMap<>();

	@Override
	public void start() throws Exception {
		logger.info("Chat parser verticle starting");
	}
	
	@Override
	public void stop() {
		logger.info("Chat parser verticle stopping");
	}
	
	public void handleChatDispatch(Message<String> event) {
		
	}

	/**
	 * Tokenizes a chat message as if it were a command.
	 * 
	 * This parser can interpret quotation marks (for arguments containing spaces)
	 * as well as escaped quotation marks.
	 * 
	 * @param message  the message to be parsed
	 * @return a list of tokens, including the chat command sent with the prefix ('!', '/', ...)
	 */
	public static List<String> parseChatCommand(String message) {
		List<String> tokens = new ArrayList<String>();
		
		// Trim the chat command itself out
		int cmdEnd = message.indexOf(' ');
		tokens.add(message.substring(0, cmdEnd));
		message = message.substring(cmdEnd + 1).trim(); // Trim again as there might be multiple separating spaces
		
		// https://stackoverflow.com/a/366239
		// Group 2: quoted strings
		// Group 3: unquoted individual words (but won't be inside a Group 2 match)
		final Pattern regex = Pattern.compile("(?:(['\"])(.*?)(?<!\\\\)(?>\\\\\\\\)*\\1|([^\\s]+))");
		Matcher matcher = regex.matcher(message);
		while (matcher.find()) {
			if (matcher.group(2) != null) {
				tokens.add(matcher.group(2));
			} else if (matcher.group(3) != null) {
				tokens.add(matcher.group(3));
			}
		}
		return tokens;
	}

}
