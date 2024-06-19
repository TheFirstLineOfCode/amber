package com.thefirstlineofcode.amber.bridge;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmberUtils {
	private static final Logger logger = LoggerFactory.getLogger(AmberUtils.class);
	
	public static final int INFO = 1;
	public static final int WARN = 2;
	public static final int ERROR = 3;
	
	public static ParcelUuid[] toParcelUuids(Parcelable[] uuids) {
		if (uuids == null) {
			return null;
		}
		
		ParcelUuid[] uuids2 = new ParcelUuid[uuids.length];
		System.arraycopy(uuids, 0, uuids2, 0, uuids.length);
		
		return uuids2;
	}
	
	public static void log(String message, int severity, Throwable ex) {
		switch (severity) {
			case AmberUtils.INFO:
				logger.info(message, ex);
				break;
			case AmberUtils.WARN:
				logger.warn(message, ex);
				break;
			case AmberUtils.ERROR:
				logger.error(message, ex);
				break;
		}
	}
	
	public static String formatRssi(short rssi) {
		return String.valueOf(rssi);
	}
	
	public static void toastInService(String message) {
		Looper mainLooper = Looper.getMainLooper();
		if (Thread.currentThread() == mainLooper.getThread()) {
			Toast.makeText(MainApplication.getInstance(), message, Toast.LENGTH_LONG).show();
		} else {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					Toast.makeText(MainApplication.getInstance(), message, Toast.LENGTH_LONG).show();
				}
			};
			
			new Handler(Looper.getMainLooper()).post(runnable);
		}
	}
	
	public static int bytesToInteger(byte[] bytes) {
		// TODO
		return -1;
	}
	
	public static String[] getPermissionsAppNeeded() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			return new String[] {
					Manifest.permission.INTERNET,
					Manifest.permission.BLUETOOTH,
					Manifest.permission.BLUETOOTH_ADMIN,
					Manifest.permission.BLUETOOTH_SCAN,
					Manifest.permission.BLUETOOTH_CONNECT,
					Manifest.permission.ACCESS_FINE_LOCATION,
					Manifest.permission.ACCESS_COARSE_LOCATION
			};
		} else {
			return new String[] {
					Manifest.permission.INTERNET,
					Manifest.permission.BLUETOOTH,
					Manifest.permission.BLUETOOTH_ADMIN,
					Manifest.permission.ACCESS_FINE_LOCATION,
					Manifest.permission.ACCESS_COARSE_LOCATION
			};
		}
	}
	
	public static boolean checkPermissionsAppNeeded(Activity activity) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			return (activity.checkSelfPermission(Manifest.permission.INTERNET) ==
					PackageManager.PERMISSION_DENIED) ||
					(activity.checkSelfPermission(Manifest.permission.BLUETOOTH) ==
							PackageManager.PERMISSION_DENIED) ||
					(activity.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) ==
							PackageManager.PERMISSION_DENIED) ||
					(activity.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)  ==
							PackageManager.PERMISSION_DENIED) ||
					(activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
							PackageManager.PERMISSION_DENIED) ||
					(activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
							PackageManager.PERMISSION_DENIED) ||
					(activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
							PackageManager.PERMISSION_DENIED);
		} else {
			return (activity.checkSelfPermission(Manifest.permission.INTERNET) ==
					PackageManager.PERMISSION_DENIED) ||
					(activity.checkSelfPermission(Manifest.permission.BLUETOOTH) ==
							PackageManager.PERMISSION_DENIED) ||
					(activity.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) ==
							PackageManager.PERMISSION_DENIED) ||
					(activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
							PackageManager.PERMISSION_DENIED) ||
					(activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
							PackageManager.PERMISSION_DENIED);
		}
	}
	
	public static boolean allPermissionsGranted(int[] grantResults) {
		for (int i = 0; i < grantResults.length; i++) {
			if (i == 1 || i == 2)
				continue;
			
			int grantResult = grantResults[i];
			if (grantResult != PackageManager.PERMISSION_GRANTED)
				return false;
		}
		
		return true;
	}
}
