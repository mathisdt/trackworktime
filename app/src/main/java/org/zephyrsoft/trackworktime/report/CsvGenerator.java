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
package org.zephyrsoft.trackworktime.report;

import org.pmw.tinylog.Logger;
import org.supercsv.cellprocessor.CellProcessorAdaptor;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.util.CsvContext;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.TimeSum;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Creates CSV reports from events.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class CsvGenerator {

	private final DAO dao;

	public CsvGenerator(DAO dao) {
		this.dao = dao;
	}

	/** time, type, task, text */
	private final CellProcessor[] eventProcessors = new CellProcessor[] {
		new CellProcessorAdaptor() {
			@Override
			public Object execute(Object arg0, CsvContext arg1) {
				if (arg0 == null) {
					throw new IllegalStateException("event time may not be null");
				} else {
					return ((OffsetDateTime) arg0).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
				}
			}
		},
		new CellProcessorAdaptor() {
			@Override
			public Object execute(Object arg0, CsvContext arg1) {
				if (arg0 == null) {
					throw new IllegalStateException("event type may not be null");
				} else {
					return TypeEnum.byValue((Integer) arg0).getReadableName();
				}
			}
		},
		new CellProcessorAdaptor() {
			@Override
			public Object execute(Object arg0, CsvContext arg1) {
				if (arg0 == null) {
					return null;
				} else {
					Task task = dao.getTask((Integer) arg0);
					return task == null ? "" : task.getName();
				}
			}
		},
		new Optional()
	};

	/** task, spent */
	private final CellProcessor[] sumsProcessors = new CellProcessor[] {
		new NotNull(),
		new CellProcessorAdaptor() {
			@Override
			public Object execute(Object arg0, CsvContext arg1) {
				if (arg0 == null) {
					throw new IllegalStateException("time sum may not be null");
				} else {
					return arg0.toString();
				}
			}
		}
	};

	/** (month|week), task, spent */
	private final CellProcessor[] sumsPerRangeProcessors = new CellProcessor[] {
		new NotNull(),
		new NotNull(),
		new CellProcessorAdaptor() {
			@Override
			public Object execute(Object arg0, CsvContext arg1) {
				if (arg0 == null) {
					throw new IllegalStateException("time sum may not be null");
				} else {
					return arg0.toString();
				}
			}
		}
	};

	/**
	 * Warning: could modify the provided event list!
	 */
	public String createEventCsv(List<Event> events) {
		ICsvBeanWriter beanWriter = null;
		StringWriter resultWriter = new StringWriter();
		try {
			beanWriter = new CsvBeanWriter(resultWriter, CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE);

			// the header elements are used to map the bean values to each column (names must match!)
			final String[] header = new String[] { "time", "type", "task", "text" };

			beanWriter.writeHeader(header);

			for (Event event : events) {
				// "clock out" events shouldn't have a task and text:
				if (TypeEnum.byValue(event.getType()) == TypeEnum.CLOCK_OUT) {
					event.setTask(null);
					event.setText(null);
				}
				beanWriter.write(event, header, eventProcessors);
			}
		} catch (IOException e) {
			Logger.error(e, "error while writing");
		} finally {
			if (beanWriter != null) {
				try {
					beanWriter.close();
				} catch (IOException e) {
					// do nothing
				}
			}
		}
		return resultWriter.toString();
	}

	public String createSumsCsv(Map<Task, TimeSum> sums) {
		List<TimeSumsHolder> prepared = new LinkedList<>();
		for (Entry<Task, TimeSum> entry : sums.entrySet()) {
			String task = "";
			if (entry.getKey() != null) {
				task = entry.getKey().getName() + " (ID=" + entry.getKey().getId() + ")";
			}
			prepared.add(new TimeSumsHolder(null, null, null, task, entry.getValue()));
		}
		Collections.sort(prepared);

		return createCsv(prepared, new String[] { "task", "spent" }, sumsProcessors);
	}

	public String createSumsPerDayCsv(Map<ZonedDateTime, Map<Task, TimeSum>> sumsPerRange) {
		List<TimeSumsHolder> prepared = new LinkedList<>();
		for (Entry<ZonedDateTime, Map<Task, TimeSum>> rangeEntry : sumsPerRange.entrySet()) {
			String day = DateTimeUtil.dateToULString(rangeEntry.getKey());
			Map<Task, TimeSum> sums = rangeEntry.getValue();
			for (Entry<Task, TimeSum> entry : sums.entrySet()) {
				String task = "";
				if (entry.getKey() != null) {
					task = entry.getKey().getName() + " (ID=" + entry.getKey().getId() + ")";
				}
				prepared.add(TimeSumsHolder.createForDay(day, task, entry.getValue()));
			}
		}
		Collections.sort(prepared);

		return createCsv(prepared, new String[] { "day", "task", "spent" }, sumsPerRangeProcessors);
	}

	public String createSumsPerWeekCsv(Map<ZonedDateTime, Map<Task, TimeSum>> sumsPerRange) {
		List<TimeSumsHolder> prepared = new LinkedList<>();
		for (Entry<ZonedDateTime, Map<Task, TimeSum>> rangeEntry : sumsPerRange.entrySet()) {
			String week = DateTimeUtil.dateToULString(rangeEntry.getKey());
			Map<Task, TimeSum> sums = rangeEntry.getValue();
			for (Entry<Task, TimeSum> entry : sums.entrySet()) {
				String task = "";
				if (entry.getKey() != null) {
					task = entry.getKey().getName() + " (ID=" + entry.getKey().getId() + ")";
				}
				prepared.add(TimeSumsHolder.createForWeek(week, task, entry.getValue()));
			}
		}
		Collections.sort(prepared);

		return createCsv(prepared, new String[] { "week", "task", "spent" }, sumsPerRangeProcessors);
	}

	public String createSumsPerMonthCsv(Map<ZonedDateTime, Map<Task, TimeSum>> sumsPerRange) {
		List<TimeSumsHolder> prepared = new LinkedList<>();
		for (Entry<ZonedDateTime, Map<Task, TimeSum>> rangeEntry : sumsPerRange.entrySet()) {
			String month = DateTimeUtil.dateToULString(rangeEntry.getKey());
			Map<Task, TimeSum> sums = rangeEntry.getValue();
			for (Entry<Task, TimeSum> entry : sums.entrySet()) {
				String task = "";
				if (entry.getKey() != null) {
					task = entry.getKey().getName() + " (ID=" + entry.getKey().getId() + ")";
				}
				prepared.add(TimeSumsHolder.createForMonth(month, task, entry.getValue()));
			}
		}
		Collections.sort(prepared);

		return createCsv(prepared, new String[] { "month", "task", "spent" }, sumsPerRangeProcessors);
	}

	/**
	 * @param header
	 *            the header elements are used to map the bean values to each column (names must match!)
	 */
	private String createCsv(List<TimeSumsHolder> dataToWrite, String[] header, CellProcessor[] processors) {
		ICsvBeanWriter beanWriter = null;
		StringWriter resultWriter = new StringWriter();
		try {
			beanWriter = new CsvBeanWriter(resultWriter, CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE);

			beanWriter.writeHeader(header);

			for (TimeSumsHolder timeSumsHolder : dataToWrite) {
				beanWriter.write(timeSumsHolder, header, processors);
			}
		} catch (IOException e) {
			Logger.error(e, "error while writing");
		} finally {
			if (beanWriter != null) {
				try {
					beanWriter.close();
				} catch (IOException e) {
					// do nothing
				}
			}
		}
		return resultWriter.toString();
	}

}
