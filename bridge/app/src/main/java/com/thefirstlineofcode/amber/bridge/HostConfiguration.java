package com.thefirstlineofcode.amber.bridge;

public class HostConfiguration {
	private String host;
	private int port;
	private boolean tlsRequired;
	private String thingName;
	private String thingCredentials;
	
	public HostConfiguration(String host) {
		this(host, 6222, false);
	}
	
	public HostConfiguration(HostConfiguration hostConfiguration) {
		this.host = hostConfiguration.getHost();
		this.port = hostConfiguration.getPort();
		this.tlsRequired = hostConfiguration.isTlsRequired();
		this.thingName = hostConfiguration.getThingName();
		this.thingCredentials = hostConfiguration.getThingCredentials();
	}
	
	public HostConfiguration(String host, int port, boolean tlsRequired) {
		if (host == null)
			throw new IllegalArgumentException("Null host.");
		
		this.host = host;
		this.port = port;
		this.tlsRequired = tlsRequired;
		thingName = null;
		thingCredentials = null;
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
	
	public String getThingCredentials() {
		return thingCredentials;
	}
	
	public void setThingCredentials(String thingCredentials) {
		this.thingCredentials = thingCredentials;
	}
}
