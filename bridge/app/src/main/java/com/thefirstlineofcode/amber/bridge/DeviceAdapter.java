package com.thefirstlineofcode.amber.bridge;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;

public class DeviceAdapter extends ListAdapter<Device, DeviceAdapter.ViewHolder> {
	
	public DeviceAdapter(Context context, List<Device> deviceList) {
		super(new DeviceDiffItemCallback());
	}
	
	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return null;
	}
	
	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
	
	}
	
	static class ViewHolder extends RecyclerView.ViewHolder {
		
		public ViewHolder(@NonNull View itemView) {
			super(itemView);
		}
	}
	
	private static class DeviceDiffItemCallback extends DiffUtil.ItemCallback<Device> {
		@Override
		public boolean areItemsTheSame(@NonNull Device oldItem, @NonNull Device newItem) {
			if (oldItem == null || newItem == null)
				return false;
			
			if (oldItem.getName() == null || newItem.getName() == null)
				return false;
			
			if (oldItem.getAddress() == null || newItem.getAddress() == null)
				return false;
			
			if (oldItem == newItem)
				return true;
			
			return oldItem.getName().equals(newItem.getName()) &&
					oldItem.getAddress().equals(newItem.getAddress());
					
		}
		
		@Override
		public boolean areContentsTheSame(@NonNull Device oldItem, @NonNull Device newItem) {
			// TODO Compare all fields of two items.
			return areItemsTheSame(oldItem, newItem);
		}
	}
}
