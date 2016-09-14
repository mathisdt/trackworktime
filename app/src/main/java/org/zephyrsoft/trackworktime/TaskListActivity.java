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

	private static enum MenuAction {
		NEW_TASK,
		RENAME_TASK,
		TOGGLE_DEFAULT,
		TOGGLE_ACTIVATION_STATE_OF_TASK,
		DELETE_TASK;

		public static MenuAction byOrdinal(int ordinal) {
			return values()[ordinal];
		}
	}

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

		parentActivity = WorkTimeTrackerActivity.getInstanceOrNull();

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

	private void refreshTasksOnParent() {
		if (parentActivity != null) {
			parentActivity.refreshTasks();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (MenuAction.byOrdinal(item.getItemId())) {
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
						Task newTask = dao.insertTask(new Task(null, value, 1, 0, 0));
						Logger.debug("inserted new task: {0}", newTask);
						tasks.add(newTask);
						tasksAdapter.notifyDataSetChanged();
						refreshTasksOnParent();
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
		menu.add(Menu.NONE, MenuAction.NEW_TASK.ordinal(), MenuAction.NEW_TASK.ordinal(), getString(R.string.new_task))
			.setIcon(R.drawable.ic_menu_add);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		menu.setHeaderTitle(R.string.availableActions);
		menu.add(Menu.NONE, MenuAction.RENAME_TASK.ordinal(), MenuAction.RENAME_TASK.ordinal(),
			getString(R.string.rename_task)).setIcon(R.drawable.ic_menu_info_details);
		menu.add(Menu.NONE, MenuAction.TOGGLE_DEFAULT.ordinal(), MenuAction.TOGGLE_DEFAULT.ordinal(),
			getString(R.string.toggle_default)).setIcon(R.drawable.ic_menu_revert);
		menu.add(Menu.NONE, MenuAction.TOGGLE_ACTIVATION_STATE_OF_TASK.ordinal(),
			MenuAction.TOGGLE_ACTIVATION_STATE_OF_TASK.ordinal(), getString(R.string.toggle_activation_state_of_task))
			.setIcon(R.drawable.ic_menu_revert);
		menu.add(Menu.NONE, MenuAction.DELETE_TASK.ordinal(), MenuAction.DELETE_TASK.ordinal(),
			getString(R.string.delete_task)).setIcon(R.drawable.ic_menu_delete);
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		final int taskPosition = info.position;
		final Task oldTask = tasks.get(taskPosition);
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		switch (MenuAction.byOrdinal(item.getItemId())) {
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
						Logger.debug("updated task with ID {0} to have the new name: {1}", oldTask.getId(), updatedTask
							.getName());
						tasks.remove(taskPosition);
						tasks.add(taskPosition, updatedTask);
						tasksAdapter.notifyDataSetChanged();
						refreshTasksOnParent();
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
			case TOGGLE_DEFAULT:
				Task previousDefault = null;
				int previousDefaultPosition = -1;
				if (oldTask.getIsDefault().equals(Integer.valueOf(0))) {
					// scan for previous default
					int i = 0;
					for (Task task : tasks) {
						if (task.getIsDefault().equals(Integer.valueOf(1))) {
							previousDefault = task;
							previousDefaultPosition = i;
							previousDefault.setIsDefault(0);
							break;
						}
						i++;
					}
					oldTask.setIsDefault(1);
				} else {
					oldTask.setIsDefault(0);
				}
				// toggle default in DB
				if (previousDefault != null) {
					// unset previous default
					Task updatedPreviousDefault = dao.updateTask(previousDefault);
					Logger.debug("updated task with ID {0} to have the new isDefault value: {1}",
						previousDefault.getId(), updatedPreviousDefault.getIsDefault());
					tasks.remove(previousDefaultPosition);
					tasks.add(previousDefaultPosition, updatedPreviousDefault);
				}
				Task toggledTask = dao.updateTask(oldTask);
				Logger.debug("updated task with ID {0} to have the new isDefault value: {1}", oldTask.getId(),
					toggledTask.getIsDefault());
				tasks.remove(taskPosition);
				tasks.add(taskPosition, toggledTask);
				tasksAdapter.notifyDataSetChanged();
				refreshTasksOnParent();

				return true;
			case TOGGLE_ACTIVATION_STATE_OF_TASK:
				alert.setTitle(getString(R.string.toggle_activation_state_of_task));
				if (oldTask.getActive().equals(Integer.valueOf(0))) {
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
						Logger.debug("updated task with ID {0} to have the new active value: {1}", oldTask.getId(),
							updatedTask.getActive());
						tasks.remove(taskPosition);
						tasks.add(taskPosition, updatedTask);
						tasksAdapter.notifyDataSetChanged();
						refreshTasksOnParent();
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
				if (dao.isTaskUsed(oldTask.getId())) {
					// can't delete, task is used in events
					alert.setCancelable(false);
					alert.setTitle(getString(R.string.delete_task));
					alert.setMessage(getString(R.string.cannot_delete_task));
					alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// do nothing
						}
					});

					alert.show();
				} else {
					// delete task after confirmation
					alert.setTitle(getString(R.string.delete_task));
					alert.setMessage(getString(R.string.really_delete_task));

					alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int whichButton) {
							// delete task in DB
							boolean success = dao.deleteTask(oldTask);
							if (success) {
								Logger.debug("deleted task with ID {0} and name {1}", oldTask.getId(), oldTask
									.getName());
								tasks.remove(taskPosition);
							} else {
								Logger.warn("could not delete task with ID {0} and name {1}", oldTask.getId(), oldTask
									.getName());
							}
							tasksAdapter.notifyDataSetChanged();
							refreshTasksOnParent();
						}
					});
					alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// do nothing
						}
					});

					alert.show();
				}

				return true;
			default:
				Logger.warn("context menu: unknown item selected");
		}
		return super.onContextItemSelected(item);
	}
}
