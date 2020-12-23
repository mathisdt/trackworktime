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

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.pmw.tinylog.Logger;
import org.threeten.bp.OffsetDateTime;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.databinding.ListActivityBinding;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.EventSeparator;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;
import org.zephyrsoft.trackworktime.util.WeekDayHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Activity for managing the events of a week.
 *
 * @author Mathis Dirksen-Thedens
 */
public class EventListActivity extends AppCompatActivity {

	private static final int NEW_EVENT = 0;
	private static final int EDIT_EVENT = 1;
	private static final int DELETE_EVENT = 2;

	private static EventListActivity instance = null;

	private DAO dao = null;
	private TimerManager timerManager = null;

	private Week week;
	private List<Event> events = null;

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

		RecyclerView myRecyclerView = binding.recyclerView;

		instance = this;

		dao = Basics.getInstance().getDao();
		timerManager = Basics.getInstance().getTimerManager();

		long epochDay = getIntent().getLongExtra(Constants.WEEK_START_EXTRA_KEY, -1);
		if (epochDay == -1) {
			throw new IllegalArgumentException("Week start must be given.");
		}

		week = new Week(epochDay);
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
		).withSelectionPredicate(new SelectionTracker.SelectionPredicate<Long>() {
			@Override
			public boolean canSetStateForKey(Long key, boolean nextState) {
				if (key != null) {
					return !(events.get(key.intValue()) instanceof EventSeparator);
				}

				return false;
			}

			@Override
			public boolean canSetStateAtPosition(int position, boolean nextState) {
				return !(events.get(position) instanceof EventSeparator);
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

		selectionTracker.addObserver(new SelectionTracker.SelectionObserver() {
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

	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		selectionTracker.onSaveInstanceState(outState);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case NEW_EVENT:
				Logger.debug("starting to enter a new event");
				Intent i = new Intent(this, EventEditActivity.class);
				i.putExtra(Constants.WEEK_START_EXTRA_KEY, week.toEpochDay());
				startActivity(i);
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
		events.clear();
		events.addAll(dao.getEventsInWeek(week, timerManager.getHomeTimeZone()));

		insertSeparators(events);
		if (myEventAdapter != null) {
			myEventAdapter.notifyDataSetChanged();
		}
	}

	private static void insertSeparators(List<Event> eventList) {
		ListIterator<Event> iter = eventList.listIterator();
		Event prev = null;
		while (iter.hasNext()) {
			Event cur = iter.next();
			if (prev == null || !isOnSameDay(prev, cur)) {
				iter.previous();
				iter.add(new EventSeparator(WeekDayHelper.getWeekDayLongName(cur.getDateTime())));
				iter.next();
			}
			prev = cur;
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
							for (int i = events.size(); i >= 0; i--) {
								if (selectionTracker.isSelected((long) i)) {

									// delete event in list and DB
									Event event = events.remove(i);
									boolean success = dao.deleteEvent(event);

									Basics.getInstance().safeCheckExternalControls();

									if (success) {
										Logger.debug("deleted event with ID {}",
												event.getId());

										// we have to call this manually when using the DAO directly:
										timerManager.invalidateCacheFrom(event.getDateTime());
									} else {
										Logger.warn("could not delete event with ID {}",
												event.getId());
									}
									myEventAdapter.notifyItemRemoved(i);
								}
							}

							selectionTracker.clearSelection();
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

	private class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
		private static final int VIEW_TYPE_SEPARATOR = 0;
		private static final int VIEW_TYPE_EVENT = 1;

		class EventViewHolder extends RecyclerView.ViewHolder
				implements View.OnClickListener {

			public EventViewHolder(View itemView, int viewType) {
				super(itemView);

				if (viewType == VIEW_TYPE_EVENT) {
					itemView.setOnClickListener(this);
				}
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

				return new ItemDetailsLookup.ItemDetails<Long>() {
					@Override
					public int getPosition() {
						return getAdapterPosition();
					}

					@Override
					public Long getSelectionKey() {
						return getItemId();
					}
				};
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
		public EventViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			final View itemView = LayoutInflater.from(parent.getContext())
					.inflate(R.layout.list_item, parent, false);

			return new EventViewHolder(itemView, viewType);
		}

		@Override
		public void onBindViewHolder(EventViewHolder holder, int position) {
			final Event event = events.get(position);
			if (event instanceof EventSeparator) {
				((TextView) holder.itemView).setText(event.toString());
			} else {
				((TextView) holder.itemView).setText(extractText(event));
			}

			holder.itemView.setActivated(selectionTracker.isSelected((long)position));
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getItemCount() {
			return events.size();
		}

		private String extractText(Event event) {
			TypeEnum type = TypeEnum.byValue(event.getType());
			String typeString;
			switch (type) {
				case CLOCK_IN:
					typeString = "IN";
					break;
				case CLOCK_OUT:
					typeString = "OUT";
					break;
				default:
					throw new IllegalStateException("unrecognized event type");
			}

			OffsetDateTime dateTime = event.getDateTime();
			return DateTimeUtil.formatLocalizedDateTime(dateTime) + ": " + typeString;
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
