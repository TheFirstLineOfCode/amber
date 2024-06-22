package com.thefirstlineofcode.amber.protocol;

import com.thefirstlineofcode.basalt.oxm.coc.annotations.ProtocolObject;
import com.thefirstlineofcode.basalt.xmpp.core.Protocol;

@ProtocolObject(namespace="urn:leps:things:amber", localName="watch-state")
public class WatchState {
	public static final Protocol PROTOCOL = new Protocol("urn:leps:things:amber", "watch-state");
	
	private String address;
	private int batteryLevel;
	private int stepCount;
	
	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getBatteryLevel() {
		return batteryLevel;
	}
	
	public void setBatteryLevel(int batteryLevel) {
		this.batteryLevel = batteryLevel;
	}
	
	public int getStepCount() {
		return stepCount;
	}
	
	public void setStepCount(int stepCount) {
		this.stepCount = stepCount;
	}
}
