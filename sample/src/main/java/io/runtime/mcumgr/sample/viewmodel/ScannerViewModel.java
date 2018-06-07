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

package io.runtime.mcumgr.sample.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.runtime.mcumgr.ble.McuMgrBleTransport;
import io.runtime.mcumgr.sample.utils.Utils;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class ScannerViewModel extends AndroidViewModel {

	/** MutableLiveData containing the scanner state to notify MainActivity. */
	private final ScannerLiveData mScannerLiveData;

	public ScannerLiveData getScannerState() {
		return mScannerLiveData;
	}

	@Inject
	public ScannerViewModel(final Application application) {
		super(application);

		mScannerLiveData = new ScannerLiveData(Utils.isBleEnabled(), Utils.isLocationEnabled(application));
		registerBroadcastReceivers(application);
	}

	@Override
	protected void onCleared() {
		super.onCleared();
		getApplication().unregisterReceiver(mBluetoothStateBroadcastReceiver);

		if (Utils.isMarshmallowOrAbove()) {
			getApplication().unregisterReceiver(mLocationProviderChangedReceiver);
		}
	}

	public void refresh() {
		mScannerLiveData.refresh();
	}

	/**
	 * Start scanning for Bluetooth devices.
	 */
	public void startScan() {
		if (mScannerLiveData.isScanning()) {
			return;
		}

		// Scanning settings
		final ScanSettings settings = new ScanSettings.Builder()
				.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
				.build();

		// Let's use the filter to scan only for devices with SMP server UUID in the adv packets
		final ParcelUuid uuid = new ParcelUuid(McuMgrBleTransport.SMP_SERVICE_UUID);
		final List<ScanFilter> filters = new ArrayList<>();
		filters.add(new ScanFilter.Builder().setServiceUuid(uuid).build());

		final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
		scanner.startScan(filters, settings, scanCallback);
		mScannerLiveData.scanningStarted();
	}

	/**
	 * stop scanning for bluetooth devices.
	 */
	public void stopScan() {
		final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
		scanner.stopScan(scanCallback);
		mScannerLiveData.scanningStopped();
	}

	private final ScanCallback scanCallback = new ScanCallback() {
		@Override
		public void onScanResult(final int callbackType, final ScanResult result) {
			// If the packet has been obtained while Location was disabled, mark Location as not required
			if (Utils.isLocationRequired(getApplication()) && !Utils.isLocationEnabled(getApplication()))
				Utils.markLocationNotRequired(getApplication());

			mScannerLiveData.deviceDiscovered(result);
		}

		@Override
		public void onBatchScanResults(final List<ScanResult> results) {
			// Batch scan is disabled (report delay = 0)
		}

		@Override
		public void onScanFailed(final int errorCode) {
			// TODO This should be handled
			mScannerLiveData.scanningStopped();
		}
	};

	/**
	 * Register for required broadcast receivers.
	 */
	private void registerBroadcastReceivers(final Application application) {
		application.registerReceiver(mBluetoothStateBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
		if (Utils.isMarshmallowOrAbove()) {
			application.registerReceiver(mLocationProviderChangedReceiver, new IntentFilter(LocationManager.MODE_CHANGED_ACTION));
		}
	}

	/**
	 * Broadcast receiver to monitor the changes in the location provider
	 */
	private final BroadcastReceiver mLocationProviderChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final boolean enabled = Utils.isLocationEnabled(context);
			mScannerLiveData.setLocationEnabled(enabled);
		}
	};

	/**
	 * Broadcast receiver to monitor the changes in the bluetooth adapter
	 */
	private final BroadcastReceiver mBluetoothStateBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
			final int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF);

			switch (state) {
				case BluetoothAdapter.STATE_ON:
					mScannerLiveData.bluetoothEnabled();
					break;
				case BluetoothAdapter.STATE_TURNING_OFF:
				case BluetoothAdapter.STATE_OFF:
					if (previousState != BluetoothAdapter.STATE_TURNING_OFF && previousState != BluetoothAdapter.STATE_OFF) {
						stopScan();
						mScannerLiveData.bluetoothDisabled();
					}
					break;
			}
		}
	};
}
