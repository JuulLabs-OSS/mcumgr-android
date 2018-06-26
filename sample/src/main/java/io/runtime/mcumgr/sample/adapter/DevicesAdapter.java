/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.ScannerActivity;
import io.runtime.mcumgr.sample.viewmodel.ScannerLiveData;

@SuppressWarnings("unused")
public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder> {
	private final ScannerActivity mContext;
	private final List<DiscoveredBluetoothDevice> mDevices;
	private OnItemClickListener mOnItemClickListener;

	@FunctionalInterface
	public interface OnItemClickListener {
		void onItemClick(final BluetoothDevice device);
	}

	public void setOnItemClickListener(final Context context) {
		mOnItemClickListener = (OnItemClickListener) context;
	}

	@SuppressWarnings("ConstantConditions")
	public DevicesAdapter(final ScannerActivity activity, final ScannerLiveData scannerLiveData) {
		mContext = activity;
		mDevices = scannerLiveData.getDevices();
		scannerLiveData.observe(activity, devices -> {
			final Integer i = devices.getUpdatedDeviceIndex();
			if (i != null)
				notifyItemChanged(i);
			else
				notifyDataSetChanged();
		});
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
		final View layoutView = LayoutInflater.from(mContext)
				.inflate(R.layout.device_item, parent, false);
		return new ViewHolder(layoutView);
	}

	@Override
	public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
		final DiscoveredBluetoothDevice device = mDevices.get(position);
		final String deviceName = device.getName();

		if (!TextUtils.isEmpty(deviceName))
			holder.deviceName.setText(deviceName);
		else
			holder.deviceName.setText(R.string.unknown_device);
		holder.deviceAddress.setText(device.getAddress());
		final int rssiPercent = (int) (100.0f * (127.0f + device.getRssi()) / (127.0f + 20.0f));
		holder.rssi.setImageLevel(rssiPercent);
	}

	@Override
	public long getItemId(final int position) {
		return position;
	}

	@Override
	public int getItemCount() {
		return mDevices.size();
	}

	public boolean isEmpty() {
		return getItemCount() == 0;
	}

	final class ViewHolder extends RecyclerView.ViewHolder {
		@BindView(R.id.device_address) TextView deviceAddress;
		@BindView(R.id.device_name) TextView deviceName;
		@BindView(R.id.rssi) ImageView rssi;

		private ViewHolder(final View view) {
			super(view);
			ButterKnife.bind(this, view);

			view.findViewById(R.id.device_container).setOnClickListener(v -> {
				if (mOnItemClickListener != null) {
					mOnItemClickListener.onItemClick(mDevices.get(getAdapterPosition()).getDevice());
				}
			});
		}
	}
}
