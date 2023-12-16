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
import static org.zephyrsoft.trackworktime.util.DateTimeUtil.truncateEventsToMinute;

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

import org.apache.commons.lang3.StringUtils;
import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.databinding.ReportsBinding;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.Range;
import org.zephyrsoft.trackworktime.model.Report;
import org.zephyrsoft.trackworktime.model.Target;
import org.zephyrsoft.trackworktime.model.TargetWrapper;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.report.TaskAndHint;
import org.zephyrsoft.trackworktime.model.TimeSum;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.model.Unit;
import org.zephyrsoft.trackworktime.options.Key;
import org.zephyrsoft.trackworktime.report.CsvGenerator;
import org.zephyrsoft.trackworktime.report.ReportPreviewActivity;
import org.zephyrsoft.trackworktime.timer.TimeCalculator;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
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

		dao = Basics.get(this).getDao();
		timeCalculator = Basics.get(this).getTimeCalculator();
		csvGenerator = new CsvGenerator(dao, this);
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
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		throw new IllegalArgumentException("options menu: unknown item selected");
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
			case R.id.groupingByTaskAndHint:
				report = createReportForTimesByTaskAndHint();
				break;
			case R.id.groupingByTaskPerDay:
				report = createReportForTimesByTaskPerDay();
				break;
			case R.id.groupingByTaskAndHintPerDay:
				report = createReportForTimesByTaskAndHintPerDay();
				break;
			case R.id.groupingByTaskPerWeek:
				report = createReportForTimesByTaskPerWeek();
				break;
			case R.id.groupingByTaskAndHintPerWeek:
				report = createReportForTimesByTaskAndHintPerWeek();
				break;
			case R.id.groupingByTaskPerMonth:
				report = createReportForTimesByTaskPerMonth();
				break;
			case R.id.groupingByTaskAndHintPerMonth:
				report = createReportForTimesByTaskAndHintPerMonth();
				break;
			case R.id.targetGroupingNone:
				report = createReportForAllTargets();
				break;
			case R.id.targetGroupingPerWeek:
				report = createReportForTargetsDaysPerTypeAndCommentPerWeek();
				break;
			case R.id.targetGroupingPerMonth:
				report = createReportForTargetsDaysPerTypeAndCommentPerMonth();
				break;
			default:
				throw new RuntimeException("Grouping " + binding.grouping.getCheckedRadioButtonId() + " not implemented");
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
                Constants.PERMISSION_REQUEST_CODE_DOCUMENT_TREE_ON_REPORT,
                R.string.documentTreePermissionsRequestTextOnUserAction);
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
			case R.id.groupingByTaskAndHint:
				exportTimesByTaskAndHint();
				break;
			case R.id.groupingByTaskPerDay:
				exportTimesByTaskPerDay();
				break;
			case R.id.groupingByTaskAndHintPerDay:
				exportTimesByTaskAndHintPerDay();
				break;
			case R.id.groupingByTaskPerWeek:
				exportTimesByTaskPerWeek();
				break;
			case R.id.groupingByTaskAndHintPerWeek:
				exportTimesByTaskAndHintPerWeek();
				break;
			case R.id.groupingByTaskPerMonth:
				exportTimesByTaskPerMonth();
				break;
			case R.id.groupingByTaskAndHintPerMonth:
				exportTimesByTaskAndHintPerMonth();
				break;
			case R.id.targetGroupingNone:
				exportAllTargets();
				break;
			case R.id.targetGroupingPerWeek:
				exportTargetsDaysPerTypeAndCommentPerWeek();
				break;
			case R.id.targetGroupingPerMonth:
				exportTargetsDaysPerTypeAndCommentPerMonth();
				break;
			default:
				throw new RuntimeException("Grouping " + binding.grouping.getCheckedRadioButtonId() + " not implemented");
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
		truncateEventsToMinute(events);

		String report = csvGenerator.createEventCsv(events);
		String reportName = describeTimeRange(beginAndEnd);
		if (report == null) {
			logAndShowError(reportName);
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
		truncateEventsToMinute(events);
		Map<Task, TimeSum> sums = timeCalculator.calculateSums(beginAndEnd[0].toOffsetDateTime(), beginAndEnd[1].toOffsetDateTime(), events);

		String report = csvGenerator.createSumsCsv(sums);
		String reportName = describeTimeRange(beginAndEnd);
		if (report == null) {
			logAndShowError(reportName);
			return null;
		}

		return new Report(reportName, report);
	}

	private void exportTimesByTaskAndHint() {
		Report report = createReportForTimesByTaskAndHint();
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

	private Report createReportForTimesByTaskAndHint() {
		Range selectedRange = getSelectedRange();
		Unit selectedUnit = getSelectedUnit();

		ZonedDateTime[] beginAndEnd = timeCalculator.calculateBeginAndEnd(selectedRange, selectedUnit);
		List<Event> events = dao.getEvents(beginAndEnd[0].toInstant(), beginAndEnd[1].toInstant());
		truncateEventsToMinute(events);
		Map<TaskAndHint, TimeSum> sums = timeCalculator.calculateSumsPerTaskAndHint(beginAndEnd[0].toOffsetDateTime(), beginAndEnd[1].toOffsetDateTime(), events);

		String report = csvGenerator.createSumsCsvWithHints(sums);
		String reportName = describeTimeRange(beginAndEnd);
		if (report == null) {
			logAndShowError(reportName);
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
		String reportName = describeTimeRange(beginAndEnd);
		if (report == null) {
			logAndShowError(reportName);
			return null;
		}

		return new Report(reportName, report);
	}

	private void exportTimesByTaskAndHintPerDay() {
		Report report = createReportForTimesByTaskAndHintPerDay();
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

	private Report createReportForTimesByTaskAndHintPerDay() {
		Range selectedRange = getSelectedRange();
		Unit selectedUnit = getSelectedUnit();

		ZonedDateTime[] beginAndEnd = timeCalculator.calculateBeginAndEnd(selectedRange, selectedUnit);
		List<ZonedDateTime> rangeBeginnings = timeCalculator.calculateRangeBeginnings(Unit.DAY, beginAndEnd[0],
				beginAndEnd[1]);
		Map<ZonedDateTime, Map<TaskAndHint, TimeSum>> sumsPerRange = calculateSumsWithHintsPerRange(rangeBeginnings, beginAndEnd[1]);

		String report = csvGenerator.createSumsWithHintsPerDayCsv(sumsPerRange);
		String reportName = describeTimeRange(beginAndEnd);
		if (report == null) {
			logAndShowError(reportName);
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

		String report = csvGenerator.createSumsPerWeekCsv(sumsPerRange,
			new String[] { "week", "task", "spent" },
			task -> task.getName() + " (ID=" + task.getId() + ")");
		String reportName = describeTimeRange(beginAndEnd);
		if (report == null) {
			logAndShowError(reportName);
			return null;
		}

		return new Report(reportName, report);
	}

	private void exportTimesByTaskAndHintPerWeek() {
		Report report = createReportForTimesByTaskAndHintPerWeek();
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

	private Report createReportForTimesByTaskAndHintPerWeek() {
		Range selectedRange = getSelectedRange();
		Unit selectedUnit = getSelectedUnit();

		ZonedDateTime[] beginAndEnd = timeCalculator.calculateBeginAndEnd(selectedRange, selectedUnit);
		List<ZonedDateTime> rangeBeginnings = timeCalculator.calculateRangeBeginnings(Unit.WEEK, beginAndEnd[0],
				beginAndEnd[1]);
		Map<ZonedDateTime, Map<TaskAndHint, TimeSum>> sumsPerRange = calculateSumsWithHintsPerRange(rangeBeginnings, beginAndEnd[1]);

		String report = csvGenerator.createSumsWithHintsPerWeeksCsv(sumsPerRange,
				new String[] { "week", "task", "text", "spent" },
				taskAndHint -> taskAndHint.getTask().getName() + " (ID=" + taskAndHint.getTask().getId() + ")",
				taskAndHint -> taskAndHint.getText());
		String reportName = describeTimeRange(beginAndEnd);
		if (report == null) {
			logAndShowError(reportName);
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

		String report = csvGenerator.createSumsPerMonthCsv(sumsPerRange,
			new String[] { "month", "task", "spent" },
			task -> task.getName() + " (ID=" + task.getId() + ")");
		String reportName = describeTimeRange(beginAndEnd);
		if (report == null) {
			logAndShowError(reportName);
			return null;
		}

		return new Report(reportName, report);
	}

	private void exportTimesByTaskAndHintPerMonth() {
		Report report = createReportForTimesByTaskAndHintPerMonth();
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

	private Report createReportForTimesByTaskAndHintPerMonth() {
		Range selectedRange = getSelectedRange();
		Unit selectedUnit = getSelectedUnit();

		ZonedDateTime[] beginAndEnd = timeCalculator.calculateBeginAndEnd(selectedRange, selectedUnit);
		List<ZonedDateTime> rangeBeginnings = timeCalculator.calculateRangeBeginnings(Unit.MONTH, beginAndEnd[0],
				beginAndEnd[1]);
		Map<ZonedDateTime, Map<TaskAndHint, TimeSum>> sumsPerRange = calculateSumsWithHintsPerRange(rangeBeginnings, beginAndEnd[1]);

		String report = csvGenerator.createSumsWithHintsPerMonthCsv(sumsPerRange,
				new String[] { "month", "task", "text", "spent" },
				taskAndHint -> taskAndHint.getTask().getName() + " (ID=" + taskAndHint.getTask().getId() + ")",
				taskAndHint -> taskAndHint.getText());
		String reportName = describeTimeRange(beginAndEnd);
		if (report == null) {
			logAndShowError(reportName);
			return null;
		}

		return new Report(reportName, report);
	}

	private void exportAllTargets() {
		Report report = createReportForAllTargets();
		if (report == null) {
			return;
		}

		String name = report.getName();
		String data = report.getData();
		boolean success = saveAndSendReport(name,
			"targets-" + name.replaceAll(" ", "-"),
			data);

		if (success) {
			// close this dialog
			finish();
		}
	}

	private Report createReportForAllTargets() {
		Range selectedRange = getSelectedRange();
		Unit selectedUnit = getSelectedUnit();

		ZonedDateTime[] beginAndEnd = timeCalculator.calculateBeginAndEnd(selectedRange, selectedUnit);
		List<Target> targets = dao.getTargets(beginAndEnd[0].toInstant(), beginAndEnd[1].toInstant());

		String report = csvGenerator.createTargetCsv(targets);
		String reportName = describeTimeRange(beginAndEnd);
		if (report == null) {
			logAndShowError(reportName);
			return null;
		}

		return new Report(reportName, report);
	}

	private Report createReportForTargetsDaysPerTypeAndCommentPerWeek() {
		Range selectedRange = getSelectedRange();
		Unit selectedUnit = getSelectedUnit();

		ZonedDateTime[] beginAndEnd = timeCalculator.calculateBeginAndEnd(selectedRange, selectedUnit);
		List<ZonedDateTime> rangeBeginnings = timeCalculator.calculateRangeBeginnings(Unit.WEEK, beginAndEnd[0], beginAndEnd[1]);
		Map<ZonedDateTime, Map<String, Integer>> sumsPerRange = calculateTargetSumsPerRange(rangeBeginnings, beginAndEnd[1]);

		String report = csvGenerator.createDayCountPerWeekCsv(sumsPerRange,
			new String[] { "week", "target", "days" });
		String reportName = describeTimeRange(beginAndEnd);
		if (report == null) {
			logAndShowError(reportName);
			return null;
		}

		return new Report(reportName, report);
	}

	private void exportTargetsDaysPerTypeAndCommentPerWeek() {
		Report report = createReportForTargetsDaysPerTypeAndCommentPerWeek();
		if (report == null) {
			return;
		}

		String name = report.getName();
		String data = report.getData();
		boolean success = saveAndSendReport(name,
			"targets-per-week-" + name.replaceAll(" ", "-"),
			data);

		if (success) {
			// close this dialog
			finish();
		}
	}

	private Report createReportForTargetsDaysPerTypeAndCommentPerMonth() {
		Range selectedRange = getSelectedRange();
		Unit selectedUnit = getSelectedUnit();

		ZonedDateTime[] beginAndEnd = timeCalculator.calculateBeginAndEnd(selectedRange, selectedUnit);
		List<ZonedDateTime> rangeBeginnings = timeCalculator.calculateRangeBeginnings(Unit.MONTH, beginAndEnd[0], beginAndEnd[1]);
		Map<ZonedDateTime, Map<String, Integer>> sumsPerRange = calculateTargetSumsPerRange(rangeBeginnings, beginAndEnd[1]);

		String report = csvGenerator.createDayCountPerMonthCsv(sumsPerRange,
			new String[] { "month", "target", "days" });
		String reportName = describeTimeRange(beginAndEnd);
		if (report == null) {
			logAndShowError(reportName);
			return null;
		}

		return new Report(reportName, report);
	}

	private void exportTargetsDaysPerTypeAndCommentPerMonth() {
		Report report = createReportForTargetsDaysPerTypeAndCommentPerMonth();
		if (report == null) {
			return;
		}

		String name = report.getName();
		String data = report.getData();
		boolean success = saveAndSendReport(name,
			"targets-per-month-" + name.replaceAll(" ", "-"),
			data);

		if (success) {
			// close this dialog
			finish();
		}
	}

	private Map<ZonedDateTime, Map<Task, TimeSum>> calculateSumsPerRange(List<ZonedDateTime> rangeBeginnings, ZonedDateTime end) {
		Map<ZonedDateTime, Map<Task, TimeSum>> sumsPerRange = new HashMap<>();

		for (int i = 0; i < rangeBeginnings.size(); i++) {
			ZonedDateTime rangeStart = rangeBeginnings.get(i);
			ZonedDateTime rangeEnd = (i >= rangeBeginnings.size() - 1 ? end : rangeBeginnings.get(i + 1));
			List<Event> events = dao.getEvents(rangeStart.toInstant(), rangeEnd.toInstant());
			ZonedDateTime now = ZonedDateTime.now();
			if (rangeStart.isBefore(now) && rangeEnd.isAfter(now)
				&& !events.isEmpty()
				&& events.get(events.size() - 1).getTypeEnum() == TypeEnum.CLOCK_IN
				&& events.get(events.size() - 1).getDateTime().isBefore(now.toOffsetDateTime())) {
				events.add(new Event(null, null, TypeEnum.CLOCK_OUT_NOW.getValue(), now.toOffsetDateTime(), null));
			}
			truncateEventsToMinute(events);
			Map<Task, TimeSum> sums = timeCalculator.calculateSums(rangeStart.toOffsetDateTime(), rangeEnd.toOffsetDateTime(), events);
			sumsPerRange.put(rangeStart, sums);
		}
		return sumsPerRange;
	}

	private Map<ZonedDateTime, Map<TaskAndHint, TimeSum>> calculateSumsWithHintsPerRange(List<ZonedDateTime> rangeBeginnings, ZonedDateTime end) {
		Map<ZonedDateTime, Map<TaskAndHint, TimeSum>> sumsPerRange = new HashMap<>();

		for (int i = 0; i < rangeBeginnings.size(); i++) {
			ZonedDateTime rangeStart = rangeBeginnings.get(i);
			ZonedDateTime rangeEnd = (i >= rangeBeginnings.size() - 1 ? end : rangeBeginnings.get(i + 1));
			List<Event> events = dao.getEvents(rangeStart.toInstant(), rangeEnd.toInstant());
			ZonedDateTime now = ZonedDateTime.now();
			if (rangeStart.isBefore(now) && rangeEnd.isAfter(now)
					&& !events.isEmpty()
					&& events.get(events.size() - 1).getTypeEnum() == TypeEnum.CLOCK_IN
					&& events.get(events.size() - 1).getDateTime().isBefore(now.toOffsetDateTime())) {
				events.add(new Event(null, null, TypeEnum.CLOCK_OUT_NOW.getValue(), now.toOffsetDateTime(), null));
			}
			truncateEventsToMinute(events);
			Map<TaskAndHint, TimeSum> sums = timeCalculator.calculateSumsPerTaskAndHint(rangeStart.toOffsetDateTime(), rangeEnd.toOffsetDateTime(), events);
			sumsPerRange.put(rangeStart, sums);
		}
		return sumsPerRange;
	}

	private Map<ZonedDateTime, Map<String, Integer>> calculateTargetSumsPerRange(List<ZonedDateTime> rangeBeginnings, ZonedDateTime end) {
		Map<ZonedDateTime, Map<String, Integer>> sumsPerRange = new HashMap<>();

		for (int i = 0; i < rangeBeginnings.size(); i++) {
			ZonedDateTime rangeStart = rangeBeginnings.get(i);
			ZonedDateTime rangeEnd = (i >= rangeBeginnings.size() - 1 ? end : rangeBeginnings.get(i + 1));
			List<Target> targets = dao.getTargets(rangeStart.toInstant(), rangeEnd.toInstant());
			Map<String, Integer> sums = new HashMap<>();
			for (Target target : targets) {
				String key = TargetWrapper.getType(target, this)
					+ (StringUtils.isBlank(target.getComment()) ? "" : " - " + target.getComment());
				if (!sums.containsKey(key)) {
					sums.put(key, 0);
				}
				sums.put(key, sums.get(key) + 1);
			}
			sumsPerRange.put(rangeStart, sums);
		}
		return sumsPerRange;
	}

	private void logAndShowError(String reportName) {
		Logger.error("could not generate report " + reportName);
		Toast.makeText(this, getString(R.string.reportGenerationError, reportName), Toast.LENGTH_LONG).show();
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

	private String describeTimeRange(ZonedDateTime[] beginAndEnd) {
		return getString(R.string.timeframe_description,
			DateTimeUtil.dateToULString(beginAndEnd[0]),
			DateTimeUtil.dateToULString(beginAndEnd[1].minusSeconds(1)));
	}

	/**
	 * @param reportName readable name for time frame
	 * @param filePrefix file name without the extension ".csv"
	 * @param report report contents
	 */
	private boolean saveAndSendReport(String reportName, String filePrefix, String report) {
		String fileName = filePrefix.replaceAll(" ", "-") + ".csv";
		Uri reportUri;
		try {
			reportUri = DocumentTreeStorage.writing(this, DocumentTreeStorage.Type.REPORT,
					fileName,
					outputStream -> {
						try {
							outputStream.write(report.getBytes(StandardCharsets.UTF_8));
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					});
		} catch (Exception e) {
			Logger.error("could not write report " + fileName);
			Toast.makeText(this, getString(R.string.reportWritingError, fileName), Toast.LENGTH_LONG).show();
			return false;
		}

		// send the report
		Intent sendingIntent = new Intent(Intent.ACTION_SEND);
		sendingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.reportTitle));
		sendingIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.reportTimeFrame, reportName));
		sendingIntent.putExtra(Intent.EXTRA_STREAM, reportUri);
		sendingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		sendingIntent.setType("text/plain");
		startActivity(Intent.createChooser(sendingIntent, getString(R.string.sendReport)));

		return true;
	}

}
