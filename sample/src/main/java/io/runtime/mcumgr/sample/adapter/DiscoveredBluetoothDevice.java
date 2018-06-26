/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.runtime.mcumgr.sample.adapter;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class DiscoveredBluetoothDevice implements Parcelable {
	private final BluetoothDevice device;
	private String name;
	private int rssi;

	public DiscoveredBluetoothDevice(final ScanResult scanResult) {
		device = scanResult.getDevice();
		update(scanResult);
	}

	public BluetoothDevice getDevice() {
		return device;
	}

	public String getAddress() {
		return device.getAddress();
	}

	public String getName() {
		return name;
	}

	public int getRssi() {
		return rssi;
	}

	public void update(final ScanResult scanResult) {
		name = scanResult.getScanRecord() != null ?
				scanResult.getScanRecord().getDeviceName() : null;
		rssi = scanResult.getRssi();
	}

	public boolean matches(final ScanResult scanResult) {
		return device.getAddress().equals(scanResult.getDevice().getAddress());
	}

	@Override
	public boolean equals(final Object o) {
		if (o instanceof DiscoveredBluetoothDevice) {
			final DiscoveredBluetoothDevice that = (DiscoveredBluetoothDevice) o;
			return device.getAddress().equals(that.device.getAddress());
		}
		return super.equals(o);
	}

	// Parcelable implementation

	private DiscoveredBluetoothDevice(final Parcel in) {
		device = in.readParcelable(BluetoothDevice.class.getClassLoader());
		name = in.readString();
		rssi = in.readInt();
	}

	@Override
	public void writeToParcel(final Parcel parcel, final int flags) {
		parcel.writeParcelable(device, flags);
		parcel.writeString(name);
		parcel.writeInt(rssi);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Creator<DiscoveredBluetoothDevice> CREATOR = new Creator<DiscoveredBluetoothDevice>() {
		@Override
		public DiscoveredBluetoothDevice createFromParcel(final Parcel source) {
			return new DiscoveredBluetoothDevice(source);
		}

		@Override
		public DiscoveredBluetoothDevice[] newArray(final int size) {
			return new DiscoveredBluetoothDevice[size];
		}
	};
}
