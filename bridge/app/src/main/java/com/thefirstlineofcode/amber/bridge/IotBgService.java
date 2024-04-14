package com.thefirstlineofcode.amber.bridge;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class IotBgService extends Service {
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return Service.START_STICKY;
	}
	
	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
