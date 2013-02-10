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

import hirondelle.date4j.DateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.ListView;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.EventSeparator;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.model.WeekPlaceholder;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;
import org.zephyrsoft.trackworktime.util.FlexibleArrayAdapter;
import org.zephyrsoft.trackworktime.util.Logger;
import org.zephyrsoft.trackworktime.util.SeparatorIdentificationMethod;
import org.zephyrsoft.trackworktime.util.StringExtractionMethod;
import org.zephyrsoft.trackworktime.util.WeekDayHelper;

/**
 * Activity for managing the events of a week.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class EventListActivity extends ListActivity {
	
	/** key for the intent extra "week start" */
	public static final String WEEK_START_EXTRA_KEY = "WEEK_START_EXTRA_KEY";
	
	private static final int NEW_EVENT = 0;
	private static final int EDIT_EVENT = 1;
	private static final int DELETE_EVENT = 2;
	
	private static EventListActivity instance = null;
	
	private DAO dao = null;
	private TimerManager timerManager = null;
	
	private String weekStart;
	private Week week;
	private List<Event> events = null;
	
	private WorkTimeTrackerActivity parentActivity = null;
	
	private ArrayAdapter<Event> eventsAdapter;
	
	@Override
	protected void onPause() {
		dao.close();
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		getListView().post(new Runnable() {
			@Override
			public void run() {
				getListView().setSelection(getListAdapter().getCount() - 1);
			}
		});
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		instance = this;
		parentActivity = WorkTimeTrackerActivity.getInstanceOrNull();
		
		dao = Basics.getInstance().getDao();
		timerManager = Basics.getInstance().getTimerManager();
		weekStart = getIntent().getStringExtra(WEEK_START_EXTRA_KEY);
		events = new ArrayList<Event>();
		refreshView();
		eventsAdapter =
			new FlexibleArrayAdapter<Event>(this, android.R.layout.simple_list_item_1, events,
				new StringExtractionMethod<Event>() {
					@Override
					public String extractText(Event object) {
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
				}, R.layout.list_header, new SeparatorIdentificationMethod<Event>() {
					@Override
					public String extractText(Event object) {
						return object.toString();
					}
					
					@Override
					public boolean isSeparator(Event object) {
						return object instanceof EventSeparator;
					}
				});
		eventsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		setListAdapter(eventsAdapter);
		
		final ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		
		registerForContextMenu(lv);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Event event = (Event) lv.getItemAtPosition(position);
				if (!(event instanceof EventSeparator)) {
					startEditing(event);
				}
			}
		});
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case NEW_EVENT:
				Logger.debug("starting to enter a new event");
				Intent i = new Intent(this, EventEditActivity.class);
				i.putExtra(EventEditActivity.WEEK_START_EXTRA_KEY, weekStart);
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
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		menu.setHeaderTitle(R.string.availableActions);
		menu.add(Menu.NONE, EDIT_EVENT, EDIT_EVENT, getString(R.string.edit_event)).setIcon(
			R.drawable.ic_menu_info_details);
		menu.add(Menu.NONE, DELETE_EVENT, DELETE_EVENT, getString(R.string.delete_event)).setIcon(
			R.drawable.ic_menu_delete);
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		final int eventPosition = info.position;
		final Event oldEvent = (Event) getListView().getItemAtPosition(eventPosition);
		if (oldEvent instanceof EventSeparator) {
			return true;
		}
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		switch (item.getItemId()) {
			case EDIT_EVENT:
				startEditing(oldEvent);
				return true;
			case DELETE_EVENT:
				alert.setTitle(getString(R.string.delete_event));
				alert.setMessage(getString(R.string.really_delete_event));
				
				alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						// delete event in DB
						boolean success = dao.deleteEvent(oldEvent);
						// we have to call this manually when using the DAO directly:
						timerManager.updateWeekSum(week);
						Basics.getInstance().safeCheckPersistentNotification();
						refreshView();
						if (success) {
							Logger.debug("deleted event with ID {0}", oldEvent.getId());
						} else {
							Logger.warn("could not delete event with ID {0}", oldEvent.getId());
						}
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
	
	private void startEditing(Event event) {
		Logger.debug("starting to edit the existing event with ID {0} ({1} @ {2})", event.getId(),
			TypeEnum.byValue(event.getType()).toString(), event.getTime());
		Intent i = new Intent(this, EventEditActivity.class);
		i.putExtra(EventEditActivity.EVENT_ID_EXTRA_KEY, event.getId());
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
		
		if (eventsAdapter != null) {
			// only do this if not called from onCreate()
			eventsAdapter.notifyDataSetChanged();
			if (parentActivity != null) {
				parentActivity.refreshView();
			}
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
				iter.add(new EventSeparator(WeekDayHelper.getWeekDayLongName(DateTimeUtil.stringToDateTime(cur
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
}
