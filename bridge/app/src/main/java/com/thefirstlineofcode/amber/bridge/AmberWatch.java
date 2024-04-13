package com.thefirstlineofcode.amber.bridge;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AmberWatch extends BleThing implements IBleDevice {
	private static final Logger logger = LoggerFactory.getLogger(AmberWatch.class);
	
	private State state;
	private List<StateListener> stateListeners;
	
	private int batteryLevel;
	private int heartRate;
	private int totalSteps;
	
	private boolean autoConnect;
	
	private BluetoothDevice bluetoothDevice;
	
	private AmberWatch(BluetoothDevice bluetoothDevice, String thingId, String name, String address) {
		super(thingId, name, address);
		this.bluetoothDevice = bluetoothDevice;
		
		state = State.NOT_CONNECTED;
		stateListeners = new ArrayList<>();
		
		batteryLevel = 50;
		heartRate = 0;
		totalSteps = 0;
		
		autoConnect = true;
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
	public BluetoothDevice getBluetoothDevice() {
		return bluetoothDevice;
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
			stateListener.occurred(this, error);
		}
	}
	
	public void addStateListener(StateListener stateListener) {
		if (!stateListeners.contains(stateListener))
			stateListeners.add(stateListener);
	}
	
	public boolean removeStateListener(StateListener stateListener) {
		return stateListeners.remove(stateListener);
	}
	
	public void setAutoConnect(boolean autoConnect) {
		this.autoConnect = autoConnect;
	}
	
	public boolean isAutoConnect() {
		return autoConnect;
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
	
	public static AmberWatch createInstance(BluetoothAdapter adapter, IBleThing thing) {
		BluetoothDevice device = adapter.getRemoteDevice(thing.getAddress());
		if (device == null)
			throw new RuntimeException("Can't get remote device.");
		
		return new AmberWatch(device, thing.getThingId(), thing.getName(), thing.getAddress());
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		
		if (o == null || getClass() != o.getClass())
			return false;
		
		return super.equals(o);
	}
}
