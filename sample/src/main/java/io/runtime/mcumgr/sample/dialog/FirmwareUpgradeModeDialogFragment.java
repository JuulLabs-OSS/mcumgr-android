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
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.fragment.mcumgr.ImageUpgradeFragment;

public class FirmwareUpgradeModeDialogFragment extends DialogFragment {
	private static final String SIS_ITEM = "item";

	private int mSelectedItem;

	public static DialogFragment getInstance() {
		return new FirmwareUpgradeModeDialogFragment();
	}

	@SuppressWarnings("ConstantConditions")
	@NonNull
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			mSelectedItem = savedInstanceState.getInt(SIS_ITEM);
		} else {
			mSelectedItem = 0;
		}

		return new AlertDialog.Builder(requireContext())
				.setTitle(R.string.image_upgrade_mode)
				.setSingleChoiceItems(R.array.image_upgrade_options, mSelectedItem,
						(dialog, which) -> mSelectedItem = which)
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(R.string.image_upgrade_action_start, (dialog, which) -> {
					final ImageUpgradeFragment parent = (ImageUpgradeFragment) getParentFragment();
					parent.start(getMode());
				})
				.create();
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SIS_ITEM, mSelectedItem);
	}

	private FirmwareUpgradeManager.Mode getMode() {
		switch (mSelectedItem) {
			case 2:
				return FirmwareUpgradeManager.Mode.CONFIRM_ONLY;
			case 1:
				return FirmwareUpgradeManager.Mode.TEST_ONLY;
			case 0:
			default:
				return FirmwareUpgradeManager.Mode.TEST_AND_CONFIRM;
		}
	}
}
