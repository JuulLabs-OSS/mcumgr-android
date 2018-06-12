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

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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

public class ImageUpgradeFragment extends Fragment implements Injectable, LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = ImageUpgradeFragment.class.getSimpleName();

	private static final int SELECT_FILE_REQ = 1;
	private static final int LOAD_FILE_LOADER_REQ = 2;
	private static final String EXTRA_FILE_URI = "uri";

	private static final String SIS_DATA = "data";

	@Inject
	McuMgrViewModelFactory mViewModelFactory;

	@BindView(R.id.dfu_file_name)
	TextView mFileName;
	@BindView(R.id.dfu_file_hash)
	TextView mFileHash;
	@BindView(R.id.dfu_file_size)
	TextView mFileSize;
	@BindView(R.id.dfu_status)
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
	private byte[] mFileContent;

	@Override
	public void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mViewModel = ViewModelProviders.of(this, mViewModelFactory)
				.get(ImageUpgradeViewModel.class);

		if (savedInstanceState != null) {
			mFileContent = savedInstanceState.getByteArray(SIS_DATA);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull final Bundle outState) {
		super.onSaveInstanceState(outState);
 		outState.putByteArray(SIS_DATA, mFileContent);
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
					mFileContent = null;
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
			mFileContent = null;
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
			mStartAction.setEnabled(mFileContent != null && !busy);
		});

		// Configure SELECT FILE action
		mSelectFileAction.setOnClickListener(v -> selectFile());

		// Restore START action state after rotation
		mStartAction.setEnabled(mFileContent != null);
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
		mViewModel.upgrade(mFileContent, mode);
	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
				case SELECT_FILE_REQ: {
					mFileContent = null;
					mStartAction.setEnabled(false);

					final Uri uri = data.getData();

					if (uri == null) {
						Toast.makeText(requireContext(), R.string.image_error_no_uri,
								Toast.LENGTH_SHORT).show();
						return;
					}

					// The URI returned may be of 2 schemes: file:// (legacy) or content:// (new)
					if (uri.getScheme().equals("file")) {
						// TODO This may require WRITE_EXTERNAL_STORAGE permission!
						final String path = uri.getPath();
						final String fileName = path.substring(path.lastIndexOf('/'));
						mFileName.setText(fileName);

						final File file = new File(path);
						final int fileSize = (int) file.length();
						mFileSize.setText(getString(R.string.image_upgrade_size_value, fileSize));
						try {
							loadContent(new FileInputStream(file));
						} catch (final FileNotFoundException e) {
							Log.e(TAG, "File not found", e);
							mStatus.setText(R.string.image_error_no_uri);
						}
					} else {
						// File name and size must be obtained from Content Provider
						final Bundle bundle = new Bundle();
						bundle.putParcelable(EXTRA_FILE_URI, uri);
						getLoaderManager().restartLoader(LOAD_FILE_LOADER_REQ, bundle, this);
					}
				}
			}
		}
	}

	@SuppressWarnings("ConstantConditions")
	@NonNull
	@Override
	public Loader<Cursor> onCreateLoader(final int id, @Nullable final Bundle args) {
		switch (id) {
			case LOAD_FILE_LOADER_REQ:
				final Uri uri = args.getParcelable(EXTRA_FILE_URI);
				return new CursorLoader(requireContext(), uri,
						null/* projection */, null, null, null);
		}
		throw new UnsupportedOperationException("Invalid loader ID: " + id);
	}

	@Override
	public void onLoadFinished(@NonNull final Loader<Cursor> loader, final Cursor data) {
		if (data == null) {
			Toast.makeText(requireContext(), R.string.image_error_loading_file_failed,
					Toast.LENGTH_SHORT).show();
			return;
		}

		switch (loader.getId()) {
			case LOAD_FILE_LOADER_REQ: {
				final int displayNameColumn = data.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
				final int sizeColumn = data.getColumnIndex(MediaStore.MediaColumns.SIZE);

				if (displayNameColumn == -1 || sizeColumn == -1) {
					Toast.makeText(requireContext(), R.string.image_error_loading_file_failed,
							Toast.LENGTH_SHORT).show();
					break;
				}

				if (data.moveToNext()) {
					final String fileName = data.getString(displayNameColumn);
					final int fileSize = data.getInt(sizeColumn);
					if (fileName == null || fileSize < 0) {
						Toast.makeText(requireContext(), R.string.image_error_loading_file_failed,
								Toast.LENGTH_SHORT).show();
						break;
					}

					mFileName.setText(fileName);
					mFileSize.setText(getString(R.string.image_upgrade_size_value, fileSize));

					try {
						final CursorLoader cursorLoader = (CursorLoader) loader;
						final InputStream is = requireContext().getContentResolver()
								.openInputStream(cursorLoader.getUri());
						loadContent(is);
					} catch (final FileNotFoundException e) {
						Log.e(TAG, "File not found", e);
						mStatus.setText(R.string.image_error_no_uri);
					}
				} else {
					Log.e(TAG, "Empty cursor");
					mStatus.setText(R.string.image_error_no_uri);
				}
				// Reset the loader as the URU read permission is one time only.
				// We keep the file content in the fragment so no need to load it again.
				// onLoaderReset(...) will be called after that.
				getLoaderManager().destroyLoader(LOAD_FILE_LOADER_REQ);
			}
		}
	}

	@Override
	public void onLoaderReset(@NonNull final Loader<Cursor> loader) {
		// ignore
	}

	private void selectFile() {
		final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("application/*");
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
			// file browser has been found on the device
			startActivityForResult(intent, SELECT_FILE_REQ);
		} else {
			Toast.makeText(requireContext(), R.string.image_error_no_file_browser,
					Toast.LENGTH_SHORT).show();
		}
	}

	private void loadContent(@Nullable final InputStream is) {
		if (is == null) {
			mStatus.setText(R.string.image_error_loading_file_failed);
			return;
		}

		try {
			final BufferedInputStream buf = new BufferedInputStream(is);
			final int size = buf.available();
			final byte[] bytes = new byte[size];
			try {
				int offset = 0;
				int retry = 0;
				while (offset < size && retry < 5) {
					offset += buf.read(bytes, offset, size - offset);
					retry ++;
				}
			} finally {
				buf.close();
			}
			final byte[] hash = McuMgrImage.getHash(bytes);
			mFileHash.setText(StringUtils.toHex(hash));
			mStatus.setText(R.string.image_upgrade_status_ready);
			mFileContent = bytes;
			mStartAction.setEnabled(true);
		} catch (final IOException e) {
			Log.e(TAG, "Reading file content failed", e);
			mStatus.setText(R.string.image_error_loading_file_failed);
		} catch (final McuMgrException e) {
			Log.e(TAG, "Reading hash failed, not a valid image", e);
			mStatus.setText(R.string.image_error_file_not_valid);
		}
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
