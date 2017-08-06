package com.aceattorneyonline.master.verticles;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.ChatCommandSyntax;
import com.aceattorneyonline.master.Client;
import com.aceattorneyonline.master.Player;
import com.aceattorneyonline.master.events.AdminEventProtos.BanPlayer;
import com.aceattorneyonline.master.events.AdminEventProtos.ReloadBans;
import com.aceattorneyonline.master.events.AdminEventProtos.UnbanPlayer;
import com.aceattorneyonline.master.events.EventErrorReason;
import com.aceattorneyonline.master.events.Events;
import com.aceattorneyonline.master.events.UuidProto.Uuid;
import com.google.protobuf.InvalidProtocolBufferException;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class BanList extends ClientListVerticle {

	private static final Logger logger = LoggerFactory.getLogger(BanList.class);

	private final Set<Ban> banList = new TreeSet<>();

	@Override
	public void start() {
		logger.info("Bans list verticle starting");

		EventBus eventBus = getVertx().eventBus();
		eventBus.consumer(Events.BAN_PLAYER.getEventName(), this::handleBanClient);
		eventBus.consumer(Events.UNBAN_PLAYER.getEventName(), this::handleUnbanClient);
		eventBus.consumer(Events.RELOAD_BANS.getEventName(), this::handleReloadBans);
		// FIXME: read ban list file
	}

	@Override
	public void stop() {
		logger.info("Bans list verticle stopping");
	}

	/**
	 * Bans or modifies the ban of a client by a free-form query, by first looking
	 * up players with the matching query string, then validating if the string is a
	 * valid IP address. If there are multiple matching names or no names were
	 * found, the request is canceled. Otherwise, the ban reason is parsed and the
	 * ban is added/updated.
	 */
	public void handleBanClient(Message<byte[]> event) {
		try {
			BanPlayer ban = BanPlayer.parseFrom(event.body());
			UUID id = UUID.fromString(ban.getId().getId());
			Player requestingPlayer = getPlayerById(id);
			String targetText = ban.getTarget();

			logger.info("{}: User is requesting to ban {}", id.toString(), targetText);

			if (requestingPlayer == null) {
				event.fail(EventErrorReason.SECURITY_ERROR, "Requester is not a player.");
				return;
			} else if (!requestingPlayer.hasAdmin()) {
				event.fail(EventErrorReason.SECURITY_ERROR, "Requester does not have admin rights.");
				return;
			}

			// Try parsing by name first.
			// If we parse by IP address first, an attacker could set their username
			// to an IP address that isn't their own, and thus it would become more
			// difficult to ban in general, as one would have to look up the real IP address
			// of the attacker.
			Player target;
			Collection<Player> targets = searchPlayerByNameFuzzy(targetText);
			int targetsSize = targets.size();
			if (targetsSize > 1) {
				event.fail(EventErrorReason.USER_ERROR, "Ambiguous result; please refine your search.");
			} else if (targetsSize == 1) {
				target = targets.iterator().next(); // Seems to be better than using a Stream and an Optional...
				target.protocolWriter().sendSystemMessage("You have been banned. Reason: " + ban.getReason());
				banPlayer(new Ban(target.address().host(), target.name(), ban.getReason()));
				target.socket().end();
				event.reply("Successfully banned " + target.toString() + " (" + target.address().host() + ")");
			} else {
				// No targets found by name...
				// Try parsing by IP address
				logger.debug("Target client not found by name. Trying by IP address instead");
				IPAddress address = new IPAddressString(targetText).getAddress();
				if (address != null) {
					String addressString = address.toCanonicalString();
					// Find clients that match this IP address and kick them
					Collection<Client> onlineMatchingClients = searchClientByAddress(addressString);
					String banName = "";
					for (Client matchingClient : onlineMatchingClients) {
						if (matchingClient instanceof Player) {
							// HACK: get player name
							banName = ((Player) matchingClient).name();
						}
						matchingClient.protocolWriter()
								.sendBanNotification("You have been banned. Reason: " + ban.getReason());
						matchingClient.socket().end();
					}
					banPlayer(new Ban(addressString, banName, ban.getReason()));
					event.reply("Successfully banned " + addressString);
				} else {
					event.fail(EventErrorReason.USER_ERROR, "No players found to ban.");
				}
			}
		} catch (InvalidProtocolBufferException e) {
			event.fail(EventErrorReason.INTERNAL_ERROR, "Could not parse BanPlayer protobuf");
		}
	}

	@ChatCommandSyntax(name = "ban", description = "Bans a player.", arguments = "<player/ip> <reason>")
	public static com.google.protobuf.Message parseBanCommand(Uuid invoker, List<String> args)
			throws IllegalArgumentException {
		try {
			if (args.size() >= 2) {
				String reason = String.join(" ", args.subList(1, args.size()));
				return BanPlayer.newBuilder().setId(invoker).setTarget(args.get(0)).setReason(reason).build();
			} else if (args.size() == 1) {
				return BanPlayer.newBuilder().setId(invoker).setTarget(args.get(0)).build();
			} else {
				throw new IllegalArgumentException();
			}
		} catch (IndexOutOfBoundsException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public void handleUnbanClient(Message<byte[]> event) {
		try {
			UnbanPlayer ban = UnbanPlayer.parseFrom(event.body());
			UUID id = UUID.fromString(ban.getId().getId());
			Player requestingPlayer = getPlayerById(id);
			String targetText = ban.getTarget();

			logger.info("{}: User is requesting to unban {}", requestingPlayer, targetText);

			if (requestingPlayer == null) {
				event.fail(EventErrorReason.SECURITY_ERROR, "Requester is not a player.");
				return;
			} else if (!requestingPlayer.hasAdmin()) {
				event.fail(EventErrorReason.SECURITY_ERROR, "Requester does not have admin rights.");
				return;
			}

			// Try parsing by IP address
			IPAddress address = new IPAddressString(targetText).getAddress();
			if (address != null) {
				String addressString = address.toCanonicalString();
				if (unbanPlayer(addressString)) {
					event.reply("Successfully unbanned " + addressString);
				} else {
					event.fail(EventErrorReason.USER_ERROR, "The specified IP address was not found in the ban list.");
				}
			} else {
				event.fail(EventErrorReason.INTERNAL_ERROR, "The specified IP address could not be parsed.");
			}
		} catch (InvalidProtocolBufferException e) {
			event.fail(EventErrorReason.INTERNAL_ERROR, "Could not parse UnbanPlayer protobuf");
		}
	}

	@ChatCommandSyntax(name = "unban", description = "Unbans a player.", arguments = "<ip>")
	public static com.google.protobuf.Message parseUnbanCommand(Uuid invoker, List<String> args)
			throws IllegalArgumentException {
		try {
			if (args.size() == 1) {
				return UnbanPlayer.newBuilder().setId(invoker).setTarget(args.get(0)).build();
			} else {
				throw new IllegalArgumentException();
			}
		} catch (IndexOutOfBoundsException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public void handleReloadBans(Message<byte[]> event) {
		event.fail(EventErrorReason.INTERNAL_ERROR, "not implemented"); // FIXME handleReloadBans
	}

	@ChatCommandSyntax(name = "reloadBans", description = "Reloads the ban list from file.", arguments = "")
	public static com.google.protobuf.Message parseReloadBans(Uuid invoker, List<String> args)
			throws IllegalArgumentException {
		return ReloadBans.newBuilder().setId(invoker).build();
	}

	public void banPlayer(Ban playerBan) {
		synchronized (banList) {
			banList.add(playerBan);
		}
		logger.info("Added ban for {} ({}) with reason {}", playerBan.ipAddress, playerBan.name, playerBan.reason);
	}

	public boolean unbanPlayer(String ipAddress) {
		boolean success;
		synchronized (banList) {
			success = banList.removeIf(ban -> ban.ipAddress.equals(ipAddress));
		}
		if (success) {
			logger.info("Removed ban for IP address {}", ipAddress);
		} else {
			logger.info("Could not unban IP address {} because it was not found in the ban list", ipAddress);
		}
		return success;
	}

	private static class Ban {
		private String ipAddress, name, reason;
		private Instant timeBanned;

		public Ban(String ipAddress, String name, String reason, Instant timeBanned) {
			this.ipAddress = ipAddress;
			this.name = name;
			this.reason = reason;
			this.timeBanned = timeBanned;
		}

		public Ban(String ipAddress, String name, String reason) {
			this(ipAddress, name, reason, Instant.now());
		}

		public String toString() {
			return String.format("%s (%s) - since %s - %s", ipAddress, name,
					DateTimeFormatter.RFC_1123_DATE_TIME.format(timeBanned), reason);
		}

	}

}
