package io.runtime.mcumgr.ble.callback;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import io.runtime.mcumgr.response.McuMgrResponse;
import no.nordicsemi.android.ble.data.Data;

public final class SmpResponse<T extends McuMgrResponse> extends SmpDataCallback<T> {
	@Nullable
	private T response;
	private boolean valid;

	public SmpResponse(Class<T> responseType) {
		super(responseType);
	}

	@Override
	public void onResponseReceived(@NonNull BluetoothDevice device, @NonNull T response) {
		this.response = response;
		this.valid = true;
	}

	@Override
	public void onInvalidDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
		this.valid = false;
	}

	@Nullable
	public T getResponse() {
		return response;
	}

	public boolean isValid() {
		return valid;
	}
}
