package com.thefirstlineofcode.amber.bridge;

public interface IHostConfigurationManager {
	String[] getAvailableHosts();
	void setCurrentHost(String host);
	String getCurrentHost();
	HostConfiguration getHostConfiguration(String host);
	int findHostConfiguration(String host);
	void addHostConfiguration(HostConfiguration hostConfiguration);
	void updateHostConfiguration(HostConfiguration hostConfiguration);
	boolean isHostConfigurationsChanged();
	void saveHostConfigurations();
}
