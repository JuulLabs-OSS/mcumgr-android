package io.runtime.mcumgr.ble.callback;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;

import io.runtime.mcumgr.response.McuMgrResponse;

public interface SmpCallback<T extends McuMgrResponse> {

	/**
	 * Callback called when response has been received and parsed successfully.
	 *
	 * @param device   the target device.
	 * @param response the response received.
	 */
	void onResponseReceived(@NonNull BluetoothDevice device, @NonNull T response);
}
