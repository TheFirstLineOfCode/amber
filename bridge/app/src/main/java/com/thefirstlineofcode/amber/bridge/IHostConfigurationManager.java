package com.thefirstlineofcode.amber.bridge;

public interface IHostConfigurationManager {
	String[] getAvailableHosts();
	HostConfiguration getHostConfiguration(String host);
	void addHostConfiguration(HostConfiguration hostConfiguration);
	void updateHostConfiguration(HostConfiguration hostConfiguration);
}
