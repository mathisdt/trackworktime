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
 * Identifies the separators in a list. As this interface extends {@link StringExtractionMethod}, it can also extract
 * the text of the separators.
 * 
 * @author Mathis Dirksen-Thedens
 */
public interface SeparatorIdentificationMethod<T> extends StringExtractionMethod<T> {
	
	/**
	 * Decides if the given object is a separator (and should be rendered as such).
	 */
	boolean isSeparator(T object);
	
}
