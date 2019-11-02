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
package org.zephyrsoft.trackworktime.model;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import org.zephyrsoft.trackworktime.database.DAO;

/**
 * Data class for a week.
 *
 * @see DAO
 * @author Mathis Dirksen-Thedens
 */
public class Week extends Base implements Comparable<Week>, Parcelable {
	private Integer id = null;
	private String start = null;
	/** amount of minutes worked in this week */
	private Integer sum = null;
	private Integer flexi = null;

	public Week() {
		// do nothing
	}

	public Week(Integer id, String start, Integer sum, Integer flexi) {
		this.id = id;
		this.start = start;
		this.sum = sum;
		this.flexi = flexi;
	}

	@SuppressLint("ParcelClassLoader") // Ok, since not restoring custom classes
	protected Week(Parcel in) {
		id = (Integer)in.readValue(null);
		start = in.readString();
		sum = (Integer)in.readValue(null);
		flexi = (Integer)in.readValue(null);
	}

	public Integer getId() {
		return id;
	}

	public String getStart() {
		return start;
	}

	public Integer getSum() {
		return sum;
	}

	public Integer getFlexi() {
		return flexi;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void setStart(String start) {
		this.start = start;
	}

	public void setSum(Integer sum) {
		if (sum==null || sum <0) {
			throw new IllegalArgumentException("sum of a week may not be negative");
		}
		this.sum = sum;
	}

	public void setFlexi(Integer flexi) {
		this.flexi = flexi;
	}

	@Override
	public int compareTo(Week another) {
		return compare(getStart(), another.getStart(), compare(getId(), another.getId(), 0));
	}

	/**
	 * This is used e.g. by an ArrayAdapter in a ListView and it is also useful for debugging.
	 *
	 * @see org.zephyrsoft.trackworktime.model.Base#toString()
	 */
	@Override
	public String toString() {
		return getStart() + " - " + getSum();
	}

	public static final Creator<Week> CREATOR = new Creator<Week>() {
		@Override
		public Week createFromParcel(Parcel in) {
			return new Week(in);
		}

		@Override
		public Week[] newArray(int size) {
			return new Week[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeValue(id);
		dest.writeString(start);
		dest.writeValue(sum);
		dest.writeValue(flexi);
	}
}
