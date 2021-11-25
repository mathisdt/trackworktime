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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.storage.StorageManager;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;

import androidx.core.util.Consumer;
import androidx.documentfile.provider.DocumentFile;

import org.pmw.tinylog.Logger;
import org.threeten.bp.LocalDate;
import org.zephyrsoft.trackworktime.util.PermissionsUtil;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Objects;

/**
 * Can manage directories and files in a document tree granted by the user.
 */
public class DocumentTreeStorage {

    public enum Type {
        MANUAL_BACKUP("twt_backup", "text/comma-separated-values"),
        AUTOMATIC_BACKUP("twt_automatic_backup", "text/comma-separated-values"),
        REPORT("twt_report", "text/comma-separated-values"),
        LOGFILE("twt_log", "text/plain");

        private final String subdirectoryName;
        private final String mimeType;

        Type(String subdirectoryName, String mimeType) {
            this.subdirectoryName = subdirectoryName;
            this.mimeType = mimeType;
        }

        public String getSubdirectoryName() {
            return subdirectoryName;
        }

        public String getMimeType() {
            return mimeType;
        }
    }

    public static boolean exists(Context context, Type type, String filename) {
        DocumentFile grantedDirectory = DocumentFile.fromTreeUri(context, Basics.getOrCreateInstance(context).getDocumentTree());
        if (grantedDirectory != null) {
            DocumentFile subDirectory = find(grantedDirectory, true, type.getSubdirectoryName());
            if (subDirectory != null) {
                DocumentFile file = find(subDirectory, false, filename);
                return file != null;
            }
        }
        return false;
    }

    public static void reading(Context context, Type type, String filename, Consumer<Reader> action) {
        DocumentFile grantedDirectory = DocumentFile.fromTreeUri(context, Basics.getOrCreateInstance(context).getDocumentTree());
        if (grantedDirectory != null) {
            DocumentFile subDirectory = find(grantedDirectory, true, type.getSubdirectoryName());
            if (subDirectory != null) {
                DocumentFile file = find(subDirectory, false, filename);
                try (ParcelFileDescriptor fileDescriptor = context.getContentResolver().openFileDescriptor(file.getUri(), "r");
                     InputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
                     Reader reader = new InputStreamReader(inputStream)) {
                    Logger.debug("reading from {} {}", type, filename);
                    action.accept(reader);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                Logger.info("couldn't read from {} {} - couldn't find subdirectory", type, filename);
            }
        } else {
            Logger.info("couldn't read from {} {} - no granted directory", type, filename);
        }
    }

    public static DocumentFile getForWriting(Context context, Type type, String filename) {
        DocumentFile grantedDirectory = DocumentFile.fromTreeUri(context, Basics.getOrCreateInstance(context).getDocumentTree());
        if (grantedDirectory != null) {
            DocumentFile subDirectory = findOrCreate(grantedDirectory, true, type.getSubdirectoryName(), type.getMimeType());
            if (subDirectory != null) {
                return findOrCreate(subDirectory, false, filename, type.getMimeType());
            } else {
                Logger.warn("couldn't get {} {} for writing - couldn't find or create subdirectory", type, filename);
            }
        } else {
            Logger.warn("couldn't get {} {} for writing - no granted directory", type, filename);
        }
        return null;
    }

    public static Uri writing(Context context, Type type, String filename, Consumer<OutputStream> action) {
        DocumentFile file = getForWriting(context, type, filename);
        if (file != null) {
            try (ParcelFileDescriptor fileDescriptor = context.getContentResolver().openFileDescriptor(file.getUri(), "w");
                 OutputStream outputStream = new FileOutputStream(fileDescriptor.getFileDescriptor())) {
                Logger.debug("writing to {} {}", type, filename);
                action.accept(outputStream);
                return file.getUri();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            Logger.warn("couldn't write to {} {} - couldn't find or create grant-directory or subdirectory", type, filename);
        }
        return null;
    }

    private static DocumentFile find(DocumentFile base, boolean isDirectory, String name) {
        for (DocumentFile docFile : base.listFiles()) {
            if (docFile.isDirectory() == isDirectory && Objects.equals(docFile.getName(), name)) {
                return docFile;
            }
        }
        return null;
    }

    private static DocumentFile findOrCreate(DocumentFile base, boolean isDirectory, String name, String mimeType) {
        DocumentFile result = find(base, isDirectory, name);
        if (result == null && isDirectory) {
            result = base.createDirectory(name);
        } else if (result == null) {
            result = base.createFile(mimeType, name);
        }
        return result;
    }

    public static boolean hasDirectoryGrant(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.contains(context.getString(R.string.keyGrantedDocumentTree));
    }

    public static boolean shouldRequestDirectoryGrant(Context context, SharedPreferences prefs) {
        return (!prefs.contains(context.getString(R.string.keyGrantedDocumentTree))
                && !prefs.contains(context.getString(R.string.keyLastAskedForDocumentTree)))
                || (!prefs.contains(context.getString(R.string.keyGrantedDocumentTree))
                && prefs.contains(context.getString(R.string.keyLastAskedForDocumentTree))
                && dateIsUnreadableOrOld(prefs.getString(context.getString(R.string.keyLastAskedForDocumentTree), null)));
    }

    private static boolean dateIsUnreadableOrOld(String dateString) {
        try {
            LocalDate date = LocalDate.parse(dateString);
            return !LocalDate.now().minusDays(7).isBefore(date);
        } catch (Exception e) {
            return true;
        }
    }

    @SuppressLint("ApplySharedPref")
    public static void requestDirectoryGrant(Activity activity, int textResourceId, int requestCode) {
        Logger.debug("showing explanation dialog for document tree permission");
        PermissionsUtil.askForDocumentTreePermission(activity,
            textResourceId,
            () -> {
                Logger.debug("document tree dialog confirmed, asking for permission");
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // initially select "Documents" directory
                    StorageManager sm = (StorageManager) activity.getSystemService(Context.STORAGE_SERVICE);
                    Intent temp = sm.getPrimaryStorageVolume().createOpenDocumentTreeIntent();
                    Uri uri = temp.getParcelableExtra(DocumentsContract.EXTRA_INITIAL_URI);
                    String scheme = uri.toString();
                    scheme = scheme.replace("/root/", "/document/");
                    scheme += "%3ADocuments";
                    uri = Uri.parse(scheme);
                    temp.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
                }
                activity.startActivityForResult(intent, requestCode);
                // result is fetched in WorkTimeTrackerActivity.onActivityResult(...)
            }, () -> {
                Logger.debug("document tree dialog cancelled");
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
                final SharedPreferences.Editor editor = prefs.edit();
                editor.putString(activity.getString(R.string.keyLastAskedForDocumentTree),
                        LocalDate.now().toString());
                editor.commit();
            });
    }

    @SuppressLint({"WrongConstant", "ApplySharedPref"})
    public static void saveDirectoryGrant(Activity activity, Intent intent) {
        Uri uri = intent.getData();
        Logger.debug("document tree permission granted for {}", uri);
        if (uri != null) {
            final int takeFlags = intent.getFlags()
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            activity.getContentResolver().takePersistableUriPermission(uri, takeFlags);

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putString(activity.getString(R.string.keyGrantedDocumentTree), uri.toString());
            editor.remove(activity.getString(R.string.keyLastAskedForDocumentTree));
            editor.commit();
        }
    }

}
