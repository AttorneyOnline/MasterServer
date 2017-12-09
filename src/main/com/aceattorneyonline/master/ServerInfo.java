package com.aceattorneyonline.master;

public class ServerInfo {

	private final String name;
	private final String description;
	private final String version;

	public ServerInfo(String name, String description, String version) {
		this.name = name;
		this.description = description;
		this.version = version;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getVersion() {
		return version;
	}

}
