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
package org.zephyrsoft.trackworktime.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

/**
 * EditText which allows for intercepting the back key.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class BackSensitiveEditText extends EditText {

	private BackListener backListener = null;

	public BackSensitiveEditText(Context context) {
		super(context);
	}

	public BackSensitiveEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public BackSensitiveEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onKeyPreIme(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event != null && event.getAction() == KeyEvent.ACTION_UP
			&& backListener != null) {
			backListener.backKeyPressed();
		}
		return super.onKeyPreIme(keyCode, event);
	}

	public void setBackListener(BackListener backListener) {
		this.backListener = backListener;
	}

}
