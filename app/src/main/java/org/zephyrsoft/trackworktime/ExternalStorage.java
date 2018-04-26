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

import android.content.Context;
import android.os.Environment;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Can manage directories and files on external storage.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class ExternalStorage {

	public static File getDirectory(String subDirectory, Context context) {
		File externalStorageDirectory = context.getExternalFilesDir(null);
		if (!isExternalStorageWritable()) {
			Logger.error("external storage {} is not writable", externalStorageDirectory);
			return null;
		}
		File twtDirectory = new File(externalStorageDirectory, "trackworktime");
		if (!twtDirectory.isDirectory() && !twtDirectory.mkdirs()) {
			Logger.error("directory {} could not be created", twtDirectory);
			return null;
		}
		File targetDirectory;
		if (subDirectory != null && subDirectory.length() > 0) {
			targetDirectory = new File(twtDirectory, subDirectory);
		} else {
			targetDirectory = twtDirectory;
		}
		if (!targetDirectory.isDirectory() && !targetDirectory.mkdirs()) {
			Logger.error("directory {} could not be created", targetDirectory);
			return null;
		}
		if (!targetDirectory.canWrite()) {
			Logger.error("directory {} is not writable", targetDirectory);
			return null;
		}
		return targetDirectory;
	}

	public static File writeFile(String subDirectory, String fileNamePrefix, String fileNameSuffix, byte[] fileContent, Context context) {
		File targetDirectory = getDirectory(subDirectory, context);
		if (targetDirectory == null) {
			Logger.error("target {} is not writable", targetDirectory);
			return null;
		}
		String timeStamp = DateTimeUtil.getCurrentDateTime().format("YYYY-MM-DD-hh-mm-ss");
		String fileName = fileNamePrefix + "-generated-at-" + timeStamp + fileNameSuffix;
		File file = new File(targetDirectory, fileName);
		return writeFile(fileContent, file);
	}

	private static File writeFile(byte[] fileContent, File file) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			fos.write(fileContent);
		} catch (Exception e) {
			Logger.error("file {} could not be written", file);
			return null;
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					// do nothing
				}
			}
		}
		return file;
	}

	private static boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		return Environment.MEDIA_MOUNTED.equals(state);
	}

}
