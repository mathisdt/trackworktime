package org.zephyrsoft.trackworktime.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtil {

	public static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while((read = in.read(buffer)) != -1) {
			out.write(buffer, 0, read);
		}
	}
}
