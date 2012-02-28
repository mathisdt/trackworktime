package org.zephyrsoft.trackworktime;

import java.util.List;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.timer.TimerManager;

public class WorkTimeTrackerActivity extends Activity {
	
	private static final int EDIT_TASKS = 0;
	private static final int OPTIONS = 1;
	
	private TextView inLabel = null;
	private TextView outLabel = null;
	private TextView workedLabel = null;
	private TextView flexiLabel = null;
	private TextView mondayLabel = null;
	private TextView mondayIn = null;
	private TextView mondayOut = null;
	private TextView mondayWorked = null;
	private TextView mondayFlexi = null;
	private TextView tuesdayLabel = null;
	private TextView tuesdayIn = null;
	private TextView tuesdayOut = null;
	private TextView tuesdayWorked = null;
	private TextView tuesdayFlexi = null;
	private TextView wednesdayLabel = null;
	private TextView wednesdayIn = null;
	private TextView wednesdayOut = null;
	private TextView wednesdayWorked = null;
	private TextView wednesdayFlexi = null;
	private TextView thursdayLabel = null;
	private TextView thursdayIn = null;
	private TextView thursdayOut = null;
	private TextView thursdayWorked = null;
	private TextView thursdayFlexi = null;
	private TextView fridayLabel = null;
	private TextView fridayIn = null;
	private TextView fridayOut = null;
	private TextView fridayWorked = null;
	private TextView fridayFlexi = null;
	private TextView saturdayLabel = null;
	private TextView saturdayIn = null;
	private TextView saturdayOut = null;
	private TextView saturdayWorked = null;
	private TextView saturdayFlexi = null;
	private TextView sundayLabel = null;
	private TextView sundayIn = null;
	private TextView sundayOut = null;
	private TextView sundayWorked = null;
	private TextView sundayFlexi = null;
	private TextView totalLabel = null;
	private TextView totalIn = null;
	private TextView totalOut = null;
	private TextView totalWorked = null;
	private TextView totalFlexi = null;
	private TextView taskLabel = null;
	private Spinner task = null;
	private TextView textLabel = null;
	private EditText text = null;
	private Button clockInOutButton = null;
	
	private OnClickListener clockInOut = new OnClickListener() {
		@Override
		public void onClick(View v) {
			
			if (timerManager.isTracking() && !taskOrTextChanged) {
				timerManager.stopTracking();
			} else {
				Task selectedTask = (Task) task.getSelectedItem();
				String description = text.getText().toString();
				if (timerManager.isTracking() && taskOrTextChanged) {
					timerManager.startTracking(selectedTask, description);
				} else if (!timerManager.isTracking()) {
					timerManager.startTracking(selectedTask, description);
				}
			}
			
			taskOrTextChanged = false;
			refreshView();
		}
	};
	private TaskAndTextListener taskAndTextListener = new TaskAndTextListener();
	
	private static WorkTimeTrackerActivity instance = null;
	
	private DAO dao = null;
	private TimerManager timerManager = null;
	private ArrayAdapter<Task> tasksAdapter;
	private boolean reloadTasksOnResume = false;
	private SharedPreferences preferences;
	private List<Task> tasks;
	private boolean taskOrTextChanged = false;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		instance = this;
		
		readPreferences();
		
		setContentView(R.layout.main);
		
		findAllViewsById();
		
		clockInOutButton.setOnClickListener(clockInOut);
		
		// make the clock-in/clock-out button change its title when changing task and/or text
		task.setOnItemSelectedListener(taskAndTextListener);
		text.setOnKeyListener(taskAndTextListener);
		
		dao = new DAO(this);
		dao.open();
		timerManager = new TimerManager(dao);
		
		setupTasksAdapter();
		
		refreshView();
	}
	
	private void readPreferences() {
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		// TODO
	}
	
	protected void refreshView() {
		if (timerManager.isTracking() && !taskOrTextChanged) {
			clockInOutButton.setText(R.string.clockOut);
		} else if (timerManager.isTracking() && taskOrTextChanged) {
			clockInOutButton.setText(R.string.clockInChange);
		} else if (!timerManager.isTracking()) {
			clockInOutButton.setText(R.string.clockIn);
		}
		// TODO update times
	}
	
	private void setupTasksAdapter() {
		tasks = dao.getActiveTasks();
		tasksAdapter = new ArrayAdapter<Task>(this, android.R.layout.simple_list_item_1, tasks);
		tasksAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		task.setAdapter(tasksAdapter);
	}
	
	private void findAllViewsById() {
		inLabel = (TextView) findViewById(R.id.inLabel);
		outLabel = (TextView) findViewById(R.id.outLabel);
		workedLabel = (TextView) findViewById(R.id.workedLabel);
		flexiLabel = (TextView) findViewById(R.id.flexiLabel);
		mondayLabel = (TextView) findViewById(R.id.mondayLabel);
		mondayIn = (TextView) findViewById(R.id.mondayIn);
		mondayOut = (TextView) findViewById(R.id.mondayOut);
		mondayWorked = (TextView) findViewById(R.id.mondayWorked);
		mondayFlexi = (TextView) findViewById(R.id.mondayFlexi);
		tuesdayLabel = (TextView) findViewById(R.id.tuesdayLabel);
		tuesdayIn = (TextView) findViewById(R.id.tuesdayIn);
		tuesdayOut = (TextView) findViewById(R.id.tuesdayOut);
		tuesdayWorked = (TextView) findViewById(R.id.tuesdayWorked);
		tuesdayFlexi = (TextView) findViewById(R.id.tuesdayFlexi);
		wednesdayLabel = (TextView) findViewById(R.id.wednesdayLabel);
		wednesdayIn = (TextView) findViewById(R.id.wednesdayIn);
		wednesdayOut = (TextView) findViewById(R.id.wednesdayOut);
		wednesdayWorked = (TextView) findViewById(R.id.wednesdayWorked);
		wednesdayFlexi = (TextView) findViewById(R.id.wednesdayFlexi);
		thursdayLabel = (TextView) findViewById(R.id.thursdayLabel);
		thursdayIn = (TextView) findViewById(R.id.thursdayIn);
		thursdayOut = (TextView) findViewById(R.id.thursdayOut);
		thursdayWorked = (TextView) findViewById(R.id.thursdayWorked);
		thursdayFlexi = (TextView) findViewById(R.id.thursdayFlexi);
		fridayLabel = (TextView) findViewById(R.id.fridayLabel);
		fridayIn = (TextView) findViewById(R.id.fridayIn);
		fridayOut = (TextView) findViewById(R.id.fridayOut);
		fridayWorked = (TextView) findViewById(R.id.fridayWorked);
		fridayFlexi = (TextView) findViewById(R.id.fridayFlexi);
		saturdayLabel = (TextView) findViewById(R.id.saturdayLabel);
		saturdayIn = (TextView) findViewById(R.id.saturdayIn);
		saturdayOut = (TextView) findViewById(R.id.saturdayOut);
		saturdayWorked = (TextView) findViewById(R.id.saturdayWorked);
		saturdayFlexi = (TextView) findViewById(R.id.saturdayFlexi);
		sundayLabel = (TextView) findViewById(R.id.sundayLabel);
		sundayIn = (TextView) findViewById(R.id.sundayIn);
		sundayOut = (TextView) findViewById(R.id.sundayOut);
		sundayWorked = (TextView) findViewById(R.id.sundayWorked);
		sundayFlexi = (TextView) findViewById(R.id.sundayFlexi);
		totalLabel = (TextView) findViewById(R.id.totalLabel);
		totalIn = (TextView) findViewById(R.id.totalIn);
		totalOut = (TextView) findViewById(R.id.totalOut);
		totalWorked = (TextView) findViewById(R.id.totalWorked);
		totalFlexi = (TextView) findViewById(R.id.totalFlexi);
		taskLabel = (TextView) findViewById(R.id.taskLabel);
		task = (Spinner) findViewById(R.id.task);
		textLabel = (TextView) findViewById(R.id.textLabel);
		text = (EditText) findViewById(R.id.text);
		clockInOutButton = (Button) findViewById(R.id.clockInOutButton);
	}
	
	public void refreshTasks() {
		reloadTasksOnResume = true;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, EDIT_TASKS, 0, getString(R.string.edit_tasks)).setIcon(android.R.drawable.ic_menu_edit);
		menu.add(Menu.NONE, OPTIONS, 1, getString(R.string.options)).setIcon(android.R.drawable.ic_menu_preferences);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case EDIT_TASKS:
				showTaskList();
				return true;
			case OPTIONS:
				showOptions();
				return true;
			default:
				Log.w(getClass().getName(), "options menu: unknown item selected");
		}
		return false;
	}
	
	private void showTaskList() {
		Log.d(getClass().getName(), "showing TaskList");
		Intent i = new Intent(this, TaskListActivity.class);
		startActivity(i);
	}
	
	private void showOptions() {
		Log.d(getClass().getName(), "showing Options");
		Intent i = new Intent(this, OptionsActivity.class);
		startActivity(i);
	}
	
	@Override
	protected void onResume() {
		initDaoAndPrefs();
		refreshView();
		super.onResume();
	}
	
	private void initDaoAndPrefs() {
		dao.open();
		if (reloadTasksOnResume) {
			reloadTasksOnResume = false;
			setupTasksAdapter();
		}
		readPreferences();
	}
	
	@Override
	protected void onPause() {
		dao.close();
		super.onPause();
	}
	
	public static WorkTimeTrackerActivity getInstance() {
		return instance;
	}
	
	private class TaskAndTextListener implements OnItemSelectedListener, OnKeyListener {
		
		private void valueChanged() {
			taskOrTextChanged = true;
			refreshView();
		}
		
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			valueChanged();
			return false;
		}
		
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			valueChanged();
		}
		
		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			valueChanged();
		}
		
	}
	
}
