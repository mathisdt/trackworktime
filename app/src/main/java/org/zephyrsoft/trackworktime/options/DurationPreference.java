/*
 * This file is part of TrackWorkTime (TWT).
 *
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TWT is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TWT. If not, see <http://www.gnu.org/licenses/>.
 */
package org.zephyrsoft.trackworktime.options;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.zephyrsoft.trackworktime.R;

/**
 * @author Peter Rosenberg
 */
public class DurationPreference extends DialogPreference {
    private String myDuration = "0:00";
    private EditText myEditText;

    public DurationPreference(Context ctxt, AttributeSet attrs) {
        super(ctxt, attrs);
    }

    @Override
    protected View onCreateDialogView() {
        myEditText =  new EditText(getContext());
        return myEditText;
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        myEditText.setText(myDuration);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        // deactivate buttonPositive if input string invalid
        final Button buttonPositive = ((AlertDialog)getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);
        myEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                buttonPositive.setEnabled(isDurationValid(String.valueOf(s)));
            }

            @Override
            public void afterTextChanged(Editable s) {
                // not used
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        final String duration = String.valueOf(myEditText.getText());
        if (isDurationValid(duration)) {
            myDuration = duration;
            if (positiveResult) {
                if (callChangeListener(myDuration)) {
                    persistString(myDuration);
                }
            }
        }
    }

    private boolean isDurationValid(String duration) {
        String[] pieces = duration.split("[:\\.]");
        if (pieces.length == 2){
            try {
                Integer.parseInt(pieces[0]);
                Integer.parseInt(pieces[1]);
                return true;
            } catch (NumberFormatException e) {
                // ignore and return false
            }
        }
        return false;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return (a.getString(index));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            if (defaultValue == null) {
                myDuration = getPersistedString("00:00");
            } else {
                myDuration = getPersistedString(defaultValue.toString());
            }
        } else {
            myDuration = defaultValue.toString();
        }
    }
}
