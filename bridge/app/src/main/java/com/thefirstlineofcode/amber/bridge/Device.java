package com.thefirstlineofcode.amber.bridge;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Device {
	public static final short RSSI_UNKNOWN = 0;
	
	public static final UUID UUID_SERVICE_BATTERY = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
	public static final UUID UUID_SERVICE_HEART_RATE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
	public static final UUID UUID_SERVICE_MOTION = UUID.fromString("00030000-78fc-48fe-8e23-433b3a1942d0");
	
	public static final UUID UUID_CHARACTERISTIC_BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
	public static final UUID UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
	public static final UUID UUID_CHARACTERISTIC_MOTION_STEP_COUNT = UUID.fromString("00030001-78fc-48fe-8e23-433b3a1942d0");
	
	private static final Logger logger = LoggerFactory.getLogger(Device.class);
	
	public interface StateListener {
		void connected(BluetoothDevice device, BluetoothGatt gatt);
		void disconnected(BluetoothDevice device);
		void occurred(BluetoothDevice device, Error error);
	}
	
	public enum Error {
		FAILED_TO_CONNECT_TO_DEVICE,
		SECURITY_EXCEPTION_HAS_THROWN
	}
	
	public enum State {
		NOT_CONNECTED,
		CONNECTING,
		CONNECTED
	}
	
	private String name;
	private String address;
	private State state;
	private List<StateListener> stateListeners;
	
	private int batteryLevel;
	private int heartRate;
	private int totalSteps;
	
	public Device(String name, String address) {
		this.name = name;
		this.address = address;
		
		state = State.NOT_CONNECTED;
		stateListeners = new ArrayList<>();
		
		batteryLevel = 50;
		heartRate = 0;
		totalSteps = 0;
	}
	
	public String getName() {
		return name;
	}
	
	public String getAddress() {
		return address;
	}
	
	public int getBatteryLevel() {
		return batteryLevel;
	}
	
	public int getHeartRate() {
		return heartRate;
	}
	
	public int getTotalSteps() {
		return totalSteps;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Device)) {
			return false;
		}
		if (((Device)obj).getAddress().equals(this.address)) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return address.hashCode() ^ 37;
	}
	
	@Override
	public String toString() {
		return String.format("Device[%s-%s]", name, address);
	}
	
	public static String getThingId(Device device) {
		return String.format("%s-%s", device.getName(), device.getAddress());
	}
	
	public void connect() {
	
	}
	
	public void disconnect() {
	
	}
	
	public State getState() {
		return state;
	}
	
	private void notifyErrorOccurred(BluetoothDevice device, Error error) {
		for (StateListener stateListener : stateListeners) {
			stateListener.occurred(device, error);
		}
	}
	
	public void addStateListener(StateListener stateListener) {
		if (!stateListeners.contains(stateListener))
			stateListeners.add(stateListener);
	}
	
	public boolean removeStateListener(StateListener stateListener) {
		return stateListeners.remove(stateListener);
	}
	
	private class GattCallback extends BluetoothGattCallback {
		private BluetoothDevice device;
		private BluetoothGatt gatt;
		
		public GattCallback(BluetoothDevice device) {
			this.device = device;
		}
		
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			super.onConnectionStateChange(gatt, status, newState);
			
			if (logger.isDebugEnabled())
				logger.debug("Connection state changed. State: {}.", newState);
			
			if (status != BluetoothGatt.GATT_SUCCESS) {
				logger.error("Failed t9o connect to device. Device: {}.", device);
				try {
					gatt.close();
				} catch (SecurityException e) {
					throw new RuntimeException("Failed to call gatt.close().", e);
				}
				
				notifyErrorOccurred(device, Error.FAILED_TO_CONNECT_TO_DEVICE);
				return;
			}
			
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				this.gatt = gatt;
				logger.info("Try to discover GATT services....");
				try {
					gatt.discoverServices();
				} catch (SecurityException e) {
					notifyErrorOccurred(device, Error.SECURITY_EXCEPTION_HAS_THROWN);
					gatt.close();
					gatt = null;
				}
			}
		}
		
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			super.onServicesDiscovered(gatt, status);
			
			if (logger.isDebugEnabled())
				logger.debug("Services has discovered. status: {}.", status);
			
			if (status != BluetoothGatt.GATT_SUCCESS) {
				logger.error("Failed to discover services. Device: {}.", device);
				try {
					gatt.close();
				} catch (SecurityException e) {
					throw new RuntimeException("Failed to call gatt.close().", e);
				}
				
				notifyErrorOccurred(device, Error.FAILED_TO_CONNECT_TO_DEVICE);
				return;
			}
			
			List<BluetoothGattService> services = gatt.getServices();
			
			boolean characteristicBatteryLevelFound = false;
			boolean characteristicHeartRateMeasurementFound = false;
			boolean characteristicMotionStepCountFound = false;
			for (BluetoothGattService service : services) {
				logger.info("Bluetooth service {} found.", service.getUuid());
				
				if (UUID_SERVICE_BATTERY.equals(service.getUuid())) {
					characteristicBatteryLevelFound = findCharacteristic(service, UUID_CHARACTERISTIC_BATTERY_LEVEL);
				} else if (UUID_SERVICE_HEART_RATE.equals(service.getUuid())) {
					characteristicHeartRateMeasurementFound = findCharacteristic(service, UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT);
				} else if (UUID_SERVICE_MOTION.equals(service.getUuid())) {
					characteristicMotionStepCountFound = findCharacteristic(service, UUID_CHARACTERISTIC_MOTION_STEP_COUNT);
				} else {
					// Ignore.
				}
			}
			
			if (characteristicBatteryLevelFound)
				logger.info("Characteristic battery level found.");
			
			if (characteristicHeartRateMeasurementFound)
				logger.info("Characteristic heart rate measurement found.");
			
			if (characteristicMotionStepCountFound)
				logger.info("Characteristic motion step count found.");
		}
		
		private boolean findCharacteristic(BluetoothGattService service, UUID uuidCharacteristic) {
			for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
				if (uuidCharacteristic.equals(characteristic.getUuid()))
					return true;
			}
			
			return false;
		}
	}
}
