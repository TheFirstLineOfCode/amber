package com.thefirstlineofcode.amber.bridge;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class LanNodesAdapter extends ListAdapter<LanNode, LanNodesAdapter.ViewHolder> {
	private List<LanNode> lanNodes;
	
	public LanNodesAdapter(Context context, List<LanNode> lanNodes) {
		super(new LanNodeDiffItemCallback());
		
		this.lanNodes = lanNodes;
	}
	
	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item, parent, false);
		return new ViewHolder(view);
	}
	
	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		LanNode lanNode = lanNodes.get(position);
		
		IBleThing thing = lanNode.getThing();
		if (!(thing instanceof AmberWatch))
			throw new IllegalArgumentException("Not amber watch.");
		
		AmberWatch device = (AmberWatch)thing;
		holder.deviceNameLabel.setText(String.format("%s - %s-%s", "?", device.getName(), device.getAddress()));
		
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
				showDeviceSubmenu(view, device);
			}
		});
		
		holder.batteryIcon.setImageResource(getBatteryIcon(device.getBatteryLevel()));
	}
	
	private void showDeviceSubmenu(final View v, final IBleDevice device) {
		IBleDevice.State state = device.getState();
		
		PopupMenu menu = new PopupMenu(v.getContext(), v);
		menu.inflate(R.menu.activity_main_device_submenu);
		
		if (state == IBleDevice.State.CONNECTING) {
			menu.getMenu().findItem(R.id.device_submenu_connect).setEnabled(false);
			menu.getMenu().findItem(R.id.device_submenu_disconnect).setEnabled(false);
			menu.getMenu().findItem(R.id.device_submenu_remove).setEnabled(false);
		} else if (state == IBleDevice.State.NOT_CONNECTED) {
			menu.getMenu().findItem(R.id.device_submenu_disconnect).setEnabled(false);
		} else {
			menu.getMenu().getItem(R.id.device_submenu_connect).setVisible(false);
		}
		
		menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				switch (menuItem.getItemId()) {
					case R.id.device_submenu_connect:
						device.connect();
						return true;
					case R.id.device_submenu_disconnect:
						return true;
					case R.id.device_submenu_remove:
						return true;
				}
				
				return false;
			}
		});
		
		menu.show();
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
	
	static class ViewHolder extends RecyclerView.ViewHolder {
		MaterialCardView container;
		
		ImageView deviceImageView;
		TextView deviceNameLabel;
		TextView deviceStatusLabel;
		
		ImageView deviceInfoView;
		ImageView batteryIcon;
		ImageView heartRateIcon;
		ImageView totalStepsIcon;
		
		public ViewHolder(@NonNull View view) {
			super(view);
			
			container = view.findViewById(R.id.card_view);
			
			deviceImageView = view.findViewById(R.id.device_image);
			deviceNameLabel = view.findViewById(R.id.device_name);
			deviceStatusLabel = view.findViewById(R.id.device_status);
			
			deviceInfoView = view.findViewById(R.id.device_info_image);
			
			batteryIcon = view.findViewById(R.id.device_battery_status);
			
			heartRateIcon = view.findViewById(R.id.device_heart_rate_status);
			
			totalStepsIcon = view.findViewById(R.id.device_total_steps_status);
		}
	}
	
	private static class LanNodeDiffItemCallback extends DiffUtil.ItemCallback<LanNode> {
		@Override
		public boolean areItemsTheSame(@NonNull LanNode oldItem, @NonNull LanNode newItem) {
			if (oldItem == null || newItem == null)
				return false;
			
			if (oldItem.getThing() == null || newItem.getThing() == null)
				return false;
			
			if (oldItem == newItem)
				return true;
			
			return oldItem.getThing().equals(newItem.getThing());
		}
		
		@Override
		public boolean areContentsTheSame(@NonNull LanNode oldItem, @NonNull LanNode newItem) {
			return areItemsTheSame(oldItem, newItem);
		}
	}
	
	@Override
	public int getItemCount() {
		if (lanNodes == null)
			return 0;
		
		return lanNodes.size();
	}
	
	@Override
	protected LanNode getItem(int position) {
		return lanNodes.get(position);
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
}
