package com.aceattorneyonline.master;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientVersion {
	
	private static final Logger logger = LoggerFactory.getLogger(ClientVersion.class);
	
	private static final String VERSION_FILE = "client_version.txt";

	/**
	 * Gets the latest version of the Attorney Online client.
	 * 
	 * @return the version as read from <tt>VERSION_FILE</tt>, or 0.0.0 if unable to
	 *         read the file.
	 */
	public static String getLatestAOVersion() {
		try {
			return Files.readAllLines(Paths.get(VERSION_FILE)).get(0);
		} catch (IOException | IndexOutOfBoundsException e) {
			logger.warn("Failed to get latest version from " + VERSION_FILE, e);
			return "0.0.0";
		}
	}

}
