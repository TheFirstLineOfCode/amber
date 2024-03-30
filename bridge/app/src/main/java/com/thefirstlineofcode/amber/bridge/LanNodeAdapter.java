package com.thefirstlineofcode.amber.bridge;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LanNodeAdapter extends ListAdapter<LanNode, LanNodeAdapter.ViewHolder> {
	
	public LanNodeAdapter(Context context, List<LanNode> lanNodeList) {
		super(new LanNodeDiffItemCallback());
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
			// TODO Compare all fields of two items.
			return areItemsTheSame(oldItem, newItem);
		}
	}
}
