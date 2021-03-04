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
package org.zephyrsoft.trackworktime;

import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Html;
import android.text.util.Linkify;

import org.zephyrsoft.trackworktime.databinding.AboutBinding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;

/**
 * About dialog.
 * 
 * @see <a
 *      href="http://www.techrepublic.com/blog/app-builder/a-reusable-about-dialog-for-your-android-apps/504">blog&nbsp;post</a>
 * @author Mathis Dirksen-Thedens
 */
public class AboutActivity extends AppCompatActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AboutBinding binding = AboutBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		String aboutText = readRawTextFile(R.raw.about);
		CharSequence name = getApplicationContext().getResources().getText(R.string.app_name);
		CharSequence version = Basics.getInstance().getVersionName();

		CharSequence email = getApplicationContext().getResources().getText(R.string.email);
		aboutText = MessageFormat.format(aboutText, name, version, email);
		binding.aboutText.setText(Html.fromHtml(aboutText));
		binding.aboutText.setLinkTextColor(Color.WHITE);
		Linkify.addLinks(binding.aboutText, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);

		binding.helpText.setText(Html.fromHtml(readRawTextFile(R.raw.help)));
		binding.helpText.setLinkTextColor(Color.WHITE);
		Linkify.addLinks(binding.helpText, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
	}

	private String readRawTextFile(int id) {
		InputStream inputStream = getApplicationContext().getResources().openRawResource(id);
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
