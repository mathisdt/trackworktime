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

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.supercsv.cellprocessor.CellProcessorAdaptor;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.util.CsvContext;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.TypeEnum;

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
	private final CellProcessor[] processors = new CellProcessor[] {
		new CellProcessorAdaptor() {
			@Override
			public Object execute(Object arg0, CsvContext arg1) {
				if (arg0 == null) {
					throw new IllegalStateException("event time may not be null");
				} else {
					return ((String) arg0).replaceAll(":\\d\\d\\.\\d\\d\\d\\d$", "");
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

	public String createCsv(List<Event> events) {
		ICsvBeanWriter beanWriter = null;
		StringWriter resultWriter = new StringWriter();
		try {
			beanWriter = new CsvBeanWriter(resultWriter, CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE);

			// the header elements are used to map the bean values to each column (names must match!)
			final String[] header = new String[] { "time", "type", "task", "text" };

			beanWriter.writeHeader(header);

			for (Event event : events) {
				beanWriter.write(event, header, processors);
			}

		} catch (IOException e) {
			e.printStackTrace();
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
