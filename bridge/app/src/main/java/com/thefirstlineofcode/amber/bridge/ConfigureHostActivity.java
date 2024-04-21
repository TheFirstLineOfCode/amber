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
		
		atvHosts.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (!atvHosts.isPopupShowing())
					atvHosts.showDropDown();
			}
		});
		
		setCurrentHostInControl(availableHosts);
		
		btConnect = findViewById(R.id.bt_connect);
		btConnect.setOnClickListener(new ConnectButtonListener());
		
		tvConfigureStream = findViewById(R.id.tv_configure_stream);
		tvConfigureStream.setOnClickListener(new ConfigureStreamTextViewListener());
	}
	
	private void setCurrentHostInControl(String[] availableHosts) {
		String currentHost = MainApplication.getInstance().getCurrentHost();
		if (currentHost == null && availableHosts != null && availableHosts.length == 1)
			currentHost = availableHosts[0];
		atvHosts.setText(currentHost);
	}
	
	private int findCurrentHost(String[] availableHosts, String currentHost) {
		for (int i = 0; i < availableHosts.length; i++) {
			if (availableHosts[i].equals(currentHost))
				return i;
		}
		
		return -1;
	}
	
	private class ConfigureStreamTextViewListener implements View.OnClickListener {
		@Override
		public void onClick(View view) {
			String host = getHostFromHostControl();
			if (!isValidHostAddress(ConfigureHostActivity.this, host))
				return;
			
			Intent configureSteamActivityIntent =
				new Intent(ConfigureHostActivity.this, ConfigureStreamActivity.class).
					putExtra(getString(R.string.configured_host), host);
			startActivity(configureSteamActivityIntent);
		}
	}
	
	private class ConnectButtonListener implements View.OnClickListener {
			@Override
			public void onClick(View view) {
				String host = getHostFromHostControl();
				if (!isValidHostAddress(ConfigureHostActivity.this, host))
					return;
				
				IHostConfigurationManager hostConfigurationManager = MainApplication.getInstance();
				if (hostConfigurationManager.getHostConfiguration(host) == null) {
					hostConfigurationManager.addHostConfiguration(new HostConfiguration(host));
				}
				
				if (!host.equals(hostConfigurationManager.getCurrentHost())) {
					hostConfigurationManager.setCurrentHost(host);
				}
				
				if (hostConfigurationManager.isHostConfigurationsChanged())
					hostConfigurationManager.saveHostConfigurations();
				
				Intent iotBgServiceIntent =
					new Intent(ConfigureHostActivity.this, IotBgService.class).
						putExtra(ConfigureHostActivity.this.getString(R.string.current_host), host);
				startService(iotBgServiceIntent);
				
				Intent mainActivityIntent = new Intent(ConfigureHostActivity.this, MainActivity.class);
				startActivity(mainActivityIntent);
			}
	}
	
	private boolean isValidHostAddress(Context context, String host) {
		if (host == null) {
			runOnUiThread(() -> {
				Toast.makeText(context, "Error: Null host.", Toast.LENGTH_SHORT).show();
			});
			
			return false;
		}
		
		try {
			InetAddress inetAddress = Inet4Address.getByName(host);
			if (!(inetAddress instanceof Inet4Address)) {
				runOnUiThread(() -> {
					Toast.makeText(context, getString(R.string.host_must_be_an_ipv4_address), Toast.LENGTH_SHORT).show();
				});
				
				return false;
			}
		} catch (Exception e) {
			runOnUiThread(() -> {
				Toast.makeText(this, getString(R.string.host_must_be_an_ipv4_address), Toast.LENGTH_LONG).show();
			});
			
			return false;
		}
		
		return true;
	}
	
	@Nullable
	private String getHostFromHostControl() {
		return atvHosts.getText().length() == 0 ? null : atvHosts.getText().toString();
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
