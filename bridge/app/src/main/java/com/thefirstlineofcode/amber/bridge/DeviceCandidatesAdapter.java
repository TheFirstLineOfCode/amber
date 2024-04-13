/*  Copyright (C) 2015-2020 Andreas Shimokawa, Carsten Pfeiffer

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package com.thefirstlineofcode.amber.bridge;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class DeviceCandidatesAdapter extends ArrayAdapter<DeviceCandidate> {

    private final Context context;

    public DeviceCandidatesAdapter(Context context, List<DeviceCandidate> deviceCandidates) {
        super(context, 0, deviceCandidates);

        this.context = context;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        DeviceCandidate device = getItem(position);
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_with_details, parent, false);
        }
        ImageView deviceImageView = view.findViewById(R.id.item_image);
        TextView deviceNameLabel = view.findViewById(R.id.item_name);
        TextView deviceAddressLabel = view.findViewById(R.id.item_details);
        TextView deviceStatus = view.findViewById(R.id.item_status);

        String name = formatDeviceCandidate(device);
        deviceNameLabel.setText(name);
        deviceAddressLabel.setText(device.getMacAddress());
        deviceImageView.setImageResource(R.drawable.ic_device_pinetime);

        final List<String> statusLines = new ArrayList<>();
        try {
            if (device.getDevice().getBondState() == BluetoothDevice.BOND_BONDED) {
                deviceStatus.setText(getContext().getString(R.string.device_is_currently_bonded));
            }
        } catch (SecurityException e) {}
        
        return view;
    }

    private String formatDeviceCandidate(DeviceCandidate device) {
        if (device.getRssi() > BleThing.RSSI_UNKNOWN) {
            return context.getString(R.string.device_with_rssi, device.getName(), AmberUtils.formatRssi(device.getRssi()));
        }

        return device.getName();
    }
}
