package com.thefirstlineofcode.amber.bridge;

public class LanNode {
	private String thingId;
	private Integer lanId;
	private Device device;
	private boolean confirmed;
	
	public LanNode() {
		this(null, null);
	}
	
	public LanNode(String thingId, Device device) {
		this(thingId, null, device);
	}
	
	public LanNode(String thingId, Integer lanId, Device device) {
		this.thingId = thingId;
		this.device = device;
		
		setLanId(lanId);
	}
	
	public String getThingId() {
		return thingId;
	}
	
	public void setThingId(String thingId) {
		this.thingId = thingId;
	}
	
	public Integer getLanId() {
		return lanId;
	}
	
	public void setLanId(Integer lanId) {
		this.lanId = lanId;
		confirmed = (lanId != null);
	}
	
	public Device getDevice() {
		return device;
	}
	
	public void setDevice(Device device) {
		this.device = device;
	}
	
	public boolean isConfirmed() {
		return confirmed;
	}
	
	public void setConfirmed(boolean confirmed) {
		this.confirmed = confirmed;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof LanNode)) {
			return false;
		}
		
		LanNode other = (LanNode)obj;
		if (other.thingId == null)
			return false;
		
		if (other.getThingId().equals(this.getThingId())) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		if (thingId == null)
			return 0;
		
		return thingId.hashCode() ^ 37;
	}
	
	@Override
	public String toString() {
		return String.format("LanNode[%s: %s, %s]", thingId, lanId, device);
	}
}
