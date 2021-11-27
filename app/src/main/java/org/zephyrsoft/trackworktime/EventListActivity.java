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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

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
import org.threeten.bp.LocalDate;
import org.threeten.bp.OffsetDateTime;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.databinding.ListActivityBinding;
import org.zephyrsoft.trackworktime.databinding.ListItemBinding;
import org.zephyrsoft.trackworktime.databinding.ListItemSeparatorBinding;
import org.zephyrsoft.trackworktime.editevent.EventEditActivity;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.EventSeparator;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;
import org.zephyrsoft.trackworktime.weektimes.WeekIndexConverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Activity for managing the events of a week.
 */
public class EventListActivity extends AppCompatActivity {

	private static final int NEW_EVENT = 0;

	private static final String BUNDLE_KEY_WEEK_START_DATE = "BUNDLE_KEY_WEEK_START_DATE";

	private static EventListActivity instance = null;

	private DAO dao = null;
	private TimerManager timerManager = null;

	private Week week;
	private List<Event> events = null;
	private final Map<Integer, Task> taskIdToTaskMap = new HashMap<>();

	private EventAdapter myEventAdapter;
	private SelectionTracker<Long> selectionTracker;

	@Override
	protected void onPause() {
		dao.close();
		super.onPause();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ListActivityBinding binding = ListActivityBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		RecyclerView myRecyclerView = binding.recyclerView;

		instance = this;

		dao = Basics.getInstance().getDao();
		timerManager = Basics.getInstance().getTimerManager();

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
		refreshView();
		myEventAdapter = new EventAdapter();
		myEventAdapter.setHasStableIds(true);
		myRecyclerView.setHasFixedSize(true);
		myRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		myRecyclerView.setAdapter(myEventAdapter);

		selectionTracker = new SelectionTracker.Builder<>(
				"event-selection",
				myRecyclerView,
				new EventKeyProvider(myRecyclerView),
				new EventDetailsLookup(myRecyclerView),
				StorageStrategy.createLongStorage()
		).withSelectionPredicate(new SelectionTracker.SelectionPredicate<>() {
			@Override
			public boolean canSetStateForKey(@NonNull Long key, boolean nextState) {
				if (key.intValue() >= 0 && key.intValue() < events.size()) {
					return !(events.get(key.intValue()) instanceof EventSeparator);
				}

				return false;
			}

			@Override
			public boolean canSetStateAtPosition(int position, boolean nextState) {
				if (position >=0 && position < events.size()){
					return !(events.get(position) instanceof EventSeparator);
				}
				return false;
			}

			@Override
			public boolean canSelectMultiple() {
				return true;
			}
		}).build();

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
				Intent i = new Intent(this, EventEditActivity.class);
				i.putExtra(Constants.WEEK_START_EXTRA_KEY, week.toEpochDay());
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

	private static void insertSeparators(List<Event> eventList) {
		ListIterator<Event> iter = eventList.listIterator();
		Event prev = null;
		while (iter.hasNext()) {
			Event cur = iter.next();
			if (prev == null || !isOnSameDay(prev, cur)) {
				iter.previous();
				String caption = DateTimeUtil.formatLocalizedDayAndDate(cur.getDateTime());
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

	@SuppressLint("NotifyDataSetChanged")
	private void refreshAdapter() {
		if (myEventAdapter != null) {
			myEventAdapter.notifyDataSetChanged();
		}
	}

	private static boolean isOnSameDay(Event e1, Event e2) {
		return e1.getDateTime().toLocalDate().isEqual(e2.getDateTime().toLocalDate());
	}

	/**
	 * Getter for the singleton.
	 */
	public static EventListActivity getInstance() {
		return instance;
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
								if (selectionTracker.isSelected((long) i)) {
									Event event = events.get(i);
									boolean success = dao.deleteEvent(event);

									if (success) {
										Logger.debug("deleted event with ID {}", event.getId());
										if (cacheInvalidationStart == null
											|| event.getDateTime().isBefore(cacheInvalidationStart)) {
											cacheInvalidationStart = event.getDateTime();
										}
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

							Basics.getInstance().safeCheckExternalControls();
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

	private class EventAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		private static final int VIEW_TYPE_SEPARATOR = 0;
		private static final int VIEW_TYPE_EVENT = 1;

		class EventViewHolder extends RecyclerView.ViewHolder
				implements View.OnClickListener {

			private final ListItemBinding binding;

			public EventViewHolder(ListItemBinding binding) {
				super(binding.getRoot());
				this.binding = binding;
				itemView.setOnClickListener(this);
			}

			@Override
			public void onClick(View v) {
				Logger.debug("View onClick");

				if (!selectionTracker.hasSelection()) {
					Event event = events.get((int)getItemId());
					if (!(event instanceof EventSeparator)) {
						startEditing(event);
					}
				}
			}

			public ItemDetailsLookup.ItemDetails<Long> getItemDetails() {

				return new ItemDetailsLookup.ItemDetails<>() {
					@Override
					public int getPosition() {
						return getBindingAdapterPosition();
					}

					@Override
					public Long getSelectionKey() {
						return getItemId();
					}
				};
			}

			public void bind(Event event, Boolean isSelected) {
				binding.time.setText(formatTime(event.getTime()));
				binding.type.setText(formatType(event.getTypeEnum()));
				binding.task.setText(getTaskName(event.getTask()));
				itemView.setActivated(isSelected);
			}

			private String formatTime(OffsetDateTime time) {
				return DateTimeUtil.formatLocalizedTime(time);
			}

			private String formatType(TypeEnum type) {
				switch (type) {
					case CLOCK_IN:
						return "IN";
					case CLOCK_OUT:
						return "OUT";
					default:
						throw new IllegalStateException("unrecognized event type");
				}
			}

			private String getTaskName(Integer taskId) {
				if (taskId == null) {
					return null;
				}
				Task task = taskIdToTaskMap.get(taskId);
				if (task == null) {
					Logger.error("No task for id: {}", taskId);
					return null;
				}
				return task.getName();
			}
		}

		class EventSeparatorHolder extends RecyclerView.ViewHolder {
			private final ListItemSeparatorBinding binding;

			public EventSeparatorHolder(ListItemSeparatorBinding binding) {
				super(binding.getRoot());
				this.binding = binding;
			}

			public void bind(EventSeparator event) {
				binding.title.setText(event.toString());
			}
		}

		@Override
		public int getItemViewType(int position) {
			if (events.get(position) instanceof EventSeparator) {
				return VIEW_TYPE_SEPARATOR;
			} else {
				return VIEW_TYPE_EVENT;
			}
		}

		@Override
		@NonNull
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			switch(viewType) {
				case VIEW_TYPE_SEPARATOR: {
					ListItemSeparatorBinding binding = ListItemSeparatorBinding.inflate(inflater,
							parent, false);
					return new EventSeparatorHolder(binding);
				} case VIEW_TYPE_EVENT: {
					ListItemBinding binding = ListItemBinding.inflate(inflater, parent, false);
					return new EventViewHolder(binding);
				} default: {
					throw new RuntimeException("Not implemented type: " + viewType);
				}
			}
		}

		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
			final Event event = events.get(position);
			if (holder instanceof EventViewHolder) {
				EventViewHolder eventHolder = (EventViewHolder) holder;
				boolean isSelected = selectionTracker.isSelected((long)position);
				eventHolder.bind(event, isSelected);
			} else if (holder instanceof EventSeparatorHolder) {
				EventSeparatorHolder eventHolder = (EventSeparatorHolder) holder;
				eventHolder.bind((EventSeparator) event);
			} else {
				throw new RuntimeException("Not implemented view holder type: " + holder);
			}
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getItemCount() {
			return events.size();
		}
	}

	private final class EventDetailsLookup extends ItemDetailsLookup<Long> {

		private final RecyclerView mRecyclerView;

		EventDetailsLookup(RecyclerView recyclerView) {
			mRecyclerView = recyclerView;
		}

		@Override
		public ItemDetails<Long> getItemDetails(MotionEvent e) {
			View view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());

			if (view != null) {
				ViewHolder holder = mRecyclerView.getChildViewHolder(view);

				if (holder instanceof EventAdapter.EventViewHolder) {
					return ((EventAdapter.EventViewHolder) holder).getItemDetails();
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
