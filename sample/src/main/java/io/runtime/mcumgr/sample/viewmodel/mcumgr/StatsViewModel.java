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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.managers.StatsManager;
import io.runtime.mcumgr.response.stat.McuMgrStatListResponse;
import io.runtime.mcumgr.response.stat.McuMgrStatResponse;

public class StatsViewModel extends ViewModel {
	private final StatsManager mManager;

	private final MutableLiveData<List<McuMgrStatResponse>> mResponseLiveData = new MutableLiveData<>();
	private final MutableLiveData<String> mErrorLiveData = new MutableLiveData<>();

	@Inject
	StatsViewModel(final StatsManager manager) {
		mManager = manager;
	}

	public LiveData<List<McuMgrStatResponse>> getResponse() {
		return mResponseLiveData;
	}

	@NonNull
	public LiveData<String> getError() {
		return mErrorLiveData;
	}

	public void readStats() {
		mManager.list(new McuMgrCallback<McuMgrStatListResponse>() {
			@Override
			public void onResponse(@NonNull final McuMgrStatListResponse listResponse) {
				final List<McuMgrStatResponse> list = new ArrayList<>(listResponse.stat_list.length);

				// Request stats for each module
				for (final String module : listResponse.stat_list) {
					mManager.read(module, new McuMgrCallback<McuMgrStatResponse>() {
						@Override
						public void onResponse(@NonNull final McuMgrStatResponse response) {
							list.add(response);
							mResponseLiveData.postValue(list);
						}

						@Override
						public void onError(@NonNull final McuMgrException error) {
							mErrorLiveData.postValue(error.getMessage());
						}
					});
				}
			}

			@Override
			public void onError(@NonNull final McuMgrException error) {
				mErrorLiveData.postValue(error.getMessage());
			}
		});
	}
}
