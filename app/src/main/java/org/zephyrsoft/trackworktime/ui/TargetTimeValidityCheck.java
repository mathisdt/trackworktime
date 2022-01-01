package org.zephyrsoft.trackworktime.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.core.util.Consumer;

import org.zephyrsoft.trackworktime.util.DateTimeUtil;

public class TargetTimeValidityCheck implements TextWatcher {

    private EditText textfield;
    private Consumer<Boolean> validityListener;

    public TargetTimeValidityCheck(EditText textfield, Consumer<Boolean> validityListener) {
        this.textfield = textfield;
        this.validityListener = validityListener;
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
            textfield.setError("Target is invalid");
            validityListener.accept(Boolean.FALSE);
        }
    }
}
