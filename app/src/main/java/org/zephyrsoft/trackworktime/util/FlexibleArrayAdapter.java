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
public class FlexibleArrayAdapter<T> extends ArrayAdapter<T> {

	private StringExtractionMethod<T> extractionMethod;
	private final int resource;
	private int dropDownResource;
	private final Context context;
	private final int fieldId;
	private final int separatorResource;
	private final SeparatorIdentificationMethod<T> separatorMethod;

	public FlexibleArrayAdapter(Context context, int resource, int textViewResourceId, List<T> objects,
		StringExtractionMethod<T> extractionMethod, int separatorResource,
		SeparatorIdentificationMethod<T> separatorMethod) {
		super(context, resource, textViewResourceId, objects);
		this.context = context;
		this.resource = resource;
		this.dropDownResource = resource;
		this.fieldId = textViewResourceId;
		this.extractionMethod = extractionMethod;
		this.separatorResource = separatorResource;
		this.separatorMethod = separatorMethod;
	}

	public FlexibleArrayAdapter(Context context, int resource, int textViewResourceId, T[] objects,
		StringExtractionMethod<T> extractionMethod, int separatorResource,
		SeparatorIdentificationMethod<T> separatorMethod) {
		super(context, resource, textViewResourceId, objects);
		this.context = context;
		this.resource = resource;
		this.dropDownResource = resource;
		this.fieldId = textViewResourceId;
		this.extractionMethod = extractionMethod;
		this.separatorResource = separatorResource;
		this.separatorMethod = separatorMethod;
	}

	public FlexibleArrayAdapter(Context context, int resource, int textViewResourceId,
		StringExtractionMethod<T> extractionMethod, int separatorResource,
		SeparatorIdentificationMethod<T> separatorMethod) {
		super(context, resource, textViewResourceId);
		this.context = context;
		this.resource = resource;
		this.dropDownResource = resource;
		this.fieldId = textViewResourceId;
		this.extractionMethod = extractionMethod;
		this.separatorResource = separatorResource;
		this.separatorMethod = separatorMethod;
	}

	public FlexibleArrayAdapter(Context context, int textViewResourceId, List<T> objects,
		StringExtractionMethod<T> extractionMethod, int separatorResource,
		SeparatorIdentificationMethod<T> separatorMethod) {
		super(context, textViewResourceId, objects);
		this.context = context;
		this.resource = textViewResourceId;
		this.dropDownResource = textViewResourceId;
		this.fieldId = 0;
		this.extractionMethod = extractionMethod;
		this.separatorResource = separatorResource;
		this.separatorMethod = separatorMethod;
	}

	public FlexibleArrayAdapter(Context context, int textViewResourceId, T[] objects,
		StringExtractionMethod<T> extractionMethod, int separatorResource,
		SeparatorIdentificationMethod<T> separatorMethod) {
		super(context, textViewResourceId, objects);
		this.context = context;
		this.resource = textViewResourceId;
		this.dropDownResource = textViewResourceId;
		this.fieldId = 0;
		this.extractionMethod = extractionMethod;
		this.separatorResource = separatorResource;
		this.separatorMethod = separatorMethod;
	}

	public FlexibleArrayAdapter(Context context, int textViewResourceId, StringExtractionMethod<T> extractionMethod,
		int separatorResource, SeparatorIdentificationMethod<T> separatorMethod) {
		super(context, textViewResourceId);
		this.context = context;
		this.resource = textViewResourceId;
		this.dropDownResource = textViewResourceId;
		this.fieldId = 0;
		this.extractionMethod = extractionMethod;
		this.separatorResource = separatorResource;
		this.separatorMethod = separatorMethod;
	}

	@Override
	public void setDropDownViewResource(int dropDownResourceToUse) {
		this.dropDownResource = dropDownResourceToUse;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		T item = getItem(position);
		return createView(item, parent, isSeparator(item) ? separatorResource : resource);
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		T item = getItem(position);
		return createView(item, parent, dropDownResource);
	}

	private View createView(T item, ViewGroup parent, int resourceToUse) {
		View view;
		TextView text;

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		view = inflater.inflate(resourceToUse, parent, false);

		try {
			if (isSeparator(item)) {
				text = (TextView) view;
				text.setText(separatorMethod.extractText(item));
			} else {
				if (fieldId == 0) {
					// If no custom field is assigned, assume the whole resource is a TextView
					text = (TextView) view;
				} else {
					// Otherwise, find the TextView field within the layout
					text = view.findViewById(fieldId);
				}
				text.setText(extractionMethod.extractText(item));
			}
		} catch (ClassCastException e) {
			Log.e("ArrayAdapter", "You must supply a resource ID for a TextView");
			throw new IllegalStateException("ArrayAdapter requires the resource ID to be a TextView", e);
		}

		return view;
	}

	private boolean isSeparator(T item) {
		return separatorMethod != null && separatorMethod.isSeparator(item);
	}
}
