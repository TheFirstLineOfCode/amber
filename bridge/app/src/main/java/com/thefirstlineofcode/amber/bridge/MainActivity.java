package com.thefirstlineofcode.amber.bridge;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
		NavigationView.OnNavigationItemSelectedListener, ILanNodeManager.Listener {
	private static final Logger logger = LoggerFactory.getLogger(MainActivity.class);
	
	public static final int BLUETOOTH_PERMISSIONS_REQUEST_CODE = 200;
	
	private FloatingActionButton fab;
	
	private RecyclerView lanNodesView;
	private List<LanNode> lanNodes;
	private LanNodesAdapter lanNodesAdapter;
	
	private BluetoothAdapter adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if ((checkSelfPermission(Manifest.permission.BLUETOOTH) ==
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
							Manifest.permission.BLUETOOTH,
							Manifest.permission.BLUETOOTH_ADMIN,
							Manifest.permission.BLUETOOTH_SCAN,
							Manifest.permission.BLUETOOTH_CONNECT,
							Manifest.permission.ACCESS_FINE_LOCATION,
							Manifest.permission.ACCESS_COARSE_LOCATION
					}, BLUETOOTH_PERMISSIONS_REQUEST_CODE);
		} else {
			onCreate();
		}
	}
	
	private void onCreate() {
		setContentView(R.layout.activity_main);
		
		MaterialToolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		
		fab = findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				launchDiscoveryActivity();
			}
		});
		fab.show();
		
		DrawerLayout drawer = findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
				this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawer.addDrawerListener(toggle);
		toggle.syncState();
		
		NavigationView navigationView = findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);
		
		lanNodesView = findViewById(R.id.lanNodesView);
		lanNodesView.setHasFixedSize(true);
		lanNodesView.setLayoutManager(new LinearLayoutManager(this));
		
		ILanNodeManager lanNodeManager = ((ILanNodeManager)getApplication());
		lanNodeManager.addLanNodeListener(this);
		
		lanNodes = getLanNodesWithDevices(lanNodeManager.getLanNodes());
		lanNodesAdapter = new LanNodesAdapter(this, lanNodes);
		lanNodesAdapter.setHasStableIds(true);
		
		lanNodesView.setAdapter(lanNodesAdapter);
		
		logger.info("Amberbridge started.");
	}
	
	private List<LanNode> getLanNodesWithDevices(LanNode[] lanNodes) {
		List<LanNode> lanNodesWithDevices = new ArrayList<>();
		
		for (LanNode lanNode : lanNodes) {
			AmberWatch device = AmberWatch.createInstance(getAdapter(), lanNode.getThing());
			lanNodesWithDevices.add(new LanNode(lanNode.getLanId(), device));
		}
		
		return lanNodesWithDevices;
	}
	
	private BluetoothAdapter getAdapter() {
		if (adapter == null)
			adapter = BluetoothAdapter.getDefaultAdapter();
		
		return adapter;
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		
		if (requestCode == BLUETOOTH_PERMISSIONS_REQUEST_CODE) {
			onCreate();
		} else {
			new AlertDialog.Builder(this).
					setTitle("Error").
					setMessage("User denied permissions request. App will exit.").
					setPositiveButton("Ok", (dialog, which) -> {
						finish();
					}).create().show();
		}
	}
	
	@Override
	public boolean onNavigationItemSelected(@NonNull MenuItem item) {
		DrawerLayout drawer = findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);
		
		switch (item.getItemId()) {
			case R.id.device_action_discover:
				launchDiscoveryActivity();
				return false;
			case R.id.action_quit:
				quit();
				return false;
			case R.id.about:
				Intent aboutIntent = new Intent(this, AboutActivity.class);
				startActivity(aboutIntent);
				return false;
		}
		
		return false;
	}
	
	private void quit() {
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle("Quit").setMessage("Are you sure you want to quit app?").
				setPositiveButton(R.string.yes_quit_app, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						MainActivity.this.finish();
						
						logger.info("Quiting Amberbridge....");
						System.exit(0);
					}
				}).
				setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						}
				);
		
		runOnUiThread(() -> {
			AlertDialog dialog = builder.create();
			dialog.show();
		});
	}
	
	private void launchDiscoveryActivity() {
		startActivity(new Intent(this, DiscoveryActivity.class));
	}
	
	@Override
	public void thingAdded(IBleThing thing) {
		LanNode lanNode = new LanNode(null, AmberWatch.createInstance(getAdapter(), thing));
		if (!lanNodes.contains(lanNode)) {
			logger.info(String.format("Device added. Device: %s.", thing));
			lanNodes.add(lanNode);
		}
	}
	
	@Override
	public void nodeAdded(String thingId, int lanId) {
	}
}