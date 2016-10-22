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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Properties;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.util.Linkify;
import android.widget.TextView;

import org.zephyrsoft.trackworktime.util.Logger;

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

		setContentView(R.layout.about);

		TextView about = (TextView) findViewById(R.id.about_text);
		String aboutText = readRawTextFile(R.raw.about);
		CharSequence name = getApplicationContext().getResources().getText(R.string.app_name);
		CharSequence version = Basics.getInstance().getVersionName();

		CharSequence website = getApplicationContext().getResources().getText(R.string.website);
		CharSequence email = getApplicationContext().getResources().getText(R.string.email);
		aboutText = MessageFormat.format(aboutText, name, version, website, email);
		about.setText(Html.fromHtml(aboutText));
		about.setLinkTextColor(Color.WHITE);
		Linkify.addLinks(about, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);

		TextView help = (TextView) findViewById(R.id.help_text);
		help.setText(Html.fromHtml(readRawTextFile(R.raw.help)));
		help.setLinkTextColor(Color.WHITE);
	}

	private String readRawTextFile(int id) {
		InputStream inputStream = getApplicationContext().getResources().openRawResource(id);
		InputStreamReader in = new InputStreamReader(inputStream);
		BufferedReader buf = new BufferedReader(in);
		String line;
		StringBuilder text = new StringBuilder();
		try {
			while ((line = buf.readLine()) != null) {
				text.append(line);
			}
		} catch (IOException e) {
			return null;
		}
		return text.toString();
	}
}
