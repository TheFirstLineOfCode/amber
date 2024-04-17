package com.thefirstlineofcode.amber.bridge;

public class HostConfiguration {
	private String host;
	private int port;
	private boolean tlsRequired;
	private String thingName;
	private String credentials;
	
	public HostConfiguration() {
		this(null);
	}
	
	public HostConfiguration(String host) {
		this(host, 6222, false);
	}
	
	public HostConfiguration(String host, int port, boolean tlsRequired) {
		this.host = host;
		this.port = port;
		this.tlsRequired = tlsRequired;
	}
	
	public String getHost() {
		return host;
	}
	
	public void setHost(String host) {
		this.host = host;
	}
	
	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public boolean isTlsRequired() {
		return tlsRequired;
	}
	
	public void setTlsRequired(boolean tlsRequired) {
		this.tlsRequired = tlsRequired;
	}
	
	public String getThingName() {
		return thingName;
	}
	
	public void setThingName(String thingName) {
		this.thingName = thingName;
	}
	
	public String getCredentials() {
		return credentials;
	}
	
	public void setCredentials(String credentials) {
		this.credentials = credentials;
	}
}
