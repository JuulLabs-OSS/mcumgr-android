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

package io.runtime.mcumgr.sample.fragment.mcumgr;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.EchoViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class EchoFragment extends Fragment implements Injectable {

	@Inject
	McuMgrViewModelFactory mViewModelFactory;

	@BindView(R.id.action_send)
	Button mSendAction;
	@BindView(R.id.echo_value)
	EditText mValue;
	@BindView(R.id.divider)
	View mDivider;
	@BindView(R.id.echo_request)
	TextView mRequest;
	@BindView(R.id.echo_response)
	TextView mResponse;

	private EchoViewModel mViewModel;
	private InputMethodManager mImm;

	@Override
	public void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mViewModel = ViewModelProviders.of(this, mViewModelFactory)
				.get(EchoViewModel.class);
		mImm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull final LayoutInflater inflater,
							 @Nullable final ViewGroup container,
							 @Nullable final Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_card_echo, container, false);
	}

	@Override
	public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ButterKnife.bind(this, view);
	}

	@SuppressWarnings("ConstantConditions")
	@Override
	public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mViewModel.getBusyState().observe(this, busy -> mSendAction.setEnabled(!busy));
		mViewModel.getRequest().observe(this, text -> {
			mDivider.setVisibility(View.VISIBLE);
			print(mRequest, text);
			if (mResponse.getVisibility() == View.VISIBLE) {
				mResponse.setVisibility(View.INVISIBLE);
			}
		});
		mViewModel.getResponse().observe(this, response -> {
			mResponse.setBackgroundResource(R.drawable.echo_response);
			print(mResponse, response);
		});
		mViewModel.getError().observe(this, error -> {
			mResponse.setVisibility(View.VISIBLE);
			mResponse.setBackgroundResource(R.drawable.echo_error);
			mResponse.setText(error);
		});
		mSendAction.setOnClickListener(v -> {
			mRequest.setText(null);
			mResponse.setText(null);

			hideKeyboard();

			final String text = mValue.getText().toString();
			mValue.setText(null);
			mViewModel.echo(text);
		});
	}

	private void hideKeyboard() {
		mImm.hideSoftInputFromWindow(mValue.getWindowToken(), 0);
	}

	private void print(final TextView view, final String text) {
		view.setVisibility(View.VISIBLE);
		if (TextUtils.isEmpty(text)) {
			view.setText(R.string.echo_empty);
			view.setTypeface(null, Typeface.ITALIC);
		} else {
			view.setText(text);
			view.setTypeface(null, Typeface.NORMAL);
		}
	}
}
