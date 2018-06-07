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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;

import io.runtime.mcumgr.sample.R;

public class HelpDialogFragment extends AppCompatDialogFragment {
	private static final String ARG_TITLE_RES_ID = "titleResId";
	private static final String ARG_MESSAGE_RES_ID = "messageResId";

	@NonNull
	public static DialogFragment getInstance(@StringRes final int titleResId, @StringRes final int messageResId) {
		final DialogFragment fragment = new HelpDialogFragment();

		final Bundle args = new Bundle();
		args.putInt(ARG_TITLE_RES_ID, titleResId);
		args.putInt(ARG_MESSAGE_RES_ID, messageResId);
		fragment.setArguments(args);

		return fragment;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final Bundle args = getArguments();
		if (args == null) {
			throw new UnsupportedOperationException("HelpDialogFragment created without arguments");
		}

		final int titleResId = getArguments().getInt(ARG_TITLE_RES_ID);
		final int messageResId = getArguments().getInt(ARG_MESSAGE_RES_ID);

		return new AlertDialog.Builder(requireContext())
				.setIcon(R.drawable.ic_help)
				.setTitle(titleResId)
				.setMessage(messageResId)
				.setPositiveButton(android.R.string.ok, null)
				.create();
	}
}
