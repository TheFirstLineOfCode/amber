package com.thefirstlineofcode.amber.protocol;

import com.thefirstlineofcode.basalt.oxm.coc.annotations.ProtocolObject;
import com.thefirstlineofcode.basalt.xmpp.core.Protocol;

@ProtocolObject(namespace="urn:leps:things:amber", localName="heart-rate")
public class HeartRate {
	public static final Protocol PROTOCOL = new Protocol("urn:leps:things:amber", "heart-rate");
	
	private int value;
	
	public HeartRate() {
		this(0);
	}
	
	public HeartRate(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}
}
