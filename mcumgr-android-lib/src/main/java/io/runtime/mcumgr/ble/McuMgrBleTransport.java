package io.runtime.mcumgr.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.ConditionVariable;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrScheme;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.InsufficientMtuException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.McuMgrResponse;
import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.Request;
import no.nordicsemi.android.ble.callback.DataReceivedCallback;
import no.nordicsemi.android.ble.callback.FailCallback;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.ble.error.GattError;

/**
 * The McuMgrBleTransport is an implementation for the {@link McuMgrScheme#BLE} transport scheme.
 * This class extends {@link BleManager}, which handles the BLE state machine and owns the
 * {@link BluetoothGatt} object that executes BLE actions. If you wish to integrate McuManager an
 * existing BLE implementation, you may simply implement {@link McuMgrTransport} or use this class
 * to perform your BLE actions by calling {@link BleManager#enqueue(Request)}.
 */
public class McuMgrBleTransport extends BleManager<McuMgrBleCallbacks> implements McuMgrTransport {

    private final static String TAG = "McuMgrBleTransport";

    private final static UUID SMP_SERVICE_UUID =
            UUID.fromString("8D53DC1D-1DB7-4CD3-868B-8A527460AA84");
    private final static UUID SMP_CHAR_UUID =
            UUID.fromString("DA2E7828-FBCE-4E01-AE9E-261174997C48");

    /**
     * Simple Management Protocol service
     */
    private BluetoothGattService mSmpService;

    /**
     * Simple Management Protocol characteristic.
     */
    private BluetoothGattCharacteristic mSmpCharacteristic;

    /**
     * Used to wait while a device is being connected and set up. This lock is opened once the
     * device is ready (opened in onDeviceReady).
     */
    private ConditionVariable mReadyLock = new ConditionVariable(false);

    /**
     * Queue of requests to send from Mcu Manager
     */
    private LinkedBlockingQueue<McuMgrRequest> mSendQueue = new LinkedBlockingQueue<>();

    /**
     * Used to wait while a writeCharacteristic request is sent. This lock is opened when a
     * notification is received (onCharacteristicNotified) or an error occurs (onError).
     */
    private ConditionVariable mSendLock = new ConditionVariable(false);

    /**
     * The current request being sent. Used to finish or fail the request from an asynchronous
     * mCallback.
     */
    private McuMgrRequest mRequest;

    /**
     * The bluetooth device for this transporter
     */
    private BluetoothDevice mDevice;

    /**
     * Construct a McuMgrBleTransport object.
     *
     * @param context The context used to connect to the device
     * @param device  the device to connect to and communicate with
     */
    public McuMgrBleTransport(Context context, BluetoothDevice device) {
        super(context);
        mDevice = device;
        new SendThread().start();
    }

    @Override
    protected BleManagerGattCallback getGattCallback() {
        return mGattCallback;
    }

    //*******************************************************************************************
    // Mcu Manager Transport
    //*******************************************************************************************

    @Override
    public McuMgrScheme getScheme() {
        return McuMgrScheme.BLE;
    }

    @Override
    public <T extends McuMgrResponse> T send(byte[] payload, Class<T> responseType)
            throws McuMgrException {
        return new McuMgrRequest<>(payload, responseType, null).synchronous(mSendQueue);
    }

    @Override
    public <T extends McuMgrResponse> void send(byte[] payload, Class<T> responseType,
                                                McuMgrCallback<T> callback) {
        new McuMgrRequest<>(payload, responseType, callback).asynchronous(mSendQueue);
    }

    //*******************************************************************************************
    // Mcu Manager Main Send Thread
    // TODO Look into disconnects causing race conditions
    //*******************************************************************************************

    /**
     * This thread loops through the send queue blocking until a request is available. Once a
     * request is popped, connection and setup is performed if necessary, otherwise the request is
     * performed.
     */
    private class SendThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (true) {
                try {
                    // Take a request for the queue, blocking until available.
                    mRequest = mSendQueue.take();
                } catch (InterruptedException e) {
                    Log.e(TAG, "Waiting for request interrupted", e);
                    continue;
                }

                // If device is not connected, connect
                if (!isConnected()) {
                    // Close the ready lock before
                    mReadyLock.close();
                    connect(mDevice);
                }

                // Wait until device is ready
                if (!mReadyLock.block(25 * 1000)) {
                    // On timeout, fail the request
                    mRequest.fail(new McuMgrException("Connection routine timed out."));
                    continue;
                }

                // Ensure that device supports SMP Service
                if (mSmpCharacteristic == null) {
                    if (!isConnected()) {
                        mRequest.fail(new McuMgrException("Device has disconnected"));
                    } else {
                        mRequest.fail(new McuMgrException("Device does not support SMP Service"));
                    }
                    continue;
                }

                // Ensure the mtu is sufficient
                if (getMtu() < mRequest.getBytes().length) {
                    mRequest.fail(new InsufficientMtuException(getMtu()));
                    continue;
                }

                // Close the send lock
                mSendLock.close();

                // Write the characteristic
                mSmpCharacteristic.setValue(mRequest.getBytes());
                Log.d(TAG, "Writing characteristic (" + mRequest.getBytes().length + " bytes)");
                writeCharacteristic(mSmpCharacteristic);

                // Block until the response is received
                if (!mSendLock.block(10 * 1000)) {
                    mRequest.fail(new McuMgrException("Send timed out."));
                }
            }
        }
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
        // called
        @Override
        protected void initialize() {
            enableNotifications(mSmpCharacteristic)
                    .merge(new McuMgrRequestMerger())
                    .with(new DataReceivedCallback() {
                        @Override
                        public void onDataReceived(@NonNull BluetoothDevice device, @NonNull Data data) {
                            mRequest.receive(data.getValue());
                            mSendLock.open();
                        }
                    })
                    .fail(new FailCallback() {
                        @Override
                        public void onRequestFailed(@NonNull BluetoothDevice device, int status) {
                            mSendLock.open();
                            mRequest.fail(new McuMgrException(GattError.parse(status)));
                        }
                    });
            requestMtu(512);
        }

        // Called once the device is ready. This method opens the lock waiting for the device to
        // become ready.
        @Override
        protected void onDeviceReady() {
            Log.d(TAG, "Device is ready");
            mReadyLock.open();
        }

        // Called when the device has disconnected. This method nulls the services and
        // characteristic variables and opens any waiting locks.
        @Override
        protected void onDeviceDisconnected() {
            mSmpService = null;
            mSmpCharacteristic = null;
            mReadyLock.open();
            mSendLock.open();
        }
    };
}
