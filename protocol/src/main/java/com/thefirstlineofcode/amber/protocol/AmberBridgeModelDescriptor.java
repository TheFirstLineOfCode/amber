package com.thefirstlineofcode.amber.protocol;

import com.thefirstlineofcode.sand.protocols.thing.SimpleThingModelDescriptor;

public class AmberBridgeModelDescriptor extends SimpleThingModelDescriptor {
	public static final String MODEL_NAME = "Amber-Bridge";
	public static final String DESCRIPTION = "Amber bridge companion app";

	public AmberBridgeModelDescriptor() {
		super(MODEL_NAME, DESCRIPTION, true, null);
	}
}
