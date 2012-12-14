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
package org.zephyrsoft.trackworktime.util;

import java.util.List;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * An {@link ArrayAdapter} which lets you choose a way to get the text from the object to display.
 * 
 * @author Mathis Dirksen-Thedens
 */
@SuppressWarnings("javadoc")
public class FlexibleArrayAdapter<T> extends ArrayAdapter<T> {
	
	private StringExtractionMethod<T> extractionMethod;
	private final int resource;
	private final Context context;
	private final int fieldId;
	
	public FlexibleArrayAdapter(Context context, int resource, int textViewResourceId, List<T> objects,
		StringExtractionMethod<T> extractionMethod) {
		super(context, resource, textViewResourceId, objects);
		this.context = context;
		this.resource = resource;
		this.fieldId = textViewResourceId;
		this.extractionMethod = extractionMethod;
	}
	
	public FlexibleArrayAdapter(Context context, int resource, int textViewResourceId, T[] objects,
		StringExtractionMethod<T> extractionMethod) {
		super(context, resource, textViewResourceId, objects);
		this.context = context;
		this.resource = resource;
		this.fieldId = textViewResourceId;
		this.extractionMethod = extractionMethod;
	}
	
	public FlexibleArrayAdapter(Context context, int resource, int textViewResourceId,
		StringExtractionMethod<T> extractionMethod) {
		super(context, resource, textViewResourceId);
		this.context = context;
		this.resource = resource;
		this.fieldId = textViewResourceId;
		this.extractionMethod = extractionMethod;
	}
	
	public FlexibleArrayAdapter(Context context, int textViewResourceId, List<T> objects,
		StringExtractionMethod<T> extractionMethod) {
		super(context, textViewResourceId, objects);
		this.context = context;
		this.resource = textViewResourceId;
		this.fieldId = 0;
		this.extractionMethod = extractionMethod;
	}
	
	public FlexibleArrayAdapter(Context context, int textViewResourceId, T[] objects,
		StringExtractionMethod<T> extractionMethod) {
		super(context, textViewResourceId, objects);
		this.context = context;
		this.resource = textViewResourceId;
		this.fieldId = 0;
		this.extractionMethod = extractionMethod;
	}
	
	public FlexibleArrayAdapter(Context context, int textViewResourceId, StringExtractionMethod<T> extractionMethod) {
		super(context, textViewResourceId);
		this.context = context;
		this.resource = textViewResourceId;
		this.fieldId = 0;
		this.extractionMethod = extractionMethod;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		TextView text;
		
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(resource, parent, false);
		} else {
			view = convertView;
		}
		
		try {
			if (fieldId == 0) {
				// If no custom field is assigned, assume the whole resource is a TextView
				text = (TextView) view;
			} else {
				// Otherwise, find the TextView field within the layout
				text = (TextView) view.findViewById(fieldId);
			}
		} catch (ClassCastException e) {
			Log.e("ArrayAdapter", "You must supply a resource ID for a TextView");
			throw new IllegalStateException("ArrayAdapter requires the resource ID to be a TextView", e);
		}
		
		T item = getItem(position);
		text.setText(extractionMethod.extractText(item));
		
		return view;
	}
	
}
