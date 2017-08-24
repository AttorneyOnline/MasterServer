package com.aceattorneyonline.master.verticles.shell;

import java.io.PrintWriter;
import java.io.StringWriter;

import io.vertx.core.Verticle;
import io.vertx.core.cli.annotations.Argument;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.impl.VertxInternal;
import io.vertx.ext.shell.command.AnnotatedCommand;
import io.vertx.ext.shell.command.CommandProcess;

@Name("verticle-redeploy")
@Summary("Redeploy a verticle")
public class VerticleRedeploy extends AnnotatedCommand {

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
		Verticle verticle = vertx.getDeployment(id).getVerticles().iterator().next();
		vertx.undeploy(id, result -> {
			if (result.succeeded()) {
				String name = verticle.getClass().getCanonicalName();
				process.write(String.format("Undeployed %s (%s)...\n", id, name));
				vertx.deployVerticle(name, result2 -> {
					if (result2.succeeded()) {
						process.write(String.format("Redeployed %s successfully.\n", name)).end();
					} else {
						process.write(String.format("Could not redeploy %s!", id));
						StringWriter buf = new StringWriter();
						PrintWriter writer = new PrintWriter(buf);
						result2.cause().printStackTrace(writer);
						process.write(buf.toString()).end(1);
					}
				});
			} else {
				process.write(String.format("Could not undeploy %s!", id));
				StringWriter buf = new StringWriter();
				PrintWriter writer = new PrintWriter(buf);
				result.cause().printStackTrace(writer);
				process.write(buf.toString()).end(1);
			}
		});
	}
	
}