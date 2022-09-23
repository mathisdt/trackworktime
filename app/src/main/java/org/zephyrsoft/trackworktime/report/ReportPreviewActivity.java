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
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.R;
import org.zephyrsoft.trackworktime.databinding.ReportPreviewBinding;
import org.zephyrsoft.trackworktime.model.Report;

public class ReportPreviewActivity extends AppCompatActivity {

	private static final String EXTRA_REPORT = "report";

	private ReportPreviewBinding binding;

	public static Intent createIntent(@NonNull Context context, @NonNull Report report) {
		Intent intent = new Intent(context, ReportPreviewActivity.class);
		intent.putExtra(EXTRA_REPORT, report);
		return intent;
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = DataBindingUtil.setContentView(this, R.layout.report_preview);
		setTitle();

		Report report = getReport();
		loadReport(report);
	}

	private Report getReport() {
		return (Report) getIntent().getSerializableExtra(EXTRA_REPORT);
	}

	private void loadReport(Report report) {
		setContent(report.getData());
	}

	private void setTitle() {
		ActionBar bar = getSupportActionBar();
		if (bar == null) {
			Logger.error("Action bar was null");
			return;
		}
		bar.setTitle(R.string.report_preview);
		bar.setDisplayHomeAsUpEnabled(true);
	}

	private void setContent(String content) {
		binding.setData(content);
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

}
