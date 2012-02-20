package org.zephyrsoft.worktimetracker.database;

import java.util.Collection;
import java.util.TreeSet;
import android.database.Cursor;
import org.zephyrsoft.worktimetracker.model.Event;
import org.zephyrsoft.worktimetracker.model.Task;
import org.zephyrsoft.worktimetracker.model.Week;

public class DBUtil {
	
	/**
	 * @param c
	 * @param index 0-based
	 * @return
	 */
	public static String getColumnAsString(Cursor c, int index) {
		return c.getString(index);
	}
	
	/**
	 * If used with a result set directly from the DB adapter (and not via {@link #toTasks}), make sure that
	 * {@link Cursor#moveToFirst()} was called before!
	 */
	public static Task toTask(Cursor c) {
		return new Task(c.getInt(0), c.getString(1), c.getInt(2));
	}
	
	public static Collection<Task> toTasks(Cursor c) {
		Collection<Task> ret = new TreeSet<Task>();
		
		for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
			ret.add(toTask(c));
		}
		
		return ret;
	}
	
	/**
	 * If used with a result set directly from the DB adapter (and not via {@link #toWeeks}), make sure that
	 * {@link Cursor#moveToFirst()} was called before!
	 */
	public static Week toWeek(Cursor c) {
		return new Week(c.getInt(0), c.getString(1), c.getInt(2));
	}
	
	public static Collection<Week> toWeeks(Cursor c) {
		Collection<Week> ret = new TreeSet<Week>();
		
		for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
			ret.add(toWeek(c));
		}
		
		return ret;
	}
	
	/**
	 * If used with a result set directly from the DB adapter (and not via {@link #toEvents}), make sure that
	 * {@link Cursor#moveToFirst()} was called before!
	 */
	public static Event toEvent(Cursor c) {
		return new Event(c.getInt(0), c.getInt(1), c.getInt(2), c.getInt(3), c.getString(4));
	}
	
	public static Collection<Event> toEvents(Cursor c) {
		Collection<Event> ret = new TreeSet<Event>();
		
		for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
			ret.add(toEvent(c));
		}
		
		return ret;
	}
}
