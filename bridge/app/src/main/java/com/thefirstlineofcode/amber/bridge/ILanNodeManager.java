package com.thefirstlineofcode.amber.bridge;

public interface ILanNodeManager {
	public interface Listener {
		void thingAdded(IBleThing thing);
		void nodeAdded(String thingId, int lanId);
	}
	
	LanNode[] getLanNodes();
	void addThing(IBleThing thing);
	void nodeAdded(String thingId, int lanId);
	void addListener(Listener listener);
	boolean removeListener(Listener listener);
	void save();
}
