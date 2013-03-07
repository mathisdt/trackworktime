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
package org.zephyrsoft.trackworktime.location;

/**
 * Result which gets returned by {@link LocationTracker#startTrackingByLocation(double, double, double, boolean)}.
 * 
 * @author Mathis Dirksen-Thedens
 */
public enum Result {
	
	/** successfully started the tracking */
	SUCCESS,
	
	/** could not start the tracking because is was already running */
	FAILURE_ALREADY_RUNNING,
	
	/** could not start the tracking, the app doesn't have the necessary rights */
	FAILURE_INSUFFICIENT_RIGHTS
	
}
