package com.thefirstlineofcode.amber.bridge;

import android.Manifest;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import com.thefirstlineofcode.chalk.android.logger.LogConfigurator;
import com.thefirstlineofcode.chalk.core.IChatClient;
import com.thefirstlineofcode.chalk.core.StandardChatClient;
import com.thefirstlineofcode.chalk.core.stream.StandardStreamConfig;
import com.thefirstlineofcode.sand.client.actuator.ActuatorPlugin;
import com.thefirstlineofcode.sand.client.concentrator.ConcentratorPlugin;
import com.thefirstlineofcode.sand.client.ibtr.IbtrPlugin;
import com.thefirstlineofcode.sand.client.sensor.SensorPlugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

public class MainApplication extends Application implements ILanNodeManager, IHostConfigurationManager {
	public static final String APP_NAME_AMBERBRIDGE = "amberbridge";
	private static final String FILE_PATH_LAN_NODES_PROPERTIES = ".com.thefirstlineofcode.amber/lan-nodes.properties";
	private static final String FILE_HOSTS_PROPERTIES = ".com.thefirstlineofcode.amber/hosts.properties";
	
	private static final Logger logger = LoggerFactory.getLogger(MainApplication.class);
	
	private static MainApplication instance;
	
	private List<LanNode> lanNodes;
	private List<Listener> listeners;
	
	private MainActivity mainActivity;
	
	private IChatClient chatClient;
	
	private List<HostConfiguration> hostConfigurations;
	private boolean hostConfigurationsChanged;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		if (instance == null)
			instance = this;
		
		File dataDir = getApplicationContext().getFilesDir();
		new LogConfigurator().configure(dataDir.getAbsolutePath(), APP_NAME_AMBERBRIDGE, LogConfigurator.LogLevel.INFO);
		
		LoggingExceptionHandler handler = new LoggingExceptionHandler(Thread.getDefaultUncaughtExceptionHandler());
		Thread.setDefaultUncaughtExceptionHandler(handler);
		
		listeners = new ArrayList<>();
		lanNodes = loadLanNodes();
		
		hostConfigurations = loadHostConfigurations();
		hostConfigurationsChanged = false;
	}
	
	private List<HostConfiguration> loadHostConfigurations() {
		File dataDir = MainApplication.getInstance().getApplicationContext().getFilesDir();
		File hostsPropertiesFile = dataDir.toPath().resolve(FILE_HOSTS_PROPERTIES).toFile();
		
		if (!hostsPropertiesFile.exists())
			return null;
		
		Properties hostsProperties = new Properties();
		try {
			hostsProperties.load(new BufferedReader(new FileReader(hostsPropertiesFile)));
			
			String[] availableHosts = getAvailableHosts(hostsProperties.getProperty(getString(R.string.available_hosts)));
			if (availableHosts == null || availableHosts.length == 0)
				return null;
			
			String currentHost = hostsProperties.getProperty(getString(R.string.current_host));
			if (currentHost == null)
				throw new RuntimeException("Null current host.");
			
			HostConfiguration[] hostConfigurations = new HostConfiguration[availableHosts.length];
			for (int i = 0; i < availableHosts.length; i++) {
				String host = availableHosts[i];
				HostConfiguration hostConfiguration = new HostConfiguration(host);
				
				String streamConfiguration = hostsProperties.getProperty(host);
				if (streamConfiguration != null) {
					StringTokenizer st = new StringTokenizer(streamConfiguration, ",");
					
					int port = Integer.parseInt(st.nextToken());
					boolean tlsRequired = Boolean.parseBoolean(st.nextToken());
					
					hostConfiguration.setPort(port);
					hostConfiguration.setTlsRequired(tlsRequired);
					
					hostConfigurations[i] = hostConfiguration;
					if (st.countTokens() == 2) {
						// Ignore
					} else if (st.countTokens() == 4) {
						hostConfiguration.setThingName(st.nextToken());
						hostConfiguration.setCredentials(st.nextToken());
					} else {
						throw new RuntimeException("Illegal host configuration string.");
					}
				}
			}
			
			return Arrays.asList(hostConfigurations);
		} catch (IOException e) {
			logger.error("Can't read host configurations from hosts properties file. We will remove hosts properties file and your all host configurations data will lost.");
			hostsPropertiesFile.delete();
			
			return null;
		}
	}
	
	public IChatClient getChatClient() {
		return chatClient;
	}
	
	private IChatClient createChaClient(String host) {
		IChatClient chatClient = new StandardChatClient(getStreamConfig(host));
		
		registerPlugins(chatClient);
		
		return chatClient;
	}
	
	private StandardStreamConfig getStreamConfig(String host) {
		HostConfiguration hostConfiguration = getHostConfiguration(host);
		
		return new StandardStreamConfig(hostConfiguration.getHost(), hostConfiguration.getPort(), hostConfiguration.isTlsRequired());
	}
	
	private void registerPlugins(IChatClient chatClient) {
		chatClient.register(IbtrPlugin.class);
		chatClient.register(ConcentratorPlugin.class);
		chatClient.register(SensorPlugin.class);
		chatClient.register(ActuatorPlugin.class);
	}
	
	public static MainApplication getInstance() {
		return instance;
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
				lanNodes.add(createLanNode(thingId, lanNodesProperties.getProperty(thingId)));
			}
			
			return Collections.unmodifiableList(lanNodes);
		} catch (IOException e) {
			logger.error("Can't load LAN nodes from LAN nodes properties file. We will remove LAN nodes properties file and your all LAN nodes data will lost.");
			lanNodesPropertiesFile.delete();
			
			return null;
		}
	}
	
	private LanNode createLanNode(String thingId, String lanNodeDetails) {
		StringTokenizer st = new StringTokenizer(lanNodeDetails, ",");
		if (st.countTokens() != 3)
			throw new IllegalArgumentException("Illegal LAN node details info.");
		
		Integer lanId = Integer.parseInt(st.nextToken());
		String thingName = st.nextToken();
		String thingAddress = st.nextToken();
		
		return new LanNode(lanId == 0 ? null : lanId, new BleThing(thingId, thingName, thingAddress));
	}
	
	@Override
	public LanNode[] getLanNodes() {
		if (lanNodes == null || lanNodes.size() == 0)
			return new LanNode[0];
		
		return lanNodes.toArray(new LanNode[lanNodes.size()]);
	}
	
	@Override
	public void addThing(IBleThing thing) {
		LanNode lanNode = new LanNode(null, thing);
		if (lanNodes.contains(lanNode)) {
			if (logger.isWarnEnabled())
				logger.warn(String.format("Try to add a existed thing. Thing: %s.", thing));
			
			return;
		}
		
		lanNodes.add(lanNode);
		notifyThingAdded(thing);
	}
	
	private void notifyThingAdded(IBleThing thing) {
		for (Listener listener : listeners)
			listener.thingAdded(thing);
	}
	
	@Override
	public void nodeAdded(String thingId, int lanId) {
	
	}
	
	@Override
	public void addLanNodeListener(Listener listener) {
		if (!listeners.contains(listener))
			listeners.add(listener);
	}
	
	@Override
	public boolean removeLanNodeListener(Listener listener) {
		return listeners.remove(listener);
	}
	
	@Override
	public void saveLanNodes() {
		Properties lanNodesProperties = new Properties();
		for (LanNode lanNode : lanNodes) {
			lanNodesProperties.put(lanNode.getThing().getThingId(), getLanNodeDetails(lanNode));
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
				lanNode.getThing().getName(), lanNode.getThing().getAddress());
	}
	
	public static boolean checkBluetoothAvailable(Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
				logger.warn("No BLUETOOTH_SCAN permission");
				
				return false;
			}
			
			if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
				logger.warn("No BLUETOOTH_CONNECT permission");
				
				return false;
			}
		}
		
		BluetoothManager bluetoothManager = (BluetoothManager)context.getSystemService(BLUETOOTH_SERVICE);
		if (bluetoothManager == null) {
			logger.warn("No bluetooth service available");
			
			return false;
		}
		
		BluetoothAdapter adapter = bluetoothManager.getAdapter();
		if (adapter == null) {
			logger.warn("No bluetooth adapter available");
			
			return false;
		}
		
		if (!adapter.isEnabled()) {
			logger.warn("Bluetooth not enabled");
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			
			try {
				context.startActivity(enableBtIntent);
			} catch (SecurityException e) {
                /* This should never happen because we did checkSelfPermission above.
                   But we add try...catch to stop Android Studio errors */
				logger.warn("startActivity(enableBtIntent) failed with SecurityException");
			}
			
			return false;
		}
		
		return true;
	}
	
	public String[] getAvailableHosts(String hosts) {
		if (hosts == null || hosts.length() == 0)
			return null;
		
		return hosts.split(",");
	}
	
	@Override
	public HostConfiguration getHostConfiguration(String host) {
		return null;
	}
	
	@Override
	public void addHostConfiguration(HostConfiguration hostConfiguration) {
		hostConfigurations.add(hostConfiguration);
		hostConfigurationsChanged = true;
	}
	
	@Override
	public void updateHostConfiguration(HostConfiguration hostConfiguration) {
		int hostConfigurationIndex = findHostConfiguration(hostConfiguration.getHost());
		if (hostConfigurationIndex == -1)
			throw new IllegalArgumentException("Not a existed host configuration.");
		
		hostConfigurations.set(hostConfigurationIndex, hostConfiguration);
		hostConfigurationsChanged = true;
	}
	
	private int findHostConfiguration(String host) {
		for (int i = 0; i < hostConfigurations.size(); i++) {
			if (hostConfigurations.get(i).getHost().equals(host))
				return i;
		}
		
		return -1;
	}
	
	@Override
	public String[] getAvailableHosts() {
		if (hostConfigurations == null || hostConfigurations.size() == 0)
			return null;
		
		String[] availableHosts = new String[hostConfigurations.size()];
		for (int i = 0; i < availableHosts.length; i++)
			availableHosts[i] = hostConfigurations.get(i).getHost();
		
		return availableHosts;
	}
}
