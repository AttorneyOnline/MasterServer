package com.aceattorneyonline.master.verticles.shell;

import static org.fusesource.jansi.Ansi.ansi;

import java.time.Duration;
import java.time.Instant;

import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.AdvertisedServer;
import com.aceattorneyonline.master.MasterServer;
import com.aceattorneyonline.master.Player;
import com.aceattorneyonline.master.verticles.ClientServerList;

import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Option;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.ext.shell.command.AnnotatedCommand;
import io.vertx.ext.shell.command.CommandProcess;

@Name("top")
@Summary("Displays a realtime table of connected clients")
public class Top extends AnnotatedCommand {

	private static final Logger logger = LoggerFactory.getLogger(RemoteShell.class);

	boolean showPlayers = false;
	boolean showServers = false;
	boolean showAll = false;
	
	private int startRow = 3;

	@Option(shortName = "p", longName = "show-players", flag = true)
	@Description("Include players in the client list")
	public void setShowPlayers(boolean showPlayers) {
		this.showPlayers = showPlayers;
	}

	@Option(shortName = "s", longName = "show-servers", flag = true)
	@Description("Include advertised servers in the client list")
	public void setShowServers(boolean showServers) {
		this.showServers = showServers;
	}

	@Option(shortName = "a", longName = "show-all", flag = true)
	@Description("Include all clients in the client list")
	public void setShowAll(boolean showAll) {
		this.showAll = showAll;
	}

	@Override
	public void process(CommandProcess process) {
		logger.info("Starting process top (show-players={}, show-servers={}, show-all={})",
				showPlayers, showServers, showAll);

		if (RemoteShell.checkTermSizeError(process)) return;

		ClientServerList masterList = ClientServerList.getSingleton();
		long id = process.vertx().setPeriodic(1000, handler -> {
			if (RemoteShell.checkTermSizeError(process)) return;
			StringBuilder builder = new StringBuilder();
			Ansi ansi = ansi(builder);
			ansi.a(Ansi.Attribute.CONCEAL_ON).cursor(0, 0);

			// Print header
			Duration uptime = Duration.between(MasterServer.START_TIME, Instant.now());
			ansi.format("top - %tT - up %d days %02d:%02d, %2d players, %2d advertisers, %2d servers", Instant.now().toEpochMilli(),
					uptime.toDays(),
					uptime.toHours() % 24,
					uptime.toMinutes() % 60,
					masterList.getPlayersList().size(),
					masterList.getAdvertisersList().size(),
					masterList.getSortedServerList().size());
			ansi.eraseLine(Ansi.Erase.FORWARD);

			// Print columns
			ansi.newline().a(Ansi.Attribute.NEGATIVE_ON)
					.format("%36s  %35s  %24s  %11s  %1s  %25s", "UUID", "Name", "IP", "Uptime", "A", "Protocol")
					.a(Ansi.Attribute.NEGATIVE_OFF).eraseLine(Ansi.Erase.FORWARD).newline();

			showAll = !(showPlayers || showServers); // If no special filter, just show all of them.

			int curRow = 3;
			if (showAll || showPlayers)
				for (Player player : masterList.getPlayersList()) {
					if (curRow >= process.height() && process.height() > 0)
						break;
					if (curRow < startRow)
						continue;
					ansi.format("%36s  %35s  %24s  %11s  %1s  %25s", player.id(), player.name(), player.address(),
							"", player.hasAdmin() ? "A" : "", player.protocolWriter().getClass().getSimpleName());
					ansi.eraseLine(Ansi.Erase.FORWARD).newline();
					curRow++;
				}
			if (showAll || showServers)
				for (AdvertisedServer server : masterList.getSortedServerList()) {
					if (curRow >= process.height() && process.height() > 0)
						break;
					if (curRow < startRow)
						continue;
					if (server != null) {
						Duration advUptime = server.uptime();
						ansi.format("%36s  %35s  %24s  %02d:%02d:%02d:%02d  %1s  %25s", "", server.name(), server.address(),
								advUptime.toDays(), advUptime.toHours() % 24, advUptime.toMinutes() % 60, advUptime.getSeconds() % 60, "",
								"Server");
						ansi.eraseLine(Ansi.Erase.FORWARD).newline();
						curRow++;
					}
				}
			ansi.eraseScreen(Ansi.Erase.FORWARD);
			process.write(builder.toString());
		});
		
		process.stdinHandler(stdin -> {
			if (stdin.startsWith("\u001b[") && stdin.length() >= 3) {
				switch (stdin.charAt(2)) {
				case 'A':
					// Move up
					startRow = Math.max(0, startRow - 1);
					break;
				case 'B':
					// Move down
					startRow++;
					break;
				}
			}
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