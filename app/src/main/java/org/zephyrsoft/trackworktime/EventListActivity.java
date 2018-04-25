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

import hirondelle.date4j.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.StateSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.bignerdranch.android.multiselector.SwappingHolder;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.EventSeparator;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.model.WeekPlaceholder;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;
import org.zephyrsoft.trackworktime.util.WeekDayHelper;

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

	private String weekStart;
	private Week week;
	private List<Event> events = null;

	private RecyclerView myRecyclerView;
	private EventAdapter myEventAdapter;

	@Override
	protected void onPause() {
		dao.close();
		super.onPause();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_activity);
		myRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

		instance = this;

		dao = Basics.getInstance().getDao();
		timerManager = Basics.getInstance().getTimerManager();
		weekStart = getIntent().getStringExtra(Constants.WEEK_START_EXTRA_KEY);
		events = new ArrayList<Event>();
		refreshView();
		myEventAdapter = new EventAdapter();
		myRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		myRecyclerView.setAdapter(myEventAdapter);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case NEW_EVENT:
				Logger.debug("starting to enter a new event");
				Intent i = new Intent(this, EventEditActivity.class);
				i.putExtra(Constants.WEEK_START_EXTRA_KEY, weekStart);
				startActivity(i);
				return true;
			default:
				throw new IllegalArgumentException("options menu: unknown item selected");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, NEW_EVENT, NEW_EVENT, getString(R.string.new_event)).setIcon(
				R.drawable.ic_menu_add);
		return super.onCreateOptionsMenu(menu);
	}

	private void startEditing(Event event) {
		Logger.debug("starting to edit the existing event with ID {} ({} @ {})", event.getId(),
				TypeEnum.byValue(
						event.getType()).toString(), event.getTime());
		Intent i = new Intent(this, EventEditActivity.class);
		i.putExtra(Constants.EVENT_ID_EXTRA_KEY, event.getId());
		startActivity(i);
	}

	/**
	 * Refresh the event list and the main activity.
	 */
	public void refreshView() {
		events.clear();
		// re-read the week in case the first event for a week was just created
		// (then the week would have been created just now)
		week = dao.getWeek(weekStart);
		if (weekStart != null && week == null) {
			week = new WeekPlaceholder(weekStart);
		}
		events.addAll(dao.getEventsInWeek(week));

		insertSeparators(events);
		if (myEventAdapter != null) {
			myEventAdapter.notifyDataSetChanged();
		}
	}

	private static void insertSeparators(List<Event> eventList) {
		ListIterator<Event> iter = eventList.listIterator();
		Event prev = null;
		Event cur = null;
		while (iter.hasNext()) {
			cur = iter.next();
			if (prev == null || !isOnSameDay(prev, cur)) {
				iter.previous();
				iter.add(new EventSeparator(
						WeekDayHelper.getWeekDayLongName(DateTimeUtil.stringToDateTime(cur
								.getTime()))));
				iter.next();
			}
			prev = cur;
		}
	}

	private static boolean isOnSameDay(Event e1, Event e2) {
		DateTime d1 = DateTimeUtil.stringToDateTime(e1.getTime());
		DateTime d2 = DateTimeUtil.stringToDateTime(e2.getTime());
		return d1.isSameDayAs(d2);
	}

	/**
	 * Getter for the singleton.
	 */
	public static EventListActivity getInstance() {
		return instance;
	}

	private MultiSelector myMultiSelector = new MultiSelector();
	private ActionMode myActionMode;

	private ModalMultiSelectorCallback mActionModeCallback
			= new ModalMultiSelectorCallback(myMultiSelector) {

		@Override
		public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
			super.onCreateActionMode(actionMode, menu);
			getMenuInflater().inflate(R.menu.list_context_menu, menu);
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
			switch (menuItem.getItemId()) {
				case R.id.menu_item_delete:
					AlertDialog.Builder alert = new AlertDialog.Builder(EventListActivity.this);
					alert.setTitle(getString(R.string.delete_event));
					alert.setMessage(getString(R.string.really_delete_event));

					alert.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int whichButton) {
									for (int i = events.size(); i >= 0; i--) {
										if (myMultiSelector.isSelected(i, 0)) {
											Event event = events.remove(i);
											// delete event in DB
											boolean success = dao.deleteEvent(event);
											// we have to call this manually when using the DAO directly:
											timerManager.updateWeekSum(week);
											Basics.getInstance().safeCheckPersistentNotification();
											if (success) {
												Logger.debug("deleted event with ID {}",
														event.getId());
											} else {
												Logger.warn("could not delete event with ID {}",
														event.getId());
											}
											myRecyclerView.getAdapter().notifyItemRemoved(i);
										}
									}

									myMultiSelector.clearSelections();
								}
							});
					alert.setNegativeButton(getString(R.string.cancel),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// do nothing
								}
							});
					alert.show();

					actionMode.finish();

					return true;
				default:
					return false;
			}
		}

		@Override
		public void onDestroyActionMode(ActionMode actionMode) {
			myMultiSelector.setSelectable(false);
			myActionMode = null;
		}
	};

	private class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
		private static final int VIEW_TYPE_SEPARATOR = 0;
		private static final int VIEW_TYPE_EVENT = 1;

		class EventViewHolder extends SwappingHolder
				implements View.OnLongClickListener, View.OnClickListener {

			public EventViewHolder(View itemView, int viewType) {
				super(itemView, myMultiSelector);
				if (viewType == VIEW_TYPE_EVENT) {
					itemView.setLongClickable(true);
					itemView.setOnLongClickListener(this);
					itemView.setOnClickListener(this);
				}
				setSelectionModeBackgroundDrawable(getSelectedStateDrawable());
			}

			@Override
			public boolean onLongClick(View view) {
				if (!myMultiSelector.isSelectable()) {
					myActionMode = startSupportActionMode(mActionModeCallback);
					myMultiSelector.setSelectable(true);
					myMultiSelector.setSelected(this, true);
					return true;
				}
				return false;
			}

			@Override
			public void onClick(View v) {

				if (!myMultiSelector.tapSelection(this)) {
					Event event = events.get(getAdapterPosition());
					if (!(event instanceof EventSeparator)) {
						startEditing(event);
					}
				} else if (myMultiSelector.getSelectedPositions().isEmpty()) {
					if (myActionMode != null) {
						myActionMode.finish();
					}
				}
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
			final View itemView = LayoutInflater.from(parent.getContext()).inflate(
					R.layout.list_item, parent, false);
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
		}

		@Override
		public int getItemCount() {
			return events.size();
		}

		private String extractText(Event object) {
			DateTime dateTime = DateTimeUtil.stringToDateTime(object.getTime());
			TypeEnum type = TypeEnum.byValue(object.getType());
			String typeString;
			if (type == TypeEnum.CLOCK_IN) {
				typeString = "IN";
			} else if (type == TypeEnum.CLOCK_OUT) {
				typeString = "OUT";
			} else {
				throw new IllegalStateException("unrecognized event type");
			}
			return DateTimeUtil.dateTimeToDateString(dateTime) + " / "
					+ DateTimeUtil.dateTimeToHourMinuteString(dateTime) + ": " + typeString;
		}
		private Drawable getSelectedStateDrawable() {
			Drawable colorDrawable = new ColorDrawable(
					ContextCompat.getColor(EventListActivity.this, R.color.selected_background));

			StateListDrawable stateListDrawable = new StateListDrawable();
			stateListDrawable.addState(new int[]{android.R.attr.state_activated}, colorDrawable);
			stateListDrawable.addState(StateSet.WILD_CARD, null);

			return stateListDrawable;
		}
	}
}
