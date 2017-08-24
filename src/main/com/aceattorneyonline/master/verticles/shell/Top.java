package com.aceattorneyonline.master.verticles.shell;

import static org.fusesource.jansi.Ansi.ansi;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;

import org.fusesource.jansi.Ansi;

import com.aceattorneyonline.master.AdvertisedServer;
import com.aceattorneyonline.master.Advertiser;
import com.aceattorneyonline.master.MasterServer;
import com.aceattorneyonline.master.Player;
import com.aceattorneyonline.master.verticles.ClientListVerticle;

import io.vertx.core.cli.annotations.Description;
import io.vertx.ext.shell.command.AnnotatedCommand;
import io.vertx.ext.shell.command.CommandProcess;

class Top extends AnnotatedCommand {
	
	boolean showPlayers = false;
	boolean showServers = false;
	boolean showAll = false;

	@io.vertx.core.cli.annotations.Option(shortName = "p", argName = "show-players", required = false)
	@Description("Include players in the client list")
	public void setShowPlayers(boolean showPlayers) {
		this.showPlayers = showPlayers;
	}

	@io.vertx.core.cli.annotations.Option(shortName = "s", argName = "show-servers", required = false)
	@Description("Include advertised servers in the client list")
	public void setShowServers(boolean showServers) {
		this.showServers = showServers;
	}

	@io.vertx.core.cli.annotations.Option(shortName = "a", argName = "show-all", required = false)
	@Description("Include all clients in the client list")
	public void setShowAll(boolean showAll) {
		this.showAll = showAll;
	}

	@Override
	public void process(CommandProcess process) {
		if (RemoteShell.checkTermSizeError(process)) return;

		ClientListVerticle clv = ClientListVerticle.getSingleton();
		long id = process.vertx().setPeriodic(1000, handler -> {
			if (RemoteShell.checkTermSizeError(process)) return;
			StringBuilder builder = new StringBuilder();
			Ansi ansi = ansi(builder);
			ansi.a(Ansi.Attribute.CONCEAL_ON).cursor(0, 0);

			// Print header
			Duration uptime = Duration.between(MasterServer.START_TIME, Instant.now());
			ansi.format("top - %tT - up %d days %02d:%02d, %2d clients, %2d advertised", Instant.now().toEpochMilli(),
					uptime.toDays(),
					uptime.toHours() % 24,
					uptime.toMinutes() % 60,
					clv.getClientsList().size(),
					clv.getAdvertisersList().size());
			ansi.eraseLine(Ansi.Erase.FORWARD);

			// Print columns
			ansi.newline().a(Ansi.Attribute.NEGATIVE_ON)
					.format("%36s  %35s  %24s  %11s  %1s  %25s", "UUID", "Name", "IP", "Uptime", "A", "Protocol")
					.a(Ansi.Attribute.NEGATIVE_OFF).eraseLine(Ansi.Erase.FORWARD).newline();
			

			boolean showAll = process.commandLine().isFlagEnabled("show-all");
			boolean showPlayers = process.commandLine().isFlagEnabled("show-players");
			boolean showServers = process.commandLine().isFlagEnabled("show-servers");

			showAll = !(showPlayers || showServers); // If no special filter, just show all of them.

			int curRow = 3;
			if (showAll || showPlayers)
				for (Player player : clv.getPlayersList()) {
					if (curRow >= process.height() && process.height() > 0) break;
					ansi.format("%36s  %35s  %24s  %11s  %1s  %25s", player.id(), player.name(), player.address(),
							"", player.hasAdmin() ? "A" : "", player.protocolWriter().getClass().getSimpleName());
					ansi.eraseLine(Ansi.Erase.FORWARD).newline(); curRow++;
				}
			if (showAll || showServers)
				for (Advertiser advertiser : clv.getAdvertisersList().stream()
						.sorted()
						.collect(Collectors.toList())) {
					if (curRow >= process.height() && process.height() > 0) break;
					AdvertisedServer server = advertiser.server();
					if (server != null) {
						Duration advUptime = server.uptime();
						ansi.format("%36s  %35s  %24s  %02d:%02d:%02d:%02d  %1s  %25s", advertiser.id(), server.name(), server.address(),
								advUptime.toDays(), advUptime.toHours() % 24, advUptime.toMinutes() % 60, advUptime.getSeconds() % 60, "",
								advertiser.protocolWriter().getClass().getSimpleName());
						ansi.eraseLine(Ansi.Erase.FORWARD).newline(); curRow++;
					}
				}
			ansi.eraseScreen(Ansi.Erase.FORWARD);
			process.write(builder.toString());
		});

		process.interruptHandler(interrupt -> {
			process.vertx().cancelTimer(id);
			process.write(ansi().reset().toString());
			process.end();
		});

		process.endHandler(handler -> {
			process.vertx().cancelTimer(id);
			process.end();
		});
	}
	
}