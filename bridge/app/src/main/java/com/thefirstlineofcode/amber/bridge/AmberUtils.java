package com.thefirstlineofcode.amber.bridge;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.widget.Toast;

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
}
