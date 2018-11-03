package org.zephyrsoft.trackworktime.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;

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
 * We can take advantage of this and cache results (in {@link #mLatestScanResults}), to minimize
 * scan requests and lower battery drain.
 */
public class WifiScanner extends BroadcastReceiver {
	@NonNull private final WifiManager mWifiManager;
	/** Max scan age [sec]. How old scan results are still considered "good enough". */
	private final int mMaxScanAge;
	/** Timeout value [sec], implying when next scan request can be made. */
	private final int mScanRequestTimeout;

	/** Flag indicating if {@code this} {@link BroadcastReceiver} was already registered.
	 * {@code true} if registered, {@code false} otherwise. */
	private boolean mRegistered = false;
	/** Most recently received scan results */
	@NonNull private List<ScanResult> mLatestScanResults = new ArrayList<>();
	/** Most recent update date time of {@link #mLatestScanResults} */
	@NonNull private DateTime mLatestScanResultTime = DateTime.forInstant(0, TimeZone.getDefault());
	/** Listener reference, for anyone who is interested in scanning results */
	@Nullable private WifiScanListener mWifiScanListener;
	/** Flag, when set to {@code true}, disables scan requests to prevent flooding. */
	private boolean mScanRequested = false;
	@NonNull private DateTime mLatestScanRequestTime = DateTime.forInstant(0, TimeZone.getDefault());

	public enum Result {
		/** When {@link WifiManager#isWifiEnabled()} returns false */
		FAIL_WIFI_DISABLED,
		/** When {@link WifiManager#startScan()} fails */
		FAIL_SCAN_REQUEST_FAILED,
		/** When {@link WifiScanner} receives results not updated broadcast */
		FAIL_RESULTS_NOT_UPDATED,
		/** When calling {@link #requestWifiScanResults()} too fast.
		 * @see #mScanRequestTimeout */
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

	@SuppressWarnings("ConstantConditions")
	public WifiScanner(@NonNull WifiManager wifiManager, int maxScanAge, int scanRequestTimeout) {
		if(wifiManager == null)
			throw new IllegalArgumentException(WifiManager.class.getSimpleName() + " should not be" +
					" null");
		if(maxScanAge < 0)
			throw new IllegalArgumentException("Scan result age should not be negative number");
		if(scanRequestTimeout < 0)
			throw new IllegalArgumentException("Scan timeout should not be negative number");

		mWifiManager = wifiManager;
		mMaxScanAge = maxScanAge;
		mScanRequestTimeout = scanRequestTimeout;
	}

	/**
	 * Registers this {@link WifiScanner}
	 * @param context any type of {@link Context}, to get application context from,
	 *                needed for registering receiver
	 */
	public void register(@NonNull Context context) {
		if(isRegistered()) {
			Logger.warn(getClass().getSimpleName() + " trying to register, but is already registered!");
			return;
		}

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		// Note: Android API only allows registering broadcast receivers to application context!
		context.getApplicationContext().registerReceiver(this, intentFilter);
		setRegistered(true);
	}

	/**
	 * Unregisters this {@link WifiScanner}
	 * @param context any type of {@link Context}, to get application context from,
	 *                needed for un-registering receiver
	 */
	public void unregister(@NonNull Context context) {
		if(!isRegistered()) {
			Logger.warn(getClass().getSimpleName() + " trying to unregister, but is already not" +
					" registered!");
			return;
		}

		context.getApplicationContext().unregisterReceiver(this);
		setRegistered(false);
	}

	/**
	 * Check if this {@link WifiScanner} is registered
	 * @return {@code true} if registered
	 */
	public boolean isRegistered() {
		return mRegistered;
	}

	private void setRegistered(boolean registered) {
		mRegistered = registered;
		Logger.debug(getClass().getSimpleName() + " changed registered state to: " + registered);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		boolean success;

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
			// EXTRA_RESULTS_UPDATED is representing if the scan was successful or not. Scans may
			// fail if:
			// - App requested too many scans in a certain period of time. This may lead to
			//   additional scan request rejections via "scan throttling" for both foreground and
			//   background apps.
			// - The device is idle and scanning is disabled.
			// - Wifi hardware reported a scan failure.
			success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
		else
			// Doze mode was implemented in API23 (M), so there shouldn't be any problems
			// on earlier versions. We can't really check anyways... presume it was successful.
			success = true;

		onWifiScanFinished(success);
	}

	public void onWifiScanFinished(boolean success) {
		if(success) {
			mLatestScanResults.clear();
			mLatestScanResults.addAll(mWifiManager.getScanResults());
			mLatestScanResultTime = DateTimeUtil.getCurrentDateTime();
		}

		if(!mScanRequested) {
			Logger.debug("Another app initiated wifi-scan, caching results.");
			return;
		}

		if(mWifiScanListener == null)
			Logger.warn("Cannot dispatch scan results! " + WifiScanListener.class.getSimpleName() +
					" was null!");
		else if(success)
			mWifiScanListener.onScanResultsUpdated(mLatestScanResults);
		else
			mWifiScanListener.onScanRequestFailed(Result.FAIL_RESULTS_NOT_UPDATED);

		mScanRequested = false;
	}

	/**
	 * @see WifiScanListener
	 * @param wifiScanListener callback or {@code null} for unregistering it
	 */
	public void setWifiScanListener(@Nullable WifiScanListener wifiScanListener) {
		mWifiScanListener = wifiScanListener;
	}

	/**
	 * Make a request to scan wifi networks.
	 *
	 * Results will be returned with {@link WifiScanListener}. Callback can happen instant or
	 * delayed (usually a few seconds).
	 */
	public void requestWifiScanResults() {
		Logger.debug("Requested wifi scan results");

		if(mWifiScanListener == null) {
			// No point in requesting scans nobody cares about...
            Logger.warn("Requesting wifi scan, but no " + WifiScanListener.class.getSimpleName()
					+ " is registered!");
			return;
		}

		if(!mWifiManager.isWifiEnabled()) {
			mWifiScanListener.onScanRequestFailed(Result.FAIL_WIFI_DISABLED);
			return;
		}

		// It's possible another application requested scan results, check last received scan results
		// and use them if they are not too old.
		if(areLastResultsOk()) {
			Logger.debug("Returning cached wifi scan results");
			mWifiScanListener.onScanResultsUpdated(mLatestScanResults);
			return;
		}

		// Note: Let's be nice, and allow returning valid cached scan results. I.e. allow returning
		// cached results, before checking if we can scan again.
		if(!canScanAgain()) {
			mWifiScanListener.onScanRequestFailed(Result.CANCEL_SPAMMING);
			return;
		}

		boolean success = mWifiManager.startScan();
		mLatestScanResultTime = DateTimeUtil.getCurrentDateTime();
		Logger.debug("Wifi start scan succeeded: " + success);

		if(!success) {
			mWifiScanListener.onScanRequestFailed(Result.FAIL_SCAN_REQUEST_FAILED);
			return;
		}

		mScanRequested = true;
	}

	/**
	 * Check if current {@link #mLatestScanResults} are still considered to be usable, i.e. not too
	 * old.
	 * @return {@code true} if still ok, {@code false} if they are stale
	 */
	private boolean areLastResultsOk() {
		DateTime current = DateTimeUtil.getCurrentDateTime();
		DateTime validUntil = mLatestScanResultTime.plus(
				0, 0, 0, 0, 0, mMaxScanAge, 0, DateTime.DayOverflow.Spillover);

		return validUntil.gteq(current);
	}

	/**
	 * Checks if scan request can be made with {@link #requestWifiScanResults()}
	 * @return {@code true} if it can, {@code false otherwise}
	 */
	public boolean canScanAgain(){
		DateTime current = DateTimeUtil.getCurrentDateTime();
		DateTime disabledUntil = mLatestScanRequestTime.plus(
				0, 0, 0, 0, 0, mScanRequestTimeout, 0, DateTime.DayOverflow.Spillover);

		return disabledUntil.lteq(current);
	}
}
