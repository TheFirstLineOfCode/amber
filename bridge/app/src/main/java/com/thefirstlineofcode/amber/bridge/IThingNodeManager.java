package com.thefirstlineofcode.amber.bridge;

public interface IThingNodeManager {
	public interface Listener {
		void thingAdded(IBleThing thing);
		void nodeAdded(String thingId, int lanId);
	}
	
	ThingNode[] getThingNodes();
	void addThing(IBleThing thing);
	void nodeAdded(String thingId, int lanId);
	void addThingNodeListener(Listener listener);
	boolean removeThingNodeListener(Listener listener);
	void saveThingNodes();
}
