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
	 * @param direction the direction, signaled via {@link SimpleGestureFilter#SWIPE_UP} or one of the other directions
	 */
	void onSwipe(int direction);
	
	/**
	 * Called when a double-tap gesture is recognized.
	 */
	void onDoubleTap();
}
