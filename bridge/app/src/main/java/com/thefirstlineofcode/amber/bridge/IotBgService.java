package com.thefirstlineofcode.amber.bridge;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.thefirstlineofcode.chalk.android.StandardChatClient;
import com.thefirstlineofcode.chalk.core.AuthFailureException;
import com.thefirstlineofcode.chalk.core.IChatClient;
import com.thefirstlineofcode.chalk.core.stream.StandardStreamConfig;
import com.thefirstlineofcode.chalk.core.stream.UsernamePasswordToken;
import com.thefirstlineofcode.chalk.network.ConnectionException;
import com.thefirstlineofcode.chalk.network.IConnectionListener;
import com.thefirstlineofcode.sand.client.actuator.ActuatorPlugin;
import com.thefirstlineofcode.sand.client.concentrator.ConcentratorPlugin;
import com.thefirstlineofcode.sand.client.concentrator.IConcentrator;
import com.thefirstlineofcode.sand.client.concentrator.LanNode;
import com.thefirstlineofcode.sand.client.ibtr.IRegistration;
import com.thefirstlineofcode.sand.client.ibtr.IbtrError;
import com.thefirstlineofcode.sand.client.ibtr.IbtrPlugin;
import com.thefirstlineofcode.sand.client.ibtr.RegistrationException;
import com.thefirstlineofcode.sand.client.sensor.SensorPlugin;
import com.thefirstlineofcode.sand.client.thing.ThingsUtils;
import com.thefirstlineofcode.sand.protocols.thing.CommunicationNet;
import com.thefirstlineofcode.sand.protocols.thing.RegisteredEdgeThing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class IotBgService extends Service implements IIotBgService,
		IConnectionListener {
	private static final Logger logger = LoggerFactory.getLogger(IotBgService.class);
	
	private HostConfiguration hostConfiguration;
	private IChatClient chatClient;
	private IConcentrator concentratorMaybeNull;
	
	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		String host = intent.getStringExtra(getString(R.string.current_host));
		hostConfiguration = MainApplication.getInstance().getHostConfiguration(host);
		if (hostConfiguration == null)
			throw new IllegalArgumentException(String.format("Can't get host configuration for host '%s'.", host));
		
		chatClient = createChaClient(hostConfiguration);
		if (!isEdgeThingRegistered()) {
			registerEdgeThing();
		} else {
			connectToHost();
		}
		
		logger.info("IoT background service has started.");
		
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
		
		if (hostConfiguration.getThingName() == null) {
			hostConfiguration.setThingName(createThingId());
		}
		
		IChatClient registrationChatClient = new StandardChatClient(new StandardStreamConfig(
				hostConfiguration.getHost(),
				hostConfiguration.getPort(),
				hostConfiguration.isTlsRequired()
		));
		registrationChatClient.register(IbtrPlugin.class);
		
		IRegistration registration = registrationChatClient.createApi(IRegistration.class);
		try {
			RegisteredEdgeThing registeredEdgeThing = registration.register(
					hostConfiguration.getThingName(),"abcdefghijkl");
			edgeThingRegistered(registeredEdgeThing);
		} catch (RegistrationException e) {
			if (e.getError() == IbtrError.NOT_AUTHORIZED) {
				MainApplication.getInstance().updateHostConfiguration(hostConfiguration);
				MainApplication.getInstance().saveHostConfigurations();
				
				AmberUtils.toastInService("Try to registered a unauthorized thing. Please authorize the thing first.");
			} else {
				if (e.getError() == IbtrError.CONFLICT) {
					hostConfiguration.setThingName(null);
				}
				
				AmberUtils.toastInService("Failed to register edge thing to host.");
			}
		}
	}
	
	private String createThingId() {
		return "Amber-Bridge-" + ThingsUtils.generateRandomId(8);
	}
	
	@Override
	public boolean isEdgeThingRegistered() {
		return hostConfiguration.getThingName() != null &&
				hostConfiguration.getThingCredentials() != null;
	}
	
	@Override
	public void connectToHost() {
		if (!isEdgeThingRegistered())
			throw new IllegalStateException("Not register yet.");
		
		chatClient.addConnectionListener(this);
		try {
			chatClient.connect(new UsernamePasswordToken(
					hostConfiguration.getThingName(), hostConfiguration.getThingCredentials()));
			
			hostConnected();
		} catch (ConnectionException e) {
			AmberUtils.toastInService(String.format("Can't connect to host. Connection exception type: %s.", e.getType()));
		} catch (AuthFailureException e) {
			AmberUtils.toastInService("Failed to authenticate edge thing.");
		}
	}
	
	@Override
	public void disconnectFromHost() {
		concentratorMaybeNull = null;
		
		if (chatClient.isConnected())
			chatClient.close();
		chatClient = null;
	}
	
	@Override
	public boolean isConnectedToHost() {
		return chatClient.isConnected();
	}
	
	private void edgeThingRegistered(RegisteredEdgeThing registeredEdgeThing) {
		hostConfiguration.setThingName(registeredEdgeThing.getThingName());
		hostConfiguration.setThingCredentials(registeredEdgeThing.getCredentials());
		
		MainApplication.getInstance().updateHostConfiguration(hostConfiguration);
		MainApplication.getInstance().saveHostConfigurations();
		
		connectToHost();
	}
	
	@Override
	public void exceptionOccurred(ConnectionException exception) {
		disconnectFromHost();
		
		AmberUtils.toastInService(String.format("Connection exception threw. Connection error type: %s.",
				exception.getType()));
		
		try {
			Thread.sleep(30 * 1000);
		} catch (InterruptedException e) {
			throw new RuntimeException("????", e);
		}
		
		// Reconnecting....
		connectToHost();
	}
	
	public void hostConnected() {}
	
	@Override
	public IConcentrator getConcentrator() {
		if (concentratorMaybeNull != null)
			return concentratorMaybeNull;
		
		Map<Integer, LanNode> lanNodes = new HashMap<Integer, LanNode>();
		IThingNodeManager lanNodeManager = (MainApplication)getApplication();
		for (ThingNode thingNode : lanNodeManager.getThingNodes()) {
			if (thingNode.getLanId() != null) {
				lanNodes.put(thingNode.getLanId(), thingNodeToLanNode(thingNode));
			}
		}
		
		concentratorMaybeNull = chatClient.createApi(IConcentrator.class);
		concentratorMaybeNull.setNodes(lanNodes);
		
		return concentratorMaybeNull;
	}
	
	@NonNull
	private LanNode thingNodeToLanNode(ThingNode thingNode) {
		LanNode lanNode = new LanNode();
		lanNode.setLanId(thingNode.getLanId());
		lanNode.setThingId(thingNode.getThing().getThingId());
		lanNode.setAddress(thingNode.getThing().getAddress());
		lanNode.setConfirmed(true);
		lanNode.setCommunicationNet(CommunicationNet.BLE);
		lanNode.setModel("Amber-Watch");
		lanNode.setRegistrationCode("abcdefghijkl");
		
		return lanNode;
	}
	
	@Override
	public void messageReceived(String message) {
		// Ignore.
	}
	
	@Override
	public void heartBeatsReceived(int length) {
		// Ignore.
		logger.info("ChatClient is still connected.");
	}
	
	@Override
	public void messageSent(String message) {
		// Ignore.
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		disconnectFromHost();
		
		return super.onUnbind(intent);
	}
	
	@Override
	public void onDestroy() {
		disconnectFromHost();
		
		logger.info("IoTBgService.onDestory() is being called.");
		
		super.onDestroy();
	}
}
