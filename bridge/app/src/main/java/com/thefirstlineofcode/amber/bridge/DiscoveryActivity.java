package com.thefirstlineofcode.amber.bridge;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.thefirstlineofcode.sand.client.thing.ThingsUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class DiscoveryActivity extends AppCompatActivity
		implements AdapterView.OnItemClickListener {
	private static final Logger logger = LoggerFactory.getLogger(DiscoveryActivity.class);
	private static final long SCAN_DURATION = 30000;
	private static final short RSSI_UNKNOWN = 0;
	
	private List<DeviceCandidate> deviceCandidates = new ArrayList<>();
	private DeviceCandidatesAdapter deviceCandidatesAdapter;
	private Set<BTUUIDPair> foundCandidates = new HashSet<>();
	private boolean ignoreBonded;
	
	private Button startButton;
	private ProgressBar bluetoothProgress;
	private Status status;
	private final Handler handler = new Handler();
	private BluetoothAdapter adapter;
	private ScanCallback newScanCallback = null;
	
	private enum Status {
		NORMAL,
		SCANNING,
		BONDING
	}
	
	private final BroadcastReceiver scanningReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			switch (Objects.requireNonNull(intent.getAction())) {
				case BluetoothAdapter.ACTION_DISCOVERY_STARTED: {
					logger.debug("ACTION_DISCOVERY_STARTED");
					break;
				}
				case BluetoothAdapter.ACTION_STATE_CHANGED: {
					logger.debug("ACTION_STATE_CHANGED ");
					bluetoothStateChanged(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF));
					break;
				}
				case BluetoothDevice.ACTION_FOUND: {
					logger.debug("ACTION_FOUND");
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					handleDeviceFound(device, intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, RSSI_UNKNOWN));
					break;
				}
				case BluetoothDevice.ACTION_UUID: {
					logger.debug("ACTION_UUID");
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					String deviceName = null;
					try {
						deviceName = device.getName();
					} catch (SecurityException e) {
						logger.error("Security exception has been thrown when calling device.getName().");
					}
					
					if (!isInfiniTimeDevice(deviceName)) {
						break;
					}
					
					short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, RSSI_UNKNOWN);
					Parcelable[] uuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
					ParcelUuid[] uuids2 = AmberUtils.toParcelUuids(uuids);
					addToCandidateListIfNotAlreadyProcessed(device, rssi, uuids2);
					break;
				}
			}
		}
	};
	
	private final BroadcastReceiver bondingReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (!BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
				logger.error("Not ACTION_BOND_STATE_CHANGED action????");
				return;
			}
			
			BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			if (device == null) {
				if (logger.isErrorEnabled())
					logger.error("Bonding receiver received a null bonded device!!!");
				return;
			}
			
			try {
				if (!isInfiniTimeDevice(device.getName())) {
					return;
				}
			} catch (SecurityException e) {
				throw new RuntimeException("Failed to call device.getName().", e);
			}
			
			int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
			if (logger.isDebugEnabled()) {
				logger.debug("ACTION_BOND_STATE_CHANGED");
				logger.debug(String.format(Locale.ENGLISH, "Bond state: %d", bondState));
			}
			
			switch (bondState) {
				case BluetoothDevice.BOND_BONDED: {
					if (device != null) {
						if (bondState == BluetoothDevice.BOND_BONDED) {
							DiscoveryActivity.this.deviceFound(device);
						}
					}
					break;
				}
				case BluetoothDevice.BOND_BONDING: {
					// Still bonding
					break;
				}
				case BluetoothDevice.BOND_NONE: {
					// Not bonded
					logger.warn("Failed to binding device: " + device.getAddress() + ": " + bondState);
					break;
				}
				default: {
					logger.warn("Unknown bond state for device: " + device.getAddress() + ": " + bondState);
				}
			}
		}
	};
	
	private final Runnable stopRunnable = new Runnable() {
		@Override
		public void run() {
			stopDiscovery();
			logger.info("Discovery stopped by thread timeout.");
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_discovery);
		
		startButton = findViewById(R.id.discovery_start);
		startButton.setOnClickListener(view -> onStartButtonClick(startButton));
		
		bluetoothProgress = findViewById(R.id.discovery_progressbar);
		bluetoothProgress.setProgress(0);
		bluetoothProgress.setIndeterminate(true);
		bluetoothProgress.setVisibility(View.GONE);
		
		ignoreBonded = false;
		
		ListView deviceCandidatesView = findViewById(R.id.discovery_device_candidates_list);
		
		deviceCandidatesAdapter = new DeviceCandidatesAdapter(this, deviceCandidates);
		deviceCandidatesView.setAdapter(deviceCandidatesAdapter);
		deviceCandidatesView.setOnItemClickListener(this);
		
		registerBroadcastReceivers();
		
		status = Status.NORMAL;
	}
	
	public void unregisterBroadcastReceivers() {
		safeUnregisterBroadcastReceiver(this, bondingReceiver);
		safeUnregisterBroadcastReceiver(this, scanningReceiver);
	}
	
	public boolean safeUnregisterBroadcastReceiver(Context context, BroadcastReceiver receiver) {
		try {
			context.unregisterReceiver(receiver);
			return true;
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}
	
	@Override
	protected void onResume() {
		registerBroadcastReceivers();
		
		super.onResume();
	}
	
	public void registerBroadcastReceivers() {
		registerScanningReceiver();
		registerBondingReceiver();
	}
	
	private void registerScanningReceiver() {
		IntentFilter scanningIntents = new IntentFilter();
		scanningIntents.addAction(BluetoothDevice.ACTION_FOUND);
		scanningIntents.addAction(BluetoothDevice.ACTION_UUID);
		scanningIntents.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		scanningIntents.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		
		registerReceiver(scanningReceiver, scanningIntents);
	}
	
	private void registerBondingReceiver() {
		IntentFilter bondingIntents = new IntentFilter();
		bondingIntents.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		
		registerReceiver(bondingReceiver, bondingIntents);
	}
	
	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		if (status == Status.SCANNING) {
			logger.warn("Try to bond device in Status: {}.", status);
			Toast.makeText(this,
					String.format("Try to bond device in Status: %s.", status),
					Toast.LENGTH_SHORT).show();
			return;
		}
		
		if (status == Status.BONDING) {
			Toast.makeText(this,
					"Is bonding....",
					Toast.LENGTH_SHORT).show();
			return;
		}
		
		DeviceCandidate deviceCandidate = deviceCandidates.get(position);
		if (deviceCandidate == null) {
			logger.error("Device candidate clicked, but item not found.");
			return;
		}
		
		bondToDevice(deviceCandidate);
	}
	
	private void bondToDevice(DeviceCandidate deviceCandidate) {
		stopDiscovery();
		
		try {
			adapter.cancelDiscovery();
			
			BluetoothDevice device = deviceCandidate.getDevice();
			if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
				logger.warn("Try to add a bonded device.!");
				deviceFound(device);
			} else {
				device.createBond();
			}
		} catch (SecurityException e) {
			logger.error("SecurityException has thrown when calling adapter.cancelDiscovery().");
			return;
		}
	}
	
	private void deviceFound(BluetoothDevice device) throws SecurityException {
		finish();
		
		ILanNodeManager lanNodeManager = (MainApplication)getApplication();
		lanNodeManager.addThing(
				new BleThing(String.format("amber-%s", ThingsUtils.generateRandomId(9)), device.getName(), device.getAddress()));
		lanNodeManager.saveLanNodes();
	}
	
	public void onStartButtonClick(View button) {
		if (logger.isDebugEnabled())
			logger.debug("Start button clicked");
		
		if (status == Status.SCANNING) {
			stopDiscovery();
		} else if (status == Status.NORMAL) {
			deviceCandidates.clear();
			deviceCandidatesAdapter.notifyDataSetChanged();
			
			startDiscovery();
		} else {
			logger.warn("Is start button clicked in Status.Bonding???");
		}
	}
	
	private boolean startDiscovery() {
		if (status != Status.NORMAL) {
			logger.warn("Not starting discovery in Status.NORMAL.");
			return false;
		}
		
		logger.info("Starting discovery");
		
		if (ensureBluetoothReady()) {
			startBTLEDiscovery();
			setStatus(Status.SCANNING);
		} else {
			runOnUiThread(() ->
					Toast.makeText(this,
							getString(R.string.discovery_enable_bluetooth),
							Toast.LENGTH_SHORT).show());
			setStatus(Status.NORMAL);
			return false;
		}
		
		return true;
	}
	
	private void startBTLEDiscovery() {
		logger.info("Starting BTLE discovery");
		
		try {
			// LineageOS quirk, can't start scan properly,
			// if scan has been started by something else
			stopBTLEDiscovery();
		} catch (Exception ignored) {}
		
		handler.removeMessages(0, stopRunnable);
		handler.sendMessageDelayed(getPostMessage(stopRunnable), SCAN_DURATION);
		
		try {
			adapter.getBluetoothLeScanner().startScan(null, getScanSettings(), getScanCallback());
		} catch (SecurityException e) {
            /* This should never happen because we call this from startDiscovery,
            which checks ensureBluetoothReady. But we add try...catch to stop Android Studio errors */
			logger.error("SecurityException on startScan");
		}
	}
	
	private boolean ensureBluetoothReady() {
		boolean available = MainApplication.checkBluetoothAvailable(this);
		if (available) {
			if (adapter == null)
				adapter = BluetoothAdapter.getDefaultAdapter();
			
			try {
				adapter.getBluetoothLeScanner().stopScan(getScanCallback());
			} catch (SecurityException e) {
                /* This should never happen because checkBluetoothAvailable should return false
                if we don't have permissions. But we add try...catch to stop Android Studio errors */
				logger.error("SecurityException on adapter.cancelDiscovery, but checkBluetoothAvailable()=true!");
			}
			// must not return the result of cancelDiscovery()
			// appears to return false when currently not scanning
			return true;
		}
		
		return false;
	}
	
	private Message getPostMessage(Runnable runnable) {
		Message message = Message.obtain(handler, runnable);
		message.obj = runnable;
		return message;
	}
	
	private void stopDiscovery() {
		logger.info("Stopping discovery");
		stopBTLEDiscovery();
		
		setStatus(Status.NORMAL);
		handler.removeMessages(0, stopRunnable);
	}
	
	private void setStatus(Status status) {
		if (adapter == null)
			return;
		
		this.status = status;
		if (status == Status.SCANNING) {
			startButton.setText(getString(R.string.discovery_stop_scanning));
			bluetoothProgress.setVisibility(View.VISIBLE);
		} else {
			startButton.setText(getString(R.string.discovery_start_scanning));
			bluetoothProgress.setVisibility(View.GONE);
		}
	}
	
	private void stopBTLEDiscovery() {
		if (adapter == null)
			return;
		
		BluetoothLeScanner bluetoothLeScanner = adapter.getBluetoothLeScanner();
		if (bluetoothLeScanner == null) {
			logger.warn("Could not get BluetoothLeScanner()!");
			return;
		}
		
		if (newScanCallback == null) {
			logger.warn("newLeScanCallback == null!");
			return;
		}
		
		try {
			bluetoothLeScanner.stopScan(newScanCallback);
		} catch (NullPointerException e) {
			logger.warn("Internal NullPointerException when stopping the scan!");
			return;
		} catch (SecurityException e) {
            /* This should never happen because ensureBluetoothReady should set adaptor=null,
            but we add try...catch to stop Android Studio errors */
			logger.error("SecurityException on adapter.stopScan");
		}
		
		logger.debug("Stopped BLE discovery");
	}
	
	private ScanCallback getScanCallback() {
		if (newScanCallback != null) {
			return newScanCallback;
		}
		
		newScanCallback = new ScanCallback() {
			@Override
			public void onScanResult(int callbackType, ScanResult result)  {
				super.onScanResult(callbackType, result);
				try {
					ScanRecord scanRecord = result.getScanRecord();
					ParcelUuid[] uuids = null;
					if (scanRecord != null) {
						List<ParcelUuid> serviceUuids = scanRecord.getServiceUuids();
						if (serviceUuids != null) {
							uuids = serviceUuids.toArray(new ParcelUuid[0]);
						}
					}
					String deviceName = "[unknown]";
					try {
						deviceName = result.getDevice().getName();
					} catch (SecurityException e) {
                        /* This should never happen because we need all the permissions
                        to get to the point where we can even scan, but 'SecurityException' check
                        is added to stop Android Studio errors */
						logger.error("SecurityException on device.getName()");
					}
					
					if (deviceName == null)
						deviceName = "[unknown]";
					
					logger.warn("Scan result: " + deviceName + " - " +
							result.getDevice().getAddress() +
							((scanRecord != null) ? scanRecord.getBytes().length : -1));
					
					if (!isInfiniTimeDevice(deviceName)) {
						if (logger.isDebugEnabled())
							logger.debug("Not a InfiniTime device. Ignore it.");
						
						return;
					}
					
					addToCandidateListIfNotAlreadyProcessed(result.getDevice(), (short)result.getRssi(), uuids);
				} catch (NullPointerException e) {
					logger.warn("Error handling scan result", e);
				}
			}
		};
		
		return newScanCallback;
	}
	
	private ScanSettings getScanSettings() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			return new ScanSettings.Builder()
					.setCallbackType(android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
					.setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
					.setMatchMode(android.bluetooth.le.ScanSettings.MATCH_MODE_STICKY)
					.setPhy(android.bluetooth.le.ScanSettings.PHY_LE_ALL_SUPPORTED)
					.setNumOfMatches(android.bluetooth.le.ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
					.build();
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			return new ScanSettings.Builder()
					.setCallbackType(android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
					.setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
					.setMatchMode(android.bluetooth.le.ScanSettings.MATCH_MODE_STICKY)
					.setNumOfMatches(android.bluetooth.le.ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
					.build();
		} else {
			return new ScanSettings.Builder()
					.setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
					.build();
		}
	}
	
	private void bluetoothStateChanged(int newState) {
		if (newState == BluetoothAdapter.STATE_ON) {
			this.adapter = BluetoothAdapter.getDefaultAdapter();
			startButton.setEnabled(true);
		} else if (newState == BluetoothAdapter.STATE_OFF) {
			this.adapter = null;
			startButton.setEnabled(false);
			bluetoothProgress.setVisibility(View.GONE);
		} else {
			// Ignore.
		}
	}
	
	private void handleDeviceFound(BluetoothDevice device, short rssi) throws SecurityException {
		if (device.getName() != null) {
			if (handleDeviceFound(device, rssi, null)) {
				logger.info("found supported device " + device.getName() + " without scanning services, skipping service scan.");
				return;
			}
		}
		
		ParcelUuid[] uuids = device.getUuids();
		if (uuids == null) {
			if (device.fetchUuidsWithSdp()) {
				return;
			}
		}
		
		addToCandidateListIfNotAlreadyProcessed(device, rssi, uuids);
	}
	
	private boolean handleDeviceFound(BluetoothDevice device, short rssi, ParcelUuid[] uuids) throws SecurityException {
		logger.info("Found device: " + device.getName() + ", " + device.getAddress());
		
		if (logger.isInfoEnabled()) {
			if (uuids != null && uuids.length > 0) {
				for (ParcelUuid uuid : uuids) {
					logger.debug("  supports uuid: " + uuid.toString());
				}
			}
		}
		
		boolean bonded = false;
		try {
			bonded = (device.getBondState() == BluetoothDevice.BOND_BONDED);
		} catch (SecurityException e) {
            /* This should never happen because we need all the permissions
               to get to the point where we can even scan, but 'SecurityException' check
               is added to stop Android Studio errors */
			logger.error("SecurityException on device.getBondState().");
		}
		
		if (bonded && ignoreBonded) {
			if (logger.isDebugEnabled())
				logger.debug("The device has already bonded. Ignore to rebond it.");
			
			return true; // Ignore already bonded devices
		}
		
		if (!isInfiniTimeDevice(device.getName())) {
			if (logger.isInfoEnabled())
				logger.info("Not a InfiniTime device. Ignore it. Device name: {}.", device.getName());
			
			return false;
		}
		
		DeviceCandidate candidate = new DeviceCandidate(device, rssi, uuids);
		
		logger.info("InfiniTime device found. Device: {}.", candidate);
		
		int index = deviceCandidates.indexOf(candidate);
		if (index >= 0) {
			deviceCandidates.set(index, candidate); // replace
		} else {
			deviceCandidates.add(candidate);
		}
			
		deviceCandidatesAdapter.notifyDataSetChanged();
		return true;
	}
	
	private boolean isInfiniTimeDevice(String name) {
		if (name == null)
			return false;
		
		return name.startsWith("Pinetime-JF") || name.startsWith("InfiniTime");
	}
	
	private void addToCandidateListIfNotAlreadyProcessed(BluetoothDevice device, short rssi, ParcelUuid[] uuids) {
		if (uuids == null) {
			String deviceName = null;
			try {
				deviceName = device.getName();
			} catch (SecurityException e) {
				logger.error("Security exception has been thrown when calling device.getName().");
				return;
			}
			
			if (logger.isWarnEnabled())
				logger.warn("Null UUIDs device. Device name: {}.", deviceName);
		}
		
		BTUUIDPair btUuidPair = new BTUUIDPair(device, uuids);
		if (foundCandidates.contains(btUuidPair)) {
			logger.info("candidate already processed, skipping");
			return;
		}
		
		if (handleDeviceFound(device, rssi, uuids)) {
			//device was considered a candidate, do not process it again unless something changed
			foundCandidates.add(btUuidPair);
		}
	}
	
	@Override
	protected void onPause() {
		unregisterBroadcastReceivers();
		stopDiscovery();
		
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		unregisterBroadcastReceivers();
		stopDiscovery();
		
		super.onDestroy();
	}
	
	private class BTUUIDPair {
		private final BluetoothDevice bluetoothDevice;
		private final ParcelUuid[] parcelUuid;
		
		public BTUUIDPair(BluetoothDevice bluetoothDevice, ParcelUuid[] parcelUuid) {
			this.bluetoothDevice = bluetoothDevice;
			this.parcelUuid = parcelUuid;
		}
		
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			BTUUIDPair that = (BTUUIDPair) o;
			return bluetoothDevice.equals(that.bluetoothDevice) && Arrays.equals(parcelUuid, that.parcelUuid);
		}
		
		@Override
		public int hashCode() {
			int result = Objects.hash(bluetoothDevice);
			result = 31 * result + Arrays.hashCode(parcelUuid);
			return result;
		}
	}
}
