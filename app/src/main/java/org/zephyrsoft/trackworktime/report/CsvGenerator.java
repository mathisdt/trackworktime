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
package org.zephyrsoft.trackworktime.report;

import android.content.Context;

import androidx.arch.core.util.Function;

import org.pmw.tinylog.Logger;
import org.supercsv.cellprocessor.CellProcessorAdaptor;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.util.CsvContext;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.Target;
import org.zephyrsoft.trackworktime.model.TargetWrapper;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.TimeSum;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Creates CSV reports from events.
 */
public class CsvGenerator {

	private final DAO dao;
	private final Context context;

	public CsvGenerator(DAO dao, Context context) {
		this.dao = dao;
		this.context = context;
	}

	/** time, type, task, text */
	@SuppressWarnings("unchecked")
	private CellProcessor[] getEventProcessors() {
		return new CellProcessor[]{
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
						return TypeEnum.byValue((Integer) arg0).getReadableName(context);
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
	}

	/**
	 * date, type, value, comment
	 */
	@SuppressWarnings("unchecked")
	private CellProcessor[] getTargetProcessors() {
		return new CellProcessor[]{
			new CellProcessorAdaptor() {
				@Override
				public Object execute(Object arg0, CsvContext arg1) {
					if (arg0 == null) {
						throw new IllegalStateException("target date may not be null");
					} else {
						return ((LocalDate) arg0).format(DateTimeFormatter.ISO_LOCAL_DATE);
					}
				}
			},
			new CellProcessorAdaptor() {
				@Override
				public Object execute(Object arg0, CsvContext arg1) {
					if (arg0 == null) {
						throw new IllegalStateException("target type may not be null");
					} else {
						return arg0;
					}
				}
			},
			new CellProcessorAdaptor() {
				@Override
				public Object execute(Object arg0, CsvContext arg1) {
					if (arg0 == null || (arg0 instanceof Integer && ((Integer) arg0) == 0)) {
						return null;
					} else if (arg0 instanceof Integer) {
						return DateTimeUtil.formatDuration((Integer) arg0);
					} else {
						return null;
					}
				}
			},
			new Optional()
		};
	}

	/** task, spent */
	@SuppressWarnings("unchecked")
	private CellProcessor[] getSumsProcessors() {
		return new CellProcessor[] {
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
	}

	/** task, text, spent */
	@SuppressWarnings("unchecked")
	private CellProcessor[] getSumsAndHintsProcessors() {
		return new CellProcessor[] {
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
	}

	/** (day|month|week), task, spent */
	@SuppressWarnings("unchecked")
	private CellProcessor[] getSumsPerRangeProcessors() {
		return new CellProcessor[]{
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
	}

	/** (day|month|week), task, text, spent */
	@SuppressWarnings("unchecked")
	private CellProcessor[] getSumsPerRangeWithHintsProcessors() {
		return new CellProcessor[]{
				new NotNull(),
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
	}

	/**
	 * Warning: could modify the provided event list!
	 */
	public String createTargetCsv(List<Target> targets) {
		ICsvBeanWriter beanWriter = null;
		StringWriter resultWriter = new StringWriter();
		try {
			beanWriter = new CsvBeanWriter(resultWriter, CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE);

			// the header elements are used to map the bean values to each column (names must match!)
			final String[] header = new String[] { "date", "type", "value", "comment" };

			beanWriter.writeHeader(header);

			CellProcessor[] targetProcessors = getTargetProcessors();
			for (Target target : targets) {
				beanWriter.write(new TargetWrapper(target), header, targetProcessors);
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

			CellProcessor[] eventProcessors = getEventProcessors();
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
		List<TimeSumsHolder> prepared = new ArrayList<>();
		for (Entry<Task, TimeSum> entry : sums.entrySet()) {
			String task = "";
			if (entry.getKey() != null) {
				task = entry.getKey().getName() + " (ID=" + entry.getKey().getId() + ")";
			}
			prepared.add(new TimeSumsHolder(null, null, null, task, entry.getValue()));
		}
		Collections.sort(prepared);

		return createCsv(prepared, new String[] { "task", "spent" }, getSumsProcessors());
	}

	public String createSumsCsvWithHints(Map<TaskAndHint, TimeSum> sums) {
		List<TimeSumsAndHintsHolder> prepared = new ArrayList<>();
		for (Entry<TaskAndHint, TimeSum> entry : sums.entrySet()) {
			String task = "";
			String hint = "";
			if (entry.getKey() != null) {
				if (entry.getKey().getTask() != null) {
					task = entry.getKey().getTask().getName() + " (ID=" + entry.getKey().getTask().getId() + ")";
				}
				if (entry.getKey().getText() != null) {
					hint = entry.getKey().getText();
				}
			}
			prepared.add(new TimeSumsAndHintsHolder(null, null, null, task, hint, entry.getValue()));
		}
		Collections.sort(prepared);

		return createCsv(prepared, new String[] { "task", "text", "spent" }, getSumsAndHintsProcessors());
	}

	public String createSumsPerDayCsv(Map<ZonedDateTime, Map<Task, TimeSum>> sumsPerRange) {
		List<TimeSumsHolder> prepared = new ArrayList<>();
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

		return createCsv(prepared, new String[] { "day", "task", "spent" }, getSumsPerRangeProcessors());
	}

	public String createSumsWithHintsPerDayCsv(Map<ZonedDateTime, Map<TaskAndHint, TimeSum>> sumsPerRange) {
		List<TimeSumsAndHintsHolder> prepared = new ArrayList<>();
		for (Entry<ZonedDateTime, Map<TaskAndHint, TimeSum>> rangeEntry : sumsPerRange.entrySet()) {
			String day = DateTimeUtil.dateToULString(rangeEntry.getKey());
			Map<TaskAndHint, TimeSum> sums = rangeEntry.getValue();
			for (Entry<TaskAndHint, TimeSum> entry : sums.entrySet()) {
				String task = "";
				String hint = "";
				if (entry.getKey() != null) {
					if (entry.getKey().getTask() != null) {
						task = entry.getKey().getTask().getName() + " (ID=" + entry.getKey().getTask().getId() + ")";
					}
					if (entry.getKey().getText() != null) {
						hint = entry.getKey().getText();
					}
				}
				prepared.add(TimeSumsAndHintsHolder.createForDay(day, task, hint, entry.getValue()));
			}
		}
		Collections.sort(prepared);

		return createCsv(prepared, new String[] { "day", "task", "text", "spent" }, getSumsPerRangeWithHintsProcessors());
	}

	public <T> String createSumsPerWeekCsv(Map<ZonedDateTime, Map<T, TimeSum>> sumsPerRange,
										   String[] header, Function<T, String> extractor) {
		List<TimeSumsHolder> prepared = new ArrayList<>();
		for (Entry<ZonedDateTime, Map<T, TimeSum>> rangeEntry : sumsPerRange.entrySet()) {
			String week = DateTimeUtil.dateToULString(rangeEntry.getKey());
			Map<T, TimeSum> sums = rangeEntry.getValue();
			for (Entry<T, TimeSum> entry : sums.entrySet()) {
				String key = "";
				if (entry.getKey() != null) {
					key = extractor.apply(entry.getKey());
				}
				prepared.add(TimeSumsHolder.createForWeek(week, key, entry.getValue()));
			}
		}
		Collections.sort(prepared);

		return createCsv(prepared, header, getSumsPerRangeProcessors());
	}

	public String createSumsWithHintsPerWeeksCsv(Map<ZonedDateTime, Map<TaskAndHint, TimeSum>> sumsPerRange,
												 String[] header, Function<TaskAndHint, String> keyExtractor,
												 Function<TaskAndHint, String> hintExtractor) {
		List<TimeSumsAndHintsHolder> prepared = new ArrayList<>();
		for (Entry<ZonedDateTime, Map<TaskAndHint, TimeSum>> rangeEntry : sumsPerRange.entrySet()) {
			String week = DateTimeUtil.dateToULString(rangeEntry.getKey());
			Map<TaskAndHint, TimeSum> sums = rangeEntry.getValue();
			for (Entry<TaskAndHint, TimeSum> entry : sums.entrySet()) {
				String key = "";
				String hint = "";
				if (entry.getKey() != null) {
					if (entry.getKey().getTask() != null) {
						key = keyExtractor.apply(entry.getKey());
					}
					if (entry.getKey().getText() != null) {
						hint = hintExtractor.apply(entry.getKey());
					}
				}
				prepared.add(TimeSumsAndHintsHolder.createForWeek(week, key, hint, entry.getValue()));
			}
		}
		Collections.sort(prepared);

		return createCsv(prepared, header, getSumsPerRangeWithHintsProcessors());
	}

	public String createDayCountPerWeekCsv(Map<ZonedDateTime, Map<String, Integer>> sumsPerRange, String[] header) {
		List<TargetDaysHolder> prepared = new ArrayList<>();
		for (Entry<ZonedDateTime, Map<String, Integer>> rangeEntry : sumsPerRange.entrySet()) {
			String week = DateTimeUtil.dateToULString(rangeEntry.getKey());
			Map<String, Integer> sums = rangeEntry.getValue();
			for (Entry<String, Integer> entry : sums.entrySet()) {
				prepared.add(TargetDaysHolder.createForWeek(week, entry.getKey(), entry.getValue()));
			}
		}
		Collections.sort(prepared);

		return createCsv(prepared, header, getSumsPerRangeProcessors());
	}

	public <T> String createSumsPerMonthCsv(Map<ZonedDateTime, Map<T, TimeSum>> sumsPerRange,
											String[] header, Function<T, String> extractor) {
		List<TimeSumsHolder> prepared = new ArrayList<>();
		for (Entry<ZonedDateTime, Map<T, TimeSum>> rangeEntry : sumsPerRange.entrySet()) {
			String month = DateTimeUtil.dateToULString(rangeEntry.getKey());
			Map<T, TimeSum> sums = rangeEntry.getValue();
			for (Entry<T, TimeSum> entry : sums.entrySet()) {
				String task = "";
				if (entry.getKey() != null) {
					task = extractor.apply(entry.getKey());
				}
				prepared.add(TimeSumsHolder.createForMonth(month, task, entry.getValue()));
			}
		}
		Collections.sort(prepared);

		return createCsv(prepared, header, getSumsPerRangeProcessors());
	}

	public String createSumsWithHintsPerMonthCsv(Map<ZonedDateTime, Map<TaskAndHint, TimeSum>> sumsPerRange,
											String[] header, Function<TaskAndHint, String> keyExtractor,
										Function<TaskAndHint, String> hintExtractor) {
		List<TimeSumsAndHintsHolder> prepared = new ArrayList<>();
		for (Entry<ZonedDateTime, Map<TaskAndHint, TimeSum>> rangeEntry : sumsPerRange.entrySet()) {
			String month = DateTimeUtil.dateToULString(rangeEntry.getKey());
			Map<TaskAndHint, TimeSum> sums = rangeEntry.getValue();
			for (Entry<TaskAndHint, TimeSum> entry : sums.entrySet()) {
				String task = "";
				String hint = "";
				if (entry.getKey() != null) {
					if (entry.getKey().getTask() != null) {
						task = keyExtractor.apply(entry.getKey());
					}
					if (entry.getKey().getText() != null) {
						hint = hintExtractor.apply(entry.getKey());
					}
				}
				prepared.add(TimeSumsAndHintsHolder.createForMonth(month, task, hint, entry.getValue()));
			}
		}
		Collections.sort(prepared);

		return createCsv(prepared, header, getSumsPerRangeWithHintsProcessors());
	}

	public String createDayCountPerMonthCsv(Map<ZonedDateTime, Map<String, Integer>> sumsPerRange, String[] header) {
		List<TargetDaysHolder> prepared = new ArrayList<>();
		for (Entry<ZonedDateTime, Map<String, Integer>> rangeEntry : sumsPerRange.entrySet()) {
			String month = DateTimeUtil.dateToULString(rangeEntry.getKey());
			Map<String, Integer> sums = rangeEntry.getValue();
			for (Entry<String, Integer> entry : sums.entrySet()) {
				prepared.add(TargetDaysHolder.createForMonth(month, entry.getKey(), entry.getValue()));
			}
		}
		Collections.sort(prepared);

		return createCsv(prepared, header, getSumsPerRangeProcessors());
	}

	/**
	 * @param header
	 *            the header elements are used to map the bean values to each column (names must match!)
	 */
	private String createCsv(List<?> dataToWrite, String[] header, CellProcessor[] processors) {
		ICsvBeanWriter beanWriter = null;
		StringWriter resultWriter = new StringWriter();
		try {
			beanWriter = new CsvBeanWriter(resultWriter, CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE);

			beanWriter.writeHeader(header);

			for (Object dataElement : dataToWrite) {
				beanWriter.write(dataElement, header, processors);
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
