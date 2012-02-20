package org.zephyrsoft.worktimetracker;

import java.util.ArrayList;
import java.util.Collection;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import org.zephyrsoft.worktimetracker.database.DBAdapter;
import org.zephyrsoft.worktimetracker.database.DBUtil;
import org.zephyrsoft.worktimetracker.model.Task;

public class TaskListActivity extends ListActivity {
	
	private static final int NEW_TASK = 0;
	
	private DBAdapter dbAdapter = null;
	
	private Collection<Task> tasks = null;
	
	private final WorkTimeTrackerActivity parentActivity;
	
	public TaskListActivity() {
		this.parentActivity = WorkTimeTrackerActivity.getInstance();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		dbAdapter = new DBAdapter(this);
		dbAdapter.open();
		dbAdapter.beginTransaction();
		this.tasks = DBUtil.toTasks(dbAdapter.getAllTasks());
		
		setListAdapter(new TaskAdapter(this, R.layout.task_list_row, new ArrayList<Task>(tasks)));
		
		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO
				// When clicked, show a toast with the TextView text
				Task task = tasks.toArray(new Task[0])[position];
				Toast.makeText(getApplicationContext(), task.getName() + " ID=" + task.getId(), Toast.LENGTH_SHORT)
					.show();
			}
		});
		
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case NEW_TASK:
				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				alert.setTitle(getString(R.string.new_task));
				alert.setMessage(getString(R.string.enter_new_task_name));
				
				// Set an EditText view to get user input
				final EditText input = new EditText(this);
				alert.setView(input);
				
				alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();
						// create new task in DB
						dbAdapter.insertTask(value);
						Log.d(TaskListActivity.class.getName(), "inserted new task: " + value);
						return;
					}
				});
				alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// do nothing
					}
				});
				
				alert.show();
				
				return true;
		}
		return false;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, NEW_TASK, 0, getString(R.string.new_task)).setIcon(android.R.drawable.ic_menu_add);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	protected void onPause() {
		// commit data but do not close the database
		dbAdapter.commitTransaction();
		dbAdapter.close();
		parentActivity.refreshTasks();
		Log.d(TaskListActivity.class.getName(), "refreshed task list");
		super.onPause();
	}
	
}
