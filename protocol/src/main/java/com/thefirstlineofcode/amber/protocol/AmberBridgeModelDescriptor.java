package com.thefirstlineofcode.amber.protocol;

import java.util.HashMap;
import java.util.Map;

import com.thefirstlineofcode.basalt.xmpp.core.Protocol;
import com.thefirstlineofcode.sand.protocols.thing.SimpleThingModelDescriptor;

public class AmberBridgeModelDescriptor extends SimpleThingModelDescriptor {
	public static final String MODEL_NAME = "Amber-Bridge";
	public static final String DESCRIPTION = "Amber bridge companion app";

	public AmberBridgeModelDescriptor() {
		super(MODEL_NAME, DESCRIPTION, true, null, createSupportedData(), createSupportedActions());
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
}
