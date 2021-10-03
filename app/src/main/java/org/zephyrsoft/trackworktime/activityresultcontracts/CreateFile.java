package org.zephyrsoft.trackworktime.activityresultcontracts;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CreateFile extends ActivityResultContract<Void, Uri> {

	@NonNull
	private final String contentType;

	@NonNull
	private final String fileName;

	public CreateFile(@NonNull String fileName, @NonNull String contentType) {
		if (fileName.isEmpty()) {
			throw new IllegalArgumentException(String.format("Invalid file name %s", fileName));
		}
		this.fileName = fileName;

		if (contentType.isEmpty()) {
			throw new IllegalArgumentException(String.format("Invalid content type %s", contentType));
		}
		this.contentType = contentType;
	}

	@NonNull
	public Intent createIntent(@NonNull Context context, @NonNull Void input) {
		var intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
		intent.setType(contentType);
		intent.putExtra(Intent.EXTRA_TITLE, fileName);
		return intent;
	}

	@Override
	public Uri parseResult(int resultCode, @Nullable Intent intent) {
		if (intent == null || resultCode != Activity.RESULT_OK) return null;
		return intent.getData();
	}
}
