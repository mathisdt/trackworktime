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
package org.zephyrsoft.trackworktime.util;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;

import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;

import org.zephyrsoft.trackworktime.R;

public class ThemeUtil {
    public static ActionBar styleActionBar(Context context, ActionBar actionBar) {
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);

            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.colorPrimaryVariant, typedValue, true);
            int color = ContextCompat.getColor(context, typedValue.resourceId);
            actionBar.setBackgroundDrawable(new ColorDrawable(color));
        }
        return actionBar;
    }
}
