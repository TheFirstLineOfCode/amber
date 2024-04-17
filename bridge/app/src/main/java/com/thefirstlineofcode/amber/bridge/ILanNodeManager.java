package com.thefirstlineofcode.amber.bridge;

public interface ILanNodeManager {
	public interface Listener {
		void thingAdded(IBleThing thing);
		void nodeAdded(String thingId, int lanId);
	}
	
	LanNode[] getLanNodes();
	void addThing(IBleThing thing);
	void nodeAdded(String thingId, int lanId);
	void addLanNodeListener(Listener listener);
	boolean removeLanNodeListener(Listener listener);
	void saveLanNodes();
}
