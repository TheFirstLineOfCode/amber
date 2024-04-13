package com.thefirstlineofcode.amber.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class BleThing implements IBleThing {
	public static final short RSSI_UNKNOWN = 0;
	
	public static final UUID UUID_SERVICE_BATTERY = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
	public static final UUID UUID_SERVICE_HEART_RATE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
	public static final UUID UUID_SERVICE_MOTION = UUID.fromString("00030000-78fc-48fe-8e23-433b3a1942d0");
	
	public static final UUID UUID_CHARACTERISTIC_BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
	public static final UUID UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
	public static final UUID UUID_CHARACTERISTIC_MOTION_STEP_COUNT = UUID.fromString("00030001-78fc-48fe-8e23-433b3a1942d0");
	
	private static final Logger logger = LoggerFactory.getLogger(BleThing.class);
	
	private String thingId;
	private String name;
	private String address;
	
	public BleThing(String thingId, String name, String address) {
		this.thingId = thingId;
		this.name = name;
		this.address = address;
	}
	
	@Override
	public String getThingId() {
		return thingId;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public String getAddress() {
		return address;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		
		if (obj == null || getClass() != obj.getClass())
			return false;
		
		if (this.address == null)
			return false;
		
		BleThing other = (BleThing)obj;
		return this.address.equals(other.address);
	}
	
	@Override
	public int hashCode() {
		return address.hashCode() ^ 37;
	}
	
	@Override
	public String toString() {
		return String.format("Device[%s: %s-%s]", thingId, name, address);
	}
}
