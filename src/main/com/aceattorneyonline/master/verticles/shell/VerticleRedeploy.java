package com.aceattorneyonline.master.verticles.shell;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Verticle;
import io.vertx.core.cli.annotations.Argument;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.impl.Deployment;
import io.vertx.core.impl.VertxInternal;
import io.vertx.ext.shell.command.AnnotatedCommand;
import io.vertx.ext.shell.command.CommandProcess;

@Name("verticle-redeploy")
@Summary("Redeploy a verticle")
public class VerticleRedeploy extends AnnotatedCommand {

	private static final Logger logger = LoggerFactory.getLogger(VerticleRedeploy.class);

	private String id;

	@Argument(index = 0, argName = "id")
	@Description("The verticle's id")
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public void process(CommandProcess process) {
		// HACK: cast to VertxInternal so we can get verticles by deployment id
		VertxInternal vertx = (VertxInternal) process.vertx();

		Deployment deployment = vertx.getDeployment(id);
		try {
			Verticle verticle = deployment.getVerticles().iterator().next();
			// If the verticle being targeted is the remote shell, then the pipe to stdout
			// WILL break. If we attempt to write to stdout anyway, an unhandled exception
			// will
			// occur and we will not get our shell back.
			final boolean pipeBreaks = verticle.getClass().equals(RemoteShell.class);

			vertx.undeploy(id, result -> {
				if (result.succeeded()) {
					String name = verticle.getClass().getCanonicalName();
					if (!pipeBreaks)
						process.write(String.format("Undeployed %s (%s)...\n", id, name));
					vertx.deployVerticle(name, result2 -> {
						if (result2.succeeded()) {
							if (!pipeBreaks)
								process.write(String.format("Redeployed %s successfully.\n", name));
							process.end();
						} else {
							logger.error("Could not redeploy " + id + " (" + name + ")!", result2.cause());
							if (!pipeBreaks) {
								process.write(String.format("Could not redeploy %s!", id));
								// Write the exception to stdout
								StringWriter buf = new StringWriter();
								PrintWriter writer = new PrintWriter(buf);
								result2.cause().printStackTrace(writer);
								process.write(buf.toString());
							}
							process.end(1);
						}
					});
				} else {
					logger.error("Could not undeploy " + id + "!", result.cause());
					process.write(String.format("Could not undeploy %s!", id));
					// Write the exception to stdout
					StringWriter buf = new StringWriter();
					PrintWriter writer = new PrintWriter(buf);
					result.cause().printStackTrace(writer);
					process.write(buf.toString()).end(1);
				}
			});
		} catch (NullPointerException e) {
			if (deployment == null) {
				logger.warn("Could not find verticle for redeployment: " + id, e);
				process.write("Verticle of id '" + id + "' was not found.\n").end(1);
			} else {
				logger.error("NullPointerException during redeployment of " + id, e);
			}
		}
	}

}