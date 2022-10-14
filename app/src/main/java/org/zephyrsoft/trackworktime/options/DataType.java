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

import android.content.SharedPreferences;

import org.zephyrsoft.trackworktime.util.DateTimeUtil;

import java.time.LocalTime;
import java.util.regex.Pattern;

/**
 * Data types for the options.
 * 
 * @see Key
 */
public enum DataType {

	/** string */
	TIMEZONEID {
		@Override
		public boolean validate(String value) {
			// FIXME
			return true;
		}

		@Override
		public boolean validateFromSharedPreferences(SharedPreferences sharedPreferences, String key) {
			// FIXME
			return true;
		}
	},
	/** boolean */
	BOOLEAN {
		@Override
		public boolean validate(String value) {
			// method will not be used as boolean options don't have input fields
			return true;
		}

		@Override
		public boolean validateFromSharedPreferences(SharedPreferences sharedPreferences, String key) {
			// method will not be used as boolean options don't have input fields
			return true;
		}
	},
	/** integer */
	INTEGER {
		@Override
		public boolean validate(String value) {
			try {
				Integer.parseInt(value);
				return true;
			} catch (Exception nfe) {
				return false;
			}
		}

		@Override
		public boolean validateFromSharedPreferences(SharedPreferences sharedPreferences, String key) {
			String value = sharedPreferences.getString(key, "0");
			return validate(value);
		}
	},
	/** integer or empty string */
	INTEGER_OR_EMPTY {
		@Override
		public boolean validate(String value) {
			if (value == null || value.trim().length() == 0) {
				return true;
			}
			try {
				Integer.parseInt(value);
				return true;
			} catch (Exception nfe) {
				return false;
			}
		}

		@Override
		public boolean validateFromSharedPreferences(SharedPreferences sharedPreferences, String key) {
			String value = sharedPreferences.getString(key, "0");
			return validate(value);
		}
	},
	/** double */
	DOUBLE {
		@Override
		public boolean validate(String value) {
			try {
				Double.parseDouble(value);
				return true;
			} catch (Exception nfe) {
				return false;
			}
		}

		@Override
		public boolean validateFromSharedPreferences(SharedPreferences sharedPreferences, String key) {
			String value = sharedPreferences.getString(key, "0.0");
			return validate(value);
		}
	},
	/** long */
	LONG {
		@Override
		public boolean validate(String value) {
			try {
				Long.parseLong(value);
				return true;
			} catch (Exception nfe) {
				return false;
			}
		}

		@Override
		public boolean validateFromSharedPreferences(SharedPreferences sharedPreferences, String key) {
			String value = sharedPreferences.getString(key, "0");
			return validate(value);
		}
	},
	/** time in 24-hour format, e.g. "15.30" or "5.01" */
	TIME {
		@Override
		public boolean validate(String value) {
			String refinedValue = DateTimeUtil.refineTime(value);
			try {
				LocalTime.parse(refinedValue).getHour();
				return true;
			} catch (Exception nfe) {
				return false;
			}
		}

		@Override
		public boolean validateFromSharedPreferences(SharedPreferences sharedPreferences, String key) {
			String value = sharedPreferences.getString(key, "00:00");
			return validate(value);
		}
	},
	/** hours and minutes separated by colon (not necessarily under 24 hours), e.g. "27:25" */
	HOUR_MINUTE {
		@Override
		public boolean validate(String value) {
			return value != null && Pattern.matches("-?\\d+:\\d\\d", value);
		}

		@Override
		public boolean validateFromSharedPreferences(SharedPreferences sharedPreferences, String key) {
			String value = sharedPreferences.getString(key, "0:00");
			return validate(value);
		}
	},
	/** string for a wi-fi ssid */
	SSID {
		@Override
		public boolean validate(String value) {
			if (value != null && value.length() != 0 // not empty
				&& value.trim().length() != 0) { // not only whitespaces

				for (int i = 0; i < value.length(); i++) {
					// containing whitespaces? space is allowed
					if (Character.isWhitespace(value.charAt(i)) && value.charAt(i) != ' ') {
						return false;
					}
				}
				return true;

			} else {
				return false;
			}
		}

		@Override
		public boolean validateFromSharedPreferences(SharedPreferences sharedPreferences, String key) {
			String value = sharedPreferences.getString(key, "");
			return validate(value);
		}
	},
	/** String value for enum, as returned by enum#name() */
	ENUM_NAME {
		@Override
		public boolean validate(String value) {
			return value != null && !value.isEmpty();
		}

		@Override
		public boolean validateFromSharedPreferences(SharedPreferences sharedPreferences, String key) {
			String value = sharedPreferences.getString(key, "");
			return validate(value);
		}
	};

	/**
	 * Validate that a value is correct for this data type.
	 */
	public abstract boolean validate(String value);

	/**
	 * Validate that the value found under "key" in "sharedPreferences" is correct for this data type.
	 */
	public abstract boolean validateFromSharedPreferences(SharedPreferences sharedPreferences, String key);

}
