package org.zephyrsoft.trackworktime.ui;

import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.zephyrsoft.trackworktime.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.android.timezonepicker.TimeZoneInfo;
import com.android.timezonepicker.TimeZonePickerDialog;

public class TimeZonePicker extends LinearLayout
		implements TimeZonePickerDialog.OnTimeZoneSetListener {

	private static final String FRAG_TAG_TIME_ZONE_PICKER = "timeZonePickerDialogFragment";

	private String timeZoneId = ZoneId.systemDefault().getId();

	private final TextView timeZone;

	public TimeZonePicker(Context context, AttributeSet attrs) {
		super(context, attrs);

		setOrientation(LinearLayout.HORIZONTAL);
		setGravity(Gravity.CENTER);

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.view_timezone_select, this, true);

		timeZone = (TextView) getChildAt(0);
		timeZone.setText(timeZoneId);

		ImageButton editTimeZone = (ImageButton) getChildAt(1);
		editTimeZone.setOnClickListener(v -> showTimeZoneDialog());
	}

	public TimeZonePicker(Context context) {
		this(context, null);
	}

	private void showTimeZoneDialog() {

		if (getContext() instanceof Activity) {
			Bundle b = new Bundle();
			b.putLong(TimeZonePickerDialog.BUNDLE_START_TIME_MILLIS, System.currentTimeMillis());
			b.putString(TimeZonePickerDialog.BUNDLE_TIME_ZONE, ZonedDateTime.now().getZone().toString());

			FragmentManager fm = ((AppCompatActivity) getContext()).getSupportFragmentManager();
			TimeZonePickerDialog tzpd = (TimeZonePickerDialog) fm.findFragmentByTag(FRAG_TAG_TIME_ZONE_PICKER);

			if (tzpd != null) {
				tzpd.dismiss();
			}

			tzpd = new TimeZonePickerDialog();
			tzpd.setArguments(b);
			tzpd.setOnTimeZoneSetListener(this);
			tzpd.show(fm, FRAG_TAG_TIME_ZONE_PICKER);
		}
	}

	@Override
	public void onTimeZoneSet(TimeZoneInfo tzi) {
		timeZoneId = tzi.mTzId;
		timeZone.setText(timeZoneId);
	}

	public void setZoneIdFromOffset(ZoneOffset zoneOffset) {
		timeZoneId = ZoneId.ofOffset("UTC", zoneOffset).getId();
		timeZone.setText(timeZoneId);
	}

	public void setZoneId(ZoneId zoneId) {
		timeZoneId = zoneId.getId();
		timeZone.setText(timeZoneId);
	}

	public ZoneId getZoneId() {
		return ZoneId.of(timeZoneId);
	}
}
