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

/**
 * Listener interface for recognizing simple gestures.
 * 
 * @see <a href="http://android-journey.blogspot.com/2010/01/android-gestures.html">blog post</a>
 * @author Amir Sadrinia <amir.sadrinia@gmail.com>
 */
public interface SimpleGestureListener {

	/**
	 * Called when a swipe gesture is recognized.
	 * 
	 * @param direction
	 *            the direction, signaled via {@link SimpleGestureFilter#SWIPE_UP} or one of the other directions
	 */
	void onSwipe(int direction);

	/**
	 * Called when a double-tap gesture is recognized.
	 */
	void onDoubleTap();
}
