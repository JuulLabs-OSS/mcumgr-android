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

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.image.McuMgrImage;
import io.runtime.mcumgr.managers.ImageManager;
import io.runtime.mcumgr.response.img.McuMgrImageStateResponse;
import io.runtime.mcumgr.sample.viewmodel.SingleLiveEvent;

public class ImageUploadViewModel extends McuMgrViewModel implements ImageManager.ImageUploadCallback {
	public enum State {
		IDLE,
		VALIDATING,
		UPLOADING,
		PAUSED,
		COMPLETE
	}

	private final ImageManager mManager;

	private final MutableLiveData<State> mStateLiveData = new MutableLiveData<>();
	private final MutableLiveData<Integer> mProgressLiveData = new MutableLiveData<>();
	private final MutableLiveData<String> mErrorLiveData = new MutableLiveData<>();
	private final SingleLiveEvent<Void> mCancelledEvent = new SingleLiveEvent<>();

	@Inject
	ImageUploadViewModel(final ImageManager manager,
						 @Named("busy") final MutableLiveData<Boolean> state) {
		super(state);
		mManager = manager;
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

	public void upload(@NonNull final byte[] data) {
		setBusy();
		mStateLiveData.setValue(State.VALIDATING);

		byte[] hash;
		try {
			hash = McuMgrImage.getHash(data);
		} catch (final McuMgrException e) {
			// TODO Externalize the text
			mErrorLiveData.setValue("Invalid image file.");
			return;
		}

		mManager.list(new McuMgrCallback<McuMgrImageStateResponse>() {
			@Override
			public void onResponse(@NonNull final McuMgrImageStateResponse response) {
				// Check if the new firmware is different than the active one.
				if (response.images.length > 0 && Arrays.equals(hash, response.images[0].hash)) {
					// TODO Externalize the text
					mErrorLiveData.setValue("Firmware already active.");
					postReady();
					return;
				}

				// Check if the new firmware was already sent.
				if (response.images.length > 1 && Arrays.equals(hash, response.images[1].hash)) {
					// Firmware is identical to one on slot 1. No need to send anything.
					mStateLiveData.setValue(State.COMPLETE);
					postReady();
					return;
				}

				// Send the firmware.
				mStateLiveData.postValue(State.UPLOADING);
				mManager.upload(data, ImageUploadViewModel.this);
			}

			@Override
			public void onError(@NonNull final McuMgrException error) {
				mErrorLiveData.postValue(error.getMessage());
				postReady();
			}
		});
	}

	public void pause() {
		mStateLiveData.postValue(State.PAUSED);
		mManager.pauseUpload();
	}

	public void resume() {
		mStateLiveData.postValue(State.UPLOADING);
		mManager.continueUpload();
	}

	public void cancel() {
		mManager.cancelUpload();
	}

	@Override
	public void onProgressChange(final int bytesSent, final int imageSize, final long timestamp) {
		mProgressLiveData.postValue((int) (bytesSent * 100.f / imageSize)); // Convert to percent
	}

	@Override
	public void onUploadFail(@NonNull final McuMgrException error) {
		mProgressLiveData.postValue(0);
		mErrorLiveData.postValue(error.getMessage());
		postReady();
	}

	@Override
	public void onUploadCancel() {
		mProgressLiveData.postValue(0);
		mStateLiveData.postValue(State.IDLE);
		mCancelledEvent.post();
		postReady();
	}

	@Override
	public void onUploadFinish() {
		mProgressLiveData.postValue(0);
		mStateLiveData.postValue(State.COMPLETE);
		postReady();
	}
}
