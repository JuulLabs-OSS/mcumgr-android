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
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.image.McuMgrImage;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.dialog.FirmwareUpgradeModeDialogFragment;
import io.runtime.mcumgr.sample.utils.StringUtils;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.ImageUpgradeViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class ImageUpgradeFragment extends FileBrowserFragment implements Injectable {

	@Inject
	McuMgrViewModelFactory mViewModelFactory;

	@BindView(R.id.file_name)
	TextView mFileName;
	@BindView(R.id.file_hash)
	TextView mFileHash;
	@BindView(R.id.file_size)
	TextView mFileSize;
	@BindView(R.id.status)
	TextView mStatus;
	@BindView(R.id.progress)
	ProgressBar mProgress;
	@BindView(R.id.action_select_file)
	Button mSelectFileAction;
	@BindView(R.id.action_start)
	Button mStartAction;
	@BindView(R.id.action_cancel)
	Button mCancelAction;
	@BindView(R.id.action_pause_resume)
	Button mPauseResumeAction;

	private ImageUpgradeViewModel mViewModel;

	@Override
	public void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mViewModel = ViewModelProviders.of(this, mViewModelFactory)
				.get(ImageUpgradeViewModel.class);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull final LayoutInflater inflater,
							 @Nullable final ViewGroup container,
							 @Nullable final Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_card_image_upgrade, container, false);
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

		mViewModel.getState().observe(this, state -> {
			switch (state) {
				case VALIDATING:
					mSelectFileAction.setVisibility(View.GONE);
					mStartAction.setVisibility(View.GONE);
					mCancelAction.setVisibility(View.VISIBLE);
					mPauseResumeAction.setVisibility(View.VISIBLE);
					mCancelAction.setEnabled(false);
					mPauseResumeAction.setEnabled(false);
					mStatus.setText(R.string.image_upgrade_status_validating);
					break;
				case UPLOADING:
					mCancelAction.setEnabled(true);
					mPauseResumeAction.setEnabled(true);
					mPauseResumeAction.setText(R.string.image_action_pause);
					mStatus.setText(R.string.image_upgrade_status_uploading);
					break;
				case PAUSED:
					mPauseResumeAction.setText(R.string.image_action_resume);
					break;
				case TESTING:
					mCancelAction.setEnabled(false);
					mPauseResumeAction.setEnabled(false);
					mStatus.setText(R.string.image_upgrade_status_testing);
					break;
				case CONFIRMING:
					mCancelAction.setEnabled(false);
					mPauseResumeAction.setEnabled(false);
					mStatus.setText(R.string.image_upgrade_status_confirming);
					break;
				case RESETTING:
					mCancelAction.setEnabled(false);
					mPauseResumeAction.setEnabled(false);
					mStatus.setText(R.string.image_upgrade_status_resetting);
					break;
				case COMPLETE:
					clearFileContent();
					mSelectFileAction.setVisibility(View.VISIBLE);
					mStartAction.setVisibility(View.VISIBLE);
					mStartAction.setEnabled(false);
					mCancelAction.setVisibility(View.GONE);
					mPauseResumeAction.setVisibility(View.GONE);
					mStatus.setText(R.string.image_upgrade_status_completed);
					break;
			}
		});
		mViewModel.getProgress().observe(this, progress -> mProgress.setProgress(progress));
		mViewModel.getError().observe(this, error -> {
			mSelectFileAction.setVisibility(View.VISIBLE);
			mStartAction.setVisibility(View.VISIBLE);
			mCancelAction.setVisibility(View.GONE);
			mPauseResumeAction.setVisibility(View.GONE);
			printError(error);
		});
		mViewModel.getCancelledEvent().observe(this, nothing -> {
			clearFileContent();
			mFileName.setText(null);
			mFileSize.setText(null);
			mFileHash.setText(null);
			mStatus.setText(null);
			mSelectFileAction.setVisibility(View.VISIBLE);
			mStartAction.setVisibility(View.VISIBLE);
			mStartAction.setEnabled(false);
			mCancelAction.setVisibility(View.GONE);
			mPauseResumeAction.setVisibility(View.GONE);
		});
		mViewModel.getBusyState().observe(this, busy -> {
			mSelectFileAction.setEnabled(!busy);
			mStartAction.setEnabled(isFileLoaded() && !busy);
		});

		// Configure SELECT FILE action
		mSelectFileAction.setOnClickListener(v -> selectFile("application/*"));

		// Restore START action state after rotation
		mStartAction.setEnabled(isFileLoaded());
		mStartAction.setOnClickListener(v -> {
			// Show a mode picker. When mode is selected, the upgrade(Mode) method will be called.
			final DialogFragment dialog = FirmwareUpgradeModeDialogFragment.getInstance();
			dialog.show(getChildFragmentManager(), null);
		});

		// Cancel and Pause/Resume buttons
		mCancelAction.setOnClickListener(v -> mViewModel.cancel());
		mPauseResumeAction.setOnClickListener(v -> {
			if (mViewModel.getState().getValue() == ImageUpgradeViewModel.State.UPLOADING) {
				mViewModel.pause();
			} else {
				mViewModel.resume();
			}
		});
	}

	/**
	 * Starts the Firmware Upgrade using a selected mode.
	 */
	public void start(@NonNull final FirmwareUpgradeManager.Mode mode) {
		mViewModel.upgrade(getFileContent(), mode);
	}

	@Override
	protected void onFileCleared() {
		mStartAction.setEnabled(false);
	}

	@Override
	protected void onFileSelected(@NonNull final String fileName, final int fileSize) {
		mFileName.setText(fileName);
		mFileSize.setText(getString(R.string.image_upgrade_size_value, fileSize));
	}

	@Override
	protected void onFileLoaded(@NonNull final byte[] data) {
		try {
			final byte[] hash = McuMgrImage.getHash(data);
			mFileHash.setText(StringUtils.toHex(hash));
			mStartAction.setEnabled(true);
			mStatus.setText(R.string.image_upgrade_status_ready);
		} catch (final McuMgrException e) {
			clearFileContent();
			onFileLoadingFailed(R.string.image_error_file_not_valid);
		}
	}

	@Override
	protected void onFileLoadingFailed(final int error) {
		mStatus.setText(error);
	}

	private void printError(@NonNull final String error) {
		final SpannableString spannable = new SpannableString(error);
		spannable.setSpan(new ForegroundColorSpan(
						ContextCompat.getColor(requireContext(), R.color.error)),
				0, error.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		spannable.setSpan(new StyleSpan(Typeface.BOLD),
				0, error.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		mStatus.setText(spannable);
	}
}
