package com.thefirstlineofcode.amber.bridge;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;

public class ConfigureHostActivity extends AppCompatActivity {
	public static final int PERMISSIONS_REQUEST_CODE = 100;
	
	private static final Logger logger = LoggerFactory.getLogger(ConfigureHostActivity.class);
	
	private AutoCompleteTextView atvHosts;
	private Button btConnect;
	private TextView tvConfigureStream;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if ((checkSelfPermission(Manifest.permission.INTERNET) ==
					PackageManager.PERMISSION_DENIED) ||
				(checkSelfPermission(Manifest.permission.BLUETOOTH) ==
					PackageManager.PERMISSION_DENIED) ||
				(checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) ==
						PackageManager.PERMISSION_DENIED) ||
				(checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)  ==
						PackageManager.PERMISSION_DENIED) ||
				(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
						PackageManager.PERMISSION_DENIED) ||
				(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
						PackageManager.PERMISSION_DENIED) ||
				(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
						PackageManager.PERMISSION_DENIED)) {
			requestPermissions(
					new String[] {
							Manifest.permission.INTERNET,
							Manifest.permission.BLUETOOTH,
							Manifest.permission.BLUETOOTH_ADMIN,
							Manifest.permission.BLUETOOTH_SCAN,
							Manifest.permission.BLUETOOTH_CONNECT,
							Manifest.permission.ACCESS_FINE_LOCATION,
							Manifest.permission.ACCESS_COARSE_LOCATION
					}, PERMISSIONS_REQUEST_CODE);
		} else {
			onCreate();
		}
	}
	
	private void onCreate() {
		String[] availableHosts = MainApplication.getInstance().getAvailableHosts();
		
		setContentView(R.layout.activity_configure_host);
		
		atvHosts = findViewById(R.id.actv_hosts);
		ArrayAdapter<String> hostsAdapter = new ArrayAdapter<String>
				(this, android.R.layout.select_dialog_item, availableHosts);
		
		atvHosts.setThreshold(2);
		atvHosts.setAdapter(hostsAdapter);
		
		btConnect = findViewById(R.id.bt_connect);
		btConnect.setOnClickListener(new ConnectButtonListener());
		
		tvConfigureStream = findViewById(R.id.tv_configure_stream);
		int selectedHost = atvHosts.getListSelection();
		tvConfigureStream.setEnabled(selectedHost != ListView.INVALID_POSITION);
		tvConfigureStream.setOnClickListener(new ConfigureStreamTextViewListener());
	}
	
	private class ConfigureStreamTextViewListener implements View.OnClickListener {
		@Override
		public void onClick(View view) {
			String host = getHostFromHostControl();
			if (!isValidHostAddress(ConfigureHostActivity.this, host))
				return;
			
			Intent configureSteamActivityIntent = new Intent(ConfigureHostActivity.this, ConfigureHostActivity.class);
			configureSteamActivityIntent.putExtra(getString(R.string.configured_host), host);
			startActivity(configureSteamActivityIntent);
		}
	}
	
	private class ConnectButtonListener implements View.OnClickListener {
			@Override
			public void onClick(View view) {
				String host = getHostFromHostControl();
				if (!isValidHostAddress(ConfigureHostActivity.this, host))
					return;
				
				Intent iotBgServiceIntent = new Intent(ConfigureHostActivity.this, IotBgService.class);
				iotBgServiceIntent.putExtra(ConfigureHostActivity.this.getString(R.string.current_host), host);
				startService(iotBgServiceIntent);
				
				Intent mainActivityIntent = new Intent(ConfigureHostActivity.this, MainActivity.class);
				startActivity(mainActivityIntent);
			}
	}
	
	private boolean isValidHostAddress(Context context, String host) {
		if (host == null) {
			runOnUiThread(() -> {
				Toast.makeText(context, "Error: Can't connect to null host.", Toast.LENGTH_SHORT).show();
			});
			
			return false;
		}
		
		return true;
	}
	
	@Nullable
	private String getHostFromHostControl() {
		return atvHosts.getText().length() == 0 ? null : atvHosts.getText().toString();
	}
	
	private String[] getHosts(HostConfiguration[] hostConfigurations) {
		if (hostConfigurations == null || hostConfigurations.length == 0)
			return new String[0];
		
		String[] hosts = new String[hostConfigurations.length];
		
		for (int i = 0; i < hostConfigurations.length; i++) {
			hosts[i] = hostConfigurations[i].getHost();
		}
		
		return hosts;
	}
	
	private String[] getHostNames(String hostNames) {
		return hostNames.split(",");
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		
		if (requestCode == PERMISSIONS_REQUEST_CODE && allPermissionsGranted(grantResults)) {
			onCreate();
		} else {
			new AlertDialog.Builder(this).
					setTitle("Error").
					setMessage("User denied permissions request. App will exit.").
					setPositiveButton("Ok", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									finish();
								}
							}
					).create().show();
		}
	}
	
	private boolean allPermissionsGranted(int[] grantResults) {
		for (int grantResult : grantResults) {
			if (grantResult != PackageManager.PERMISSION_GRANTED)
				return false;
		}
		
		return true;
	}
}
