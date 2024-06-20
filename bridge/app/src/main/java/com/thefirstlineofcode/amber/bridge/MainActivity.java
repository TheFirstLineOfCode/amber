package com.thefirstlineofcode.amber.bridge;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
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
import com.thefirstlineofcode.amber.protocol.QueryWatchState;
import com.thefirstlineofcode.amber.protocol.WatchState;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.BadRequest;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.ItemNotFound;
import com.thefirstlineofcode.sand.client.actuator.IExecutor;
import com.thefirstlineofcode.sand.client.concentrator.IConcentrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
		NavigationView.OnNavigationItemSelectedListener, IThingNodeManager.Listener,
			ServiceConnection {
	private static final Logger logger = LoggerFactory.getLogger(MainActivity.class);
	
	public static final int BLUETOOTH_PERMISSIONS_REQUEST_CODE = 200;
	
	private FloatingActionButton fab;
	
	private RecyclerView thingNodesView;
	private List<ThingNode> thingNodes;
	private ThingNodesAdapter thingNodesAdapter;
	
	private BluetoothAdapter adapter;
	
	private IIotBgService iotBgService;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (!AmberUtils.checkPermissionsAppNeeded(this)) {
			requestPermissions(AmberUtils.getPermissionsAppNeeded(),
					BLUETOOTH_PERMISSIONS_REQUEST_CODE);
		} else {
			onCreate();
		}
	}
	
	private void onCreate() {
		String host = getIntent().getStringExtra(getString(R.string.current_host));
		bindIotBgService(host);
		
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
		
		thingNodesView = findViewById(R.id.thingNodesView);
		thingNodesView.setHasFixedSize(true);
		thingNodesView.setLayoutManager(new LinearLayoutManager(this));
		
		IThingNodeManager thingNodeManager = ((IThingNodeManager)getApplication());
		thingNodeManager.addThingNodeListener(this);
		
		thingNodes = getThingNodesWithAmberWatch(thingNodeManager.getThingNodes());
		thingNodesAdapter = new ThingNodesAdapter(this, thingNodes);
		thingNodesAdapter.setHasStableIds(true);
		thingNodesView.setAdapter(thingNodesAdapter);
		
		logger.info("Amberbridge started.");
	}
	
	private void bindIotBgService(String host) {
		Intent bindIotBgServiceIntent =
				new Intent(this, IotBgService.class).
						putExtra(getString(R.string.current_host), host);
		
		bindService(bindIotBgServiceIntent, this, BIND_AUTO_CREATE);
	}
	
	private List<ThingNode> getThingNodesWithAmberWatch(List<ThingNode> thingNodes) {
		List<ThingNode> thingNodesWithAmberWatch = new ArrayList<>();
		
		for (ThingNode thingNode : thingNodes) {
			AmberWatch device = AmberWatch.createInstance(getAdapter(), thingNode.getThing());
			thingNodesWithAmberWatch.add(new ThingNode(thingNode.getLanId(), device));
		}
		
		return thingNodesWithAmberWatch;
	}
	
	public class QueryWatchStateExecutor implements IExecutor<QueryWatchState> {
		@Override
		public Object execute(Iq iq, QueryWatchState action) throws ProtocolException {
			JabberId watchJid = iq.getTo();
			if (watchJid.getResource() == null ||
					Integer.toString(IConcentrator.LAN_ID_CONCENTRATOR).equals(watchJid.getResource())) {
				throw new ProtocolException(new BadRequest("Resource is NULL or is '0'."));
			}
			
			WatchState watchState = null;
			for (ThingNode thingNode : MainActivity.this.thingNodes) {
				if (thingNode.getLanId() == null)
					continue;
				
				if (Integer.toString(thingNode.getLanId()).equals(watchJid.getResource())) {
					AmberWatch watch = (AmberWatch) thingNode.getThing();
					watchState = new WatchState();
					watchState.setBatteryLevel(watch.getBatteryLevel());
					watchState.setStepCount(watch.getStepCount());
					
					break;
				}
			}
			
			if (watchState == null)
				throw new ProtocolException(new ItemNotFound(String.format(
						"Watch which's LAN ID is '%s' not be found", watchJid.getResource())));
			
			return watchState;
		}
	}
	
	private BluetoothAdapter getAdapter() {
		if (!MainApplication.checkBluetoothAvailable(this)) {
			AmberUtils.toastInService("Bluetooth isn't available");
		}
		
		if (adapter == null)
			adapter = BluetoothAdapter.getDefaultAdapter();
		
		return adapter;
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		
		if (requestCode == BLUETOOTH_PERMISSIONS_REQUEST_CODE &&
				AmberUtils.allPermissionsGranted(grantResults)) {
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
			case R.id.action_reconfigure_host:
				reconfigureHost();
				return false;
			case R.id.about:
				Intent aboutIntent = new Intent(this, AboutActivity.class);
				startActivity(aboutIntent);
				return false;
		}
		
		return false;
	}
	
	private void reconfigureHost() {
		if (iotBgService != null) {
			if (iotBgService.isConnectedToHost())
				iotBgService.disconnectFromHost();
			
			Intent stopIotBgServiceIntent = new Intent(this, IotBgService.class);
			stopService(stopIotBgServiceIntent);
			
			finish();
		}
		
		Intent configureHostActivityIntent = new Intent(this, ConfigureHostActivity.class);
		startActivity(configureHostActivityIntent);
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
		ThingNode thingNode = new ThingNode(null, AmberWatch.createInstance(getAdapter(), thing));
		if (!thingNodes.contains(thingNode)) {
			logger.info(String.format("Device added. Device: %s.", thing));
			thingNodes.add(thingNode);
			
			thingNodesAdapter.notifyDataSetChanged();
		}
	}
	
	@Override
	public void nodeAdded(String thingId, int lanId) {
		int position = -1;
		for (int i = 0; i < thingNodes.size(); i++) {
			if (thingNodes.get(i).getThing().getThingId().equals(thingId)) {
				position = i;
				break;
			}
		}
		
		if (position == -1)
			throw new IllegalArgumentException(String.format("Can't find the thing node which's thing ID is %s.", thingId));
		
		thingNodesAdapter.notifyItemChanged(position);
	}
	
	public IIotBgService getIotBgService() {
		return iotBgService;
	}
	
	@Override
	public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
		IotBgBinder binder = (IotBgBinder)iBinder;
		iotBgService = binder.getService();
	}
	
	@Override
	public void onServiceDisconnected(ComponentName componentName) {
		logger.warn("IoT background servie has disconnected.");
		
		iotBgService.disconnectFromHost();
		iotBgService = null;
	}
	
	@Override
	protected void onDestroy() {
		unbindService(this);
		
		super.onDestroy();
	}
}