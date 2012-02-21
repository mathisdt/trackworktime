package org.zephyrsoft.trackworktime;

import java.util.List;
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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Task;

public class TaskListActivity extends ListActivity {
	
	private static final int NEW_TASK = 0;
	
	private DAO dao = null;
	
	private List<Task> tasks = null;
	
	private WorkTimeTrackerActivity parentActivity = null;
	
	private ArrayAdapter<Task> tasksAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		parentActivity = WorkTimeTrackerActivity.getInstance();
		
		dao = new DAO(this);
		dao.open();
		tasks = dao.getAllTasks();
		tasksAdapter = new ArrayAdapter<Task>(this, android.R.layout.simple_list_item_1, tasks);
		tasksAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		setListAdapter(tasksAdapter);
		
		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO
				// When clicked, show a toast with the TextView text
				Task task = tasks.get(position);
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
						Task newTask = dao.insertTask(new Task(null, value, 1, 0));
						Log.d(TaskListActivity.class.getName(), "inserted new task: " + newTask);
						tasks.add(newTask);
						tasksAdapter.notifyDataSetChanged();
						parentActivity.refreshTasks();
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
		dao.close();
		Log.d(TaskListActivity.class.getName(), "refreshed task list");
		super.onPause();
	}
	
}
