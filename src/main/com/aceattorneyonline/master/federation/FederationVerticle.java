package com.aceattorneyonline.master.federation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import io.vertx.core.AbstractVerticle;

public class FederationVerticle extends AbstractVerticle {
	
	private static final Logger logger = LoggerFactory.getLogger(FederationVerticle.class);
	
	public static final List<IPAddress> hosts = new ArrayList<>();
	public static final String hostsFilename = "federation_hosts.txt";

	public void start() {
		readHosts(hostsFilename);
	}

	private void readHosts(String filename) {
		try {
			Files.readAllLines(Paths.get(filename)).stream().<IPAddress>map((ip) -> {
				IPAddress addr = new IPAddressString(ip).getAddress();
				if (addr != null) {
					return addr;
				} else {
					logger.warn("IP address {} could not be parsed.", ip);
					return null;
				}
			}).collect(Collectors.toList());
		} catch (IOException e) {
			logger.error("Could not read hosts file " + filename, e);
		}
	}

	public void stop() {
		
	}

}
