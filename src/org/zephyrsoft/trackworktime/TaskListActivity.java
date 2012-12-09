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
package org.zephyrsoft.trackworktime;

import java.util.List;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.util.Logger;

/**
 * Activity for managing the tasks that the user can select. A task can be deleted if no reference to it exists, but it
 * can be deactivated even if references exist. Deactivated tasks are not shown in the dropdown list on the main screen.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class TaskListActivity extends ListActivity {
	
	private static final int NEW_TASK = 0;
	private static final int RENAME_TASK = 1;
	private static final int TOGGLE_ACTIVATION_STATE_OF_TASK = 2;
	private static final int DELETE_TASK = 3;
	
	private DAO dao = null;
	
	private List<Task> tasks = null;
	
	private WorkTimeTrackerActivity parentActivity = null;
	
	private ArrayAdapter<Task> tasksAdapter;
	
	@Override
	protected void onPause() {
		dao.close();
		super.onPause();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		parentActivity = WorkTimeTrackerActivity.getInstance();
		
		dao = Basics.getInstance().getDao();
		tasks = dao.getAllTasks();
		tasksAdapter = new ArrayAdapter<Task>(this, android.R.layout.simple_list_item_1, tasks);
		tasksAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		setListAdapter(tasksAdapter);
		
		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		
		registerForContextMenu(lv);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				openContextMenu(view);
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
						Logger.debug("inserted new task: " + newTask);
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
			default:
				Logger.warn("options menu: unknown item selected");
		}
		return false;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, NEW_TASK, 0, getString(R.string.new_task)).setIcon(android.R.drawable.ic_menu_add);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		menu.add(Menu.NONE, RENAME_TASK, 0, getString(R.string.rename_task)).setIcon(
			android.R.drawable.ic_menu_info_details);
		menu.add(Menu.NONE, TOGGLE_ACTIVATION_STATE_OF_TASK, 1, getString(R.string.toggle_activation_state_of_task))
			.setIcon(android.R.drawable.ic_menu_revert);
		menu.add(Menu.NONE, DELETE_TASK, 2, getString(R.string.delete_task)).setIcon(android.R.drawable.ic_menu_delete);
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		final int taskPosition = info.position;
		final Task oldTask = tasks.get(taskPosition);
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		switch (item.getItemId()) {
			case RENAME_TASK:
				alert.setTitle(getString(R.string.rename_task));
				alert.setMessage(getString(R.string.enter_new_task_name));
				
				// Set an EditText view to get user input
				final EditText input = new EditText(this);
				alert.setView(input);
				input.setText(oldTask.getName());
				
				alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();
						oldTask.setName(value);
						// update task in DB
						Task updatedTask = dao.updateTask(oldTask);
						Logger.debug("updated task with ID " + oldTask.getId() + " to have the new name: "
							+ updatedTask.getName());
						tasks.remove(taskPosition);
						tasks.add(taskPosition, updatedTask);
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
			case TOGGLE_ACTIVATION_STATE_OF_TASK:
				alert.setTitle(getString(R.string.toggle_activation_state_of_task));
				if (oldTask.getActive() == 0) {
					alert.setMessage(getString(R.string.really_enable_task));
				} else {
					alert.setMessage(getString(R.string.really_disable_task));
				}
				
				alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						if (oldTask.getActive() == 0) {
							oldTask.setActive(1);
						} else {
							oldTask.setActive(0);
						}
						// enable or disable task in DB
						Task updatedTask = dao.updateTask(oldTask);
						Logger.debug("updated task with ID " + oldTask.getId() + " to have the new active value: "
							+ updatedTask.getActive());
						tasks.remove(taskPosition);
						tasks.add(taskPosition, updatedTask);
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
			case DELETE_TASK:
				alert.setTitle(getString(R.string.delete_task));
				alert.setMessage(getString(R.string.really_delete_task));
				
				alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						// delete task in DB
						boolean success = dao.deleteTask(oldTask);
						if (success) {
							Logger.debug("deleted task with ID " + oldTask.getId() + " and name " + oldTask.getName());
							tasks.remove(taskPosition);
						} else {
							Logger.warn("could not delete task with ID " + oldTask.getId() + " and name "
								+ oldTask.getName());
						}
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
		return super.onContextItemSelected(item);
	}
}
