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

import static android.view.View.NO_ID;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import org.pmw.tinylog.Logger;
import org.threeten.bp.ZonedDateTime;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.databinding.ReportsBinding;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.Range;
import org.zephyrsoft.trackworktime.model.Report;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.TimeSum;
import org.zephyrsoft.trackworktime.model.Unit;
import org.zephyrsoft.trackworktime.options.Key;
import org.zephyrsoft.trackworktime.report.CsvGenerator;
import org.zephyrsoft.trackworktime.report.ReportPreviewActivity;
import org.zephyrsoft.trackworktime.timer.TimeCalculator;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reports dialog.
 */
public class ReportsActivity extends AppCompatActivity {

	private ReportsBinding binding;

	private DAO dao;
	private TimeCalculator timeCalculator;
	private CsvGenerator csvGenerator;
	private SharedPreferences preferences;

	@Override
	protected void onPause() {
		super.onPause();
		saveSelectionState();
	}

	private void saveSelectionState() {
		preferences.edit()
				.putInt(Key.REPORT_LAST_RANGE.getName(), binding.range.getCheckedRadioButtonId())
				.putInt(Key.REPORT_LAST_UNIT.getName(), binding.unit.getCheckedRadioButtonId())
				.putInt(Key.REPORT_LAST_GROUPING.getName(), binding.grouping.getCheckedRadioButtonId())
				.apply();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		dao = Basics.getInstance().getDao();
		timeCalculator = Basics.getInstance().getTimeCalculator();
		csvGenerator = new CsvGenerator(dao);
		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		binding = ReportsBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		binding.rangeAllData.setOnCheckedChangeListener((buttonView, isChecked) -> {
			binding.unitWeek.setEnabled(!isChecked);
			binding.unitMonth.setEnabled(!isChecked);
			binding.unitYear.setEnabled(!isChecked);
		});

		binding.reportPreview.setOnClickListener(v -> preview());
		binding.reportExport.setOnClickListener(v -> export());

		restoreSelectionState();
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
			default:
				throw new IllegalArgumentException("options menu: unknown item selected");
		}
	}

	private void restoreSelectionState() {
		int rangeId = loadSelectedId(Key.REPORT_LAST_RANGE);
		checkRadioGroup(binding.range, rangeId);

		int unitId = loadSelectedId(Key.REPORT_LAST_UNIT);
		checkRadioGroup(binding.unit, unitId);

		int groupingId = loadSelectedId(Key.REPORT_LAST_GROUPING);
		checkRadioGroup(binding.grouping, groupingId);
	}

	private int loadSelectedId(Key key) {
		return preferences.getInt(key.getName(), NO_ID);
	}

	private void checkRadioGroup(RadioGroup group, int idToCheck) {
		if (idToCheck == NO_ID || group.findViewById(idToCheck) == null) {
			return;
		}
		group.check(idToCheck);
	}

	private void preview() {
		Report report;
		switch (binding.grouping.getCheckedRadioButtonId()) {
			case R.id.groupingNone:
				report = createReportForAllEvents();
				break;
			case R.id.groupingByTask:
				report = createReportForTimesByTask();
				break;
			case R.id.groupingByTaskPerDay:
				report = createReportForTimesByTaskPerDay();
				break;
			case R.id.groupingByTaskPerWeek:
				report = createReportForTimesByTaskPerWeek();
				break;
			case R.id.groupingByTaskPerMonth:
				report = createReportForTimesByTaskPerMonth();
				break;
			default:
				throw new RuntimeException("Grouping not implemented");
		}

		if (report == null) {
			return;
		}

		Intent intent = ReportPreviewActivity.createIntent(this, report);
		startActivity(intent);
	}

	private void export() {
		if (DocumentTreeStorage.hasValidDirectoryGrant(this)) {
			doExport();
		} else {
			DocumentTreeStorage.requestDirectoryGrant(this,
					R.string.documentTreePermissionsRequestTextOnUserAction,
					Constants.PERMISSION_REQUEST_CODE_DOCUMENT_TREE_ON_REPORT);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
		if (requestCode == Constants.PERMISSION_REQUEST_CODE_DOCUMENT_TREE_ON_REPORT
				&& resultCode == RESULT_OK) {
			if (intent != null) {
				DocumentTreeStorage.saveDirectoryGrant(this, intent);
				doExport();
			}
		} else {
			super.onActivityResult(requestCode, resultCode, intent);
		}
	}

	private void doExport() {
		switch (binding.grouping.getCheckedRadioButtonId()) {
			case R.id.groupingNone:
				exportAllEvents();
				break;
			case R.id.groupingByTask:
				exportTimesByTask();
				break;
			case R.id.groupingByTaskPerDay:
				exportTimesByTaskPerDay();
				break;
			case R.id.groupingByTaskPerWeek:
				exportTimesByTaskPerWeek();
				break;
			case R.id.groupingByTaskPerMonth:
				exportTimesByTaskPerMonth();
				break;
			default:
				throw new RuntimeException("Grouping not implemented");
		}
	}

	private void exportAllEvents() {
		Report report = createReportForAllEvents();
		if (report == null) {
			return;
		}

		String name = report.getName();
		String data = report.getData();
		boolean success = saveAndSendReport(name,
			"events-" + name.replaceAll(" ", "-"),
			data);

		if (success) {
			// close this dialog
			finish();
		}
	}

	private Report createReportForAllEvents() {
		Range selectedRange = getSelectedRange();
		Unit selectedUnit = getSelectedUnit();

		ZonedDateTime[] beginAndEnd = timeCalculator.calculateBeginAndEnd(selectedRange, selectedUnit);
		List<Event> events = dao.getEvents(beginAndEnd[0].toInstant(), beginAndEnd[1].toInstant());

		String report = csvGenerator.createEventCsv(events);
		String reportName = getNameForSelection(selectedRange, selectedUnit);
		if (report == null) {
			logAndShowError("could not generate report " + reportName);
			return null;
		}

		return new Report(reportName, report);
	}

	private void exportTimesByTask() {
		Report report = createReportForTimesByTask();
		if (report == null) {
			return;
		}

		String name = report.getName();
		String data = report.getData();
		boolean success = saveAndSendReport(name,
			"sums-" + name.replaceAll(" ", "-"),
			data);

		if (success) {
			// close this dialog
			finish();
		}
	}

	private Report createReportForTimesByTask() {
		Range selectedRange = getSelectedRange();
		Unit selectedUnit = getSelectedUnit();

		ZonedDateTime[] beginAndEnd = timeCalculator.calculateBeginAndEnd(selectedRange, selectedUnit);
		List<Event> events = dao.getEvents(beginAndEnd[0].toInstant(), beginAndEnd[1].toInstant());
		Map<Task, TimeSum> sums = timeCalculator.calculateSums(beginAndEnd[0].toOffsetDateTime(), beginAndEnd[1].toOffsetDateTime(), events);

		String report = csvGenerator.createSumsCsv(sums);
		String reportName = getNameForSelection(selectedRange, selectedUnit);
		if (report == null) {
			logAndShowError("could not generate report " + reportName);
			return null;
		}

		return new Report(reportName, report);
	}

	private void exportTimesByTaskPerDay() {
		Report report = createReportForTimesByTaskPerDay();
		if (report == null) {
			return;
		}

		String name = report.getName();
		String data = report.getData();
		boolean success = saveAndSendReport(name,
				"sums-per-day-" + name.replaceAll(" ", "-"),
				data);

		if (success) {
			// close this dialog
			finish();
		}
	}

	private Report createReportForTimesByTaskPerDay() {
		Range selectedRange = getSelectedRange();
		Unit selectedUnit = getSelectedUnit();

		ZonedDateTime[] beginAndEnd = timeCalculator.calculateBeginAndEnd(selectedRange, selectedUnit);
		List<ZonedDateTime> rangeBeginnings = timeCalculator.calculateRangeBeginnings(Unit.DAY, beginAndEnd[0],
				beginAndEnd[1]);
		Map<ZonedDateTime, Map<Task, TimeSum>> sumsPerRange = calculateSumsPerRange(rangeBeginnings, beginAndEnd[1]);

		String report = csvGenerator.createSumsPerDayCsv(sumsPerRange);
		String reportName = getNameForSelection(selectedRange, selectedUnit);
		if (report == null) {
			logAndShowError("could not generate report " + reportName);
			return null;
		}

		return new Report(reportName, report);
	}

	private void exportTimesByTaskPerWeek() {
		Report report = createReportForTimesByTaskPerWeek();
		if (report == null) {
			return;
		}

		String name = report.getName();
		String data = report.getData();
		boolean success = saveAndSendReport(name,
			"sums-per-week-" + name.replaceAll(" ", "-"),
			data);

		if (success) {
			// close this dialog
			finish();
		}
	}

	private Report createReportForTimesByTaskPerWeek() {
		Range selectedRange = getSelectedRange();
		Unit selectedUnit = getSelectedUnit();

		ZonedDateTime[] beginAndEnd = timeCalculator.calculateBeginAndEnd(selectedRange, selectedUnit);
		List<ZonedDateTime> rangeBeginnings = timeCalculator.calculateRangeBeginnings(Unit.WEEK, beginAndEnd[0],
				beginAndEnd[1]);
		Map<ZonedDateTime, Map<Task, TimeSum>> sumsPerRange = calculateSumsPerRange(rangeBeginnings, beginAndEnd[1]);

		String report = csvGenerator.createSumsPerWeekCsv(sumsPerRange);
		String reportName = getNameForSelection(selectedRange, selectedUnit);
		if (report == null) {
			logAndShowError("could not generate report " + reportName);
			return null;
		}

		return new Report(reportName, report);
	}

	private void exportTimesByTaskPerMonth() {
		Report report = createReportForTimesByTaskPerMonth();
		if (report == null) {
			return;
		}

		String name = report.getName();
		String data = report.getData();
		boolean success = saveAndSendReport(name,
			"sums-per-month-" + name.replaceAll(" ", "-"),
			data);

		if (success) {
			// close this dialog
			finish();
		}
	}

	private Report createReportForTimesByTaskPerMonth() {
		Range selectedRange = getSelectedRange();
		Unit selectedUnit = getSelectedUnit();

		ZonedDateTime[] beginAndEnd = timeCalculator.calculateBeginAndEnd(selectedRange, selectedUnit);
		List<ZonedDateTime> rangeBeginnings = timeCalculator.calculateRangeBeginnings(Unit.MONTH, beginAndEnd[0],
				beginAndEnd[1]);
		Map<ZonedDateTime, Map<Task, TimeSum>> sumsPerRange = calculateSumsPerRange(rangeBeginnings, beginAndEnd[1]);

		String report = csvGenerator.createSumsPerMonthCsv(sumsPerRange);
		String reportName = getNameForSelection(selectedRange, selectedUnit);
		if (report == null) {
			logAndShowError("could not generate report " + reportName);
			return null;
		}

		return new Report(reportName, report);
	}

	private Map<ZonedDateTime, Map<Task, TimeSum>> calculateSumsPerRange(List<ZonedDateTime> rangeBeginnings, ZonedDateTime end) {
		Map<ZonedDateTime, Map<Task, TimeSum>> sumsPerRange = new HashMap<>();

		for (int i = 0; i < rangeBeginnings.size(); i++) {
			ZonedDateTime rangeStart = rangeBeginnings.get(i);
			ZonedDateTime rangeEnd = (i >= rangeBeginnings.size() - 1 ? end : rangeBeginnings.get(i + 1));
			List<Event> events = dao.getEvents(rangeStart.toInstant(), rangeEnd.toInstant());
			Map<Task, TimeSum> sums = timeCalculator.calculateSums(rangeStart.toOffsetDateTime(), rangeEnd.toOffsetDateTime(), events);
			sumsPerRange.put(rangeStart, sums);
		}
		return sumsPerRange;
	}

	private void logAndShowError(String errorMessage) {
		Logger.error(errorMessage);
		Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
	}

	private Range getSelectedRange() {
		if (binding.rangeLast.isChecked()) {
			return Range.LAST;
		} else if (binding.rangeCurrent.isChecked()) {
			return Range.CURRENT;
		} else if (binding.rangeLastAndCurrent.isChecked()) {
			return Range.LAST_AND_CURRENT;
		} else if (binding.rangeAllData.isChecked()) {
			return Range.ALL_DATA;
		} else {
			throw new IllegalStateException("unknown range");
		}
	}

	private Unit getSelectedUnit() {
		if (binding.unitWeek.isChecked()) {
			return Unit.WEEK;
		} else if (binding.unitMonth.isChecked()) {
			return Unit.MONTH;
		} else if (binding.unitYear.isChecked()) {
			return Unit.YEAR;
		} else {
			throw new IllegalStateException("unknown unit");
		}
	}

	private String getNameForSelection(Range range, Unit unit) {
		return range == Range.ALL_DATA
            ? range.getName()
            : range.getName() + " " + unit.getName();
	}

	/**
	 * @param reportName readable name
	 * @param filePrefix file name without the extension ".csv"
	 * @param report report contents
	 */
	private boolean saveAndSendReport(String reportName, String filePrefix, String report) {
		String fileName = filePrefix.replaceAll(" ", "-") + ".csv";
		Uri reportUri = null;
		try {
			reportUri = DocumentTreeStorage.writing(this, DocumentTreeStorage.Type.REPORT,
					fileName,
					outputStream -> {
						try {
							outputStream.write(report.getBytes());
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					});
		} catch (Exception e) {
			String errorMessage = "could not write report " + fileName;
			Logger.error(errorMessage);
			Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
			return false;
		}

		// send the report
		Intent sendingIntent = new Intent(Intent.ACTION_SEND);
		sendingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Track Work Time Report");
		sendingIntent.putExtra(android.content.Intent.EXTRA_TEXT, "report time frame: " + reportName);
		sendingIntent.putExtra(Intent.EXTRA_STREAM, reportUri);
		sendingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		sendingIntent.setType("text/plain");
		startActivity(Intent.createChooser(sendingIntent, "Send report..."));

		return true;
	}

}
