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
package org.zephyrsoft.trackworktime.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.pmw.tinylog.Logger;
import org.threeten.bp.LocalDateTime;
import org.zephyrsoft.trackworktime.util.PermissionsUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Class responsible for retrieving wifi {@link ScanResult}s.
 *
 * Before use, you must register this {@link BroadcastReceiver} with {@link #register(Context)}.
 *
 * For requesting wifi {@link ScanResult}s, call {@link #requestWifiScanResults()}.
 *
 * Listen for wifi {@link ScanResult}s, by registering {@link WifiScanListener} with
 * {@link #setWifiScanListener(WifiScanListener)}.
 *
 * Notes:
 * This class receives wifi {@link ScanResult}s every time ANY APP on the phone initiates scanning.
 * We can take advantage of this and cache results (in {@link #latestScanResults}), to minimize
 * scan requests and lower battery drain.
 */
public class WifiScanner extends BroadcastReceiver {
	@NonNull private final WifiManager wifiManager;
	/** Max scan age [sec]. How old scan results are still considered "good enough". */
	private final int maxScanAge;
	/** Timeout value [sec], implying when next scan request can be made. */
	private final int scanRequestTimeout;

	/** Flag indicating if {@code this} {@link BroadcastReceiver} was already registered.
	 * {@code true} if registered, {@code false} otherwise. */
	private boolean registered = false;
	/** Most recently received scan results */
	@NonNull private final List<ScanResult> latestScanResults = new ArrayList<>();
	/** Most recent update date time of {@link #latestScanResults} */
	@NonNull private LocalDateTime latestScanResultTime = LocalDateTime.now().minusYears(1);
	/** Listener reference, for anyone who is interested in scanning results */
	@Nullable private WifiScanListener wifiScanListener;
	/** Flag, when set to {@code true}, disables scan requests to prevent flooding. */
	private boolean scanRequested = false;
	@NonNull private final LocalDateTime latestScanRequestTime = LocalDateTime.now().minusYears(1);
	/** only filled when registered */
	@Nullable private Context context;

	public enum Result {
		/** When {@link WifiManager#isWifiEnabled()} returns false */
		FAIL_WIFI_DISABLED,
		/** When {@link WifiManager#startScan()} fails */
		FAIL_SCAN_REQUEST_FAILED,
		/** When {@link WifiScanner} receives results not updated broadcast */
		FAIL_RESULTS_NOT_UPDATED,
		/** When calling {@link #requestWifiScanResults()} too fast.
		 * @see #scanRequestTimeout */
		CANCEL_SPAMMING
	}

	/**
	 * Callback for anyone that anyone who is interested in wifi scans.
	 */
	public interface WifiScanListener {
		/**
		 * Called when wifi scan results were successfully updated
		 * @param scanResults scan results, never null
		 */
		void onScanResultsUpdated(@NonNull List<ScanResult> scanResults);

		/**
		 * Called when wifi scan fails
		 * @see Result
		 * @param failCode code describing what exactly went wrong
		 */
		void onScanRequestFailed(@NonNull Result failCode);
	}

	public WifiScanner(@NonNull WifiManager wifiManager, int maxScanAge, int scanRequestTimeout) {
		if(wifiManager == null) {
			throw new IllegalArgumentException("wifi manager must not be null");
		}
		if(maxScanAge < 0) {
			throw new IllegalArgumentException("wifi scan result age must not be negative number");
		}
		if(scanRequestTimeout < 0) {
			throw new IllegalArgumentException("wifi scan timeout must not be negative number");
		}

		this.wifiManager = wifiManager;
		this.maxScanAge = maxScanAge;
		this.scanRequestTimeout = scanRequestTimeout;
	}

	/**
	 * Registers this {@link WifiScanner}
	 * @param context any type of {@link Context}, to get application context from,
	 *                needed for registering receiver
	 */
	public void register(@NonNull Context context) {
		if(isRegistered()) {
			Logger.warn("trying to register wifi scanner, but is already registered");
			return;
		}

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		// Note: Android API only allows registering broadcast receivers to application context!
		context.getApplicationContext().registerReceiver(this, intentFilter);
		this.context = context;
		setRegistered(true);
	}

	/**
	 * Unregisters this {@link WifiScanner}
	 * @param context any type of {@link Context}, to get application context from,
	 *                needed for un-registering receiver
	 */
	public void unregister(@NonNull Context context) {
		if(!isRegistered()) {
			Logger.warn("trying to unregister wifi scanner, but is already unregistered");
			return;
		}


		context.getApplicationContext().unregisterReceiver(this);
		setRegistered(false);
		this.context = null;
	}

	/**
	 * Check if this {@link WifiScanner} is registered
	 * @return {@code true} if registered
	 */
	public boolean isRegistered() {
		return registered;
	}

	private void setRegistered(boolean registered) {
		this.registered = registered;
		Logger.debug("changed registered state of wifi scanner to: {}", registered);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		boolean success;

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
			// EXTRA_RESULTS_UPDATED is representing if the scan was successful or not. Scans may
			// fail if:
			// - App requested too many scans in a certain period of time. This may lead to
			//   additional scan request rejections via "scan throttling" for both foreground and
			//   background apps.
			// - The device is idle and scanning is disabled.
			// - Wifi hardware reported a scan failure.
			success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
		} else {
			// Doze mode was implemented in API23 (M), so there shouldn't be any problems
			// on earlier versions. We can't really check anyways... presume it was successful.
			success = true;
		}

		onWifiScanFinished(success);
	}

	public void onWifiScanFinished(boolean success) {
		if (success) {
			List<ScanResult> scanResults = wifiManager.getScanResults();
			if (!scanResults.isEmpty()) {
				latestScanResults.clear();
				latestScanResults.addAll(scanResults);
				latestScanResultTime = LocalDateTime.now();
			}

			if (!scanRequested) {
				Logger.debug("another app initiated wifi scan, cached results");
				return;
			}
		}


		if (wifiScanListener == null) {
			Logger.warn("cannot dispatch wifi scan results, scan listener is null");
		} else if (success) {
			wifiScanListener.onScanResultsUpdated(latestScanResults);
		} else {
			wifiScanListener.onScanRequestFailed(Result.FAIL_RESULTS_NOT_UPDATED);
		}

		scanRequested = false;
	}

	/**
	 * @see WifiScanListener
	 * @param wifiScanListener callback or {@code null} for unregistering it
	 */
	public void setWifiScanListener(@Nullable WifiScanListener wifiScanListener) {
		this.wifiScanListener = wifiScanListener;
	}

	/**
	 * Make a request to scan wifi networks.
	 *
	 * Results will be returned with {@link WifiScanListener}. Callback can happen instant or
	 * delayed (usually a few seconds).
	 */
	public void requestWifiScanResults() {
		if(wifiScanListener == null) {
            Logger.warn("not requesting wifi scan: no listener registered");
			return;
		}

		if(!wifiManager.isWifiEnabled()) {
			Logger.debug("not requesting wifi scan: wifi is disabled");
			wifiScanListener.onScanRequestFailed(Result.FAIL_WIFI_DISABLED);
			return;
		}

		// It's possible another application requested scan results, check last received scan results
		// and use them if they are not too old.
		if(areLastResultsOk()) {
			Logger.debug("returning cached wifi scan results");
			wifiScanListener.onScanResultsUpdated(latestScanResults);
			return;
		}

		// Note: Let's be nice, and allow returning valid cached scan results. I.e. allow returning
		// cached results, before checking if we can scan again.
		if(!canScanAgain()) {
			Logger.debug("not requesting wifi scan: waiting");
			wifiScanListener.onScanRequestFailed(Result.CANCEL_SPAMMING);
			return;
		}

		List<String> missingPermissions = PermissionsUtil.missingPermissionsForTracking(context);
		if (!missingPermissions.isEmpty()) {
			Logger.warn("wifi scanner - missing permissions: {}", missingPermissions);
		}

		boolean success = wifiManager.startScan();
		Logger.debug("wifi start scan succeeded: {}", success);

		if (success) {
			scanRequested = true;
			latestScanResultTime = LocalDateTime.now();
		} else {
			wifiScanListener.onScanRequestFailed(Result.FAIL_SCAN_REQUEST_FAILED);
		}
	}

	/**
	 * Check if current {@link #latestScanResults} are still considered to be usable, i.e. not too
	 * old.
	 * @return {@code true} if still ok, {@code false} if they are stale
	 */
	private boolean areLastResultsOk() {
		LocalDateTime current = LocalDateTime.now();
		LocalDateTime validUntil = latestScanResultTime.plusSeconds(maxScanAge);

		return !current.isAfter(validUntil);
	}

	/**
	 * Checks if scan request can be made with {@link #requestWifiScanResults()}
	 *
	 * @return {@code true} if it can, {@code false otherwise}
	 */
	public boolean canScanAgain() {
		LocalDateTime current = LocalDateTime.now();
		LocalDateTime disabledUntil = latestScanRequestTime.plusSeconds(scanRequestTimeout);

		return !current.isBefore(disabledUntil);
	}
}
