package com.thefirstlineofcode.amber.bridge;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.thefirstlineofcode.chalk.core.IChatClient;
import com.thefirstlineofcode.chalk.core.StandardChatClient;
import com.thefirstlineofcode.chalk.core.stream.StandardStreamConfig;
import com.thefirstlineofcode.sand.client.actuator.ActuatorPlugin;
import com.thefirstlineofcode.sand.client.concentrator.ConcentratorPlugin;
import com.thefirstlineofcode.sand.client.ibtr.IRegistration;
import com.thefirstlineofcode.sand.client.ibtr.IbtrPlugin;
import com.thefirstlineofcode.sand.client.sensor.SensorPlugin;
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
	
	private IChatClient createChaClient(String host) {
		IChatClient chatClient = new StandardChatClient(getStreamConfig(host));
		
		registerPlugins(chatClient);
		
		return chatClient;
	}
	
	private StandardStreamConfig getStreamConfig(String host) {
		HostConfiguration hostConfiguration = MainApplication.getInstance().getHostConfiguration(host);
		
		return new StandardStreamConfig(hostConfiguration.getHost(), hostConfiguration.getPort(), hostConfiguration.isTlsRequired());
	}
	
	private void registerPlugins(IChatClient chatClient) {
		chatClient.register(IbtrPlugin.class);
		chatClient.register(ConcentratorPlugin.class);
		chatClient.register(SensorPlugin.class);
		chatClient.register(ActuatorPlugin.class);
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
	public void connectToHost() {
	
	}
}
