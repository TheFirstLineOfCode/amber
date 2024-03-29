package com.thefirstlineofcode.amber.bridge;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DeviceCandidate {
	private static final Logger logger = LoggerFactory.getLogger(DeviceCandidate.class);
	
	private final BluetoothDevice device;
	private final short rssi;
	private final ParcelUuid[] serviceUuids;
	private String deviceName;
	
	public DeviceCandidate(BluetoothDevice device, short rssi, ParcelUuid[] serviceUuids) {
		this.device = device;
		this.rssi = rssi;
		this.serviceUuids = mergeServiceUuids(serviceUuids, device.getUuids());
	}
	
	private DeviceCandidate(Parcel in) {
		device = in.readParcelable(getClass().getClassLoader());
		if (device == null) {
			throw new IllegalStateException("Unable to read state from Parcel");
		}
		rssi = (short) in.readInt();
		
		ParcelUuid[] uuids = AmberUtils.toParcelUuids(in.readParcelableArray(getClass().getClassLoader()));
		serviceUuids = mergeServiceUuids(uuids, device.getUuids());
	}
	
	public BluetoothDevice getDevice() {
		return device;
	}
	
	public String getMacAddress() {
/*		return device != null ? device.getAddress() :
				MainApplication.getContext().getString(R.string._unknown_);*/
		return device != null ? device.getAddress() : "Unknown";
	}
	
	private ParcelUuid[] mergeServiceUuids(ParcelUuid[] serviceUuids, ParcelUuid[] deviceUuids) {
		Set<ParcelUuid> uuids = new HashSet<>();
		if (serviceUuids != null) {
			uuids.addAll(Arrays.asList(serviceUuids));
		}
		if (deviceUuids != null) {
			uuids.addAll(Arrays.asList(deviceUuids));
		}
		return uuids.toArray(new ParcelUuid[0]);
	}
	
	@NonNull
	public ParcelUuid[] getServiceUuids() {
		return serviceUuids;
	}
	
	public boolean supportsService(UUID aService) {
		ParcelUuid[] uuids = getServiceUuids();
		if (uuids.length == 0) {
			logger.warn("no cached services available for " + this);
			return false;
		}
		
		for (ParcelUuid uuid : uuids) {
			if (uuid != null && aService.equals(uuid.getUuid())) {
				return true;
			}
		}
		return false;
	}
	
	public String getName() {
		if (this.deviceName != null ) {
			return this.deviceName;
		}
		try {
			Method method = device.getClass().getMethod("getAliasName");
			if (method != null) {
				deviceName = (String) method.invoke(device);
			}
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {
			logger.info("Could not get device alias for " + device.getName());
		}
		if (deviceName == null || deviceName.length() == 0) {
			deviceName = device.getName();
		}
		if (deviceName == null || deviceName.length() == 0) {
			deviceName = "(unknown)";
		}
		return deviceName;
	}
	
	public short getRssi() {
		return rssi;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		
		DeviceCandidate that = (DeviceCandidate)o;
		return device.getAddress().equals(that.device.getAddress());
	}
	
	@Override
	public int hashCode() {
		return device.getAddress().hashCode() ^ 37;
	}
	
	@Override
	public String toString() {
		return String.format("DeviceCandidate[]", deviceName, device.getAddress());
	}
}
