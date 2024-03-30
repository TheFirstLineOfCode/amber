package com.thefirstlineofcode.amber.bridge;

import java.util.List;

public interface ILanNodeManager {
	public interface Listener {
		void deviceAdded(Device device);
		void nodeAdded(String thingId, int lanId);
	}
	
	List<LanNode> getLanNodes();
	void addDevice(Device device);
	void nodeAdded(String thingId, int lanId);
	void addListener(Listener listener);
	boolean removeListener(Listener listener);
	void save();
}
