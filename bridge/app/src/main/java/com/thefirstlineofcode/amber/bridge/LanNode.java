package com.thefirstlineofcode.amber.bridge;

public class LanNode {
	private Integer lanId;
	private IBleThing thing;
	private boolean confirmed;
	
	public LanNode() {
		this(null, null);
	}
	
	public LanNode(Integer lanId, IBleThing thing) {
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
	
	public boolean isConfirmed() {
		return lanId != null;
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
		return String.format("LanNode[%s: %s, %s]", lanId, lanId, thing);
	}
}
