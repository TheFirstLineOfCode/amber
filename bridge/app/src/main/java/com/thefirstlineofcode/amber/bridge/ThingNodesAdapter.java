package com.thefirstlineofcode.amber.bridge;

import android.app.AlertDialog;
import android.bluetooth.BluetoothGatt;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.thefirstlineofcode.sand.client.concentrator.IConcentrator;
import com.thefirstlineofcode.sand.client.concentrator.LanNode;
import com.thefirstlineofcode.sand.protocols.thing.lora.BleAddress;

import java.util.List;

public class ThingNodesAdapter extends ListAdapter<ThingNode, ThingNodesAdapter.ViewHolder> implements IBleDevice.StateListener {
	private List<ThingNode> thingNodes;
	private MainActivity mainActivity;
	
	public ThingNodesAdapter(MainActivity mainActivity, List<ThingNode> thingNodes) {
		super(new LanNodeDiffItemCallback());
		
		this.thingNodes = thingNodes;
		this.mainActivity = mainActivity;
		
		for (ThingNode thingNode : thingNodes) {
			((AmberWatch)thingNode.getThing()).addStateListener(this);
		}
	}
	
	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item, parent, false);
		return new ViewHolder(view);
	}
	
	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		ThingNode thingNode = thingNodes.get(position);
		
		IBleThing thing = thingNode.getThing();
		if (!(thing instanceof AmberWatch))
			throw new IllegalArgumentException("Not amber watch.");
		
		AmberWatch device = (AmberWatch)thing;
		holder.deviceNameLabel.setText(String.format("%s - %s-%s",
				getLanIdText(thingNode), device.getName(), device.getAddress()));
		
		IBleDevice.State deviceState =  device.getState();
		String sDeviceStatus = null;
		if (deviceState == IBleDevice.State.CONNECTED)
			sDeviceStatus = "CONNECTED";
		else if (deviceState == IBleDevice.State.CONNECTING)
			sDeviceStatus = "CONNECTING";
		else
			sDeviceStatus = "NOT CONNECTED";
		holder.deviceStatusLabel.setText(sDeviceStatus);
		
		holder.deviceInfoView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showDeviceSubmenu(view, thingNode);
			}
		});
		
		holder.batteryIcon.setImageResource(getBatteryIcon(device.getBatteryLevel()));
	}
	
	private Object getLanIdText(ThingNode thingNode) {
		return thingNode.getLanId() == null ? "?" : thingNode.getLanId();
	}
	
	private void showDeviceSubmenu(final View v, final ThingNode thingNode) {
		IBleDevice device = (AmberWatch) thingNode.getThing();
		IBleDevice.State state = device.getState();
		
		PopupMenu menu = new PopupMenu(v.getContext(), v);
		menu.inflate(R.menu.activity_main_device_submenu);
		
		if (state == IBleDevice.State.CONNECTING) {
			menu.getMenu().findItem(R.id.device_submenu_connect).setEnabled(false);
			menu.getMenu().findItem(R.id.device_submenu_disconnect).setEnabled(false);
			menu.getMenu().findItem(R.id.device_submenu_add_device_as_node).setEnabled(false);
			menu.getMenu().findItem(R.id.device_submenu_send_message).setEnabled(false);
		} else if (state == IBleDevice.State.NOT_CONNECTED) {
			menu.getMenu().findItem(R.id.device_submenu_disconnect).setEnabled(false);
			menu.getMenu().findItem(R.id.device_submenu_send_message).setEnabled(false);
		} else {
			menu.getMenu().findItem(R.id.device_submenu_connect).setVisible(false);
		}
		
		if (thingNode.getLanId() != null) {
			menu.getMenu().findItem(R.id.device_submenu_add_device_as_node).setVisible(false);
		}
		
		menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				switch (menuItem.getItemId()) {
					case R.id.device_submenu_connect:
						device.connect();
						return true;
					case R.id.device_submenu_disconnect:
						device.disconnect();
						return true;
					case R.id.device_submenu_add_device_as_node:
						addDeviceAsNode((IBleDevice) thingNode.getThing());
						return true;
					case R.id.device_submenu_send_message:
						sendMessageToDevice(device);
						return true;
				}
				
				return false;
			}
		});
		
		menu.show();
	}
	
	protected void addDeviceAsNode(IBleDevice device) {
		if (!(device instanceof AmberWatch))
			throw new IllegalArgumentException("Not amber watch.");
		
		IIotBgService iotBgService = mainActivity.getIotBgService();
		if (!iotBgService.isConnectedToHost()) {
			AmberUtils.toastInService("Not connected to host.");
			return;
		}
		
		IConcentrator concentrator = iotBgService.getConcentrator();
		concentrator.addListener(new IConcentrator.Listener() {
			@Override
			public void nodeAdded(int lanId, LanNode lanNode) {
				int position = -1;
				ThingNode thingNode = null;
				for (int i = 0; i < thingNodes.size(); i++) {
					thingNode = thingNodes.get(i);
					if (thingNode.getThing().getThingId().equals(lanNode.getThingId())) {
						position = i;
						thingNode.setLanId(lanId);
						break;
					}
				}
				
				if (position == -1)
					throw new IllegalArgumentException(String.format("Device which's thing ID is '%d' can't be found.", position));
				
				MainApplication.getInstance().nodeAdded(lanNode.getThingId(), lanId);
				MainApplication.getInstance().saveThingNodes();
				itemChanged(thingNode.getThing());
				AmberUtils.toastInService(String.format(
						"Device which's thing ID is '%s' has added as node.", lanNode.getThingId()));
			}
			
			@Override
			public void nodeReset(int lanId, LanNode node) {}
			
			@Override
			public void nodeRemoved(int lanId, LanNode node) {}
			
			@Override
			public void occurred(IConcentrator.AddNodeError error, LanNode source) {
				AmberUtils.toastInService("Failed to add device as node.");
			}
		});
		
		concentrator.requestServerToAddNode(device.getThingId(),
				"abcdefghijkl", concentrator.getBestSuitedNewLanId(),
				new BleAddress(device.getAddress()));
	}
	
	private void sendMessageToDevice(IBleDevice device) {
		final EditText etMessage = new EditText(mainActivity);
		AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
		builder.
				setTitle("Send message to device").setView(etMessage).
				setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						String alertMessage = etMessage.getText().toString();
						if (alertMessage == null || "".equals(alertMessage)) {
							mainActivity.runOnUiThread(() ->
									Toast.makeText(mainActivity,
											mainActivity.getString(R.string.null_alert_message),
											Toast.LENGTH_SHORT).show());
							return;
						}
						
						String alertMessageWithTitle = "Message from sand-demo" + "\0" + alertMessage;
						if (alertMessageWithTitle.length() > 96) {
							mainActivity.runOnUiThread(() ->
									Toast.makeText(mainActivity,
											mainActivity.getString(R.string.alert_message_too_long),
											Toast.LENGTH_SHORT).show());
							return;
						}
						
						if (device.newAlert(alertMessageWithTitle)) {
							final String text = mainActivity.getString(R.string.alert_message_has_sent);
							mainActivity.runOnUiThread(() ->
									Toast.makeText(mainActivity, text,
											Toast.LENGTH_SHORT).show());
						} else {
							final String text = mainActivity.getString(R.string.failed_to_send_alert_message);
							mainActivity.runOnUiThread(() ->
									Toast.makeText(mainActivity, text,
											Toast.LENGTH_SHORT).show());
						}
					}
				}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						dialogInterface.dismiss();
					}
				});
		mainActivity.runOnUiThread(() -> {
			AlertDialog dialog = builder.create();
			dialog.show();
		});
	}
	
	private int getBatteryIcon(int batterLevel) {
		if (batterLevel >= 0 & batterLevel <= 40) {
			return R.drawable.ic_battery_20;
		} else if (batterLevel > 40 && batterLevel <= 60) {
			return R.drawable.ic_battery_50;
		} else if (batterLevel > 60 && batterLevel <= 90) {
			return R.drawable.ic_battery_80;
		} else {
			return R.drawable.ic_battery_full;
		}
	}
	
	@Override
	public void connecting(IBleDevice device) {
		itemChanged(device);
	}
	
	@Override
	public void connected(IBleDevice device, BluetoothGatt gatt) {
		itemChanged(device);
	}
	
	@Override
	public void disconnected(IBleDevice device) {
		itemChanged(device);
	}
	
	private void itemChanged(IBleThing thing) {
		for (int i = 0; i < thingNodes.size(); i++) {
			if (thingNodes.get(i).getThing().equals(thing)) {
				notifyItemChanged(i);
			}
		}
	}
	
	@Override
	public void occurred(IBleDevice device, IBleDevice.Error error) {
		// Ignore
	}
	
	static class ViewHolder extends RecyclerView.ViewHolder {
		MaterialCardView container;
		
		ImageView deviceImageView;
		TextView deviceNameLabel;
		TextView deviceStatusLabel;
		
		ImageView deviceInfoView;
		ImageView batteryIcon;
		
		public ViewHolder(@NonNull View view) {
			super(view);
			
			container = view.findViewById(R.id.card_view);
			
			deviceImageView = view.findViewById(R.id.device_image);
			deviceNameLabel = view.findViewById(R.id.device_name);
			deviceStatusLabel = view.findViewById(R.id.device_status);
			
			deviceInfoView = view.findViewById(R.id.device_info_image);
			
			batteryIcon = view.findViewById(R.id.device_battery_status);
		}
	}
	
	private static class LanNodeDiffItemCallback extends DiffUtil.ItemCallback<ThingNode> {
		@Override
		public boolean areItemsTheSame(@NonNull ThingNode oldItem, @NonNull ThingNode newItem) {
			if (oldItem == null || newItem == null)
				return false;
			
			if (oldItem.getThing() == null || newItem.getThing() == null)
				return false;
			
			if (oldItem == newItem)
				return true;
			
			return oldItem.getThing().equals(newItem.getThing());
		}
		
		@Override
		public boolean areContentsTheSame(@NonNull ThingNode oldItem, @NonNull ThingNode newItem) {
			return areItemsTheSame(oldItem, newItem);
		}
	}
	
	@Override
	public int getItemCount() {
		if (thingNodes == null)
			return 0;
		
		return thingNodes.size();
	}
	
	@Override
	protected ThingNode getItem(int position) {
		return thingNodes.get(position);
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
}
