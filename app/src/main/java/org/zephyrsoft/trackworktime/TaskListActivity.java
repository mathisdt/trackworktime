/*
 * This file is part of TrackWorkTime (TWT).
 *
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License 3.0 as published by
 * the Free Software Foundation.
 *
 * TWT is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License 3.0 for more details.
 *
 * You should have received a copy of the GNU General Public License 3.0
 * along with TWT. If not, see <http://www.gnu.org/licenses/>.
 */
package org.zephyrsoft.trackworktime;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.databinding.TasksActivityBinding;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.util.FlexibleArrayAdapter;
import org.zephyrsoft.trackworktime.util.SeparatorIdentificationMethod;

import java.util.List;

/**
 * Activity for managing the tasks that the user can select. A task can be deleted if no reference to it exists, but it
 * can be deactivated even if references exist. Deactivated tasks are not shown in the dropdown list on the main screen.
 */
public class TaskListActivity extends AppCompatActivity {

	private static final int NEW_TASK = 0;
	private static final int RENAME_TASK = 1;
	private static final int TOGGLE_DEFAULT = 2;
	private static final int TOGGLE_ACTIVATION_STATE_OF_TASK = 3;
	private static final int DELETE_TASK = 4;

	private DAO dao = null;

	private List<Task> tasks = null;

	private WorkTimeTrackerActivity parentActivity = null;

	private FlexibleArrayAdapter<Task> tasksAdapter;

	@Override
	protected void onPause() {
		dao.close();
		super.onPause();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		TasksActivityBinding binding = TasksActivityBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		ListView listView = binding.listView;

		parentActivity = WorkTimeTrackerActivity.getInstanceOrNull();

		dao = Basics.get(this).getDao();
		tasks = dao.getAllTasks();
		tasksAdapter = new FlexibleArrayAdapter<>(this,
            android.R.layout.simple_list_item_1, 0, tasks,
            Task::getName, R.layout.list_item_inactive, new SeparatorIdentificationMethod<>() {
            @Override
            public boolean isSeparator(Task task) {
                return !task.isActive();
            }
            @Override
            public String extractText(Task task) {
                return task.getName() + " (" + getString(R.string.inactive) + ")";
            }
        });
		tasksAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		listView.setAdapter(tasksAdapter);

		listView.setTextFilterEnabled(true);

		registerForContextMenu(listView);
		listView.setOnItemClickListener((parent, view, position, id) -> openContextMenu(view));
	}

	private void refreshTasksOnParent() {
		if (parentActivity != null) {
			parentActivity.refreshTasks();
		}
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

				alert.setPositiveButton(getString(R.string.ok), (dialog, whichButton) -> {
                    String value = input.getText().toString();
                    // create new task in DB
                    Task newTask = dao.insertTask(new Task(null, value, 1, 0, 0));
                    Logger.debug("inserted new task: {}", newTask);
                    tasks.add(newTask);
                    tasksAdapter.notifyDataSetChanged();
                    refreshTasksOnParent();
                });
				alert.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                    // do nothing
                });

				alert.show();

				return true;
			case android.R.id.home:
				finish();
				return true;
			default:
				Logger.warn("options menu: unknown item selected");
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, NEW_TASK, NEW_TASK, getString(R.string.new_task))
			.setIcon(R.drawable.ic_menu_add);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		menu.setHeaderTitle(R.string.availableActions);
		menu.add(Menu.NONE, RENAME_TASK, RENAME_TASK,
			getString(R.string.rename_task)).setIcon(R.drawable.ic_menu_info_details);
		menu.add(Menu.NONE, TOGGLE_DEFAULT, TOGGLE_DEFAULT,
			getString(R.string.make_default_task)).setIcon(R.drawable.ic_menu_revert);
		menu.add(Menu.NONE, TOGGLE_ACTIVATION_STATE_OF_TASK,
			TOGGLE_ACTIVATION_STATE_OF_TASK, getString(R.string.toggle_activation_state_of_task))
			.setIcon(R.drawable.ic_menu_revert);
		menu.add(Menu.NONE, DELETE_TASK, DELETE_TASK,
			getString(R.string.delete_task)).setIcon(R.drawable.ic_menu_delete);
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

				alert.setPositiveButton(getString(R.string.ok), (dialog, whichButton) -> {
                    String value = input.getText().toString();
                    oldTask.setName(value);
                    // update task in DB
                    Task updatedTask = dao.updateTask(oldTask);
                    Logger.debug("updated task with ID {} to have the new name: {}", oldTask.getId(), updatedTask
                        .getName());
                    tasks.remove(taskPosition);
                    tasks.add(taskPosition, updatedTask);
                    tasksAdapter.notifyDataSetChanged();
                    refreshTasksOnParent();
                });
				alert.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                    // do nothing
                });

				alert.show();

				return true;
			case TOGGLE_DEFAULT:
				Task previousDefault = null;
				int previousDefaultPosition = -1;
				if (oldTask.getIsDefault().equals(0)) {
					// scan for previous default
					int i = 0;
					for (Task task : tasks) {
						if (task.getIsDefault().equals(1)) {
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
					Logger.debug("updated task with ID {} to have the new isDefault value: {}",
						previousDefault.getId(), updatedPreviousDefault.getIsDefault());
					tasks.remove(previousDefaultPosition);
					tasks.add(previousDefaultPosition, updatedPreviousDefault);
				}
				Task toggledTask = dao.updateTask(oldTask);
				Logger.debug("updated task with ID {} to have the new isDefault value: {}", oldTask.getId(),
					toggledTask.getIsDefault());
				tasks.remove(taskPosition);
				tasks.add(taskPosition, toggledTask);
				tasksAdapter.notifyDataSetChanged();
				refreshTasksOnParent();

				return true;
			case TOGGLE_ACTIVATION_STATE_OF_TASK:
				alert.setTitle(getString(R.string.toggle_activation_state_of_task));
				if (oldTask.getActive().equals(0)) {
					alert.setMessage(getString(R.string.really_enable_task));
				} else {
					alert.setMessage(getString(R.string.really_disable_task));
				}

				alert.setPositiveButton(getString(R.string.ok), (dialog, whichButton) -> {
                    if (oldTask.getActive() == 0) {
                        oldTask.setActive(1);
                    } else {
                        oldTask.setActive(0);
                    }
                    // enable or disable task in DB
                    Task updatedTask = dao.updateTask(oldTask);
                    Logger.debug("updated task with ID {} to have the new active value: {}", oldTask.getId(),
                        updatedTask.getActive());
                    tasks.remove(taskPosition);
                    tasks.add(taskPosition, updatedTask);
                    tasksAdapter.notifyDataSetChanged();
                    refreshTasksOnParent();
                });
				alert.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                    // do nothing
                });

				alert.show();

				return true;
			case DELETE_TASK:
				if (dao.isTaskUsed(oldTask.getId())) {
					// can't delete, task is used in events
					alert.setCancelable(false);
					alert.setTitle(getString(R.string.delete_task));
					alert.setMessage(getString(R.string.cannot_delete_task_used));
					alert.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
						// do nothing
					});
				} else if (tasks.size() == 1) {
					// can't delete, task is last one
					alert.setCancelable(false);
					alert.setTitle(getString(R.string.delete_task));
					alert.setMessage(getString(R.string.cannot_delete_task_last_one));
					alert.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
						// do nothing
					});
				} else {
					// delete task after confirmation
					alert.setTitle(getString(R.string.delete_task));
					alert.setMessage(getString(R.string.really_delete_task));

					alert.setPositiveButton(getString(R.string.ok), (dialog, whichButton) -> {
                        // delete task in DB
                        boolean success = dao.deleteTask(oldTask);
                        if (success) {
                            Logger.debug("deleted task with ID {} and name {}", oldTask.getId(), oldTask
                                .getName());
                            tasks.remove(taskPosition);
                        } else {
                            Logger.warn("could not delete task with ID {} and name {}", oldTask.getId(), oldTask
                                .getName());
                        }
                        tasksAdapter.notifyDataSetChanged();
                        refreshTasksOnParent();
                    });
					alert.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                        // do nothing
                    });
				}
				alert.show();

				return true;
			default:
				Logger.warn("context menu: unknown item selected");
		}
		return super.onContextItemSelected(item);
	}
}
