package org.zephyrsoft.trackworktime.util;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtil {

	public static void copy(Context context, File from, Uri to) throws IOException {
		try(
				InputStream in = new FileInputStream(from);
				OutputStream out = context.getContentResolver().openOutputStream(to);
		) {
			copy(in, out);
		} catch(IOException e) {
			String msg = "Failed to copy file from " + from + " to " + to;
			throw new IOException(msg, e);
		}
	}

	public static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while((read = in.read(buffer)) != -1) {
			out.write(buffer, 0, read);
		}
	}
}
