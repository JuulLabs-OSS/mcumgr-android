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

package io.runtime.mcumgr.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrScheme;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.ble.callback.OneTimeSmpDataCallback;
import io.runtime.mcumgr.ble.callback.SmpMerger;
import io.runtime.mcumgr.ble.callback.SmpResponse;
import io.runtime.mcumgr.exception.InsufficientMtuException;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.McuMgrResponse;
import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.ble.Request;
import no.nordicsemi.android.ble.callback.FailCallback;
import no.nordicsemi.android.ble.callback.SuccessCallback;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.ble.data.DataMerger;
import no.nordicsemi.android.ble.error.GattError;
import no.nordicsemi.android.ble.exception.BluetoothDisabledException;
import no.nordicsemi.android.ble.exception.DeviceDisconnectedException;
import no.nordicsemi.android.ble.exception.RequestFailedException;

/**
 * The McuMgrBleTransport is an implementation for the {@link McuMgrScheme#BLE} transport scheme.
 * This class extends {@link BleManager}, which handles the BLE state machine and owns the
 * {@link BluetoothGatt} object that executes BLE actions. If you wish to integrate McuManager an
 * existing BLE implementation, you may simply implement {@link McuMgrTransport} or use this class
 * to perform your BLE actions by calling {@link BleManager#enqueue(Request)}.
 */
public class McuMgrBleTransport extends BleManager<BleManagerCallbacks> implements McuMgrTransport {
    private final static String TAG = "McuMgrBleTransport";

    public final static UUID SMP_SERVICE_UUID =
            UUID.fromString("8D53DC1D-1DB7-4CD3-868B-8A527460AA84");
    private final static UUID SMP_CHAR_UUID =
            UUID.fromString("DA2E7828-FBCE-4E01-AE9E-261174997C48");

    /**
     * Simple Management Protocol service.
     */
    private BluetoothGattService mSmpService;

    /**
     * Simple Management Protocol characteristic.
     */
    private BluetoothGattCharacteristic mSmpCharacteristic;

    /**
     * The Bluetooth device for this transporter.
     */
    private final BluetoothDevice mDevice;

    /**
     * An instance of a merger used to merge SMP packets that are split into multiple BLE packets.
     */
    private final DataMerger mSMPMerger = new SmpMerger();

    /**
     * Construct a McuMgrBleTransport object.
     *
     * @param context the context used to connect to the device.
     * @param device  the device to connect to and communicate with.
     */
    public McuMgrBleTransport(@NonNull Context context, @NonNull BluetoothDevice device) {
        super(context);
        mDevice = device;
        // By default, the callbacks will ignore all calls to it
        setGattCallbacks(new McuMgrBleCallbacksStub());
    }

    @Override
    public BluetoothDevice getBluetoothDevice() {
        return mDevice;
    }

    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return mGattCallback;
    }

    //*******************************************************************************************
    // Mcu Manager Transport
    //*******************************************************************************************

    @NonNull
    @Override
    public McuMgrScheme getScheme() {
        return McuMgrScheme.BLE;
    }

    @NonNull
    @Override
    public <T extends McuMgrResponse> T send(@NonNull final byte[] payload,
                                             @NonNull final Class<T> responseType)
            throws McuMgrException {
        // If device is not connected, connect
        final boolean wasConnected = isConnected();
        try {
            // Await will wait until the device is ready (that is initialization is complete)
            connect(mDevice).await(25 * 1000);
            if (!wasConnected) {
                notifyConnected();
            }
        } catch (RequestFailedException e) {
            switch (e.getStatus()) {
                case FailCallback.REASON_DEVICE_NOT_SUPPORTED:
                    throw new McuMgrException("Device does not support SMP Service");
                case FailCallback.REASON_REQUEST_FAILED:
                    // This could be thrown only if the manager was requested to connect for
                    // a second time and to a different device than the one that's already
                    // connected. This may not happen here.
                    throw new McuMgrException("Other device already connected");
                default:
                    // Other errors are currently never thrown for the connect request.
                    throw new McuMgrException("Unknown error");
            }
        } catch (InterruptedException e) {
            // On timeout, fail the request
            throw new McuMgrException("Connection routine timed out.");
        } catch (DeviceDisconnectedException e) {
            // When connection failed, fail the request
            throw new McuMgrException("Device has disconnected");
        } catch (BluetoothDisabledException e) {
            // When Bluetooth was disabled, fail the request
            throw new McuMgrException("Bluetooth adapter disabled");
        }

        // Ensure the MTU is sufficient
        if (getMtu() - 3 < payload.length) {
            throw new InsufficientMtuException(payload.length, getMtu());
        }

        // Send the request and wait for a notification in a synchronous way
        try {
            final SmpResponse<T> smpResponse = setNotificationCallback(mSmpCharacteristic)
                    .merge(mSMPMerger)
                    .awaitAfter(Request.newWriteRequest(mSmpCharacteristic, payload),
                            new SmpResponse<>(responseType), 1000);
            if (smpResponse.isValid()) {
                //noinspection ConstantConditions
                return smpResponse.getResponse();
            } else {
                throw new McuMgrException("Error building " +
                        "McuMgrResponse from response data: " + smpResponse.getRawData());
            }
        } catch (RequestFailedException e) {
            throw new McuMgrException(GattError.parse(e.getStatus()));
        } catch (DeviceDisconnectedException e) {
            // When connection failed, fail the request
            throw new McuMgrException("Device has disconnected");
        } catch (InterruptedException e) {
            // Device must have disconnected moment before the request was made
            throw new McuMgrException("Request timed out");
        } catch (BluetoothDisabledException e) {
            // When Bluetooth was disabled, fail the request
            throw new McuMgrException("Bluetooth adapter disabled");
        }
    }

    @Override
    public <T extends McuMgrResponse> void send(@NonNull final byte[] payload,
                                                @NonNull final Class<T> responseType,
                                                @NonNull final McuMgrCallback<T> callback) {
        // If device is not connected, connect.
        // If the device was already connected, the completion callback will be called immediately.
        final boolean wasConnected = isConnected();
        connect(mDevice).done(new SuccessCallback() {
            @Override
            public void onRequestCompleted(@NonNull final BluetoothDevice device) {
                if (!wasConnected) {
                    notifyConnected();
                }

                // Ensure the MTU is sufficient
                if (getMtu() - 3 < payload.length) {
                    callback.onError(new InsufficientMtuException(payload.length, getMtu()));
                    return;
                }

                setNotificationCallback(mSmpCharacteristic)
                        .merge(mSMPMerger)
                        .with(new OneTimeSmpDataCallback<T>(responseType) {
                            @Override
                            public void onResponseReceived(@NonNull BluetoothDevice device,
                                                           @NonNull T response) {
                                if (response.isSuccess()) {
                                    callback.onResponse(response);
                                } else {
                                    callback.onError(new McuMgrErrorException(response));
                                }
                            }

                            @Override
                            public void onInvalidDataReceived(@NonNull BluetoothDevice device,
                                                              @NonNull Data data) {
                                callback.onError(new McuMgrException("Error building " +
                                        "McuMgrResponse from response data: " + data));
                            }

                            @Override
                            public void onTimeoutOccurred(@NonNull BluetoothDevice device) {
                                callback.onError(new McuMgrException("Request timed out"));
                            }

                            @Override
                            public void onDeviceDisconnected(@NonNull BluetoothDevice device) {
                                callback.onError(new McuMgrException("Device has disconnected"));
                            }

                            @Override
                            public void onBluetoothDisabled(@NonNull final BluetoothDevice device) {
                                callback.onError(new McuMgrException("Bluetooth adapter disabled"));
                            }
                        }, 1000);
                writeCharacteristic(mSmpCharacteristic, payload)
                        .fail(new FailCallback() {
                            @Override
                            public void onRequestFailed(@NonNull BluetoothDevice device,
                                                        int status) {
                                callback.onError(new McuMgrException(GattError.parse(status)));
                            }
                        });
            }
        }).fail(new FailCallback() {
            @Override
            public void onRequestFailed(@NonNull final BluetoothDevice device, final int status) {
                switch (status) {
                    case REASON_DEVICE_DISCONNECTED:
                        callback.onError(new McuMgrException("Device has disconnected"));
                        break;
                    case REASON_DEVICE_NOT_SUPPORTED:
                        callback.onError(new McuMgrException("Device does not support SMP Service"));
                        break;
                    case REASON_REQUEST_FAILED:
                        // This could be thrown only if the manager was requested to connect for
                        // a second time and to a different device than the one that's already
                        // connected. This may not happen here.
                        callback.onError(new McuMgrException("Other device already connected"));
                        break;
                    case REASON_BLUETOOTH_DISABLED:
                        callback.onError(new McuMgrException("Bluetooth adapter disabled"));
                        break;
                    default:
                        callback.onError(new McuMgrException(GattError.parseConnectionError(status)));
                        break;
                }
            }
        });
    }

    @Override
    public void release() {
        disconnect();
    }

    @Override
    public void log(int level, @NonNull String message) {
        Log.d(TAG, message);
    }

    @Override
    public void log(int level, int messageRes, Object... params) {

    }

    //*******************************************************************************************
    // Ble Manager Callbacks
    //*******************************************************************************************

    private final BleManagerGattCallback mGattCallback = new BleManagerGattCallback() {

        // Determines whether the device supports the SMP Service
        @Override
        protected boolean isRequiredServiceSupported(BluetoothGatt gatt) {
            mSmpService = gatt.getService(SMP_SERVICE_UUID);
            if (mSmpService == null) {
                Log.e(TAG, "Device does not support SMP service");
                return false;
            }
            mSmpCharacteristic = mSmpService.getCharacteristic(SMP_CHAR_UUID);
            if (mSmpCharacteristic == null) {
                Log.e(TAG, "Device does not support SMP characteristic");
                return false;
            } else {
                final int rxProperties = mSmpCharacteristic.getProperties();
                boolean write = (rxProperties &
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0;
                boolean notify = (rxProperties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0;
                if (!write || !notify) {
                    Log.e(TAG, "SMP characteristic does not support write(" + write +
                            ") or notify(" + notify + ")");
                    return false;
                }
            }
            return true;
        }

        // Called once the connection has been established and services discovered. This method
        // adds a queue of requests necessary to set up the SMP service to begin writing
        // commands and receiving responses. Once these actions have completed onDeviceReady is
        // called.
        @Override
        protected void initialize() {
            requestMtu(515);
            enableNotifications(mSmpCharacteristic);
        }

        // Called when the device has disconnected. This method nulls the services and
        // characteristic variables.
        @Override
        protected void onDeviceDisconnected() {
            mSmpService = null;
            mSmpCharacteristic = null;
            notifyDisconnected();
        }
    };

    //*******************************************************************************************
    // Manager Connection Observers
    //*******************************************************************************************

    private final List<ConnectionObserver> mConnectionObservers = new LinkedList<>();

    @Override
    public synchronized void addObserver(@NonNull final ConnectionObserver observer) {
        mConnectionObservers.add(observer);
    }

    @Override
    public synchronized void removeObserver(@NonNull final ConnectionObserver observer) {
        mConnectionObservers.remove(observer);
    }

    private synchronized void notifyConnected() {
        for (ConnectionObserver o : mConnectionObservers) {
            o.onConnected();
        }
    }

    private synchronized void notifyDisconnected() {
        for (ConnectionObserver o : mConnectionObservers) {
            o.onDisconnected();
        }
    }
}
