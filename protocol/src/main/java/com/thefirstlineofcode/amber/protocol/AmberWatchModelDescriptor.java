package com.thefirstlineofcode.amber.protocol;

import java.util.HashMap;
import java.util.Map;

import com.thefirstlineofcode.basalt.xmpp.core.Protocol;
import com.thefirstlineofcode.sand.protocols.thing.IThingModelDescriptor;
import com.thefirstlineofcode.sand.protocols.thing.SimpleThingModelDescriptor;

public class AmberWatchModelDescriptor extends SimpleThingModelDescriptor implements IThingModelDescriptor {
	public static final String MODEL_NAME = "Amber-Watch";
	public static final String DESCRIPTION = "Amber smart watch";

	public AmberWatchModelDescriptor() {
		super(MODEL_NAME, DESCRIPTION, false, null, createSupportedData(),
				createSupportedActions(), createSupportedActionResults());
	}
	
	private static Map<Protocol, Class<?>> createSupportedData() {
		Map<Protocol, Class<?>> supportedData = new HashMap<>();
		supportedData.put(HeartRate.PROTOCOL, HeartRate.class);
		
		return supportedData;
	}
	
	private static Map<Protocol, Class<?>> createSupportedActions() {
		Map<Protocol, Class<?>> supportedActions = new HashMap<>();
		supportedActions.put(QueryWatchState.PROTOCOL, QueryWatchState.class);
		supportedActions.put(Message.PROTOCOL, Message.class);
		
		return supportedActions;
	}
	
	private static Map<Protocol, Class<?>> createSupportedActionResults() {
		Map<Protocol, Class<?>> supportedActionResults = new HashMap<>();
		supportedActionResults.put(WatchState.PROTOCOL, WatchState.class);
		
		return supportedActionResults;
	}
}
