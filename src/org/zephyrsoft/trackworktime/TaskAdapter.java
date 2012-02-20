package org.zephyrsoft.trackworktime;

import java.util.ArrayList;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import org.zephyrsoft.trackworktime.model.Task;

public class TaskAdapter extends ArrayAdapter<Task> {
	
	private ArrayList<Task> dataItems;
	
	private Activity context;
	
	public TaskAdapter(Activity context, int textViewResourceId, ArrayList<Task> dataItems) {
		super(context, textViewResourceId, dataItems);
		this.context = context;
		this.dataItems = dataItems;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		View view = convertView;
		if (view == null) {
			LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = vi.inflate(R.layout.task_list_row, null);
		}
		
		Task taskItem = dataItems.get(position);
		
		if (taskItem != null) {
			TextView nameTextView = (TextView) view.findViewById(R.id.taskName);
			nameTextView.setText(taskItem.getName());
		}
		
		return view;
		
	}
}
