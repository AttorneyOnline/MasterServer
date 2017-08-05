package com.aceattorneyonline.master.verticles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.aceattorneyonline.master.events.Events;
import com.aceattorneyonline.master.verticles.Chat;

public class ChatTest {

	@Test
	public void testParseChatCommandSimple() {
		String command = "!echo foo bar baz keke";
		String[] expected = new String[] {"!echo", "foo", "bar", "baz", "keke"};
		List<String> actual = Chat.parseChatCommand(command);
		assertEquals(Arrays.asList(expected), actual);
	}
	
	@Test
	public void testParseChatCommandQuoted() {
		String command = "!echo foo \"bar baz keke\"";
		String[] expected = new String[] {"!echo", "foo", "bar baz keke"};
		List<String> actual = Chat.parseChatCommand(command);
		assertEquals(Arrays.asList(expected), actual);
	}
	
	@Test
	public void testParseChatCommandApostrophe() {
		String command = "!motd \"It's something of the\" past.";
		String[] expected = new String[] {"!motd", "It's something of the", "past."};
		List<String> actual = Chat.parseChatCommand(command);
		assertEquals(Arrays.asList(expected), actual);
	}
	
	@Test
	public void testParseChatCommandFloatingQuotes() {
		String command = "!motd Why would you \" even do this \"?";
		String[] expected = new String[] {"!motd", "Why", "would", "you", " even do this ", "?"};
		List<String> actual = Chat.parseChatCommand(command);
		assertEquals(Arrays.asList(expected), actual);
	}
	
	@Test
	public void testParseChatCommandSingleQuotes() {
		String command = "!motd 'Does this work well?' or not?";
		String[] expected = new String[] {"!motd", "Does this work well?", "or", "not?"};
		List<String> actual = Chat.parseChatCommand(command);
		assertEquals(Arrays.asList(expected), actual);
	}
	
	@Test
	public void testRetrieveAllChatCommands() {
		//System.out.println(Arrays.stream(Events.values()).filter(e -> e.getChatCommand() != null).collect(Collectors.toMap(e -> e.getChatCommand().getSyntax(), e -> e.getChatCommand())));
		/*
		Map<String, Events> cmds = new HashMap<>();
		for (Events event : Events.values()) {
			if (event.getChatCommand() != null) {
				//System.out.println(event.getChatCommand().getSyntax());
				if (event.getChatCommand().getSyntax() != null)
					cmds.put(event.getChatCommand().getSyntax().name(), event);
			}
		}
		System.out.println(cmds);
		*/
		assertFalse(Events.getAllChatCommands().size() == 0);
	}

}
