package com.thefirstlineofcode.amber.bridge;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
		
		Device device = lanNode.getDevice();
		holder.deviceNameLabel.setText(String.format("%s - %s-%s", "?", device.getName(), device.getAddress()));
		
		Device.State deviceState =  device.getState();
		String sDeviceStatus = null;
		if (deviceState == Device.State.CONNECTED)
			sDeviceStatus = "CONNECTED";
		else if (deviceState == Device.State.CONNECTING)
			sDeviceStatus = "CONNECTING";
		else
			sDeviceStatus = "NOT CONNECTED";
		holder.deviceStatusLabel.setText(sDeviceStatus);
		
		holder.batteryIcon.setImageResource(getBatteryIcon(device.getBatteryLevel()));
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
			
			if (oldItem.getThingId() == null || newItem.getThingId() == null)
				return false;
			
			if (oldItem == newItem)
				return true;
			
			return oldItem.getThingId().equals(newItem.getThingId()) &&
					oldItem.getThingId().equals(newItem.getThingId());
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
