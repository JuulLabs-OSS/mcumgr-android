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

package io.runtime.mcumgr.sample.viewmodel.mcumgr;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.bluetooth.BluetoothDevice;

import javax.inject.Inject;
import javax.inject.Named;

import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.ble.McuMgrBleCallbacksStub;
import io.runtime.mcumgr.ble.McuMgrBleTransport;
import io.runtime.mcumgr.sample.R;

public class DeviceStatusViewModel extends McuMgrViewModel {
	private final MutableLiveData<Integer> mConnectionStateLiveData = new MutableLiveData<>();
	private final MutableLiveData<Integer> mBondStateLiveData = new MutableLiveData<>();

	@Inject
	DeviceStatusViewModel(final McuMgrTransport transport,
						  @Named("busy") final MutableLiveData<Boolean> state) {
		super(state);
		if (transport instanceof McuMgrBleTransport) {
			((McuMgrBleTransport) transport).setGattCallbacks(new DeviceCallbacks());
		}
	}

	public LiveData<Integer> getConnectionState() {
		return mConnectionStateLiveData;
	}

	public LiveData<Integer> getBondState() {
		return mBondStateLiveData;
	}

	private final class DeviceCallbacks extends McuMgrBleCallbacksStub {
		@Override
		public void onDeviceConnecting(final BluetoothDevice device) {
			mConnectionStateLiveData.postValue(R.string.status_connecting);
		}

		@Override
		public void onDeviceConnected(final BluetoothDevice device) {
			mConnectionStateLiveData.postValue(R.string.status_initializing);
		}

		@Override
		public void onDeviceReady(final BluetoothDevice device) {
			mConnectionStateLiveData.postValue(R.string.status_connected);
		}

		@Override
		public void onDeviceDisconnecting(final BluetoothDevice device) {
			mConnectionStateLiveData.postValue(R.string.status_disconnecting);
		}

		@Override
		public void onDeviceDisconnected(final BluetoothDevice device) {
			mConnectionStateLiveData.postValue(R.string.status_disconnected);
		}

		@Override
		public void onBondingRequired(final BluetoothDevice device) {
			mBondStateLiveData.postValue(R.string.status_bonding);
		}

		@Override
		public void onBonded(final BluetoothDevice device) {
			mBondStateLiveData.postValue(R.string.status_bonded);
		}

		@Override
		public void onBondingFailed(final BluetoothDevice device) {
			mBondStateLiveData.postValue(R.string.status_not_bonded);
		}
	}
}
