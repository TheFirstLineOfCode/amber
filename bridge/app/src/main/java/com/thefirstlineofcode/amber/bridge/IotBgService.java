package com.thefirstlineofcode.amber.bridge;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.thefirstlineofcode.sand.client.ibtr.IRegistration;
import com.thefirstlineofcode.sand.protocols.thing.RegisteredThing;

public class IotBgService extends Service implements IIotBgService {
	private HostConfiguration hostConfiguration;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String host = intent.getStringExtra(getString(R.string.current_host));
		hostConfiguration = MainApplication.getInstance().getHostConfiguration(host);
		
		return Service.START_REDELIVER_INTENT;
	}
	
	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return new IotBgBinder(this);
	}
	
	@Override
	public HostConfiguration getHostConfiguration() {
		return hostConfiguration;
	}
	
	@Override
	public RegisteredThing register() {
		return null;
	}
	
	@Override
	public boolean isRegistered() {
		return false;
	}
	
	@Override
	public void connectHost() {
	
	}
}
