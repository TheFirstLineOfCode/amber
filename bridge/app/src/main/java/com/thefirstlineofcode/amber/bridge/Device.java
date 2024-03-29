package com.thefirstlineofcode.amber.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Device {
	public static final short RSSI_UNKNOWN = 0;
	
	private static final Logger logger = LoggerFactory.getLogger(Device.class);
	
	private String name;
	private String address;
	
	public Device(String name, String address) {
		this.name = name;
		this.address = address;
	}
	
	public String getName() {
		return name;
	}
	
	public String getAddress() {
		return address;
	}
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Device)) {
			return false;
		}
		if (((Device)obj).getAddress().equals(this.address)) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return address.hashCode() ^ 37;
	}
	
	@Override
	public String toString() {
		return String.format("Device[%s:%s]", name, address);
	}
}
