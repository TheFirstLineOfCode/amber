package com.thefirstlineofcode.amber.bridge;

import com.thefirstlineofcode.chalk.core.AuthFailureException;
import com.thefirstlineofcode.chalk.network.ConnectionException;
import com.thefirstlineofcode.sand.protocols.thing.RegisteredEdgeThing;

public interface IIotBgService {
	public interface IEdgeThingStateistener {
		void edgeThingRegistered(RegisteredEdgeThing registeredEdgeThing);
		void connectionExceptionOccurred(ConnectionException exception);
		void hostConnected();
	}
	
	HostConfiguration getHostConfiguration();
	void registerEdgeThing();
	boolean isEdgeThingRegistered();
	void connectToHost();
	void disconnectFromHost();
	boolean isConnectedToHost();
	void startMonitorTask();
	void stopMonitorTask();
	boolean isMonitorTaskStarted();
	void addEdgeThingStateListener(IEdgeThingStateistener listener);
	boolean removeEdgeThingStateListener(IEdgeThingStateistener listener);
}
