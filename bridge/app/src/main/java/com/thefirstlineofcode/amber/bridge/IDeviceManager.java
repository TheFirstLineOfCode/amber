package com.thefirstlineofcode.amber.bridge;

import java.util.List;

public interface IDeviceManager {
	public interface Listener {
		void devicesChanged();
	}
	
	List<Device> getDevices();
	void addDevice(Device device);
	void removeDevice(int position);
	void addListener(Listener listener);
	boolean removeListener(Listener listener);
}
