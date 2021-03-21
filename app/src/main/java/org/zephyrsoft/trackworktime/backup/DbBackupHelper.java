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
package org.zephyrsoft.trackworktime.backup;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import android.annotation.TargetApi;
import android.app.backup.BackupDataInputStream;
import android.app.backup.BackupDataOutput;
import android.app.backup.BackupHelper;
import android.content.Context;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.database.DAO;

/**
 * @author Peter Rosenberg
 */
@TargetApi(Build.VERSION_CODES.FROYO)
public class DbBackupHelper implements BackupHelper {
	private static final String KEY_EVENTS = "db_events";
	private static final String KEY_TARGETS = "db_targets";
	private static final String KEY_OLD = "db_key";

	private final Context context;
	private final WorkTimeTrackerBackupManager backupManager;

	public DbBackupHelper(Context context) {
		this.context = context;
		this.backupManager = new WorkTimeTrackerBackupManager(context);
	}

	@Override
	public void performBackup(final ParcelFileDescriptor oldState, final BackupDataOutput data,
		final ParcelFileDescriptor newState) {
		// delete backup if not enabled
		if (!backupManager.isEnabled()) {
			try {
				// delete existing data if any
				data.writeEntityHeader(KEY_OLD, -1);
				data.writeEntityHeader(KEY_EVENTS, -1);
				data.writeEntityHeader(KEY_TARGETS, -1);

				writeNewState(0, newState);
			} catch (IOException e) {
				// ignored, delete data next time
			}
			return;
		}

		// Get the oldState input stream
		final FileInputStream instream = new FileInputStream(oldState.getFileDescriptor());
		final DataInputStream in = new DataInputStream(instream);

		// Get the last modified timestamp from the state file and data file
		long stateModified = -1;
		try {
			stateModified = in.readLong();
			in.close();
		} catch (IOException e1) {
			// Unable to read state file... be safe and do a backup
		}
		final DAO dao = new DAO(context);
		long fileModified = dao.getLastDbModification();

		if (stateModified != fileModified) {
			ByteArrayOutputStream byteSteam = new ByteArrayOutputStream();

			try {
				{
					// Events
					final Writer writer = new OutputStreamWriter(byteSteam);
					dao.backupEventsToWriter(writer);
					writer.close();

					data.writeEntityHeader(KEY_EVENTS, byteSteam.size());
					data.writeEntityData(byteSteam.toByteArray(), byteSteam.size());
					byteSteam.reset();
				}
				{
					// Targets
					final Writer writer = new OutputStreamWriter(byteSteam);
					dao.backupTargetsToWriter(writer);
					writer.close();

					data.writeEntityHeader(KEY_TARGETS, byteSteam.size());
					data.writeEntityData(byteSteam.toByteArray(), byteSteam.size());
					byteSteam.close();
				}
			} catch (IOException e) {
				Logger.warn(e, "problem while creating backup");
			}
		}

		writeNewState(dao.getLastDbModification(), newState);
		dao.close();
	}

	private void writeNewState(final long dbFileModification, final ParcelFileDescriptor newState) {
		// write to newState
		final FileOutputStream newStateOS = new FileOutputStream(newState.getFileDescriptor());
		final DataOutputStream newStateDataOS = new DataOutputStream(newStateOS);
		try {
			newStateDataOS.writeLong(dbFileModification);
			new WorkTimeTrackerBackupManager(context).setLastBackupTimestamp(dbFileModification);
			newStateDataOS.close();
		} catch (IOException e) {
			// error on writing the newState, ignored
		}
	}

	@Override
	public void restoreEntity(final BackupDataInputStream data) {
		final DAO dao = new DAO(context);

		if (KEY_OLD.equals(data.getKey()) || KEY_EVENTS.equals(data.getKey())) {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(data));

			try {
				dao.restoreEventsFromReader(reader);
			} catch (IOException e) {
				Logger.warn(e, "problem while restoring events");
			}

		} else if (KEY_TARGETS.equals(data.getKey())) {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(data));

			try {
				dao.restoreTargetsFromReader(reader);
			} catch (IOException e) {
				Logger.warn(e, "problem while restoring targets");
			}
		}

		dao.close();
	}

	@Override
	public void writeNewStateDescription(final ParcelFileDescriptor newState) {
		final DAO dao = new DAO(context);

		// write to newState
		final FileOutputStream newStateOS = new FileOutputStream(newState.getFileDescriptor());
		final DataOutputStream newStateDataOS = new DataOutputStream(newStateOS);
		try {
			newStateDataOS.writeLong(dao.getLastDbModification());
			newStateDataOS.close();
		} catch (IOException e) {
			// error on writing the newState, ignored
		}

		dao.close();
	}

}
