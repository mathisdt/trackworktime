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
package org.zephyrsoft.trackworktime.location;

/**
 * Location-getting callback hook (as closures are not there yet).
 */
public interface LocationCallback {

	/**
	 * Called when the current location was found.
	 */
	void callback(double latitude, double longitude, int tolerance);

	/**
	 * Called when an error happens.
	 */
	void error(Throwable t);

}
