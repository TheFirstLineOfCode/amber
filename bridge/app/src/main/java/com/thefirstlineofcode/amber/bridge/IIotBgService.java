package com.thefirstlineofcode.amber.bridge;

import com.thefirstlineofcode.sand.client.concentrator.IConcentrator;

public interface IIotBgService {
	HostConfiguration getHostConfiguration();
	void registerEdgeThing();
	boolean isEdgeThingRegistered();
	void connectToHost();
	void disconnectFromHost();
	boolean isConnectedToHost();
	IConcentrator getConcentrator();
	void setMainActivity(MainActivity activity);
}
