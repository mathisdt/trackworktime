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

public enum NightMode {
    SYSTEM_DEFAULT(-1),
    LIGHT_MODE(1),
    DARK_MODE(2);

    /**
     * see {@code xml/strings.xml}, entry {@code nightModeValues}
     */
    private final int value;

    NightMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
