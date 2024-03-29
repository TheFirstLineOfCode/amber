package com.thefirstlineofcode.amber.bridge;

import android.app.Application;

import com.thefirstlineofcode.chalk.android.logger.LogConfigurator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class MainApplication extends Application implements IDeviceManager {
	public static final String APP_NAME_AMBERBRIDGE = "amberbridge";
	private static final String FILE_PATH_DEVICES_PROPERTIES = ".com.thefirstlineofcode.amber/devices.properties";
	
	private static final Logger logger = LoggerFactory.getLogger(MainApplication.class);
	
	private List<Device> devices;
	private List<Listener> listeners;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		File dataDir = getApplicationContext().getFilesDir();
		new LogConfigurator().configure(dataDir.getAbsolutePath(), APP_NAME_AMBERBRIDGE, LogConfigurator.LogLevel.INFO);
		
		LoggingExceptionHandler handler = new LoggingExceptionHandler(Thread.getDefaultUncaughtExceptionHandler());
		Thread.setDefaultUncaughtExceptionHandler(handler);
		
		listeners = new ArrayList<>();
		devices = loadDevices();
	}
	
	public List<Device> loadDevices() {
		File dataDir = getApplicationContext().getFilesDir();
		File devicesPropertiesFile = dataDir.toPath().resolve(FILE_PATH_DEVICES_PROPERTIES).toFile();
		
		if (!devicesPropertiesFile.exists())
			return new ArrayList<>();
		
		Properties devicesProperties = new Properties();
		try {
			devicesProperties.load(new BufferedReader(new FileReader(devicesPropertiesFile)));
			
			List<Device> devices = new ArrayList<>();
			for (String deviceName : devicesProperties.stringPropertyNames()) {
				devices.add(getDevice(deviceName, devicesProperties.getProperty(deviceName)));
			}
			
			return devices;
		} catch (IOException e) {
			logger.error("Can't load devices from devices properties file. We will remove devices properties file and your all devices data will lost.");
			devicesPropertiesFile.delete();
			
			return null;
		}
	}
	
	private Device getDevice(String deviceName, String deviceDetails) {
		String address = deviceDetails;
		
		return new Device(deviceName, address);
	}
	
	public void saveDeviceCandidates() {
		// TODO
		return;
	}
	
	@Override
	public List<Device> getDevices() {
		return Collections.unmodifiableList(devices);
	}
	
	@Override
	public void addDevice(Device device) {
		if (!devices.contains(device)) {
			devices.add(device);
			
			notifyListeners();
		}
		
	}
	
	private void notifyListeners() {
		for (Listener listener : listeners)
			listener.devicesChanged();
	}
	
	@Override
	public void removeDevice(int position) {
		if (devices.size() >= (position + 1)) {
			devices.remove(position);
			
			notifyListeners();
		}
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
}
