package com.aceatorneyonline.master.verticles;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

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

}
