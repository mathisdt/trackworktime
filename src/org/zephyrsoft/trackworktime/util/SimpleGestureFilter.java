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

import android.app.Activity;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

/**
 * Filter for {@link MotionEvent}s to recognize simple gestures.
 * 
 * @see <a href="http://android-journey.blogspot.com/2010/01/android-gestures.html">blog post</a>
 * @author Amir Sadrinia <amir.sadrinia@gmail.com>
 */
public class SimpleGestureFilter extends SimpleOnGestureListener {
	
	/** swipe direction: up */
	public final static int SWIPE_UP = 1;
	/** swipe direction: down */
	public final static int SWIPE_DOWN = 2;
	/** swipe direction: left */
	public final static int SWIPE_LEFT = 3;
	/** swipe direction: right */
	public final static int SWIPE_RIGHT = 4;
	
	/** touch events should not be handled at all */
	public final static int MODE_TRANSPARENT = 0;
	/** touch events should be only used for gesture recognition and NEVER be forwarded to the window */
	public final static int MODE_SOLID = 1;
	/** touch events should be forwarded to the window if they are not gestures (default) */
	public final static int MODE_DYNAMIC = 2;
	
	private final static int ACTION_FAKE = -13; // just an unlikely number
	
	private int swipeMinDistance = 100;
	private int swipeMaxDistance = 350;
	private int swipeMinVelocity = 100;
	
	private int mode = MODE_DYNAMIC;
	private boolean running = true;
	private boolean tapIndicator = false;
	
	private Activity context;
	private GestureDetector detector;
	private SimpleGestureListener listener;
	
	/**
	 * Constructur
	 * 
	 * @param context the activity to monitor for gestures
	 * @param sgl the listener to notify when a gesture is recognized
	 */
	public SimpleGestureFilter(Activity context, SimpleGestureListener sgl) {
		
		this.context = context;
		this.detector = new GestureDetector(context, this);
		this.listener = sgl;
	}
	
	/**
	 * Examine touch events to recognize gestures.
	 * 
	 * @param event the touch event
	 */
	public void onTouchEvent(MotionEvent event) {
		if (!this.running)
			return;
		
		boolean result = this.detector.onTouchEvent(event);
		
		if (this.mode == MODE_SOLID) {
			event.setAction(MotionEvent.ACTION_CANCEL);
		} else if (this.mode == MODE_DYNAMIC) {
			if (event.getAction() == ACTION_FAKE)
				event.setAction(MotionEvent.ACTION_UP);
			else if (result)
				event.setAction(MotionEvent.ACTION_CANCEL);
			else if (this.tapIndicator) {
				event.setAction(MotionEvent.ACTION_DOWN);
				this.tapIndicator = false;
			}
			
		} else if (mode == MODE_TRANSPARENT) {
			// do nothing
		} else {
			throw new IllegalArgumentException("illegal mode");
		}
	}
	
	/**
	 * Set the mode as one of {@link #MODE_DYNAMIC} (default), {@link #MODE_SOLID} or {@link #MODE_TRANSPARENT}
	 * 
	 * @param m the mode
	 */
	public void setMode(int m) {
		this.mode = m;
	}
	
	/**
	 * Get the currently active mode.
	 * 
	 * @return the mode
	 */
	public int getMode() {
		return this.mode;
	}
	
	/**
	 * Enable or disable the gesture recognition completely.
	 * 
	 * @param enabled the new state ({@code true} = enabled)
	 */
	public void setEnabled(boolean enabled) {
		this.running = enabled;
	}
	
	/**
	 * Set the maximum distance for a swipe (default: 350).
	 * 
	 * @param distance the distance
	 */
	public void setSwipeMaxDistance(int distance) {
		this.swipeMaxDistance = distance;
	}
	
	/**
	 * Get the maximum distance for a swipe.
	 * 
	 * @return the distance
	 */
	public int getSwipeMaxDistance() {
		return this.swipeMaxDistance;
	}
	
	/**
	 * Set the minimum distance for a swipe (default: 150).
	 * 
	 * @param distance the distance
	 */
	public void setSwipeMinDistance(int distance) {
		this.swipeMinDistance = distance;
	}
	
	/**
	 * Get the minimum distance for a swipe.
	 * 
	 * @return the distance
	 */
	public int getSwipeMinDistance() {
		return this.swipeMinDistance;
	}
	
	/**
	 * Set the minimum velocity for a swipe (default: 100).
	 * 
	 * @param velocity the velocity
	 */
	public void setSwipeMinVelocity(int velocity) {
		this.swipeMinVelocity = velocity;
	}
	
	/**
	 * Get the minimum velocity for a swipe
	 * 
	 * @return the velocity
	 */
	public int getSwipeMinVelocity() {
		return this.swipeMinVelocity;
	}
	
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		
		final float xDistance = Math.abs(e1.getX() - e2.getX());
		final float yDistance = Math.abs(e1.getY() - e2.getY());
		
		if (xDistance > this.swipeMaxDistance || yDistance > this.swipeMaxDistance)
			return false;
		
		float velocityX2 = Math.abs(velocityX);
		float velocityY2 = Math.abs(velocityY);
		boolean result = false;
		
		if (velocityX2 > this.swipeMinVelocity && xDistance > this.swipeMinDistance) {
			if (e1.getX() > e2.getX()) {
				// right to left
				this.listener.onSwipe(SWIPE_LEFT);
			} else {
				this.listener.onSwipe(SWIPE_RIGHT);
			}
			
			result = true;
		} else if (velocityY2 > this.swipeMinVelocity && yDistance > this.swipeMinDistance) {
			if (e1.getY() > e2.getY()) {
				// bottom to up
				this.listener.onSwipe(SWIPE_UP);
			} else {
				this.listener.onSwipe(SWIPE_DOWN);
			}
			result = true;
		}
		
		return result;
	}
	
	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		this.tapIndicator = true;
		return false;
	}
	
	@Override
	public boolean onDoubleTap(MotionEvent arg0) {
		this.listener.onDoubleTap();
		return true;
	}
	
	@Override
	public boolean onDoubleTapEvent(MotionEvent arg0) {
		return true;
	}
	
	@Override
	public boolean onSingleTapConfirmed(MotionEvent arg0) {
		
		if (this.mode == MODE_DYNAMIC) { // we owe an ACTION_UP, so we fake an
			arg0.setAction(ACTION_FAKE); // action which will be converted to an ACTION_UP later.
			this.context.dispatchTouchEvent(arg0);
		}
		
		return false;
	}
	
}
