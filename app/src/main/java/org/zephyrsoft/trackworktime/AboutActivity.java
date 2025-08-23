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

import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.util.Linkify;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.zephyrsoft.trackworktime.databinding.AboutBinding;
import org.zephyrsoft.trackworktime.util.ThemeUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;

/**
 * About dialog.
 */
public class AboutActivity extends AppCompatActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AboutBinding binding = AboutBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		ThemeUtil.styleActionBar(this, getSupportActionBar());

		String aboutText = readRawTextFile(R.raw.about);
		String name = getString(R.string.app_name);
		String version = Basics.get(this).getVersionName();

		String email = getString(R.string.email);
		aboutText = MessageFormat.format(aboutText, name, version, email);
		binding.aboutText.setText(Html.fromHtml(aboutText));
		binding.aboutText.setLinkTextColor(Color.WHITE);
		Linkify.addLinks(binding.aboutText, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);

		binding.helpText.setText(Html.fromHtml(readRawTextFile(R.raw.help)));
		binding.helpText.setLinkTextColor(Color.WHITE);
		Linkify.addLinks(binding.helpText, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		throw new IllegalArgumentException("options menu: unknown item selected");
	}

	private String readRawTextFile(int id) {
		InputStream inputStream = getResources().openRawResource(id);
		InputStreamReader in = new InputStreamReader(inputStream);
		BufferedReader buf = new BufferedReader(in);
		String line;
		StringBuilder text = new StringBuilder();
		try {
			while ((line = buf.readLine()) != null) {
				text.append(line).append(' ');
			}
		} catch (IOException e) {
			return null;
		}
		return text.toString();
	}
}
