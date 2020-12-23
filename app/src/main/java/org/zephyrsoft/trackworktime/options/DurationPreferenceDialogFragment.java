package org.zephyrsoft.trackworktime.options;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceDialogFragmentCompat;

import org.zephyrsoft.trackworktime.util.DateTimeUtil;

public class DurationPreferenceDialogFragment extends PreferenceDialogFragmentCompat {

	private EditText editText;

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
					editText.setError("Duration is invalid");
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
