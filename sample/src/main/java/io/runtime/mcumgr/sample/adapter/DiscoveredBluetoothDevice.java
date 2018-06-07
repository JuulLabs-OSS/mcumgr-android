/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
