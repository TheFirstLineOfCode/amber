package com.thefirstlineofcode.amber.bridge;

import com.thefirstlineofcode.chalk.core.AuthFailureException;
import com.thefirstlineofcode.chalk.network.ConnectionException;
import com.thefirstlineofcode.sand.protocols.thing.RegisteredEdgeThing;

public interface IIotBgService {
	HostConfiguration getHostConfiguration();
	void registerEdgeThing();
	boolean isEdgeThingRegistered();
	void connectToHost();
	void disconnectFromHost();
	boolean isConnectedToHost();
	int addDeviceAsNode(IBleDevice device);
}
