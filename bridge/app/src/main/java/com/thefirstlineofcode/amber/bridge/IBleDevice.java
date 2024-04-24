package com.thefirstlineofcode.amber.bridge;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

public interface IBleDevice extends IBleThing {
	public enum State {
		NOT_CONNECTED,
		CONNECTING,
		CONNECTED,
		DISCONNECTING
	}
	
	public interface StateListener {
		void connected(IBleDevice device, BluetoothGatt gatt);
		void disconnected(IBleDevice device);
		void occurred(IBleDevice device, Error error);
	}
	
	public enum Error {
		FAILED_TO_CONNECT_TO_DEVICE,
		SECURITY_EXCEPTION_HAS_THROWN
	}
	
	BluetoothDevice getBluetoothDevice();
	
	void connect();
	void disconnect();
	
	State getState();
	
	void setAutoConnect(boolean autoConnect);
	boolean isAutoConnect();
}
