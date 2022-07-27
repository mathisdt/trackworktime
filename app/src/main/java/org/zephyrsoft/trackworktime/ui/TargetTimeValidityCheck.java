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
package org.zephyrsoft.trackworktime.ui;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.core.util.Consumer;

import org.zephyrsoft.trackworktime.R;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

public class TargetTimeValidityCheck implements TextWatcher {

    private EditText textfield;
    private Consumer<Boolean> validityListener;
    private Context context;

    public TargetTimeValidityCheck(EditText textfield, Consumer<Boolean> validityListener,
                                   Context context) {
        this.textfield = textfield;
        this.validityListener = validityListener;
        this.context = context;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // nothing to do
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // nothing to do
    }

    @Override
    public void afterTextChanged(Editable text) {
        check(text.toString());
    }

    public void check() {
        check(textfield.getText().toString());
    }

    private void check(String text) {
        if (!textfield.isEnabled() || DateTimeUtil.isDurationValid(text)) {
            textfield.setError(null);
            validityListener.accept(Boolean.TRUE);
        } else {
            textfield.setError(context.getString(R.string.invalidTarget));
            validityListener.accept(Boolean.FALSE);
        }
    }
}
