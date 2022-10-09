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
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.databinding.ListActivityBinding;
import org.zephyrsoft.trackworktime.editevent.EventEditActivity;
import org.zephyrsoft.trackworktime.eventlist.EventAdapter;
import org.zephyrsoft.trackworktime.eventlist.EventViewHolder;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.EventSeparator;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.BroadcastUtil;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;
import org.zephyrsoft.trackworktime.util.ForeignCall;
import org.zephyrsoft.trackworktime.weektimes.WeekIndexConverter;

import java.lang.ref.WeakReference;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

/**
 * Activity for managing the events of a week.
 */
public class EventListActivity extends AppCompatActivity {

	private static final int NEW_EVENT = 0;
	private static final int NEW_PERIOD = 1;

	private static final String BUNDLE_KEY_WEEK_START_DATE = "BUNDLE_KEY_WEEK_START_DATE";

	private static WeakReference<EventListActivity> instance = null;

	private DAO dao = null;
	private TimerManager timerManager = null;

	private Week week;
	private List<Event> events = null;
	private final Map<Integer, Task> taskIdToTaskMap = new HashMap<>();

	private EventAdapter myEventAdapter;
	private SelectionTracker<Long> selectionTracker;

	private Locale locale;

	@Override
	protected void onPause() {
		dao.close();
		super.onPause();
	}

	@Override
	protected void onResume() {
		locale = Basics.get(this).getLocale();
		super.onResume();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		locale = Basics.get(this).getLocale();

		ListActivityBinding binding = ListActivityBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		RecyclerView myRecyclerView = binding.recyclerView;

		instance = new WeakReference<>(this);

		dao = Basics.get(this).getDao();
		timerManager = Basics.get(this).getTimerManager();

		long epochDay = getIntent().getLongExtra(Constants.WEEK_START_EXTRA_KEY, -1);
		if (epochDay >= 0) {
			week = new Week(epochDay);
		} else if (savedInstanceState != null && savedInstanceState.containsKey(BUNDLE_KEY_WEEK_START_DATE)) {
			Logger.debug("using week from bundle");
			week = new Week(savedInstanceState.getLong(BUNDLE_KEY_WEEK_START_DATE));
		} else {
			// we don't know which week is meant - let's just use the current one
			Logger.debug("using current week");
			week = WeekIndexConverter.getWeekForDate(LocalDate.now());
		}

		events = new ArrayList<>();
		myEventAdapter = new EventAdapter(
				this::onEventClick,
				locale,
				this::getEventTaskName,
				this::isEventSelected
		);
		myRecyclerView.setHasFixedSize(true);
		myRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		myRecyclerView.setAdapter(myEventAdapter);

		selectionTracker = new SelectionTracker.Builder<>(
				"event-selection",
				myRecyclerView,
				new EventKeyProvider(myRecyclerView),
				new EventDetailsLookup(myRecyclerView),
				StorageStrategy.createLongStorage()
		).build();

		refreshView();

		// restore previous state
		if (savedInstanceState != null) {
			selectionTracker.onRestoreInstanceState(savedInstanceState);
		}

		selectionTracker.addObserver(new SelectionTracker.SelectionObserver<>() {
			@Override
			public void onSelectionChanged() {
				if (selectionTracker.hasSelection() && myActionMode == null) {
					myActionMode = startSupportActionMode(mActionModeCallback);

				} else if (!selectionTracker.hasSelection() && myActionMode != null) {
					myActionMode.finish();
					myActionMode = null;
				}
			}
		});
	}

	private void onEventClick(Event event) {
		Logger.debug("View onClick");

		if (!selectionTracker.hasSelection()) {
			if (!(event instanceof EventSeparator)) {
				startEditing(event);
			}
		}
	}

	private String getEventTaskName(Event event) {
		Task task = taskIdToTaskMap.get(event.getTask());
		if (task == null) {
			return "";
		} else {
			return task.getName();
		}
	}

	private boolean isEventSelected(Event event) {
		Integer id = event.getId();
		if (id == null) {
			return false;
		} else {
			return selectionTracker.isSelected((long) id);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		instance = null;
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putLong(BUNDLE_KEY_WEEK_START_DATE, week.toEpochDay());
		selectionTracker.onSaveInstanceState(outState);
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch (item.getItemId()) {
			case NEW_EVENT:
				Logger.debug("starting to enter a new event");
				Intent startEventEditActivity = new Intent(this, EventEditActivity.class);
				startEventEditActivity.putExtra(Constants.WEEK_START_EXTRA_KEY, week.toEpochDay());
				startActivity(startEventEditActivity);
				return true;
			case NEW_PERIOD:
				Logger.debug("starting to enter a new period");
				Intent i = new Intent(this, EventEditActivity.class);
				i.putExtra(Constants.WEEK_START_EXTRA_KEY, week.toEpochDay());
				i.putExtra(Constants.PERIOD_EXTRA_KEY, true);
				startActivity(i);
				return true;
			case android.R.id.home:
				finish();
				return true;
			default:
				throw new IllegalArgumentException("options menu: unknown item selected");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, NEW_EVENT, NEW_EVENT, getString(R.string.new_event)).setIcon(R.drawable.ic_menu_add);
		menu.add(Menu.NONE, NEW_PERIOD, NEW_PERIOD, getString(R.string.newPeriod)).setIcon(R.drawable.ic_menu_add);
		return super.onCreateOptionsMenu(menu);
	}

	private void startEditing(Event event) {
		Logger.debug("starting to edit the existing event with ID {} ({} @ {})", event.getId(),
				TypeEnum.byValue(event.getType()).toString(), event.getDateTime());

		Intent i = new Intent(this, EventEditActivity.class);
		i.putExtra(Constants.EVENT_ID_EXTRA_KEY, event.getId());
		startActivity(i);
	}

	/**
	 * Refresh the event list and the main activity.
	 */
	@ForeignCall
	public void refreshView() {
		refreshEvents();
		refreshTasks();
		refreshAdapter();
	}

	private void refreshEvents() {
		events.clear();
		events.addAll(dao.getEventsInWeek(week, timerManager.getHomeTimeZone()));
		insertSeparators(events);
	}

	private void insertSeparators(List<Event> eventList) {
		ListIterator<Event> iter = eventList.listIterator();
		Event prev = null;
		while (iter.hasNext()) {
			Event cur = iter.next();
			if (prev == null || !isOnSameDay(prev, cur)) {
				iter.previous();
				String caption = DateTimeUtil.formatLocalizedDayAndDate(cur.getDateTime(), locale);
				iter.add(new EventSeparator(caption));
				iter.next();
			}
			prev = cur;
		}
	}

	private void refreshTasks() {
		taskIdToTaskMap.clear();
		List<Task> tasks = dao.getAllTasks();
		for(Task t : tasks) {
			Integer id = t.getId();
			taskIdToTaskMap.put(id, t);
		}
	}

	private void refreshAdapter() {
		myEventAdapter.submitEvents(events);
	}

	private static boolean isOnSameDay(Event e1, Event e2) {
		return e1.getDateTime().toLocalDate().isEqual(e2.getDateTime().toLocalDate());
	}

	/**
	 * Getter for the weakly referenced instance.
	 */
	public static EventListActivity getInstance() {
		return instance == null
			? null
			: instance.get();
	}

	private ActionMode myActionMode;
	private final ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
		@Override
		public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
			getMenuInflater().inflate(R.menu.list_context_menu, menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
			if (menuItem.getItemId() == R.id.menu_item_delete) {
				AlertDialog.Builder alert = new AlertDialog.Builder(EventListActivity.this);
				alert.setTitle(getString(R.string.delete_event));
				alert.setMessage(getString(R.string.really_delete_event));

				alert.setPositiveButton(getString(R.string.ok),
						(dialog, whichButton) -> {
							OffsetDateTime cacheInvalidationStart = null;

							for (int i = 0; i < events.size(); i++) {
								if (selectionTracker.isSelected((long) myEventAdapter.getItemId(i))) {
									Event event = events.get(i);
									boolean success = dao.deleteEvent(event);

									if (success) {
										Logger.debug("deleted event with ID {}", event.getId());
										if (cacheInvalidationStart == null
											|| event.getDateTime().isBefore(cacheInvalidationStart)) {
											cacheInvalidationStart = event.getDateTime();
										}
										BroadcastUtil.sendEventBroadcast(event, EventListActivity.this,
											BroadcastUtil.Action.DELETED, TimerManager.EventOrigin.EVENT_LIST);
									} else {
										Logger.warn("could not delete event with ID {}", event.getId());
									}
								}
							}
							selectionTracker.clearSelection();

							if (cacheInvalidationStart != null) {
								// we have to call this manually when using the DAO directly:
								timerManager.invalidateCacheFrom(cacheInvalidationStart);
								// now reload the data
								refreshView();
							}

							Basics.get(EventListActivity.this).safeCheckExternalControls();
						});
				alert.setNegativeButton(getString(R.string.cancel),
						(dialog, which) -> {
							// do nothing
						});
				alert.show();

				return true;

			} else {
				return false;
			}
		}

		@Override
		public void onDestroyActionMode(ActionMode actionMode) {
			selectionTracker.clearSelection();
		}
	};

	private static final class EventDetailsLookup extends ItemDetailsLookup<Long> {

		private final RecyclerView mRecyclerView;

		EventDetailsLookup(RecyclerView recyclerView) {
			mRecyclerView = recyclerView;
		}

		@Override
		public ItemDetails<Long> getItemDetails(MotionEvent e) {
			View view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());

			if (view != null) {
				ViewHolder holder = mRecyclerView.getChildViewHolder(view);

				if (holder instanceof EventViewHolder) {
					return ((EventViewHolder) holder).getItemDetails();
				}
			}

			return null;
		}
	}

	private final class EventKeyProvider extends ItemKeyProvider<Long> {

		private final RecyclerView mRecyclerView;

		public EventKeyProvider(RecyclerView recyclerView) {
			super(SCOPE_MAPPED);
			mRecyclerView = recyclerView;
		}

		@Override
		public Long getKey(int position) {
			return myEventAdapter.getItemId(position);
		}

		@Override
		public int getPosition(@NonNull Long key) {
			RecyclerView.ViewHolder viewHolder = mRecyclerView.findViewHolderForItemId(key);
			return (viewHolder == null) ? RecyclerView.NO_POSITION : viewHolder.getLayoutPosition();
		}
	}
}
