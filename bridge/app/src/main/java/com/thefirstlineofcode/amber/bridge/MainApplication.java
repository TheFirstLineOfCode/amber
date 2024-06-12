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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

public class MainApplication extends Application implements IThingNodeManager, IHostConfigurationManager {
	public static final String APP_NAME_AMBERBRIDGE = "amberbridge";
	private static final String FILE_PATH_THING_NODES_PROPERTIES = ".com.thefirstlineofcode.amber/thing-nodes.properties";
	private static final String FILE_PATH_HOSTS_PROPERTIES = ".com.thefirstlineofcode.amber/hosts.properties";
	
	private static final Logger logger = LoggerFactory.getLogger(MainApplication.class);
	
	private static MainApplication instance;
	
	private List<ThingNode> thingNodes;
	private List<Listener> listeners;
	
	private MainActivity mainActivity;
	
	private List<HostConfiguration> hostConfigurations;
	private String currentHost;
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
		thingNodes = loadThingNodes();
		
		hostConfigurations = loadHostConfigurations();
		hostConfigurationsChanged = false;
	}
	
	private List<HostConfiguration> loadHostConfigurations() {
		File dataDir = MainApplication.getInstance().getApplicationContext().getFilesDir();
		File hostsPropertiesFile = dataDir.toPath().resolve(FILE_PATH_HOSTS_PROPERTIES).toFile();
		
		if (!hostsPropertiesFile.exists())
			return null;
		
		Properties hostsProperties = new Properties();
		try {
			hostsProperties.load(new BufferedReader(new FileReader(hostsPropertiesFile)));
			
			String[] availableHosts = getAvailableHosts(hostsProperties.getProperty(getString(R.string.available_hosts)));
			if (availableHosts == null || availableHosts.length == 0)
				return null;
			
			currentHost = hostsProperties.getProperty(getString(R.string.current_host));
			if (currentHost == null && (availableHosts != null && availableHosts.length == 1)) {
				currentHost = availableHosts[0];
			}
			
			if (currentHost == null)
				throw new RuntimeException("Null current host.");
			
			HostConfiguration[] hostConfigurations = new HostConfiguration[availableHosts.length];
			for (int i = 0; i < availableHosts.length; i++) {
				String host = availableHosts[i];
				HostConfiguration hostConfiguration = new HostConfiguration(host);
				
				String hostConfigurationString = hostsProperties.getProperty(host);
				if (hostConfigurationString != null) {
					StringTokenizer st = new StringTokenizer(hostConfigurationString, ",");
					int countToken = st.countTokens();
					
					if (countToken < 2 || countToken > 4)
						throw new RuntimeException("Illegal host configuration string.");
					
					int port = Integer.parseInt(st.nextToken());
					boolean tlsRequired = Boolean.parseBoolean(st.nextToken());
					
					hostConfiguration.setPort(port);
					hostConfiguration.setTlsRequired(tlsRequired);
					
					if (countToken >= 3)
						hostConfiguration.setThingName(st.nextToken());
					
					if (countToken == 4)
						hostConfiguration.setThingCredentials(st.nextToken());
					
					hostConfigurations[i] = hostConfiguration;
				}
			}
			
			return Arrays.asList(hostConfigurations);
		} catch (IOException e) {
			logger.error("Can't read host configurations from hosts properties file. We will remove hosts properties file and your all host configurations data will lost.");
			hostsPropertiesFile.delete();
			
			return null;
		}
	}
	
	public static MainApplication getInstance() {
		return instance;
	}
	
	public List<ThingNode> loadThingNodes() {
		File dataDir = getApplicationContext().getFilesDir();
		File thingNodesPropertiesFile = dataDir.toPath().resolve(FILE_PATH_THING_NODES_PROPERTIES).toFile();
		
		if (!thingNodesPropertiesFile.exists())
			return new ArrayList<>();
		
		Properties thingNodesProperties = new Properties();
		try {
			thingNodesProperties.load(new BufferedReader(new FileReader(thingNodesPropertiesFile)));
			
			List<ThingNode> thingNodes = new ArrayList<>();
			for (String thingId : thingNodesProperties.stringPropertyNames()) {
				thingNodes.add(createThingNode(thingId, thingNodesProperties.getProperty(thingId)));
			}
			
			return Collections.unmodifiableList(thingNodes);
		} catch (IOException e) {
			logger.error("Can't load LAN nodes from LAN nodes properties file. We will remove LAN nodes properties file and your all LAN nodes data will lost.");
			thingNodesPropertiesFile.delete();
			
			return null;
		}
	}
	
	private ThingNode createThingNode(String thingId, String thingNodeDetails) {
		StringTokenizer st = new StringTokenizer(thingNodeDetails, ",");
		if (st.countTokens() != 3)
			throw new IllegalArgumentException("Illegal thing node details info.");
		
		Integer lanId = Integer.parseInt(st.nextToken());
		String thingName = st.nextToken();
		String thingAddress = st.nextToken();
		
		return new ThingNode(lanId == 0 ? null : lanId, new BleThing(thingId, thingName, thingAddress));
	}
	
	@Override
	public ThingNode[] getThingNodes() {
		if (thingNodes == null || thingNodes.size() == 0)
			return new ThingNode[0];
		
		return thingNodes.toArray(new ThingNode[thingNodes.size()]);
	}
	
	@Override
	public void addThing(IBleThing thing) {
		ThingNode thingNode = new ThingNode(null, thing);
		if (thingNodes.contains(thingNode)) {
			if (logger.isWarnEnabled())
				logger.warn(String.format("Try to add a existed thing. Thing: %s.", thing));
			
			return;
		}
		
		thingNodes.add(thingNode);
		notifyThingAdded(thing);
	}
	
	private void notifyThingAdded(IBleThing thing) {
		for (Listener listener : listeners)
			listener.thingAdded(thing);
	}
	
	@Override
	public void nodeAdded(String thingId, int lanId) {
		for (ThingNode thingNode : thingNodes) {
			if (thingNode.getThing().getThingId().equals(thingId)) {
				thingNode.setLanId(lanId);
				
				return;
			}
		}
	}
	
	@Override
	public void addThingNodeListener(Listener listener) {
		if (!listeners.contains(listener))
			listeners.add(listener);
	}
	
	@Override
	public boolean removeThingNodeListener(Listener listener) {
		return listeners.remove(listener);
	}
	
	@Override
	public void saveThingNodes() {
		Properties thingNodesProperties = new Properties();
		for (ThingNode thingNode : thingNodes) {
			thingNodesProperties.put(thingNode.getThing().getThingId(), getThingNodeDetails(thingNode));
		}
		
		File dataDir = getApplicationContext().getFilesDir();
		File thingNodesPropertiesFile = dataDir.toPath().resolve(FILE_PATH_THING_NODES_PROPERTIES).toFile();
		
		if (!thingNodesPropertiesFile.getParentFile().exists()) {
			try {
				Files.createDirectories(thingNodesPropertiesFile.getParentFile().toPath());
			} catch (IOException e) {
				throw new RuntimeException(String.format("Can't create path: %s",
						thingNodesPropertiesFile.getParentFile().getAbsolutePath()), e);
			}
		}
		
		try {
			thingNodesProperties.store(new BufferedWriter(new FileWriter(thingNodesPropertiesFile)), null);
		} catch (IOException e) {
			logger.error("Can't save thing nodes to thing nodes properties file. We will remove thing nodes properties file and your all LAN nodes data will lost.");
			throw new RuntimeException("Can't save thing nodes.");
		}
	}
	
	private String getThingNodeDetails(ThingNode thingNode) {
		return String.format("%d,%s,%s", thingNode.getLanId() == null ? 0 : thingNode.getLanId(),
				thingNode.getThing().getName(), thingNode.getThing().getAddress());
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
	
	private String[] getAvailableHosts(String hosts) {
		if (hosts == null || hosts.length() == 0)
			return null;
		
		return hosts.split(",");
	}
	
	@Override
	public HostConfiguration getHostConfiguration(String host) {
		if (hostConfigurations == null)
			return null;
		
		for (HostConfiguration hostConfiguration : hostConfigurations) {
			if (hostConfiguration.getHost().equals(host))
				return new HostConfiguration(hostConfiguration);
		}
		
		return null;
	}
	
	@Override
	public void addHostConfiguration(HostConfiguration hostConfiguration) {
		if (hostConfigurations == null)
			hostConfigurations = new ArrayList<>();
		
		if (getHostConfiguration(hostConfiguration.getHost()) != null)
			throw new IllegalArgumentException(String.format("Host %s has already existed.", hostConfiguration.getHost()));
		
		hostConfigurations.add(new HostConfiguration(hostConfiguration));
		hostConfigurationsChanged = true;
	}
	
	@Override
	public void updateHostConfiguration(HostConfiguration hostConfiguration) {
		int hostConfigurationIndex = findHostConfiguration(hostConfiguration.getHost());
		if (hostConfigurationIndex == -1)
			throw new IllegalArgumentException("Not a existed host configuration.");
		
		hostConfigurations.set(hostConfigurationIndex, new HostConfiguration(hostConfiguration));
		hostConfigurationsChanged = true;
	}
	
	@Override
	public boolean isHostConfigurationsChanged() {
		return hostConfigurationsChanged;
	}
	
	@Override
	public void saveHostConfigurations() {
		if (!hostConfigurationsChanged)
			return;
		
		Properties hostConfigurationsProperties = new Properties();
		
		String[] hosts = getAvailableHosts();
		if (hosts == null || hosts.length == 0)
			throw new RuntimeException("No available hosts.");
		
		StringBuilder sbAvailbleHosts = new StringBuilder();
		for (String host : hosts)
			sbAvailbleHosts.append(host).append(",");
		sbAvailbleHosts.deleteCharAt(sbAvailbleHosts.length() - 1);
		hostConfigurationsProperties.put(getString(R.string.available_hosts), sbAvailbleHosts.toString());
		
		for (HostConfiguration hostConfiguration : hostConfigurations) {
			hostConfigurationsProperties.put(hostConfiguration.getHost(), getHostConfigurationString(hostConfiguration));
		}
		
		if (currentHost != null)
			hostConfigurationsProperties.put(getString(R.string.current_host), currentHost);
		
		File dataDir = getApplicationContext().getFilesDir();
		File hostsPropertiesFile = dataDir.toPath().resolve(FILE_PATH_HOSTS_PROPERTIES).toFile();
		
		if (!hostsPropertiesFile.getParentFile().exists()) {
			try {
				Files.createDirectories(hostsPropertiesFile.getParentFile().toPath());
			} catch (IOException e) {
				throw new RuntimeException(String.format("Can't create path: %s",
						hostsPropertiesFile.getParentFile().getAbsolutePath()), e);
			}
		}
		
		try {
			hostConfigurationsProperties.store(new BufferedWriter(new FileWriter(hostsPropertiesFile)), null);
		} catch (IOException e) {
			logger.error("Can't save host configurations to host configurations properties file. We will remove host configurations properties file and your all host configurations data will lost.");
			throw new RuntimeException("Can't save host configurations.");
		}
	}
	
	private String getHostConfigurationString(HostConfiguration hostConfiguration) {
		StringBuilder sbHostConfigurationString = new StringBuilder();
		
		sbHostConfigurationString.
				append(hostConfiguration.getPort()).
				append(",").
				append(hostConfiguration.isTlsRequired()).
				append(",");
		
		if (hostConfiguration.getThingName() != null) {
			sbHostConfigurationString.
					append(hostConfiguration.getThingName()).
					append(",");
		}
		
		if (hostConfiguration.getThingCredentials() != null) {
			sbHostConfigurationString.append(hostConfiguration.getThingCredentials());
		}
		
		if (sbHostConfigurationString.charAt(sbHostConfigurationString.length() - 1) == ',')
			sbHostConfigurationString.deleteCharAt(sbHostConfigurationString.length() - 1);
		
		return sbHostConfigurationString.toString();
	}
	
	@Override
	public int findHostConfiguration(String host) {
		if (hostConfigurations == null)
			return -1;
		
		for (int i = 0; i < hostConfigurations.size(); i++) {
			if (hostConfigurations.get(i).getHost().equals(host))
				return i;
		}
		
		return -1;
	}
	
	@Override
	public String[] getAvailableHosts() {
		if (hostConfigurations == null || hostConfigurations.size() == 0)
			return new String[0];
		
		String[] availableHosts = new String[hostConfigurations.size()];
		for (int i = 0; i < availableHosts.length; i++)
			availableHosts[i] = hostConfigurations.get(i).getHost();
		
		return availableHosts;
	}
	
	@Override
	public void setCurrentHost(String host) {
		if (host.equals(currentHost))
			return;
		
		this.currentHost = host;
		hostConfigurationsChanged = true;
	
	}
	
	@Override
	public String getCurrentHost() {
		return currentHost;
	}
}
