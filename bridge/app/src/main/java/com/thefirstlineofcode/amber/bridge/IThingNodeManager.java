package com.thefirstlineofcode.amber.bridge;

import java.util.List;

public interface IThingNodeManager {
	public interface Listener {
		void thingAdded(IBleThing thing);
		void nodeAdded(String thingId, int lanId);
	}
	
	List<ThingNode> getThingNodes();
	void addThing(IBleThing thing);
	boolean nodeAdded(String thingId, int lanId);
	void addThingNodeListener(Listener listener);
	boolean removeThingNodeListener(Listener listener);
	void saveThingNodes();
}
