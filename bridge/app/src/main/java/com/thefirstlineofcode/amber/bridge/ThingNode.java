package com.thefirstlineofcode.amber.bridge;

public class ThingNode {
	private Integer lanId;
	private IBleThing thing;
	
	public ThingNode() {
		this(null, null);
	}
	
	public ThingNode(Integer lanId, IBleThing thing) {
		this.thing = thing;
		
		setLanId(lanId);
	}
	
	public Integer getLanId() {
		return lanId;
	}
	
	public void setLanId(Integer lanId) {
		this.lanId = lanId;
	}
	
	public IBleThing getThing() {
		return thing;
	}
	
	public void setThing(BleThing thing) {
		this.thing = thing;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ThingNode)) {
			return false;
		}
		
		ThingNode other = (ThingNode)obj;
		if (lanId == null && other.lanId != null)
			return false;
		
		if (this.lanId != null && !this.lanId.equals(other.lanId))
			return false;
		
		if (this.getThing() == null)
			return false;
		
		return this.getThing().equals(other.getThing());
	}
	
	@Override
	public int hashCode() {
		int hash = 7;
		
		if (lanId != null)
			hash += 31 * hash + lanId.hashCode();
		
		if (thing != null)
			hash += 31 * hash + thing.hashCode();
		
		return hash;
	}
	
	@Override
	public String toString() {
		return String.format("ThingNode[%s: %s, %s]", lanId, thing.getName(), thing.getAddress());
	}
}
