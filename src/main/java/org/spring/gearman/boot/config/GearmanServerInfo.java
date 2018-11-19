package org.spring.gearman.boot.config;

public class GearmanServerInfo {

	private String ip = "127.0.0.1";

	private int port = 4730;

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
