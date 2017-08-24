package com.aceattorneyonline.master.verticles.shell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.Vertx;
import io.vertx.ext.shell.command.AnnotatedCommand;
import io.vertx.ext.shell.command.Command;
import io.vertx.ext.shell.command.CommandResolver;

/**
 * Contains a list of all of the new shell commands implemented specifically for
 * the master server.
 */
public class ShellCommandPack implements CommandResolver {

	private final Vertx vertx;
	private static List<Class<? extends AnnotatedCommand>> commandClassList = new ArrayList<>();
	static {
		commandClassList.add(Top.class);
		commandClassList.add(Say.class);
		commandClassList.add(VerticleRedeploy.class);
		commandClassList = Collections.unmodifiableList(commandClassList);
	}

	public ShellCommandPack(Vertx vertx) {
		this.vertx = vertx;
	}

	@Override
	public List<Command> commands() {
		return commandClassList.stream().map(cmdClass -> Command.create(vertx, cmdClass)).collect(Collectors.toList());
	}

}
