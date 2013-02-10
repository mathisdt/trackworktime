/*
 * This file is part of TrackWorkTime (TWT).
 *
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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

import java.math.BigDecimal;

/**
 * Helper for handling geo-coordinates.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class CoordinateUtil {
	
	/**
	 * Round a coordinate with arbitrary precision to a value that makes sense.
	 */
	public static String roundCoordinate(double coordinate) {
		BigDecimal ret = new BigDecimal(String.valueOf(coordinate));
		ret = ret.setScale(6, BigDecimal.ROUND_HALF_UP);
		return ret.toPlainString();
	}
	
}
