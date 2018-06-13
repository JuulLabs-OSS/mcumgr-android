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

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.managers.FsManager;
import io.runtime.mcumgr.sample.viewmodel.SingleLiveEvent;

public class FilesUploadViewModel extends McuMgrViewModel implements FsManager.FileUploadCallback {
	public enum State {
		IDLE,
		UPLOADING,
		PAUSED,
		COMPLETE
	}

	private final FsManager mManager;

	private final MutableLiveData<State> mStateLiveData = new MutableLiveData<>();
	private final MutableLiveData<Integer> mProgressLiveData = new MutableLiveData<>();
	private final MutableLiveData<String> mErrorLiveData = new MutableLiveData<>();
	private final SingleLiveEvent<Void> mCancelledEvent = new SingleLiveEvent<>();

	@Inject
    FilesUploadViewModel(final FsManager manager,
                         @Named("busy") final MutableLiveData<Boolean> state) {
		super(state);
		mStateLiveData.setValue(State.IDLE);
		mManager = manager;
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

	public void upload(final String path, final byte[] data) {
		setBusy();
		mStateLiveData.setValue(State.UPLOADING);
		mManager.upload(path, data, this);
	}

	public void pause() {
		if (mManager.getState() == FsManager.STATE_UPLOADING) {
			mStateLiveData.setValue(State.PAUSED);
			mManager.pauseTransfer();
			setReady();
		}
	}

	public void resume() {
		if (mManager.getState() == FsManager.STATE_PAUSED) {
			mStateLiveData.setValue(State.UPLOADING);
			setBusy();
			mManager.continueTransfer();
		}
	}

	public void cancel() {
		mManager.cancelTransfer();
	}

	@Override
	public void onProgressChange(final int bytesSent, final int imageSize, final long timestamp) {
		// Convert to percent
		mProgressLiveData.postValue((int) (bytesSent * 100.f / imageSize));
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
