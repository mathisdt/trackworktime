/*
 * This file is part of TrackWorkTime (TWT).
 *
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License 3.0 as published by
 * the Free Software Foundation.
 *
 * TWT is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License 3.0 for more details.
 *
 * You should have received a copy of the GNU General Public License 3.0
 * along with TWT. If not, see <http://www.gnu.org/licenses/>.
 */
package org.zephyrsoft.trackworktime.options;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceDialogFragmentCompat;

import org.zephyrsoft.trackworktime.R;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

public class DurationPreferenceDialogFragment extends PreferenceDialogFragmentCompat {

	private EditText editText;

	@Override
	protected void onPrepareDialogBuilder(@NonNull AlertDialog.Builder builder) {
		super.onPrepareDialogBuilder(builder);
		builder.setMessage(getPreference().getSummary() + "\n");
	}

	@Override
	protected View onCreateDialogView(Context context) {
		editText = new EditText(context);
		return editText;
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);

		DurationPreference pref = (DurationPreference) getPreference();
		editText.setText(pref.getDuration());
	}

	@Override
	public void onStart() {
		super.onStart();

		final Button buttonPositive = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);
		editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// not used
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// not used
			}

			@Override public void afterTextChanged(Editable s) {
				if (DateTimeUtil.isDurationValid(editText.getText().toString())) {
					editText.setError(null);
					buttonPositive.setEnabled(true);
				} else {
					editText.setError(getString(R.string.invalidDuration));
					buttonPositive.setEnabled(false);
				}
			}
		});
	}

	@Override
	public void onDialogClosed(boolean positiveResult) {

		if (positiveResult) {
			DurationPreference pref = (DurationPreference) getPreference();

			pref.updateValue(editText.getText().toString());
		}
	}
}
