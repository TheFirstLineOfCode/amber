package com.thefirstlineofcode.amber.bridge;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import java.util.List;

public class MainActivity extends AppCompatActivity implements
		NavigationView.OnNavigationItemSelectedListener, ILanNodeManager.Listener {
	private static final Logger logger = LoggerFactory.getLogger(MainActivity.class);
	
	private FloatingActionButton fab;
	
	private RecyclerView lanNodesView;
	private List<LanNode> lanNodes;
	private LanNodesAdapter lanNodesAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
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
		lanNodeManager.addListener(this);
		
		lanNodes = lanNodeManager.getLanNodes();
		lanNodesAdapter = new LanNodesAdapter(this, lanNodes);
		lanNodesAdapter.setHasStableIds(true);
		
		lanNodesView.setAdapter(lanNodesAdapter);
		
		logger.info("Amberbridge started.");
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
	public void deviceAdded(Device device) {
		LanNode lanNode = new LanNode(Device.getThingId(device), device);
		if (!lanNodes.contains(lanNode)) {
			logger.info(String.format("Device added. Device: %s.", device));
			lanNodes.add(lanNode);
		}
	}
	
	@Override
	public void nodeAdded(String thingId, int lanId) {
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		lanNodesAdapter.notifyDataSetChanged();
	}
}