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
import android.support.annotation.NonNull;

import javax.inject.Inject;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.managers.ImageManager;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.response.img.McuMgrImageStateResponse;

public class ImageControlViewModel extends ViewModel {
	private final ImageManager mManager;

	private final MutableLiveData<McuMgrImageStateResponse> mResponseLiveData = new MutableLiveData<>();
	private final MutableLiveData<Boolean> mTestAvailableLiveData = new MutableLiveData<>();
	private final MutableLiveData<Boolean> mConfirmAvailableLiveData = new MutableLiveData<>();
	private final MutableLiveData<Boolean> mEraseAvailableLiveData = new MutableLiveData<>();
	private final MutableLiveData<String> mErrorLiveData = new MutableLiveData<>();

	private byte[] mSlot1Hash;

	@Inject
	ImageControlViewModel(final ImageManager manager) {
		mManager = manager;
	}

	@NonNull
	public LiveData<McuMgrImageStateResponse> getResponse() {
		return mResponseLiveData;
	}

	@NonNull
	public LiveData<Boolean> getTestOperationAvailability() {
		return mTestAvailableLiveData;
	}

	@NonNull
	public LiveData<Boolean> getConfirmOperationAvailability() {
		return mConfirmAvailableLiveData;
	}

	@NonNull
	public LiveData<Boolean> getEraseOperationAvailability() {
		return mEraseAvailableLiveData;
	}

	@NonNull
	public LiveData<String> getError() {
		return mErrorLiveData;
	}

	public void read() {
		mManager.list(new McuMgrCallback<McuMgrImageStateResponse>() {
			@Override
			public void onResponse(@NonNull final McuMgrImageStateResponse response) {
				mResponseLiveData.postValue(response);

				// Save the hash of the image flashed to slot 1.
				final boolean hasSlot1 = response.images != null && response.images.length > 1;
				if (hasSlot1) {
					mSlot1Hash = response.images[1].hash;
				}
				updateStates(response);
			}

			@Override
			public void onError(@NonNull final McuMgrException error) {
				mErrorLiveData.postValue(error.getMessage());
			}
		});
	}

	public void test() {
		mManager.test(mSlot1Hash, new McuMgrCallback<McuMgrImageStateResponse>() {
			@Override
			public void onResponse(@NonNull final McuMgrImageStateResponse response) {
				mResponseLiveData.postValue(response);
				updateStates(response);
			}

			@Override
			public void onError(@NonNull final McuMgrException error) {
				if (error instanceof McuMgrErrorException) {
					final McuMgrErrorCode code = ((McuMgrErrorException) error).getCode();
					if (code == McuMgrErrorCode.UNKNOWN) {
						// User tried to test a firmware with hash equal to the hash of the
						// active firmware. This would result in changing the permanent flag
						// of the slot 0 to false, which is not possible.
						// TODO Externalize the text
						mErrorLiveData.postValue("Image in slot 1 is identical to the active one.");
						return;
					}
				}
				mErrorLiveData.postValue(error.getMessage());
			}
		});
	}

	public void confirm() {
		mManager.confirm(mSlot1Hash, new McuMgrCallback<McuMgrImageStateResponse>() {
			@Override
			public void onResponse(@NonNull final McuMgrImageStateResponse response) {
				mResponseLiveData.postValue(response);
				updateStates(response);
			}

			@Override
			public void onError(@NonNull final McuMgrException error) {
				mErrorLiveData.postValue(error.getMessage());
			}
		});
	}

	public void erase() {
		mManager.erase(new McuMgrCallback<McuMgrResponse>() {
			@Override
			public void onResponse(@NonNull final McuMgrResponse response) {
				read();
			}

			@Override
			public void onError(@NonNull final McuMgrException error) {
				mErrorLiveData.postValue(error.getMessage());
			}
		});
	}

	private void updateStates(@NonNull final McuMgrImageStateResponse response) {
		final boolean hasSlot1 = response.images != null && response.images.length > 1;
		final boolean slot1NotPending = hasSlot1 && !response.images[1].pending;
		final boolean slot1NotPermanent = hasSlot1 && !response.images[1].permanent;
		mTestAvailableLiveData.postValue(slot1NotPending);
		mConfirmAvailableLiveData.postValue(slot1NotPermanent);
		mEraseAvailableLiveData.postValue(hasSlot1);
	}
}
