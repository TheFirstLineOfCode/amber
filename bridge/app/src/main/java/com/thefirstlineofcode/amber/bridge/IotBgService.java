package com.thefirstlineofcode.amber.bridge;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.thefirstlineofcode.chalk.core.AuthFailureException;
import com.thefirstlineofcode.chalk.core.IChatClient;
import com.thefirstlineofcode.chalk.core.StandardChatClient;
import com.thefirstlineofcode.chalk.core.stream.INegotiationListener;
import com.thefirstlineofcode.chalk.core.stream.IStream;
import com.thefirstlineofcode.chalk.core.stream.IStreamNegotiant;
import com.thefirstlineofcode.chalk.core.stream.NegotiationException;
import com.thefirstlineofcode.chalk.core.stream.StandardStreamConfig;
import com.thefirstlineofcode.chalk.core.stream.UsernamePasswordToken;
import com.thefirstlineofcode.chalk.network.ConnectionException;
import com.thefirstlineofcode.chalk.network.IConnectionListener;
import com.thefirstlineofcode.sand.client.actuator.ActuatorPlugin;
import com.thefirstlineofcode.sand.client.concentrator.ConcentratorPlugin;
import com.thefirstlineofcode.sand.client.ibtr.IRegistration;
import com.thefirstlineofcode.sand.client.ibtr.IbtrPlugin;
import com.thefirstlineofcode.sand.client.ibtr.RegistrationException;
import com.thefirstlineofcode.sand.client.sensor.SensorPlugin;
import com.thefirstlineofcode.sand.client.thing.ThingsUtils;
import com.thefirstlineofcode.sand.protocols.thing.RegisteredEdgeThing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class IotBgService extends Service implements IIotBgService, IIotBgService.IEdgeThingStateistener {
	private static final Logger logger = LoggerFactory.getLogger(IotBgService.class);
	
	private HostConfiguration hostConfiguration;
	private IChatClient chatClient;
	private List<IEdgeThingStateistener> edgeThingRegistrationListeners;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String host = intent.getStringExtra(getString(R.string.current_host));
		hostConfiguration = MainApplication.getInstance().getHostConfiguration(host);
		if (hostConfiguration == null)
			throw new IllegalArgumentException(String.format("Can't get host configuration for host '%s'.", host));
		
		edgeThingRegistrationListeners = new ArrayList<>();
		addEdgeThingStateListener(this);
		
		chatClient = createChaClient(hostConfiguration);
		if (!isEdgeThingRegistered()) {
			registerEdgeThing();
		} else {
			connectToHost();
		}
		
		logger.info("IoT background service has started.");
		
		return Service.START_REDELIVER_INTENT;
	}
	
	@Override
	public void startMonitorTask() {
		// TODO
	}
	
	@Override
	public void stopMonitorTask() {
	
	}
	
	@Override
	public boolean isMonitorTaskStarted() {
		return false;
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
	
	private IChatClient createChaClient(HostConfiguration hostConfiguration) {
		IChatClient chatClient = new StandardChatClient(getStreamConfig(hostConfiguration));
		
		registerPlugins(chatClient);
		
		return chatClient;
	}
	
	private StandardStreamConfig getStreamConfig(HostConfiguration hostConfiguration) {
		return new StandardStreamConfig(hostConfiguration.getHost(),
				hostConfiguration.getPort(), hostConfiguration.isTlsRequired());
	}
	
	private void registerPlugins(IChatClient chatClient) {
		chatClient.register(IbtrPlugin.class);
		chatClient.register(ConcentratorPlugin.class);
		chatClient.register(SensorPlugin.class);
		chatClient.register(ActuatorPlugin.class);
	}
	
	
	@Override
	public void registerEdgeThing() {
		if (isEdgeThingRegistered()) {
			logger.warn("You are trying to register a registered edge thing.");
			return;
		}
		
		IRegistration registration = chatClient.createApi(IRegistration.class);
		try {
			RegisteredEdgeThing registeredEdgeThing = registration.register(createThingId(), "abcdefghijkl");
			for (IEdgeThingStateistener edgeThingRegistrationistener : edgeThingRegistrationListeners) {
				edgeThingRegistrationistener.edgeThingRegistered(registeredEdgeThing);
			}
		} catch (RegistrationException e) {
			toastInService("Failed to register edge thing to host.");
		}
	}
	
	private void toastInService(String message) {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				Toast.makeText(MainApplication.getInstance(), message, Toast.LENGTH_LONG).show();
			}
		};
		
		new Handler(Looper.getMainLooper()).post(runnable);
	}
	
	private String createThingId() {
		return "Amber-Watch-" + ThingsUtils.generateRandomId(8);
	}
	
	@Override
	public boolean isEdgeThingRegistered() {
		return hostConfiguration.getThingName() != null;
	}
	
	@Override
	public void connectToHost() {
		// TODO
		try {
			chatClient.connect(new UsernamePasswordToken(
					hostConfiguration.getThingName(), hostConfiguration.getThingCredentials()));
		} catch (ConnectionException e) {
			e.printStackTrace();
		} catch (AuthFailureException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void disconnectFromHost() {
		if (chatClient.isConnected())
			chatClient.close();
	}
	
	@Override
	public boolean isConnectedToHost() {
		return chatClient.isConnected();
	}
	
	@Override
	public void addEdgeThingStateListener(IEdgeThingStateistener listener) {
		if (!edgeThingRegistrationListeners.contains(listener))
			edgeThingRegistrationListeners.add(listener);
	}
	
	@Override
	public boolean removeEdgeThingStateListener(IEdgeThingStateistener listener) {
		return edgeThingRegistrationListeners.remove(listener);
	}
	
	@Override
	public void edgeThingRegistered(RegisteredEdgeThing registeredEdgeThing) {
		hostConfiguration.setThingName(registeredEdgeThing.getThingName());
		hostConfiguration.setThingCredentials(registeredEdgeThing.getCredentials());
		
		MainApplication.getInstance().updateHostConfiguration(hostConfiguration);
		MainApplication.getInstance().saveHostConfigurations();
		
		connectToHost();
	}
	
	@Override
	public void exceptionOccurred(ConnectionException exception) {
	
	}
	
	@Override
	public void hostConnected() {
	
	}
}
