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
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.utils.FsUtils;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.FilesDownloadViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class FilesDownloadFragment extends Fragment implements Injectable {

	@Inject
	McuMgrViewModelFactory mViewModelFactory;
	@Inject
	FsUtils mFsUtils;

	@BindView(R.id.file_name)
	EditText mFileName;
	@BindView(R.id.file_path)
	TextView mFilePath;
	@BindView(R.id.action_download)
	Button mDownloadAction;
	@BindView(R.id.progress)
	ProgressBar mProgress;
	@BindView(R.id.divider)
	View mDivider;
	@BindView(R.id.file_result)
	TextView mResult;
	@BindView(R.id.image)
	ImageView mImage;

	private FilesDownloadViewModel mViewModel;
	private InputMethodManager mImm;
	private String mPartition;

	@Override
	public void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mViewModel = ViewModelProviders.of(this, mViewModelFactory)
				.get(FilesDownloadViewModel.class);
		mImm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull final LayoutInflater inflater,
							 @Nullable final ViewGroup container,
							 @Nullable final Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_card_files_download, container, false);
	}

	@Override
	public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ButterKnife.bind(this, view);

		mFileName.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void onTextChanged(final CharSequence s,
									  final int start, final int before, final int count) {
				mFilePath.setText(getString(R.string.files_file_path, mPartition, s));
			}
		});
	}

	@SuppressWarnings("ConstantConditions")
	@Override
	public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mFsUtils.getPartition().observe(this, partition -> {
			mPartition = partition;
			final String fileName = mFileName.getText().toString();
			mFilePath.setText(getString(R.string.files_file_path, partition, fileName));
		});
		mViewModel.getProgress().observe(this, progress -> mProgress.setProgress(progress));
		mViewModel.getResponse().observe(this, this::printContent);
		mViewModel.getError().observe(this, this::printError);
		mViewModel.getBusyState().observe(this, busy -> mDownloadAction.setEnabled(!busy));
		mDownloadAction.setOnClickListener(v -> {
			final String fileName = mFileName.getText().toString();
			if (TextUtils.isEmpty(fileName)) {
				mFileName.setError(getString(R.string.files_download_empty));
			} else {
				hideKeyboard();
				mViewModel.download(mFilePath.getText().toString());
			}
		});
	}

	private void hideKeyboard() {
		mImm.hideSoftInputFromWindow(mFileName.getWindowToken(), 0);
	}

	private void printContent(@Nullable final byte[] data) {
		mDivider.setVisibility(View.VISIBLE);
		mResult.setVisibility(View.VISIBLE);
		mImage.setVisibility(View.VISIBLE);
		mImage.setImageDrawable(null);

		if (data == null) {
			mResult.setText(R.string.files_download_error_file_not_found);
		} else {
			if (data.length == 0) {
				mResult.setText(R.string.files_download_file_empty);
			} else {
				final String path = mFilePath.getText().toString();
				final Bitmap bitmap = FsUtils.toBitmap(getResources(), data);
				if (bitmap != null) {
					final SpannableString spannable = new SpannableString(
							getString(R.string.files_download_image, path, data.length));
					spannable.setSpan(new StyleSpan(Typeface.BOLD),
							0, path.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
					mResult.setText(spannable);
					mImage.setImageBitmap(bitmap);
				} else {
					final String content = new String(data);
					final SpannableString spannable = new SpannableString(
							getString(R.string.files_download_file, path, data.length, content));
					spannable.setSpan(new StyleSpan(Typeface.BOLD),
							0, path.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
					mResult.setText(spannable);
				}
			}
		}
	}

	private void printError(@Nullable final String error) {
		mDivider.setVisibility(View.VISIBLE);
		mResult.setVisibility(View.VISIBLE);

		if (error != null) {
			final SpannableString spannable = new SpannableString(error);
			spannable.setSpan(new ForegroundColorSpan(
							ContextCompat.getColor(requireContext(), R.color.error)),
					0, error.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			spannable.setSpan(new StyleSpan(Typeface.BOLD),
					0, error.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			mResult.setText(spannable);
		} else {
			mResult.setText(null);
		}
	}

	private abstract class SimpleTextWatcher implements TextWatcher {
		@Override
		public void beforeTextChanged(final CharSequence s,
									  final int start, final int count, final int after) {
			// empty
		}

		@Override
		public void afterTextChanged(final Editable s) {
			// empty
		}
	}
}
