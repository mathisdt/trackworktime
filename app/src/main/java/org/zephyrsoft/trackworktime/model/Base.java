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

/**
 * Base class which all other data classes extend. Basic operations are defined here.
 * 
 * @author Mathis Dirksen-Thedens
 */
public abstract class Base {

	/**
	 * Chainable compare operation.
	 */
	@SuppressWarnings("null")
	protected int compare(Object attributeOfMe, Object attributeOfOther, int useIfEqual) {
		if (attributeOfMe == null && attributeOfOther == null) {
			return useIfEqual;
		} else if (attributeOfMe != null && attributeOfOther == null) {
			return 1;
		} else if (attributeOfMe == null && attributeOfOther != null) {
			return -1;
		} else {
			return attributeOfMe.toString().compareTo(attributeOfOther.toString());
		}
	}

	@Override
	public abstract String toString();

}
