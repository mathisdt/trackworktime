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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;

import androidx.core.util.Consumer;
import androidx.documentfile.provider.DocumentFile;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.util.PermissionsUtil;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
        DocumentFile grantedDirectory = DocumentFile.fromTreeUri(context, Basics.get(context).getDocumentTree());
        if (grantedDirectory != null) {
            DocumentFile subDirectory = find(grantedDirectory, true, type.getSubdirectoryName());
            if (subDirectory != null) {
                DocumentFile file = find(subDirectory, false, filename);
                return file != null;
            }
        }
        return false;
    }

    public static void reading(Activity activity, Type type, String filename, Consumer<Reader> action) {
        DocumentFile grantedDirectory = DocumentFile.fromTreeUri(activity, Basics.get(activity).getDocumentTree());
        if (grantedDirectory != null) {
            DocumentFile subDirectory = find(grantedDirectory, true, type.getSubdirectoryName());
            if (subDirectory != null) {
                DocumentFile file = find(subDirectory, false, filename);
                try (ParcelFileDescriptor fileDescriptor = file == null ? null : activity.getContentResolver().openFileDescriptor(file.getUri(), "r");
                     InputStream inputStream = fileDescriptor == null ? null : new FileInputStream(fileDescriptor.getFileDescriptor());
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

    public static DocumentFile getForWriting(Activity activity, Type type, String filename) {
        DocumentFile grantedDirectory = DocumentFile.fromTreeUri(activity, Basics.get(activity).getDocumentTree());
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

    public static Uri writing(Activity activity, Type type, String filename, Consumer<OutputStream> action) {
        DocumentFile file = getForWriting(activity, type, filename);
        if (file != null) {
            try (ParcelFileDescriptor fileDescriptor = activity.getContentResolver().openFileDescriptor(file.getUri(), "rwt");
                 FileOutputStream fileOutputStream = fileDescriptor == null ? null : new FileOutputStream(fileDescriptor.getFileDescriptor());
                 BufferedOutputStream outputStream = fileDescriptor == null ? null : new BufferedOutputStream(fileOutputStream)) {
                Logger.debug("writing to {} {}", type, filename);
                action.accept(outputStream);
                if (outputStream != null) {
                    try {
                        outputStream.flush();
                    } catch (IOException ioe) {
                        Logger.debug("exception while flushing (probably stream already closed which is no problem): {}", ioe.getMessage());
                    }
                }
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

    public static boolean shouldRequestDirectoryGrant(Activity activity) {
        SharedPreferences prefs = Basics.get(activity).getPreferences();

        boolean noGrantAndNeverAsked = !prefs.contains(activity.getString(R.string.keyGrantedDocumentTree))
            && !prefs.contains(activity.getString(R.string.keyLastAskedForDocumentTree));
        boolean noGrantAndAskedLongAgo = !prefs.contains(activity.getString(R.string.keyGrantedDocumentTree))
            && prefs.contains(activity.getString(R.string.keyLastAskedForDocumentTree))
            && dateIsUnreadableOrOld(prefs.getString(activity.getString(R.string.keyLastAskedForDocumentTree), null));
        boolean grantPresentButInvalidAndAskedLongAgo = prefs.contains(activity.getString(R.string.keyGrantedDocumentTree))
            && !hasValidDirectoryGrant(activity)
            && dateIsUnreadableOrOld(prefs.getString(activity.getString(R.string.keyLastAskedForDocumentTree), null));

        return noGrantAndNeverAsked || noGrantAndAskedLongAgo || grantPresentButInvalidAndAskedLongAgo;
    }

    public static boolean hasValidDirectoryGrant(Activity activity) {
        String grantedDocumentTree = Basics.get(activity).getPreferences()
            .getString(activity.getString(R.string.keyGrantedDocumentTree), null);
        if (grantedDocumentTree == null) {
            return false;
        }
        try {
            Uri dir = Uri.parse(grantedDocumentTree);
            DocumentFile grantedDirectory = DocumentFile.fromTreeUri(activity, dir);
            if (grantedDirectory != null) {
                DocumentFile marker = findOrCreate(grantedDirectory, true,
                    Type.AUTOMATIC_BACKUP.getSubdirectoryName(), DocumentTreeStorage.Type.AUTOMATIC_BACKUP.getMimeType());
                return marker != null && marker.exists();
            } else {
                return false;
            }
        } catch (Exception e) {
            Logger.info(e, "access to storage " + grantedDocumentTree + " is not allowed");
            return false;
        }
    }

    private static boolean dateIsUnreadableOrOld(String dateString) {
        if (dateString == null) {
            return true;
        }
        try {
            LocalDate date = LocalDate.parse(dateString);
            return !LocalDate.now().minusDays(7).isBefore(date);
        } catch (Exception e) {
            return true;
        }
    }

    @SuppressLint("ApplySharedPref")
    public static void requestDirectoryGrant(Activity activity, int requestCode, int textResourceId, String... textParameters) {
        Logger.debug("showing explanation dialog for document tree permission");
        PermissionsUtil.askForDocumentTreePermission(activity,
            () -> {
                Logger.debug("document tree dialog confirmed, asking for permission");
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                String grantedDir = Basics.get(activity).getPreferences()
                    .getString(activity.getString(R.string.keyGrantedDocumentTree), null);
                if (grantedDir != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        Uri uri = Uri.parse(grantedDir);
                        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
                    } catch (Exception e) {
                        Logger.debug(e, "couldn't use old granted directory {}", grantedDir);
                    }
                }
                try {
                    activity.startActivityForResult(intent, requestCode);
                } catch (ActivityNotFoundException anf) {
                    Intent messageIntent = Basics.get(activity).createMessageIntent(
                        activity.getString(R.string.noFileManagerApp), null);
                    activity.startActivity(messageIntent);
                }
                // result is fetched e.g. in WorkTimeTrackerActivity.onActivityResult(...)
            }, () -> {
                Logger.debug("document tree dialog cancelled");
                final SharedPreferences.Editor editor =
                    Basics.get(activity).getPreferences().edit();
                editor.putString(activity.getString(R.string.keyLastAskedForDocumentTree),
                        LocalDate.now().toString());
                editor.commit();
            },
            textResourceId,
            textParameters);
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

    public static String getDirectoryName(Activity activity) {
        String grantedDocumentTree = Basics.get(activity).getPreferences()
            .getString(activity.getString(R.string.keyGrantedDocumentTree), null);
        if (grantedDocumentTree == null) {
            return "";
        } else {
            try {
                return URLDecoder.decode(grantedDocumentTree, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                // won't happen, but Java expects something as outcome
                throw new RuntimeException(e);
            }
        }
    }

}
