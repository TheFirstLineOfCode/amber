package com.thefirstlineofcode.amber.bridge;

import android.app.Application;
import android.content.Intent;

import com.thefirstlineofcode.chalk.android.logger.LogConfigurator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

public class MainApplication extends Application implements ILanNodeManager {
	public static final String APP_NAME_AMBERBRIDGE = "amberbridge";
	private static final String FILE_PATH_LAN_NODES_PROPERTIES = ".com.thefirstlineofcode.amber/lan-nodes.properties";
	
	private static final Logger logger = LoggerFactory.getLogger(MainApplication.class);
	
	private List<LanNode> lanNodes;
	private List<Listener> listeners;
	
	private MainActivity mainActivity;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		File dataDir = getApplicationContext().getFilesDir();
		new LogConfigurator().configure(dataDir.getAbsolutePath(), APP_NAME_AMBERBRIDGE, LogConfigurator.LogLevel.INFO);
		
		LoggingExceptionHandler handler = new LoggingExceptionHandler(Thread.getDefaultUncaughtExceptionHandler());
		Thread.setDefaultUncaughtExceptionHandler(handler);
		
		listeners = new ArrayList<>();
		lanNodes = loadLanNodes();
	}
	
	public List<LanNode> loadLanNodes() {
		File dataDir = getApplicationContext().getFilesDir();
		File lanNodesPropertiesFile = dataDir.toPath().resolve(FILE_PATH_LAN_NODES_PROPERTIES).toFile();
		
		if (!lanNodesPropertiesFile.exists())
			return new ArrayList<>();
		
		Properties lanNodesProperties = new Properties();
		try {
			lanNodesProperties.load(new BufferedReader(new FileReader(lanNodesPropertiesFile)));
			
			List<LanNode> lanNodes = new ArrayList<>();
			for (String thingId : lanNodesProperties.stringPropertyNames()) {
				lanNodes.add(getLanNode(thingId, lanNodesProperties.getProperty(thingId)));
			}
			
			return Collections.unmodifiableList(lanNodes);
		} catch (IOException e) {
			logger.error("Can't load LAN nodes from LAN nodes properties file. We will remove LAN nodes properties file and your all LAN nodes data will lost.");
			lanNodesPropertiesFile.delete();
			
			return null;
		}
	}
	
	private LanNode getLanNode(String thingId, String lanNodeDetails) {
		StringTokenizer st = new StringTokenizer(lanNodeDetails, ",");
		if (st.countTokens() != 3)
			throw new IllegalArgumentException("Illegal LAN node details info.");
		
		Integer lanId = Integer.parseInt(st.nextToken());
		String deviceName = st.nextToken();
		String deviceAddress = st.nextToken();
		
		return new LanNode(thingId, lanId == 0 ? null : lanId, new Device(deviceName, deviceAddress));
	}
	
	@Override
	public List<LanNode> getLanNodes() {
		return Collections.unmodifiableList(lanNodes);
	}
	
	@Override
	public void addDevice(Device device) {
		LanNode lanNode = new LanNode(Device.getThingId(device), device);
		if (lanNodes.contains(lanNode)) {
			if (logger.isWarnEnabled())
				logger.warn(String.format("Try to add a existed device. Device: %s.", device));
			
			return;
		}
		
		lanNodes.add(lanNode);
		notifyDeviceAdded(device);
	}
	
	private void notifyDeviceAdded(Device device) {
		for (Listener listener : listeners)
			listener.deviceAdded(device);
	}
	
	@Override
	public void nodeAdded(String thingId, int lanId) {
	
	}
	
	@Override
	public void addListener(Listener listener) {
		if (!listeners.contains(listener))
			listeners.add(listener);
	}
	
	@Override
	public boolean removeListener(Listener listener) {
		return listeners.remove(listener);
	}
	
	@Override
	public void save() {
		Properties lanNodesProperties = new Properties();
		for (LanNode lanNode : lanNodes) {
			lanNodesProperties.put(lanNode.getThingId(), getLanNodeDetails(lanNode));
		}
		
		File dataDir = getApplicationContext().getFilesDir();
		File lanNodesPropertiesFile = dataDir.toPath().resolve(FILE_PATH_LAN_NODES_PROPERTIES).toFile();
		
		if (!lanNodesPropertiesFile.getParentFile().exists()) {
			try {
				Files.createDirectories(lanNodesPropertiesFile.getParentFile().toPath());
			} catch (IOException e) {
				throw new RuntimeException(String.format("Can't create path: %s",
						lanNodesPropertiesFile.getParentFile().getAbsolutePath()), e);
			}
		}
		
		try {
			lanNodesProperties.store(new BufferedWriter(new FileWriter(lanNodesPropertiesFile)), null);
		} catch (IOException e) {
			logger.error("Can't load LAN nodes from LAN nodes properties file. We will remove LAN nodes properties file and your all LAN nodes data will lost.");
			throw new RuntimeException("Can't save LAN nodes.");
		}
	}
	
	private String getLanNodeDetails(LanNode lanNode) {
		return String.format("%d,%s,%s", lanNode.getLanId() == null ? 0 : lanNode.getLanId(),
				lanNode.getDevice().getName(), lanNode.getDevice().getAddress());
	}
	
	public void startDiscoveryActivity() {
		startActivity(new Intent(this, DiscoveryActivity.class));
	}
}
