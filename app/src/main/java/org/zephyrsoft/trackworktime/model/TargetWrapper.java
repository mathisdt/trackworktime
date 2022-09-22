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
package org.zephyrsoft.trackworktime.model;

import android.content.Context;

import org.zephyrsoft.trackworktime.R;

import java.time.LocalDate;

/**
 * Getters are used via reflection!
 */
public class TargetWrapper {
    private final Target wrapped;

    public TargetWrapper(Target wrapped) {
        this.wrapped = wrapped;
    }

    public LocalDate getDate() {
        return wrapped.getDate();
    }

    public static String getType(Target target, Context context) {
        TargetEnum type = TargetEnum.byValue(target.getType());
        if (type == TargetEnum.DAY_SET
            && target.getValue() != null
            && target.getValue() > 0) {
            return context.getString(R.string.targetTypeChangeTargetTime);
        } else if (type == TargetEnum.DAY_SET
            && (target.getValue() == null
            || target.getValue() == 0)) {
            return context.getString(R.string.targetTypeNonWorking);
        } else if (type == TargetEnum.DAY_GRANT) {
            return context.getString(R.string.targetTypeWorkingEqualsTarget);
        }
        return context.getString(R.string.targetTypeUnknown);
    }

    public String getType(Context context) {
        return getType(wrapped, context);
    }

    public Integer getValue() {
        return wrapped.getValue();
    }

    public String getComment() {
        return wrapped.getComment();
    }

    public Integer getId() {
        return wrapped.getId();
    }

    public Integer getType() {
        return wrapped.getType();
    }
}
