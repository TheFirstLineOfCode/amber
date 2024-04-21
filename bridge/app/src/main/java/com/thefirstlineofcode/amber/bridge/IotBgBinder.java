package com.thefirstlineofcode.amber.bridge;

import android.os.Binder;

public class IotBgBinder extends Binder {
	private IotBgService iotBgService;
	
	public IotBgBinder(IotBgService iotBgService) {
		this.iotBgService = iotBgService;
	}
	
	public IIotBgService getService() {
		return iotBgService;
	}
}
