package com.thefirstlineofcode.amber.bridge;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;

import androidx.annotation.Nullable;

import com.thefirstlineofcode.basalt.oxm.binary.BinaryUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AmberWatch extends BleThing implements IBleDevice {
	public static final UUID UUID_SERVICE_BATTERY = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
	public static final UUID UUID_SERVICE_HEART_RATE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
	public static final UUID UUID_SERVICE_MOTION = UUID.fromString("00030000-78fc-48fe-8e23-433b3a1942d0");
	
	public static final UUID UUID_CHARACTERISTIC_BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
	public static final UUID UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
	public static final UUID UUID_CHARACTERISTIC_MOTION_STEP_COUNT = UUID.fromString("00030001-78fc-48fe-8e23-433b3a1942d0");
	
	private static final Logger logger = LoggerFactory.getLogger(AmberWatch.class);
	
	private State state;
	private List<StateListener> stateListeners;
	
	private int batteryLevel;
	private int heartRate;
	private int totalSteps;
	
	private boolean autoConnect;
	
	private BluetoothDevice bluetoothDevice;
	
	private GattCallback gattCallback;
	private BluetoothGatt gatt;
	
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
		if (state != State.NOT_CONNECTED)
			return;
		
		if (gattCallback == null)
			gattCallback = new GattCallback();
		
		try {
			bluetoothDevice.connectGatt(MainApplication.getInstance(), false, gattCallback);
		} catch (SecurityException e) {
			AmberUtils.toastInService("SecurityException has thrown while calling method BluetoothDevice.connectGatt.");
		}
	}
	
	private class GattCallback extends BluetoothGattCallback {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			super.onConnectionStateChange(gatt, status, newState);
			
			if (logger.isDebugEnabled())
				logger.debug("Connection state changed. State: {}.", newState);
			
			if (status != BluetoothGatt.GATT_SUCCESS) {
				logger.error("Failed to connect to device. Device: {}.", AmberWatch.this.bluetoothDevice);
				try {
					gatt.close();
				} catch (SecurityException e) {
					throw new RuntimeException("Failed to call gatt.close().", e);
				}
				
				AmberUtils.toastInService(String.format("Failed to connect to device. Device: %s.",
						AmberWatch.this.bluetoothDevice));
				
				return;
			}
			
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				AmberWatch.this.gatt = gatt;
				logger.info("Try to discover GATT services....");
				try {
					gatt.discoverServices();
				} catch (SecurityException e) {
					AmberUtils.toastInService(String.format(
							"Security exception threw when calling gatt.discoverServices(). Device: %s.",
							AmberWatch.this.bluetoothDevice));
					gatt.close();
					AmberWatch.this.gatt = null;
				}
			} else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
				try {
					gatt.close();
				} catch (SecurityException e) {
					AmberUtils.toastInService(String.format(
							"Security exception threw when calling BluetoothGatt.discoverServices(). Device: %s.",
							AmberWatch.this.bluetoothDevice));
				}
				
				AmberWatch.this.gatt = null;
				state = State.NOT_CONNECTED;
			} else {
				// Ignore.
			}
		}
		
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			super.onServicesDiscovered(gatt, status);
			
			if (logger.isDebugEnabled())
				logger.debug("Services has discovered. status: {}.", status);
			
			if (status != BluetoothGatt.GATT_SUCCESS) {
				logger.error("Failed to discover services. Device: {}.", AmberWatch.this.bluetoothDevice);
				try {
					gatt.close();
				} catch (SecurityException e) {
					throw new RuntimeException("Failed to call gatt.close().", e);
				}
				
				AmberUtils.toastInService(String.format("Failed to connect to device. Device: %s.",
						AmberWatch.this.bluetoothDevice));
				return;
			}
			
			subscribeNotifications();
		}
		
		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			super.onCharacteristicChanged(gatt, characteristic);
			
			logger.info(String.format("Characteristic which's UUID is '%s' has changed. New value: %s.",
					characteristic.getUuid(), BinaryUtils.getHexStringFromBytes(characteristic.getValue())));
		}
	}
	
	private void subscribeNotifications() {
		subscribeBatteryLevelNotification();
		subscribeHeartRateMeasurementNotification();
		subscribeMotionStepCountNotification();
	}
	
	private void subscribeMotionStepCountNotification() {
		BluetoothGattService motionService = getGattService(UUID_SERVICE_MOTION);
		if (motionService == null)
			return;
		
		subscribeNotification(motionService, UUID_CHARACTERISTIC_MOTION_STEP_COUNT);
	}
	
	private boolean subscribeHeartRateMeasurementNotification() {
		BluetoothGattService heartRateService = getGattService(UUID_SERVICE_HEART_RATE);
		if (heartRateService == null)
			return false;
		
		return subscribeNotification(heartRateService, UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT);
	}
	
	private boolean subscribeBatteryLevelNotification() {
		BluetoothGattService batteryService = getGattService(UUID_SERVICE_BATTERY);
		if (batteryService == null)
			return false;
		
		return subscribeNotification(batteryService, UUID_CHARACTERISTIC_BATTERY_LEVEL);
	}
	
	@Nullable
	private BluetoothGattService getGattService(UUID uuidService) {
		BluetoothGattService batteryService = gatt.getService(uuidService);
		if (batteryService == null) {
			AmberUtils.toastInService(String.format("Failed to get GATT service. Service UUID: %s", uuidService));
		}
		
		return batteryService;
	}
	
	private boolean subscribeNotification(BluetoothGattService service, UUID uuidCharacteristic) {
		BluetoothGattCharacteristic characteristics = getGattCharacteristics(service, uuidCharacteristic);
		if (characteristics == null) {
			AmberUtils.toastInService(String.format("Failed to get GATT characteristics. Characteristics UUID: %s", uuidCharacteristic));
			return false;
		}
		
		if ((characteristics.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) <= 0) {
			AmberUtils.toastInService("Characteristics which's UUID is '%s' doesn't support notification");
			return false;
		}
		
		try {
			if (!gatt.setCharacteristicNotification(characteristics, true)) {
				AmberUtils.toastInService(String.format(
						"Failed to subscribe GATT characteristics. Characteristics UUID: %s.",
						characteristics.getUuid()));
				return false;
			}
			
			logger.info(String.format("Characteristic which's UUID is '%s' has subscribed.",
					characteristics.getUuid()));
			return true;
		} catch (SecurityException e) {
			AmberUtils.toastInService(String.format(
					"Security exception threw when calling gatt.setCharacteristicNotification(). Device: %s.",
						AmberWatch.this.bluetoothDevice));
			
			return false;
		}
	}
	
	@Nullable
	private BluetoothGattCharacteristic getGattCharacteristics(BluetoothGattService service, UUID uuidCharacteristic) {
		BluetoothGattCharacteristic characteristics = service.getCharacteristic(uuidCharacteristic);
		if (characteristics == null) {
			AmberUtils.toastInService(String.format("Failed to get characteristics. Characteristics UUID : %s.", uuidCharacteristic));
			return null;
		}
		return characteristics;
	}
	
	;
	
	public void disconnect() {
		if (state != State.CONNECTED)
			return;
		
		try {
			gatt.disconnect();
		} catch (SecurityException e) {
			logger.warn("Security exception has thrown while calling BluetoothGatt.disconnect().");
		}
		
		state = State.DISCONNECTING;
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
