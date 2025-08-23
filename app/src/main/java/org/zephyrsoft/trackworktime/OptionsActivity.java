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

import static androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.backup.WorkTimeTrackerBackupManager;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.databinding.OptionsActivityBinding;
import org.zephyrsoft.trackworktime.options.CheckIntervalPreference;
import org.zephyrsoft.trackworktime.options.CheckIntervalPreferenceDialogFragment;
import org.zephyrsoft.trackworktime.options.DurationPreference;
import org.zephyrsoft.trackworktime.options.DurationPreferenceDialogFragment;
import org.zephyrsoft.trackworktime.options.Key;
import org.zephyrsoft.trackworktime.options.TimePreference;
import org.zephyrsoft.trackworktime.options.TimePreferenceDialogFragment;
import org.zephyrsoft.trackworktime.options.TimeZonePreference;
import org.zephyrsoft.trackworktime.options.TimeZonePreferenceDialogFragment;
import org.zephyrsoft.trackworktime.util.PermissionsUtil;
import org.zephyrsoft.trackworktime.util.PreferencesUtil;
import org.zephyrsoft.trackworktime.util.ThemeUtil;

import java.text.DateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Activity to set the preferences of the application.
 */
public class OptionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OptionsActivityBinding binding = OptionsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.options_view, new SettingsFragment())
                .commit();
        }

        ThemeUtil.styleActionBar(this, getSupportActionBar());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        throw new IllegalArgumentException("options menu: unknown item selected");
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements OnSharedPreferenceChangeListener {

        private final ActivityResultLauncher<String[]> postNotificationRequest = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                // we only get here on API 33 and above
                List<String> ungranted = PermissionsUtil.notGrantedPermissions(result);
                if (!ungranted.isEmpty()) {
                    notificationPermissionNotGranted();
                }
            });
        private final ActivityResultLauncher<String[]> backgroundLocationRequest = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                // we only get here on API 29 and above
                List<String> ungranted = PermissionsUtil.notGrantedPermissions(result);
                if (ungranted.isEmpty()) {
                    allPermissionsInPlace();
                } else {
                    locationPermissionNotGranted();
                }
            });
        private final ActivityResultLauncher<String[]> locationRequest = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                List<String> ungranted = PermissionsUtil.notGrantedPermissions(result);
                if (ungranted.isEmpty()) {
                    if (getActivity() != null
                        && PermissionsUtil.isBackgroundPermissionMissing(getContext())) {
                        backgroundLocationRequest.launch(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION});
                    } else {
                        allPermissionsInPlace();
                    }
                } else {
                    locationPermissionNotGranted();
                }
            });

        private WorkTimeTrackerBackupManager backupManager;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.options, rootKey);

            backupManager = new WorkTimeTrackerBackupManager(requireContext());
            setTimestamps();
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            DialogFragment dialogFragment = null;

            if (preference instanceof TimePreference) {
                dialogFragment = new TimePreferenceDialogFragment();

            } else if (preference instanceof DurationPreference) {
                dialogFragment = new DurationPreferenceDialogFragment();

            } else if (preference instanceof CheckIntervalPreference) {
                dialogFragment = new CheckIntervalPreferenceDialogFragment();

            } else if (preference instanceof TimeZonePreference) {
                dialogFragment = new TimeZonePreferenceDialogFragment();
            }

            if (dialogFragment != null) {
                Bundle bundle = new Bundle(1);
                bundle.putString("key", preference.getKey());
                dialogFragment.setArguments(bundle);
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(getParentFragmentManager(), null);
            } else {
                super.onDisplayPreferenceDialog(preference);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onStop() {
            backupManager.checkIfBackupEnabledChanged();
            super.onStop();
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

            // make sure that location-based tracking gets enabled/disabled
            Basics.get(getActivity()).safeCheckLocationBasedTracking();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String keyName) {
            Key sectionToDisable = PreferencesUtil.check(sharedPreferences, keyName);
            if (PreferencesUtil.getBooleanPreference(sharedPreferences, sectionToDisable)) {
                Logger.warn("option {} is invalid => disabling option {}", keyName, sectionToDisable.getName());

                // show message to user
                Intent messageIntent = Basics
                    .get(getActivity())
                    .createMessageIntent(
                        getString(R.string.disabledDueToInvalidSettings, getString(sectionToDisable.getReadableNameResourceId())),
                        null);

                startActivity(messageIntent);
                Logger.debug("Disabling section {}", keyName);

                // deactivate the section
                PreferencesUtil.disablePreference(sharedPreferences, sectionToDisable);
                // reload data in options view
                setPreferenceScreen(null);
                addPreferencesFromResource(R.xml.options);
            } else {
                if ((Key.LOCATION_BASED_TRACKING_ENABLED.getName().equals(keyName)
                    || Key.WIFI_BASED_TRACKING_ENABLED.getName().equals(keyName))
                    && sharedPreferences.getBoolean(keyName, false)
                    && getActivity() != null) {

                    Set<String> missingPermissions = PermissionsUtil.missingPermissionsForTracking(getContext());
                    if (!missingPermissions.isEmpty()) {
                        Logger.debug("asking for permissions: {}", missingPermissions);
                        PermissionsUtil.askForLocationPermission(getActivity(),
                            () -> {
                                locationRequest.launch(missingPermissions.toArray(new String[]{}));
                            },
                            this::locationPermissionNotGranted);
                    } else if (PermissionsUtil.isBackgroundPermissionMissing(getContext())) {
                        Logger.debug("asking for permission ACCESS_BACKGROUND_LOCATION");
                        PermissionsUtil.askForLocationPermission(getActivity(),
                            () -> backgroundLocationRequest.launch(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}),
                            this::locationPermissionNotGranted);
                    }
                } else if (Key.NOTIFICATION_ENABLED.getName().equals(keyName)
                    && sharedPreferences.getBoolean(keyName, false)
                    && getActivity() != null
                    && PermissionsUtil.isNotificationPermissionMissing(getActivity())) {
                    Logger.debug("asking for permission POST_NOTIFICATIONS");

                    postNotificationRequest.launch(new String[]{Manifest.permission.POST_NOTIFICATIONS});

                } else if (Key.DECIMAL_TIME_SUMS.getName().equals(keyName)
                    && WorkTimeTrackerActivity.getInstanceOrNull() != null) {
                    WorkTimeTrackerActivity.getInstanceOrNull().redrawWeekTable();
                } else if (Key.NEVER_UPDATE_PERSISTENT_NOTIFICATION.getName().equals(keyName)) {
                    Basics.get(getActivity()).fixPersistentNotification();
                }
            }

            // reset cache if preference changes time calculation
            Key key = Key.getKeyWithName(keyName);
            if (key != null && (
                key.equals(Key.HOME_TIME_ZONE) ||
                    key.equals(Key.ENABLE_FLEXI_TIME) ||
                    Key.ENABLE_FLEXI_TIME.equals(key.getParent())
            )) {
                Basics.get(getActivity()).getTimerManager().invalidateCacheFrom((LocalDate) null);
            }

            int nightMode = Integer.parseInt(sharedPreferences.getString(getString(R.string.keyNightMode), "2"));
            setDefaultNightMode(nightMode);
        }

        private void allPermissionsInPlace() {
            reloadData();
            Intent messageIntent = Basics.get(getActivity())
                .createMessageIntent(getString(R.string.locationPermissionsGranted), null);
            startActivity(messageIntent);
        }

        private void locationPermissionNotGranted() {
            PreferencesUtil.disableAutomaticTracking(getActivity());

            Intent messageIntent = Basics.get(getActivity())
                .createMessageIntent(getString(R.string.locationPermissionsUngranted), null);
            startActivity(messageIntent);

            reloadData();
        }

        private void notificationPermissionNotGranted() {
            final SharedPreferences.Editor editor = getPreferenceScreen().getSharedPreferences().edit();
            editor.putBoolean(Key.NOTIFICATION_ENABLED.getName(), false);
            editor.apply();

            Intent messageIntent = Basics.get(getActivity())
                .createMessageIntent(getString(R.string.notification_permissions_ungranted), null);
            startActivity(messageIntent);

            reloadData();
        }

        private void reloadData() {
            setPreferenceScreen(null);
            addPreferencesFromResource(R.xml.options);
        }

        private void setTimestamps() {
            final Preference lastModifiedPref = findPreference(getString(R.string.keyBackupLastModifiedTimestamp));
            final Preference lastBackupPref = findPreference(getString(R.string.keyBackupLastBackupTimestamp));
            if (lastModifiedPref == null || lastBackupPref == null) {
                Logger.warn("backup timestamps preference not found!");
                return;
            }
            final DAO dao = new DAO(requireContext());
            final long lastDbModification = dao.getLastDbModification();

            final DateFormat dateFormatUser = DateFormat.getDateInstance();
            final DateFormat timeFormatUser = DateFormat.getTimeInstance();

            final Date dateLocal = new Date(lastDbModification);
            final String dateLocalStr = dateFormatUser.format(dateLocal) + " "
                + timeFormatUser.format(dateLocal);
            lastModifiedPref.setSummary(dateLocalStr);

            final long dateBackupLong = backupManager.getLastBackupTimestamp();
            final String dateBackupStr;
            if (dateBackupLong == 0) {
                dateBackupStr = "-";
            } else {
                final Date dateBackup = new Date(dateBackupLong);
                dateBackupStr = dateFormatUser.format(dateBackup) + " "
                    + timeFormatUser.format(dateBackup);
            }

            lastBackupPref.setSummary(dateBackupStr);
            showTimestampPrefIcon(lastBackupPref, dateLocalStr, dateBackupStr);
        }

        private void showTimestampPrefIcon(final Preference timestampPref, final String dateLocalStr, final String dateBackupStr) {
            if (dateLocalStr.equals(dateBackupStr)) {
                timestampPref.setIcon(R.drawable.backup_ok);
            } else {
                timestampPref.setIcon(R.drawable.backup_not_ok);
            }
        }
    }
}
