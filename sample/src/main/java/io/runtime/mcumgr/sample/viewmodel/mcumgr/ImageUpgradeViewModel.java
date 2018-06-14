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
import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Named;

import io.runtime.mcumgr.dfu.FirmwareUpgradeCallback;
import io.runtime.mcumgr.dfu.FirmwareUpgradeController;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.sample.viewmodel.SingleLiveEvent;

public class ImageUpgradeViewModel extends McuMgrViewModel implements FirmwareUpgradeCallback {
	public enum State {
		IDLE,
		VALIDATING,
		UPLOADING,
		PAUSED,
		TESTING,
		CONFIRMING,
		RESETTING,
		COMPLETE
	}

	private final FirmwareUpgradeManager mManager;

	private final MutableLiveData<State> mStateLiveData = new MutableLiveData<>();
	private final MutableLiveData<Integer> mProgressLiveData = new MutableLiveData<>();
	private final MutableLiveData<String> mErrorLiveData = new MutableLiveData<>();
	private final SingleLiveEvent<Void> mCancelledEvent = new SingleLiveEvent<>();

	@Inject
	ImageUpgradeViewModel(final FirmwareUpgradeManager manager,
						  @Named("busy") final MutableLiveData<Boolean> state) {
		super(state);
		mManager = manager;
		mManager.setFirmwareUpgradeCallback(this);
		mStateLiveData.setValue(State.IDLE);
		mProgressLiveData.setValue(0);
	}

	@NonNull
	public LiveData<State> getState() {
		return mStateLiveData;
	}

	@NonNull
	public LiveData<Integer> getProgress() {
		return mProgressLiveData;
	}

	@NonNull
	public LiveData<String> getError() {
		return mErrorLiveData;
	}

	@NonNull
	public LiveData<Void> getCancelledEvent() {
		return mCancelledEvent;
	}

	public void upgrade(@NonNull final byte[] data, @NonNull final FirmwareUpgradeManager.Mode mode) {
		try {
			mManager.setMode(mode);
			mManager.start(data);
		} catch (final McuMgrException e) {
			// TODO Externalize the text
			mErrorLiveData.setValue("Invalid image file.");
		}
	}

	public void pause() {
		if (mManager.isInProgress()) {
			mStateLiveData.postValue(State.PAUSED);
			mManager.pause();
			setReady();
		}
	}

	public void resume() {
		if (mManager.isPaused()) {
			setBusy();
			mStateLiveData.postValue(State.UPLOADING);
			mManager.resume();
		}
	}

	public void cancel() {
		mManager.cancel();
	}


	@Override
	public void onStart(final FirmwareUpgradeController controller) {
		postBusy();
		mStateLiveData.setValue(State.VALIDATING);
	}

	@Override
	public void onStateChanged(final FirmwareUpgradeManager.State prevState, final FirmwareUpgradeManager.State newState) {
		switch (newState) {
			case UPLOAD:
				mStateLiveData.postValue(State.UPLOADING);
				break;
			case TEST:
				mStateLiveData.postValue(State.TESTING);
				break;
			case CONFIRM:
				mStateLiveData.postValue(State.CONFIRMING);
				break;
			case RESET:
				mStateLiveData.postValue(State.RESETTING);
				break;
		}
	}

	@Override
	public void onUploadProgressChanged(final int bytesSent, final int imageSize, final long timestamp) {
		// Convert to percent
		mProgressLiveData.postValue((int) (bytesSent * 100.f / imageSize));
	}

	@Override
	public void onSuccess() {
		mProgressLiveData.postValue(0);
		mStateLiveData.postValue(State.COMPLETE);
		postReady();
	}

	@Override
	public void onCancel(final FirmwareUpgradeManager.State state) {
		mProgressLiveData.postValue(0);
		mStateLiveData.postValue(State.IDLE);
		mCancelledEvent.post();
		postReady();
	}

	@Override
	public void onFail(final FirmwareUpgradeManager.State state, final McuMgrException error) {
		mProgressLiveData.postValue(0);
		mErrorLiveData.postValue(error.getMessage());
		postReady();
	}
}
