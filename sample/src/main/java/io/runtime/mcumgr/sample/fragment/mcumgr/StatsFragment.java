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

import android.animation.LayoutTransition;
import android.arch.lifecycle.ViewModelProviders;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.runtime.mcumgr.response.stat.McuMgrStatResponse;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.StatsViewModel;

public class StatsFragment extends Fragment implements Injectable {

	@Inject
	McuMgrViewModelFactory mViewModelFactory;

	@BindView(R.id.stats_value)
	TextView mStatsValue;
	@BindView(R.id.image_control_error)
	TextView mError;
	@BindView(R.id.action_refresh)
	Button mActionRefresh;

	private StatsViewModel mViewModel;

	@Override
	public void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mViewModel = ViewModelProviders.of(this, mViewModelFactory)
				.get(StatsViewModel.class);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull final LayoutInflater inflater,
							 @Nullable final ViewGroup container,
							 @Nullable final Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_card_stats, container, false);
	}

	@Override
	public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ButterKnife.bind(this, view);

		// This makes the layout animate when the TextView value changes.
		// By default it animates only on hiding./showing views.
		// The view must have android:animateLayoutChanges(true) attribute set in the XML.
		((ViewGroup) view).getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
	}

	@SuppressWarnings("ConstantConditions")
	@Override
	public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mViewModel.getResponse().observe(this, this::printStats);
		mViewModel.getError().observe(this, this::printError);
		mViewModel.getBusyState().observe(this, busy -> mActionRefresh.setEnabled(!busy));
		mActionRefresh.setOnClickListener(v -> mViewModel.readStats());
	}

	private void printStats(@NonNull final List<McuMgrStatResponse> responses) {
		final SpannableStringBuilder builder = new SpannableStringBuilder();
		for (final McuMgrStatResponse response : responses) {
			builder.append(getString(R.string.stats_module, response.name)).append("\n");
			builder.setSpan(new StyleSpan(Typeface.BOLD), 0, response.name.length(),
					Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

			for (final Map.Entry<String, Integer> entry : response.fields.entrySet()) {
				builder.append(getString(R.string.stats_field,
						entry.getKey(), entry.getValue())).append("\n");
			}
		}
		mStatsValue.setText(builder);
	}

	private void printError(@Nullable final String error) {
		if (error != null) {
			final SpannableString spannable = new SpannableString(error);
			spannable.setSpan(new ForegroundColorSpan(
							ContextCompat.getColor(requireContext(), R.color.error)),
					0, error.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			spannable.setSpan(new StyleSpan(Typeface.BOLD),
					0, error.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			mError.setText(spannable);
			mError.setVisibility(View.VISIBLE);
		} else {
			mError.setText(null);
		}
	}
}
