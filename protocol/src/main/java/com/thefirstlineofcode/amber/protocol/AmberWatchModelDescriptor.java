package com.thefirstlineofcode.amber.protocol;

import com.thefirstlineofcode.sand.protocols.thing.IThingModelDescriptor;
import com.thefirstlineofcode.sand.protocols.thing.SimpleThingModelDescriptor;

public class AmberWatchModelDescriptor extends SimpleThingModelDescriptor implements IThingModelDescriptor {
	public static final String MODEL_NAME = "Amber-Watch";
	public static final String DESCRIPTION = "Amber smart watch";

	public AmberWatchModelDescriptor() {
		super(MODEL_NAME, DESCRIPTION, null);
	}

}
