package com.thefirstlineofcode.amber.bridge;

import com.thefirstlineofcode.sand.protocols.thing.RegisteredThing;

public interface IIotBgService {
	HostConfiguration getHostConfiguration();
	RegisteredThing register();
	boolean isRegistered();
	void connectToHost();
}
