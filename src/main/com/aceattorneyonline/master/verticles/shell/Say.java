package com.aceattorneyonline.master.verticles.shell;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.events.EventErrorReason;
import com.aceattorneyonline.master.events.Events;
import com.aceattorneyonline.master.events.PlayerEventProtos.SendChat;
import com.aceattorneyonline.master.events.UuidProto.Uuid;

import io.vertx.core.cli.annotations.Argument;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.ext.shell.command.AnnotatedCommand;
import io.vertx.ext.shell.command.CommandProcess;

@Name("say")
@Summary("Sends a message to the server chat")
public class Say extends AnnotatedCommand {
	private static final Logger logger = LoggerFactory.getLogger(AnnotatedCommand.class);

	@Argument(index = 0, argName = "message", required = true)
	@Description("The chat message to send")
	public void setMessage(String message) {
		// Not actually used - it could be multiple words, so we put the args together
		// when the process is actually invoked.
	}

	@Override
	public void process(CommandProcess process) {
		String chatMessage = process.args().stream().collect(Collectors.joining(" "));
		process.vertx().eventBus().<String>send(Events.SEND_CHAT.getEventName(),
				SendChat.newBuilder().setId(Uuid.newBuilder().setId(ShellPlayer.getSingleton().id().toString()).build())
						.setUsername("System").setMessage(chatMessage).build().toByteArray(),
				reply -> { // Similar reply handler to those in the protocol handlers
					if (reply.succeeded() && reply.result().body() != null) {
						process.write(reply.result().body());
					} else if (reply.failed()) {
						ReplyException e = (ReplyException) reply.cause();
						int errorCode = e.failureCode();
						String message = e.getMessage();
						switch (errorCode) {
						default: // For unhandled exceptions
						case EventErrorReason.INTERNAL_ERROR:
							process.write("Internal error: " + message);
							logger.error("Shell: Internal error: {}", message);
							break;
						case EventErrorReason.SECURITY_ERROR:
							process.write("Security error: " + message);
							logger.warn("Shell: Security error: {}", message);
							break;
						case EventErrorReason.USER_ERROR:
							process.write("User error: " + message);
							logger.info("Shell: User error: {}", message);
							break;
						}
					}
					process.write("\n");
					process.end(reply.succeeded() ? 0 : 1);
				});
	}
}