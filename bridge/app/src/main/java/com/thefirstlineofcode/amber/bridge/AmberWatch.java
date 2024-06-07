package com.thefirstlineofcode.amber.bridge;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.thefirstlineofcode.basalt.oxm.binary.BinaryUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

public class AmberWatch extends BleThing implements IBleDevice {
	public static final UUID UUID_SERVICE_BATTERY = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
	public static final UUID UUID_SERVICE_HEART_RATE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
	public static final UUID UUID_SERVICE_MOTION = UUID.fromString("00030000-78fc-48fe-8e23-433b3a1942d0");
	public static final UUID UUID_SERVICE_ALERT_NOTIFICATION = UUID.fromString("00001811-0000-1000-8000-00805f9b34fb");
	
	public static final UUID UUID_CHARACTERISTIC_BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
	public static final UUID UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
	public static final UUID UUID_CHARACTERISTIC_MOTION_STEP_COUNT = UUID.fromString("00030001-78fc-48fe-8e23-433b3a1942d0");
	public static final UUID UUID_CHARACTERISTIC_NEW_ALERT = UUID.fromString("00002a46-0000-1000-8000-00805f9b34fb");
	
	public static final UUID UUID_DESCRIPTOR_GATT_CLIENT_CHARACTERISTIC_CONFIGURATION = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	
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
	
	private Queue<ServiceAndCharacteristic> serviceAndCharacteristics;
	private BluetoothGattCharacteristic newAlertcharacteristics;
	
	
	private AmberWatch(BluetoothDevice bluetoothDevice, String thingId, String name, String address) {
		super(thingId, name, address);
		this.bluetoothDevice = bluetoothDevice;
		
		state = State.NOT_CONNECTED;
		stateListeners = new ArrayList<>();
		
		serviceAndCharacteristics = new ArrayBlockingQueue<ServiceAndCharacteristic>(5);
		serviceAndCharacteristics.add(new ServiceAndCharacteristic(UUID_SERVICE_BATTERY, UUID_CHARACTERISTIC_BATTERY_LEVEL));
		serviceAndCharacteristics.add(new ServiceAndCharacteristic(UUID_SERVICE_HEART_RATE, UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT));
		serviceAndCharacteristics.add(new ServiceAndCharacteristic(UUID_SERVICE_MOTION, UUID_CHARACTERISTIC_MOTION_STEP_COUNT));
		
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
		
		state = State.CONNECTING;
		
		notifyConnectingState();
		
		if (gattCallback == null)
			gattCallback = new GattCallback();
		
		try {
			bluetoothDevice.connectGatt(MainApplication.getInstance(), false, gattCallback);
		} catch (SecurityException e) {
			AmberUtils.toastInService("SecurityException has thrown while calling method BluetoothDevice.connectGatt.");
		}
	}
	
	private void notifyConnectingState() {
		for (StateListener listener : stateListeners) {
			listener.connecting(this);
		}
	}
	
	private void notifyConnectedState() {
		for (StateListener listener : stateListeners) {
			listener.connected(this, gatt);
		}
	}
	
	private void notifyDisconnectedState() {
		for (StateListener listener : stateListeners) {
			listener.disconnected(this);
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
				
				state = State.NOT_CONNECTED;
				
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
					
					state = State.NOT_CONNECTED;
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
				
				notifyDisconnectedState();
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
				
				state = State.NOT_CONNECTED;
				
				AmberUtils.toastInService(String.format("Failed to connect to device. Device: %s.",
						AmberWatch.this.bluetoothDevice));
				
				notifyDisconnectedState();
				return;
			}
			
			newAlertcharacteristics = getNewAlertcharacteristic();
			if (newAlertcharacteristics == null) {
				state = State.NOT_CONNECTED;
				throw new RuntimeException("Faiiled to get new alert characteristic.");
			}
			
			subscribeNotifications();
		}
		
		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			super.onCharacteristicChanged(gatt, characteristic);
			
			logger.info(String.format("Characteristic which's UUID is '%s' has changed. New value: %s.",
					characteristic.getUuid(), BinaryUtils.getHexStringFromBytes(characteristic.getValue())));
			
			if (characteristic.getUuid().equals(UUID_CHARACTERISTIC_BATTERY_LEVEL)) {
				int batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
				if (batteryLevel > 100 || batteryLevel < 0) {
					logger.warn("Unexpected percent value: " + batteryLevel);
					batteryLevel = Math.min(100, Math.max(0, batteryLevel));
				}
				
				batteryLevelChanged(batteryLevel);
			} else if (characteristic.getUuid().equals(UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT)) {
				int heartRate = ((int)characteristic.getValue()[1]) & 0xff;
				heartRateChanged(heartRate);
			} else if (characteristic.getUuid().equals(UUID_CHARACTERISTIC_MOTION_STEP_COUNT)) {
				byte[] value = characteristic.getValue();
				int stepCount = (value[0] & 0xff) | ((value[1] & 0xff) << 8) | ((value[2] & 0xff) << 16) | ((value[3] & 0xff) << 24);
				stepCountChanged(stepCount);
			} else {
				AmberUtils.toastInService(String.format("Unknown characteristic which's UUID: %s", characteristic.getUuid()));
			}
		}
		
		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			super.onDescriptorWrite(gatt, descriptor, status);
			
			if (status == BluetoothGatt.GATT_SUCCESS) {
				logger.info(String.format("Characteristic which's UUID is '%s' has subscribed on device '%s'.",
						descriptor.getCharacteristic().getUuid(), AmberWatch.this.toString()));
			} else {
				throw new RuntimeException(String.format(
						"Faiiled to subscribe characteristic which's UUID is '%s'.",
						descriptor.getCharacteristic().getUuid()));
			}
			
			
			subscribeNextNotification();
		}
	}
	
	private void stepCountChanged(int stepCount) {
		logger.info("Step count changed to '{}'.", stepCount);
	}
	
	private void heartRateChanged(int heartRate) {
		logger.info("Heart rate changed to '{}'.", heartRate);
	}
	
	private void batteryLevelChanged(int batteryLevel) {
		logger.info("Battery level changed to '{}'.", batteryLevel);
	}
	
	private BluetoothGattCharacteristic getNewAlertcharacteristic() {
		BluetoothGattService alertNotificationService = getGattService(UUID_SERVICE_ALERT_NOTIFICATION);
		if (alertNotificationService == null)
			return null;
		
		return getWritableCharacteristic(alertNotificationService, UUID_CHARACTERISTIC_NEW_ALERT);
	}
	
	private BluetoothGattCharacteristic getWritableCharacteristic(BluetoothGattService service, UUID uuidCharacteristic) {
		BluetoothGattCharacteristic characteristics = getGattCharacteristics(service, uuidCharacteristic);
		if (characteristics == null) {
			AmberUtils.toastInService(String.format("Failed to get GATT characteristics. Characteristics UUID: %s", uuidCharacteristic));
			return null;
		}
		
		if ((characteristics.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) <= 0 &&
				(characteristics.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) <= 0) {
			AmberUtils.toastInService("Characteristics which's UUID is '%s' doesn't support writing.");
			return null;
		}
		
		return characteristics;
	}
	
	private void subscribeNotifications() {
		subscribeNextNotification();
	}
	
	private void subscribeNextNotification() {
		if (serviceAndCharacteristics.isEmpty() && state == State.CONNECTING) {
			state = State.CONNECTED;
			notifyConnectedState();
			
			return;
		}
		
		ServiceAndCharacteristic serviceAndCharacteristic = serviceAndCharacteristics.poll();
		BluetoothGattService service = getGattService(serviceAndCharacteristic.service);
		if (service == null) {
			AmberUtils.toastInService(String.format("Can't get service which's UUID is '%s'.", serviceAndCharacteristic.service));
			subscribeNextNotification();
			
			return;
		}
		
		subscribeNotification(service, serviceAndCharacteristic.charactistic);
	}
	
	private class ServiceAndCharacteristic {
		public UUID service;
		public UUID charactistic;
		
		public ServiceAndCharacteristic(UUID service, UUID charactistic) {
			this.service = service;
			this.charactistic = charactistic;
		}
	}
	
	@Nullable
	private BluetoothGattService getGattService(UUID uuidService) {
		BluetoothGattService batteryService = gatt.getService(uuidService);
		if (batteryService == null) {
			AmberUtils.toastInService(String.format("Failed to get GATT service. Service UUID: %s", uuidService));
		}
		
		return batteryService;
	}
	
	private void subscribeNotification(BluetoothGattService service, UUID uuidCharacteristic) {
		BluetoothGattCharacteristic characteristics = getGattCharacteristics(service, uuidCharacteristic);
		if (characteristics == null) {
			AmberUtils.toastInService(String.format("Failed to get GATT characteristics. Characteristics UUID: %s", uuidCharacteristic));
			subscribeNextNotification();
			
			return;
		}
		
		if ((characteristics.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) <= 0 &&
				(characteristics.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) <= 0) {
			AmberUtils.toastInService("Characteristics which's UUID is '%s' doesn't support notification");
			subscribeNextNotification();
			
			return;
		}
		
		try {
			if (!gatt.setCharacteristicNotification(characteristics, true)) {
				AmberUtils.toastInService(String.format(
						"Failed to subscribe GATT characteristics. Characteristics UUID: %s.",
						characteristics.getUuid()));
				
				subscribeNextNotification();
				return;
			}
			
			BluetoothGattDescriptor notifyDescriptor = characteristics.getDescriptor(UUID_DESCRIPTOR_GATT_CLIENT_CHARACTERISTIC_CONFIGURATION);
			if (notifyDescriptor == null) {
				logger.info(String.format("Characteristic which's UUID is '%s' has subscribed on device '%s'.",
						characteristics.getUuid(), this.toString()));
				
				subscribeNextNotification();
				return;
			}
			
			int properties = characteristics.getProperties();
			if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
				notifyDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
				gatt.writeDescriptor(notifyDescriptor);
			}
			
			if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
				notifyDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
				gatt.writeDescriptor(notifyDescriptor);
			}
		} catch (SecurityException e) {
			AmberUtils.toastInService(String.format(
					"Security exception threw when calling gatt.setCharacteristicNotification(). Device: %s.",
						AmberWatch.this.bluetoothDevice));
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
	
	public void disconnect() {
		if (state != State.CONNECTED)
			return;
		
		try {
			gatt.disconnect();
		} catch (SecurityException e) {
			logger.warn("Security exception has thrown while calling BluetoothGatt.disconnect().");
		}
	}
	
	public State getState() {
		return state;
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
	
	@Override
	public boolean newAlert(String alertMessage) {
		if (alertMessage == null)
			return false;
		
		if (alertMessage.length() > 96)
			return false;
		
		try {
			byte[] alertMessageBytes = getAlertMessageBytes(alertMessage);
			if (newAlertcharacteristics.setValue(alertMessageBytes))
				return gatt.writeCharacteristic(newAlertcharacteristics);
			
			return false;
		} catch (IOException e) {
			AmberUtils.toastInService("Failed to get alert message bytes.");
			return false;
		}
	}
	
	private byte fromUint8(int value) {
		return (byte)(value & 0xff);
	}
	
	private byte[] toUtf8s(String message) {
		return message.getBytes(StandardCharsets.UTF_8);
	}
	
	private byte[] getAlertMessageBytes(String alertMessage) throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream(96);
		stream.write(fromUint8(9));
		stream.write(fromUint8(1));
		stream.write(0x00);
		stream.write(toUtf8s(alertMessage));
		
		return stream.toByteArray();
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
