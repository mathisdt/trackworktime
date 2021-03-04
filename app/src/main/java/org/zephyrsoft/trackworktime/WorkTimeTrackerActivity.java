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

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.navigation.NavigationView;

import org.pmw.tinylog.Logger;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;
import org.zephyrsoft.trackworktime.backup.BackupFileInfo;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.databinding.ActivityMainBinding;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.options.Key;
import org.zephyrsoft.trackworktime.timer.TimeCalculator;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.BackupUtil;
import org.zephyrsoft.trackworktime.util.ExternalNotificationManager;
import org.zephyrsoft.trackworktime.util.PermissionsUtil;
import org.zephyrsoft.trackworktime.util.PreferencesUtil;
import org.zephyrsoft.trackworktime.weektimes.WeekAdapter;
import org.zephyrsoft.trackworktime.weektimes.WeekIndexConverter;
import org.zephyrsoft.trackworktime.weektimes.WeekStateCalculatorFactory;
import org.zephyrsoft.trackworktime.weektimes.WeekStateLoaderFactory;
import org.zephyrsoft.trackworktime.weektimes.WeekStateLoaderManager;
import org.zephyrsoft.trackworktime.weektimes.WeekTimesView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import static java.lang.Math.abs;

/**
 * Main activity of the application.
 *
 * @author Mathis Dirksen-Thedens
 */
public class WorkTimeTrackerActivity extends AppCompatActivity
		implements NavigationView.OnNavigationItemSelectedListener {

	private static final String KEY_CURRENT_WEEK = "current_week";

	private enum MenuAction {
		RECENTER_WEEK, RAISE_EXCEPTION, SHOW_DEBUG;

		public static MenuAction byOrdinal(int ordinal) {
			return values()[ordinal];
		}
	}

	private static WorkTimeTrackerActivity instance = null;

	private ActivityMainBinding binding;

	private ActionBarDrawerToggle toggle;
	private MenuItem recenterMenuItem;

	private boolean visible = false;

	private SharedPreferences preferences;
	private DAO dao = null;
	private TimerManager timerManager = null;
	private ExternalNotificationManager externalNotificationManager = null;
	private ArrayAdapter<Task> tasksAdapter;
	private boolean reloadTasksOnResume = false;
	private List<Task> tasks;
	
	private WeekAdapter weekAdapter;

	private void checkAllOptions() {
		int disabledSections = PreferencesUtil.checkAllPreferenceSections();

		if (disabledSections > 0) {
			// show message to user
			Intent messageIntent = Basics
				.getInstance()
				.createMessageIntent(
					disabledSections == 1
						? "One option was disabled due to invalid values or value combinations.\n\nYou can re-enable it after you checked the values you entered."
						: disabledSections
							+ " options were disabled due to invalid values or value combinations.\n\nYou can re-enable them after you checked the values you entered.",
					null);
			startActivity(messageIntent);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		instance = this;
		Basics basics = Basics.getOrCreateInstance(getApplicationContext());
		// fill basic data from central structures
		preferences = basics.getPreferences();
		dao = basics.getDao();
		timerManager = basics.getTimerManager();
		externalNotificationManager = basics.getExternalNotificationManager();

		binding = ActivityMainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		// set toolbar as action bar
		setSupportActionBar(binding.toolbar);

		// configure navigation drawer
		DrawerLayout drawer = binding.drawer;
		toggle = new ActionBarDrawerToggle(this, drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawer.addDrawerListener(toggle);

		// make entry visible
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			binding.navView.getMenu().findItem(R.id.nav_ignore_battery_optimizations).setVisible(true);
		}

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setHomeButtonEnabled(true);
		}

		binding.navView.setNavigationItemSelectedListener(this);

		// bind click listener for navigation buttons (may not be visible)
		binding.main.today.setOnClickListener(v -> recenterWeek());
		binding.main.previous.setOnClickListener(v -> changeDisplayedWeek(-1));
		binding.main.next.setOnClickListener(v -> changeDisplayedWeek(1));


		initWeekPager(savedInstanceState);

		Button clockInButton = binding.main.clockInButton;
		clockInButton.setOnClickListener(v -> clockInAction(0));
		clockInButton.setOnLongClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), TimeAheadActivity.class);
            String typeString;
            if (timerManager.isTracking()) {
                typeString = getString(R.string.clockInChange);
            } else {
                typeString = getString(R.string.clockIn);
            }
            i.putExtra(Constants.TYPE_EXTRA_KEY, 0);
            i.putExtra(Constants.TYPE_STRING_EXTRA_KEY, typeString);
            startActivity(i);
            return true;
        });

		Button clockOutButton = binding.main.clockOutButton;
		clockOutButton.setOnClickListener(v -> clockOutAction(0));
		clockOutButton.setOnLongClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), TimeAheadActivity.class);
            String typeString;
            if (timerManager.isTracking()) {
                typeString = getString(R.string.clockOut);
                i.putExtra(Constants.TYPE_EXTRA_KEY, 1);
                i.putExtra(Constants.TYPE_STRING_EXTRA_KEY, typeString);
                startActivity(i);
            }
            return true;
        });

		// delegate the rest of the work to onResume()
		reloadTasksOnResume = true;

		// check options for logical errors
		checkAllOptions();

		if (!preferences.getBoolean(getString(R.string.keyBackupSettingAsked), false)) {
			DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                final Editor editor = preferences.edit();
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        editor.putBoolean(getText(R.string.keyBackupEnabled) + "", true);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        editor.putBoolean(getText(R.string.keyBackupEnabled) + "", false);
                        break;
                }
                editor.putBoolean(getString(R.string.keyBackupSettingAsked), true);
                editor.commit();
            };
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.backup_on_google_servers)
				.setPositiveButton(R.string.yes, dialogClickListener)
				.setNegativeButton(R.string.no, dialogClickListener).show();
		}
	}

	private void initWeekPager(@Nullable Bundle state) {
		ViewPager2 weekPager = binding.main.week;

		weekPager.setOffscreenPageLimit(1);
		initWeekPagerAdapter();
		initWeekPagerAnimation();
		initWeekPagerPosition(state);

		weekPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
			@Override
			public void onPageSelected(int selectedWeekIndex) {
				super.onPageSelected(selectedWeekIndex);
				refreshRecenterMenuItem();
			}
		});
	}

	private void initWeekPagerAdapter() {
		WeekStateLoaderManager weekStateLoaderManager = createWeekLoaderManger();
		View.OnClickListener weekClickListener = v -> showEventList();
		WeekTimesView.OnDayClickListener dayClickListener = (v, day) -> setTarget(day);

		weekAdapter = new WeekAdapter(weekStateLoaderManager, dayClickListener, weekClickListener);
		binding.main.week.setAdapter(weekAdapter);
	}

	private WeekStateLoaderManager createWeekLoaderManger() {
		WeekStateLoaderFactory weekStateLoaderFactory = createWeekLoaderFactory();
		return new WeekStateLoaderManager(weekStateLoaderFactory);
	}

	private WeekStateLoaderFactory createWeekLoaderFactory() {
		TimeCalculator timeCalculator = Basics.getInstance().getTimeCalculator();
		WeekStateCalculatorFactory weekStateCalculatorFactory = new WeekStateCalculatorFactory(
				this, dao, timerManager, timeCalculator, preferences);
		return new WeekStateLoaderFactory(weekStateCalculatorFactory);
	}

	private void initWeekPagerAnimation() {
		binding.main.week.setPageTransformer((view, position) -> {
			if(position >= 1 || position < -1) {
				return;
			}
			if (position >= 0) {
				view.setAlpha(1);
				view.setTranslationX(0);
				view.setScaleX(1);
				view.setScaleY(1);
			} else if (position >= -1) {
				view.setAlpha(1 + position);
				view.setPivotY(0.5f * view.getHeight());
				view.setTranslationX(view.getWidth() * -position);
				final float MIN_SCALE = 0.95f;
				float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position));
				view.setScaleX(scaleFactor);
				view.setScaleY(scaleFactor);
			}
		});
	}

	private void initWeekPagerPosition(@Nullable Bundle state) {
		int restoredWeekIndex = (state != null) ? state.getInt(KEY_CURRENT_WEEK) : 0;
		int initialWeek = (restoredWeekIndex == 0) ? getTodaysWeekIndex() : restoredWeekIndex;

		showWeek(initialWeek, false);
	}

	private void recenterWeek() {
		showWeek(getTodaysWeekIndex(), true);
	}

	private int getTodaysWeekIndex() {
		return WeekIndexConverter.getIndexForDate(LocalDate.now(timerManager.getHomeTimeZone()));
	}

	private void showWeek(int weekIndex, boolean animate) {
		binding.main.week.setCurrentItem(weekIndex, animate);
	}

	@Override
	public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
		super.onSaveInstanceState(outState, outPersistentState);

		outState.putInt(KEY_CURRENT_WEEK, getCurrentWeekIndex());
	}

	@Override
	protected void onStart() {
		super.onStart();

		// request location permissions if necessary
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs.getBoolean(Key.LOCATION_BASED_TRACKING_ENABLED.getName(), false)
			|| prefs.getBoolean(Key.WIFI_BASED_TRACKING_ENABLED.getName(), false)) {
				requestMissingPermissionsForTracking();
		}

		// request file permission if necessary
		if (PermissionsUtil.missingPermissionForExternalStorage(this)) {
			PermissionsUtil.askForStoragePermission(this,
				() -> ActivityCompat.requestPermissions(this,
					new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
					Constants.MISSING_PRIVILEGE_ACCESS_STORAGE_ID));
		}
	}

	private void requestMissingPermissionsForTracking() {
		List<String> missingPermissions = PermissionsUtil.missingPermissionsForTracking(this);
		if (!missingPermissions.isEmpty()) {
			PermissionsUtil.askForLocationPermission(this,
				() -> ActivityCompat.requestPermissions(this,
					missingPermissions.toArray(new String[missingPermissions.size()]),
					Constants.MISSING_PRIVILEGE_ACCESS_COARSE_LOCATION_ID),
				() -> {
					// do nothing
				});
		}
	}

	@Override
	protected void onPostCreate(@Nullable Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		toggle.syncState();
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		toggle.onConfigurationChanged(newConfig);
	}

	@Override
	public void onBackPressed() {
		DrawerLayout drawer = binding.drawer;

		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
		} else {
			super.onBackPressed();
		}
	}

	/**
	 * @param minutesToPredate
	 *            if greater than 0, predate the event this many minutes
	 */
	public void clockInAction(int minutesToPredate) {
		if (minutesToPredate < 0) {
			throw new IllegalArgumentException("no negative argument allowed");
		}
		// commit text field
		binding.main.text.clearFocus();

		Task selectedTask = (Task) binding.main.task.getSelectedItem();
		String description = binding.main.text.getText().toString();
		timerManager.startTracking(minutesToPredate, selectedTask, description);
		externalNotificationManager.notifyPebble("started tracking");
		refreshView();
	}

	public void setTarget(DayOfWeek day) {
		if (day == null) {
			// add or change general target
			Logger.debug("Starting to edit general target");

			Intent i = new Intent(this, TargetEditActivity.class);
			startActivity(i);

		} else {
			// add or change target of a day
			Week currentWeek = getCurrentWeek();

			if (currentWeek != null) {
				LocalDate targetDay = currentWeek.getStart().plusDays(day.ordinal());

				Intent i = new Intent(this, TargetEditActivity.class);
				i.putExtra(Constants.DATE_EXTRA_KEY, targetDay.toEpochDay());
				startActivity(i);
			}
		}
	}

	/**
	 * @param minutesToPredate
	 *            if greater than 0, predate the event this many minutes
	 */
	public void clockOutAction(int minutesToPredate) {
		if (minutesToPredate < 0) {
			throw new IllegalArgumentException("no negative argument allowed");
		}

		timerManager.stopTracking(minutesToPredate);
		externalNotificationManager.notifyPebble("stopped tracking");
		refreshView();
	}

	/**
	 * @param interval position of the week relative to the currently displayed week, e.g. -2 for two weeks before the
	 *                 currently displayed week
	 */
	private void changeDisplayedWeek(int interval) {
		if (interval == 0) {
			return;
		}

		// display a Toast indicating the change interval (helps the user for more than one week difference)
		if (Math.abs(interval) > 1) {
			CharSequence backwardOrForward = interval < 0 ? getText(R.string.backward) : getText(R.string.forward);
			Toast.makeText(this, backwardOrForward + " " + Math.abs(interval) + " " + getText(R.string.weeks),
					Toast.LENGTH_SHORT).show();
		}

		showWeek(getCurrentWeekIndex() + interval, true);
	}

	/**
	 * Reloads the view's data if the view is currently shown.
	 */
	public static void refreshViewIfShown() {
		if (instance != null && instance.visible) {
			Logger.debug("refreshing main view (it is visible at the moment)");
			instance.refreshView();
		}
	}

	protected void refreshView() {
		if (preferences.getBoolean(getString(R.string.keyShowNavigationButtons), false)) {
			binding.main.navigation.setVisibility(View.VISIBLE);
		} else {
			binding.main.navigation.setVisibility(View.GONE);
		}

		binding.main.clockOutButton.setEnabled(timerManager.isTracking());
		Task taskToSelect;
		if (timerManager.isTracking()) {
			binding.main.clockInButton.setText(R.string.clockInChange);
			taskToSelect = timerManager.getCurrentTask();
		} else {
			binding.main.clockInButton.setText(R.string.clockIn);
			taskToSelect = dao.getDefaultTask();
		}
		if (taskToSelect != null) {
			int i = 0;
			for (Task oneTask : tasks) {
				if (oneTask.getId().equals(taskToSelect.getId())) {
					binding.main.task.setSelection(i);
					break;
				}
				i++;
			}
		}

		weekAdapter.notifyDataSetChanged();
		refreshRecenterMenuItem();
	}

	private void refreshRecenterMenuItem() {
		if(recenterMenuItem == null) {
			// Can happen during startup, since onCreateOptionsMenu() is called after onResume()
			return;
		}

		// 0, when current week is displayed
		// negative value, when currently displayed week is in the past
		// positive value, when displaying future week
		int difference = getTodaysWeekIndex() - getCurrentWeekIndex();

		boolean itemVisible = abs(difference) > 0;
		recenterMenuItem.setVisible(itemVisible);

		if(!itemVisible) {
			return;
		}
		@DrawableRes int icon = difference > 0
				? R.drawable.ic_calendar_recenter_right
				: R.drawable.ic_calendar_recenter_left;
		recenterMenuItem.setIcon(icon);
	}

	private void setupTasksAdapter() {
		tasks = dao.getActiveTasks();
		tasksAdapter = new ArrayAdapter<>(this, R.layout.list_item_spinner, tasks);
		tasksAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		binding.main.task.setAdapter(tasksAdapter);
	}

	/**
	 * Mark task list as changed so it will be re-read from the database the next time the GUI is refreshed.
	 */
	public void refreshTasks() {
		reloadTasksOnResume = true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		if (Basics.getOrCreateInstance(this).isDevelopmentVersion()) {
			menu.add(Menu.NONE, MenuAction.RAISE_EXCEPTION.ordinal(), MenuAction.RAISE_EXCEPTION.ordinal(), "[DEV] Raise Exception")
					.setIcon(R.drawable.ic_menu_star);
			menu.add(Menu.NONE, MenuAction.SHOW_DEBUG.ordinal(), MenuAction.SHOW_DEBUG.ordinal(), "[DEV] Show Debug")
					.setIcon(R.drawable.ic_menu_info_details);
		}

		int recenterId = MenuAction.RECENTER_WEEK.ordinal();
		recenterMenuItem = menu.add(Menu.NONE, recenterId, recenterId, R.string.recenter_week);
		recenterMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		recenterMenuItem.setVisible(false);
		refreshRecenterMenuItem();

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (toggle.onOptionsItemSelected(item)) {
			return true;
		}

		switch (MenuAction.byOrdinal(item.getItemId())) {
			case RECENTER_WEEK:
				recenterWeek();
				return true;

			case RAISE_EXCEPTION:
				throw new IllegalStateException("this exception is for testing purposes only");
			case SHOW_DEBUG:
				showDebug();
				return true;
			default:
				throw new IllegalArgumentException("options menu: unknown item selected");
		}
	}

	@Override
	public boolean onNavigationItemSelected(MenuItem menuItem) {
		Logger.debug("onNavigationItemSelected");

		int itemId = menuItem.getItemId();

		if (itemId == R.id.nav_about) {
			showAbout();

		} else if (itemId == R.id.nav_preferences) {
			showOptions();

		} else if (itemId == R.id.nav_set_work_location) {
			useCurrentLocationAsWorkplace();

		} else if (itemId == R.id.nav_edit_events) {
			showEventList();

		} else if (itemId == R.id.nav_edit_tasks) {
				showTaskList();

		} else if (itemId == R.id.nav_insert_default_times) {
			showInsertDefaultTimes();

		} else if (itemId == R.id.nav_reports) {
			showReports();

		} else if (itemId == R.id.nav_data_backup) {
			backupToSd();

		} else if (itemId == R.id.nav_data_restore) {
			restoreFromSd();

		} else if (itemId == R.id.nav_send_logs) {
			sendLogs();

		} else if (itemId == R.id.nav_ignore_battery_optimizations) {
			showRequestToIgnoreBatteryOptimizations();

		} else {
			return false;
		}

		return true;
	}

	private Week getCurrentWeek() {
		int weekIndex = getCurrentWeekIndex();
		return (weekIndex > 0) ? WeekIndexConverter.getWeekForIndex(weekIndex) : null;
	}

	private int getCurrentWeekIndex() {
		return binding.main.week.getCurrentItem();
	}

	private void showEventList() {
		Logger.debug("showing EventList");

		Week currentWeek = getCurrentWeek();
		if (currentWeek != null) {
			Intent i = new Intent(this, EventListActivity.class);
			i.putExtra(Constants.WEEK_START_EXTRA_KEY, currentWeek.toEpochDay());
			startActivity(i);
		}
	}

	private void showTaskList() {
		Logger.debug("showing TaskList");
		Intent i = new Intent(this, TaskListActivity.class);
		startActivity(i);
	}

	private void showInsertDefaultTimes() {
		Logger.debug("showing InsertDefaultTimes");
		Intent i = new Intent(this, InsertDefaultTimesActivity.class);
		startActivity(i);
	}

	private void showOptions() {
		Logger.debug("showing Options");
		Intent i = new Intent(this, OptionsActivity.class);
		startActivity(i);
	}

	private void showRequestToIgnoreBatteryOptimizations() {
		Logger.debug("showing request to ignore battery optimizations");
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getString(R.string.request_to_ignore_battery_optimizations_title));
		alert.setMessage(getString(R.string.request_to_ignore_battery_optimizations_text));
		alert.setPositiveButton(getString(R.string.request_to_ignore_battery_optimizations_go_to_settings),
			(dialog, whichButton) -> Basics.getInstance().openBatterySettings());
		alert.setNegativeButton(getString(R.string.cancel), (dialog, whichButton) -> {
			// do nothing
		});
		alert.show();
	}

	private void useCurrentLocationAsWorkplace() {
		Logger.debug("use current location as work place");
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getString(R.string.use_current_location));
		alert.setMessage(getString(R.string.really_use_current_location));
		alert.setPositiveButton(getString(R.string.ok), (dialog, whichButton) -> Basics.getInstance().useCurrentLocationAsWorkplace(WorkTimeTrackerActivity.this));
		alert.setNegativeButton(getString(R.string.cancel), (dialog, whichButton) -> {
            // do nothing
        });
		alert.show();
	}

	private void showReports() {
		Logger.debug("showing Reports");
		Intent i = new Intent(this, ReportsActivity.class);
		startActivity(i);
	}

	private void showAbout() {
		Logger.debug("showing About");
		Intent i = new Intent(this, AboutActivity.class);
		startActivity(i);
	}

	private void sendLogs() {
		Logger.debug("sending logs");

		new AlertDialog.Builder(this)
			.setTitle(R.string.sendLogs)
			.setMessage(R.string.sendLogsQuestion)
			.setPositiveButton(R.string.ok, (DialogInterface dialog, int id) -> doSendLogs())
			.setNegativeButton(R.string.cancel, (DialogInterface dialog, int id) -> {
				// do nothing
			})
			.create()
			.show();
	}

	private void doSendLogs() {
		Logger.info("app version: {}", Basics.getOrCreateInstance(getApplicationContext()).getVersionName());
		Intent emailIntent = new Intent(Intent.ACTION_SEND);
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.sendLogsSubject));
		Uri fileUri = FileProvider.getUriForFile(this,
			BuildConfig.APPLICATION_ID + ".util.GenericFileProvider", Basics.getInstance().getCurrentLogFile());
		emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
		emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		emailIntent.setType("text/plain");
		String[] to = {getString(R.string.email)};
		emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
		startActivity(Intent.createChooser(emailIntent , getString(R.string.sendLogs)));
	}

	private void showDebug() {
		Logger.debug("showing Debug");
		Intent i = new Intent(this, DebugActivity.class);
		startActivity(i);
	}

	@Override
	protected void onResume() {
		Logger.debug("onResume called");
		visible = true;
		if (reloadTasksOnResume) {
			reloadTasksOnResume = false;
			setupTasksAdapter();
		}

		Basics.getInstance().safeCheckExternalControls();

		// always start with closed navigation drawer
		binding.drawer.closeDrawer(GravityCompat.START, false);

		refreshView();
		super.onResume();
	}

	@Override
	protected void onPause() {
		Logger.debug("onPause called");
		dao.close();
		visible = false;
		super.onPause();
	}

	/**
	 * Get the instance of this activity. If it was garbage-collected in the meantime, throw an exception.
	 */
	public static WorkTimeTrackerActivity getInstance() {
		if (instance == null) {
			throw new IllegalStateException("the main activity is not created yet or was dumped in the meantime");
		}
		return instance;
	}

	/**
	 * Get the instance of this activity. If it was garbage-collected in the meantime, return {@code null}.
	 */
	public static WorkTimeTrackerActivity getInstanceOrNull() {
		return instance;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions,
			int[] grantResults) {
		switch (requestCode) {
			case Constants.PERMISSION_REQUEST_CODE_BACKUP:
				if (grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					backupToSd();
				}
				break;
			case Constants.PERMISSION_REQUEST_CODE_RESTORE:
				if (grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					restoreFromSd();
				}
				break;
			case Constants.MISSING_PRIVILEGE_ACCESS_COARSE_LOCATION_ID:
				List<String> ungranted = PermissionsUtil.notGrantedPermissions(permissions, grantResults);
				if (ungranted.isEmpty()) {
					Intent messageIntent = Basics.getOrCreateInstance(this).createMessageIntent(
						getString(R.string.locationPermissionsGranted), null);
					startActivity(messageIntent);
				} else {
					Intent messageIntent = Basics.getOrCreateInstance(this).createMessageIntent(
						getString(R.string.locationPermissionsUngranted), null);
					startActivity(messageIntent);
					Logger.debug("ungranted tracking permissions: {}", ungranted);
				}
				break;
			case Constants.MISSING_PRIVILEGE_ACCESS_STORAGE_ID:
				if (grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					Basics.getOrCreateInstance(this).initTinyLog();
				}
				break;
			default:
				super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}

	// ---------------------------------------------------------------------------------------------
	// Backup, Restore
	// ---------------------------------------------------------------------------------------------

	/**
	 * Check if file exists and ask user if so.
	 */
	private void backupToSd() {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.PERMISSION_REQUEST_CODE_BACKUP);
			return;
		}

		final BackupFileInfo info = BackupFileInfo.getBackupFiles(false);

		if (info.eventsBackupFile.exists() || info.targetsBackupFile.exists()) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			final String msgBackupOverwrite = getString(R.string.backup_overwrite) + "\n" + info.listAvailable();

			builder.setMessage(msgBackupOverwrite)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    backup(info);
                    dialog.dismiss();
                })
				.setNegativeButton(android.R.string.cancel, null).show();
		} else {
			backup(info);
		}
	}

	private void backup(final BackupFileInfo info) {
		// do in background
		new AsyncTask<Void, Void, Boolean>() {
			private ProgressDialog dialog;

			@Override
			protected void onPreExecute() {
				dialog = ProgressDialog.show(WorkTimeTrackerActivity.this,
					getString(R.string.backup), getString(R.string.please_wait), true);
				dialog.show();
			}

			@Override
			protected Boolean doInBackground(Void... none) {
				return BackupUtil.doBackup(getApplicationContext(), info);
			}

			@Override
			protected void onPostExecute(Boolean successful) {
				if (successful) {
					refreshView();
					Toast.makeText(WorkTimeTrackerActivity.this, info.toString(), Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(WorkTimeTrackerActivity.this, R.string.backup_failed, Toast.LENGTH_LONG).show();
				}
				dialog.dismiss();
			}

		}.execute(null, null);
	}

	private void restoreFromSd() {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.PERMISSION_REQUEST_CODE_RESTORE);
			return;
		}
		final BackupFileInfo info = BackupFileInfo.getBackupFiles(true);

		if (info != null) {
			final Cursor cur = dao.getAllEventsAndTasks();
			final int medCount = cur.getCount();
			cur.close();
			if (medCount > 0) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(this);
				final String msgBackupOverwrite = String.format(
					getString(R.string.restore_warning), info.listAvailable());
				builder.setMessage(msgBackupOverwrite)
					.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        restore(info);
                        dialog.dismiss();
                    })
					.setNegativeButton(android.R.string.cancel, null).show();
			} else {
				// no entries, skip warning
				restore(info);
			}
		} else {
			Toast.makeText(this, getString(R.string.restore_failed_file_not_found), Toast.LENGTH_LONG).show();
		}
	}

	private void restore(final BackupFileInfo info) {
		// do in background
		new AsyncTask<Void, Void, Boolean>() {
			private ProgressDialog dialog;

			@Override
			protected void onPreExecute() {
				dialog = ProgressDialog.show(WorkTimeTrackerActivity.this,
					getString(R.string.restore), getString(R.string.please_wait), true);
				dialog.show();
			}

			@Override
			protected Boolean doInBackground(Void... none) {
				try {
					if (info.eventsBackupFile.exists()) {
						final BufferedReader input = new BufferedReader(
								new InputStreamReader(new FileInputStream(info.eventsBackupFile)));

						dao.restoreEventsFromReader(input);
					}
					if (info.targetsBackupFile.exists()) {
						final BufferedReader input = new BufferedReader(
								new InputStreamReader(new FileInputStream(info.targetsBackupFile)));

						dao.restoreTargetsFromReader(input);
					}
					return true;
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}
			}

			@Override
			protected void onPostExecute(Boolean successful) {
				if (successful) {
					refreshView();
					Toast.makeText(WorkTimeTrackerActivity.this, info.listAvailable(), Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(WorkTimeTrackerActivity.this, R.string.restore_failed, Toast.LENGTH_LONG).show();
				}
				dialog.dismiss();
			}

		}.execute(null, null);
	}
}
