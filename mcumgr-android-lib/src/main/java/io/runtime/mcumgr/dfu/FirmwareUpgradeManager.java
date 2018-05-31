/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.dfu;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.concurrent.Executor;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.image.McuMgrImage;
import io.runtime.mcumgr.managers.DefaultManager;
import io.runtime.mcumgr.managers.ImageManager;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.response.img.McuMgrImageStateResponse;

// TODO Add retries for each step

/**
 * Manages a McuManager firmware upgrade. Once initialized, <b>this object can only perform a single
 * firmware upgrade for a single device to completion</b>. In other words, the same
 * FirmwareUpgradeManager may start the upload, cancel the upload, and call start once again to
 * restart the firmware upgrade. However, once the upload is completed, the same
 * FirmwareUpgradeManager cannot not be used for subsequent firmware upgrades.
 * <p>
 * Like other MCU managers, FirmwareUpgradeManagers must be initialized with a
 * {@link McuMgrTransport} which defines the scheme and implements the transport methods for
 * communicating with the device. FirmwareUpgradeManagers additionally require a callback and
 * the image data to upload.
 * <p>
 * Once initialized, a firmware upgrade is started by calling {@link FirmwareUpgradeManager#start},
 * and can be paused, resumed, and canceled using {@link FirmwareUpgradeManager#pause},
 * {@link FirmwareUpgradeManager#resume}, and {@link FirmwareUpgradeManager#cancel}.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class FirmwareUpgradeManager implements FirmwareUpgradeController {
    private final static String TAG = "FirmwareUpgradeManager";

    /**
     * Performs the image upload, test, and confirmation steps.
     */
    private ImageManager mImageManager;

    /**
     * Performs the reset command.
     */
    private DefaultManager mDefaultManager;

    /**
     * Firmware upgrade callback passed into the constructor or set before the upload has started.
     */
    private FirmwareUpgradeCallback mCallback;

    /**
     * Image data to upload.
     */
    private byte[] mImageData;

    /**
     * Hash of the image data.
     */
    private byte[] mHash;

    /**
     * State of the firmware upgrade.
     */
    private State mState;

    /**
     * Paused flag.
     */
    private boolean mPaused = false;

    /**
     * Flag for setting callbacks to run on the main UI thread.
     */
    private boolean mUiThreadCallbacks = true;

    /**
     * Construct a firmware upgrade manager. If using this constructor, image data must be set
     * using {@link FirmwareUpgradeManager#setImageData} before calling
     * {@link FirmwareUpgradeManager#start}.
     *
     * @param transport the transporter to use.
     * @param callback  the callback.
     */
    public FirmwareUpgradeManager(@NonNull McuMgrTransport transport,
                                  @NonNull FirmwareUpgradeCallback callback) {
        mCallback = callback;
        mState = State.NONE;
        mImageManager = new ImageManager(transport);
        mDefaultManager = new DefaultManager(transport);
    }

    /**
     * Construct a firmware upgrade manager.
     *
     * @param transport the transporter to use.
     * @param imageData the data of the image to send.
     * @param callback  the callback.
     * @throws McuMgrException Thrown if the image data is invalid.
     */
    public FirmwareUpgradeManager(@NonNull McuMgrTransport transport, @NonNull byte[] imageData,
                                  @NonNull FirmwareUpgradeCallback callback) throws McuMgrException {
        this(transport, callback);
        mImageData = imageData;
		mHash = McuMgrImage.getHash(imageData);
    }

    /**
     * If true, run all callbacks on the UI thread (default).
     *
     * @param uiThreadCallbacks true if all callbacks should run on the UI thread.
     */
    public void setCallbackOnUiThread(boolean uiThreadCallbacks) {
        mUiThreadCallbacks = uiThreadCallbacks;
    }

    /**
     * Set the image data of the upgrade. This must be called prior to starting the upgrade.
     *
     * @param imageData the image data to set.
     * @throws McuMgrException if the format of the image data is not valid.
     */
    public void setImageData(byte[] imageData) throws McuMgrException {
        if (mState != State.NONE) {
            Log.e(TAG, "Cannot set image data in state " + mState);
            return;
        }
        mImageData = imageData;
        mHash = McuMgrImage.getHash(imageData);
    }

    /**
     * Set the MTU of the image upload.
     *
     * @param mtu the mtu.
     */
    public void setUploadMtu(int mtu) {
        mImageManager.setUploadMtu(mtu);
    }

    /**
     * Start the upgrade.
     */
    public synchronized void start() {
        if (mState != State.NONE) {
            Log.i(TAG, "Firmware upgrade is already in progress");
            return;
        } else if (mImageData == null) {
            Log.e(TAG, "Cannot start upgrade, image data is null!");
            return;
        }
        // Begin the upload
        mState = State.UPLOAD;
        mInternalCallback.onStart(this);
        mImageManager.upload(mImageData, mImageUploadCallback);
    }

    //******************************************************************
    // Upload Controller
    //******************************************************************

    @Override
    public synchronized void cancel() {
        if (mState.isInProgress()) {
            cancelPrivate();
            mInternalCallback.onCancel(mState);
        }
    }

    @Override
    public synchronized void pause() {
        if (mState.isInProgress()) {
            Log.d(TAG, "Pausing upgrade.");
            mPaused = true;
            if (mState == State.UPLOAD) {
                mImageManager.pauseUpload();
            }
        }
    }

    @Override
    public synchronized void resume() {
        if (mPaused) {
            mPaused = false;
            currentState();
        }
    }

    @Override
    public boolean isPaused() {
        return mPaused;
    }

    @Override
    public boolean isInProgress() {
        return mState.isInProgress() && !isPaused();
    }

    //******************************************************************
    // Implementation
    //******************************************************************


    private synchronized void fail(McuMgrException error) {
        cancelPrivate();
        mInternalCallback.onFail(mState, error);
    }

    private synchronized void cancelPrivate() {
        mState = State.NONE;
        mPaused = false;
        mImageManager.cancelUpload();
        if (mResetPollThread != null) {
            mResetPollThread.interrupt();
        }
    }

    //******************************************************************
    // Request Handlers
    //******************************************************************

    /**
     * State: TEST.
     * Callback for the test command.
     */
    private McuMgrCallback<McuMgrImageStateResponse> mTestCallback = new McuMgrCallback<McuMgrImageStateResponse>() {
        @Override
        public void onResponse(McuMgrImageStateResponse response) {
            if (response.getRc() != McuMgrErrorCode.OK) {
                Log.e(TAG, "Test failed due to McuManager error: " + response.getRc());
                fail(new McuMgrErrorException(response.getRc()));
                return;
            }
            Log.v(TAG, "Test response: " + response.toString());
            if (response.images.length != 2) {
                fail(new McuMgrException("Test response does not contain enough info"));
                return;
            }
            if (!response.images[1].pending) {
                Log.e(TAG, "Tested image is not in a pending state.");
                fail(new McuMgrException("Tested image is not in a pending state."));
                return;
            }
            // Test image success, begin device reset
            nextState();
        }

        @Override
        public void onError(McuMgrException e) {
            fail(e);
        }
    };

    /**
     * State: RESET.
     * Callback for the reset command.
     */
    private McuMgrCallback<McuMgrResponse> mResetCallback = new McuMgrCallback<McuMgrResponse>() {
        @Override
        public void onResponse(McuMgrResponse response) {
            Log.d(TAG, "Reset successful");
            mResetPollThread.start();
        }

        @Override
        public void onError(McuMgrException e) {
            fail(e);
        }
    };

    /**
     * State: CONFIRM.
     * Callback for the confirm command.
     */
    private McuMgrCallback<McuMgrImageStateResponse> mConfirmCallback =
            new McuMgrCallback<McuMgrImageStateResponse>() {
                @Override
                public void onResponse(McuMgrImageStateResponse response) {
                    if (response.getRc() != McuMgrErrorCode.OK) {
                        Log.e(TAG, "Confirm failed due to Mcu Manager error: " + response.getRc());
                        fail(new McuMgrErrorException(response.getRc()));
                        return;
                    }
                    Log.v(TAG, "Confirm response: " + response.toString());
                    if (response.images.length == 0) {
                        fail(new McuMgrException("Confirm response does not contain enough info"));
                        return;
                    }
                    if (!response.images[0].confirmed) {
                        Log.e(TAG, "Image is not in a confirmed state.");
                        fail(new McuMgrException("Image is not in a confirmed state."));
                        return;
                    }
                    // Confirm image success
                    nextState();
                }

                @Override
                public void onError(McuMgrException e) {
                    fail(e);
                }
            };

    //******************************************************************
    // Firmware Upgrade State
    //******************************************************************

    public enum State {
        NONE, UPLOAD, TEST, RESET, CONFIRM, SUCCESS;

        public boolean isInProgress() {
            return this == UPLOAD || this == TEST || this == RESET || this == CONFIRM;
        }
    }

    /**
     * Get the current {@link State} of the firmware upgrade.
     *
     * @return The current state.
     */
    public State getState() {
        return mState;
    }

    /**
     * Called by {@link FirmwareUpgradeManager#resume} to run the current state.
     */
    private synchronized void currentState() {
        if (mPaused) {
            return;
        }
        switch (mState) {
            case NONE:
                return;
            case UPLOAD:
                mImageManager.continueUpload();
                break;
            case TEST:
                mImageManager.test(mHash, mTestCallback);
                break;
            case RESET:
                nextState();
                break;
            case CONFIRM:
                mImageManager.confirm(mHash, mConfirmCallback);
                break;
        }
    }

    /**
     * Called when a state has completed. Sets and executes the next state.
     */
    private synchronized void nextState() {
        if (mPaused) {
            return;
        }
        final State prevState = mState;
        switch (mState) {
            case NONE:
                return;
            case UPLOAD:
                mState = State.TEST;
                mImageManager.test(mHash, mTestCallback);
                break;
            case TEST:
                mState = State.RESET;
                mDefaultManager.reset(mResetCallback);
                break;
            case RESET:
                mState = State.CONFIRM;
                mImageManager.confirm(mHash, mConfirmCallback);
                break;
            case CONFIRM:
                mState = State.SUCCESS;
                break;
        }
        Log.d(TAG, "Moving from state " + prevState.name() + " to state " + mState.name());
        mInternalCallback.onStateChanged(prevState, mState);

        if (mState == State.SUCCESS) {
            mInternalCallback.onSuccess();
        }
    }

    //******************************************************************
    // Poll Reset Runnable
    //******************************************************************

    /**
     * Waits 21 seconds for the disconnect to occur after a reset, then begins polling the device
     * until a response is received.
     */
    private Thread mResetPollThread = new Thread(new Runnable() {
        @Override
        public void run() {
            int attempts = 0;
            try {
                //TODO need to figure out a better way for UDP
                synchronized (this) {
                    wait(21 * 1000);
                }
                while (true) {
                    checkResetComplete();

                    if (attempts == 3) {
                        fail(new McuMgrException("Reset poller has reached attempt limit."));
                        return;
                    }
                    attempts++;
                    synchronized (this) {
                        wait(7000);
                    }
                }
            } catch (InterruptedException e) {
                // Do nothing...
            }
        }

        private void checkResetComplete() {
            Log.d(TAG, "Calling image list...");
            mImageManager.list(new McuMgrCallback<McuMgrImageStateResponse>() {
                @Override
                public synchronized void onResponse(McuMgrImageStateResponse response) {
                    if (mState == State.RESET) {
                        // Device has reset, begin confirm
                        nextState();
                        // Interrupt the thread
                        mResetPollThread.interrupt();
                    }
                }

                @Override
                public void onError(McuMgrException e) {
                    // Do nothing...
                }
            });
        }
    });

    //******************************************************************
    // Image Upload Callback
    //******************************************************************

    /**
     * Image upload callback. Forwards upload callbacks to the FirmwareUpgradeCallback.
     */
    private ImageManager.ImageUploadCallback mImageUploadCallback = new ImageManager.ImageUploadCallback() {
        @Override
        public void onProgressChange(int bytesSent, int imageSize, long timestamp) {
            mInternalCallback.onUploadProgressChanged(bytesSent, imageSize, timestamp);
        }

        @Override
        public void onUploadFail(McuMgrException error) {
            mInternalCallback.onFail(mState, error);
        }

        @Override
        public void onUploadFinish() {
            // Upload finished, move to next state
            nextState();
        }
    };

    //******************************************************************
    // Main Thread Executor
    //******************************************************************

    /**
     * Internal callback to route callbacks to the UI thread if the flag has been set.
     */
    private FirmwareUpgradeCallback mInternalCallback = new FirmwareUpgradeCallback() {
        private MainThreadExecutor mMainThreadExecutor;

        private MainThreadExecutor getMainThreadExecutor() {
            if (mMainThreadExecutor == null) {
                mMainThreadExecutor = new MainThreadExecutor();
            }
            return mMainThreadExecutor;
        }

        @Override
        public void onStart(final FirmwareUpgradeController controller) {
            if (mUiThreadCallbacks) {
                getMainThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onStart(controller);
                    }
                });
            } else {
                mCallback.onStart(controller);
            }
        }

        @Override
        public void onStateChanged(final State prevState, final State newState) {
            if (mUiThreadCallbacks) {
                getMainThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onStateChanged(prevState, newState);
                    }
                });
            } else {
                mCallback.onStateChanged(prevState, newState);
            }
        }

        @Override
        public void onSuccess() {
            if (mUiThreadCallbacks) {
                getMainThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onSuccess();
                    }
                });
            } else {
                mCallback.onSuccess();
            }
        }

        @Override
        public void onFail(final State state, final McuMgrException error) {
            if (mUiThreadCallbacks) {
                getMainThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onFail(state, error);
                    }
                });
            } else {
                mCallback.onFail(state, error);
            }
        }

        @Override
        public void onCancel(final State state) {
            if (mUiThreadCallbacks) {
                getMainThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onCancel(state);
                    }
                });
            } else {
                mCallback.onCancel(state);
            }
        }

        @Override
        public void onUploadProgressChanged(final int bytesSent, final int imageSize, final long timestamp) {
            if (mUiThreadCallbacks) {
                getMainThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onUploadProgressChanged(bytesSent, imageSize, timestamp);
                    }
                });
            } else {
                mCallback.onUploadProgressChanged(bytesSent, imageSize, timestamp);
            }
        }
    };

    /**
     * Used to execute callbacks on the main UI thread.
     */
    private static class MainThreadExecutor implements Executor {
        private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            mainThreadHandler.post(command);
        }
    }
}
