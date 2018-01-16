package com.aceattorneyonline.master.verticles;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.ChatCommandSyntax;
import com.aceattorneyonline.master.Player;
import com.aceattorneyonline.master.events.EventErrorReason;
import com.aceattorneyonline.master.events.Events;
import com.aceattorneyonline.master.events.PlayerEventProtos.ReplyPM;
import com.aceattorneyonline.master.events.PlayerEventProtos.SendPM;
import com.aceattorneyonline.master.events.UuidProto.Uuid;
import com.google.protobuf.InvalidProtocolBufferException;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class PrivateMessage extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(PrivateMessage.class);

	// When we use a WeakHashMap, we no longer have to be concerned about
	// de-registering our players once clients have disconnected. They are just GC'd
	// and struck off this map.
	// Note, however, that WeakHashMaps hold weak references to keys, not values. If
	// we come across a bad value, we should strike it off.
	private Map<Player, Player> lastRecipient = new WeakHashMap<Player, Player>();

	@Override
	public void start() {
		logger.info("Private message verticle starting");

		EventBus eventBus = getVertx().eventBus();
		eventBus.consumer(Events.PRIVATE_MESSAGE.getEventName(), this::handleSendPM);
		eventBus.consumer(Events.PRIVATE_MESSAGE_REPLY.getEventName(), this::handleReply);
	}

	@Override
	public void stop() {
		logger.info("Private message verticle stopping");
	}

	public void handleSendPM(Message<byte[]> event) {
		try {
			SendPM pm = SendPM.parseFrom(event.body());
			UUID id = UUID.fromString(pm.getId().getId());
			ClientServerList masterList = ClientServerList.getSingleton();
			Player sender = masterList.getPlayerById(id);
			String targetText = pm.getTarget();

			if (sender == null) {
				event.fail(EventErrorReason.SECURITY_ERROR, "Sender is not a player.");
				return;
			} else if (sender.name() == null || sender.name().isEmpty()) {
				event.fail(EventErrorReason.SECURITY_ERROR, "Anonymous PMs are not allowed.");
				return;
			}

			Player target;
			Collection<Player> targets = masterList.searchPlayerByNameFuzzy(targetText);
			int targetsSize = targets.size();
			if (targetsSize > 1) {
				event.fail(EventErrorReason.USER_ERROR,
						"Ambiguous result; use quotes if the name has spaces.");
			} else if (targetsSize == 1) {
				target = targets.iterator().next();
				target.protocolWriter().sendChatMessage(
						"PM from " + sender.name(), pm.getMessage());
				lastRecipient.put(target, sender);
				// XXX: PMs not very private are they? All I need to do now is forward
				// them to the NSA
				logger.info("{} to {} via PM: {}", sender, target, pm.getMessage());
				event.reply(null);
			} else {
				event.fail(EventErrorReason.USER_ERROR, "No player found to PM.");
			}
		} catch (InvalidProtocolBufferException e) {
			event.fail(EventErrorReason.INTERNAL_ERROR, "Could not parse SendPM protobuf");
		}
	}

	@ChatCommandSyntax(name = "pm", description = "Sends a private message to a player.", arguments = "<target> <message>")
	public static com.google.protobuf.Message parseSendPM(Uuid invoker, List<String> args)
			throws IllegalArgumentException {
		try {
			if (args.size() >= 2) {
				String message = String.join(" ", args.subList(1, args.size()));
				return SendPM.newBuilder().setId(invoker).setTarget(args.get(0)).setMessage(message).build();
			} else {
				throw new IllegalArgumentException();
			}
		} catch (IndexOutOfBoundsException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public void handleReply(Message<byte[]> event) {
		try {
			ReplyPM pm = ReplyPM.parseFrom(event.body());
			UUID id = UUID.fromString(pm.getId().getId());
			ClientServerList masterList = ClientServerList.getSingleton();
			Player sender = masterList.getPlayerById(id);
			Player target = lastRecipient.get(sender);
			if (target != null) {
				target.protocolWriter().sendChatMessage("PM from " + sender.name(), pm.getMessage());
				lastRecipient.put(target, sender);
				logger.info("{} to {} via PM: {}", sender, target, pm.getMessage());
				event.reply(null);
			} else {
				event.fail(EventErrorReason.USER_ERROR, "That player does not exist anymore.");
			}
		} catch (InvalidProtocolBufferException e) {
			event.fail(EventErrorReason.INTERNAL_ERROR, "Could not parse ReplyPM protobuf");
		}
	}

	@ChatCommandSyntax(name = "r", description = "Replies to the last player who sent you a private message. If no message is sent, an acknowledgment is sent.", arguments = "[message]")
	public static com.google.protobuf.Message parseReply(Uuid invoker, List<String> args)
			throws IllegalArgumentException {
		if (args.size() >= 1) {
			String message = String.join(" ", args);
			return ReplyPM.newBuilder().setId(invoker).setMessage(message).build();
		} else {
			return ReplyPM.newBuilder().setId(invoker).setMessage("").build();
		}
	}

}
