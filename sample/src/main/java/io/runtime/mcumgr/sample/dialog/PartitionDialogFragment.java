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

package io.runtime.mcumgr.sample.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import javax.inject.Inject;

import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.utils.FsUtils;

public class PartitionDialogFragment extends DialogFragment implements Injectable {

    @Inject
    FsUtils mFsUtils;

	private InputMethodManager mImm;

	public static DialogFragment getInstance() {
		return new PartitionDialogFragment();
	}

	@Override
	public void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mImm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final LayoutInflater inflater = requireActivity().getLayoutInflater();
		final View view = inflater.inflate(R.layout.dialog_files_settings, null);
		final EditText partition = view.findViewById(R.id.partition);
		partition.setText(mFsUtils.getPartitionString());
		partition.selectAll();

		final AlertDialog dialog = new AlertDialog.Builder(requireContext())
				.setTitle(R.string.files_settings_title)
				.setView(view)
				.setPositiveButton(R.string.files_settings_action_save, (di, which) -> {
					final String newPartition = partition.getText().toString().trim();
					mFsUtils.setPartition(newPartition);
				})
				.setNegativeButton(android.R.string.cancel, null)
				// Setting the neutral button listener here would cause the dialog to dismiss.
				.setNeutralButton(R.string.files_settings_action_restore, null)
				.create();
		dialog.setOnShowListener(d -> mImm.showSoftInput(partition, InputMethodManager.SHOW_IMPLICIT));

		// The neutral button should not dismiss the dialog.
		// We have to overwrite the default OnClickListener.
		// This can be done only after the dialog was shown.
		dialog.show();
		dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v -> {
		    final String defaultPartition = mFsUtils.getDefaultPartition();
			partition.setText(defaultPartition);
			partition.setSelection(defaultPartition.length());
		});
		return dialog;
	}
}
