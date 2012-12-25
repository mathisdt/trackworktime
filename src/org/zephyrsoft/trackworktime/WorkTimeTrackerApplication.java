package org.zephyrsoft.trackworktime;

import android.app.Application;
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.zephyrsoft.trackworktime.model.TimeSum;
import org.zephyrsoft.trackworktime.util.Logger;

/**
 * Application entry point.
 * 
 * @author Mathis Dirksen-Thedens
 */
@ReportsCrashes(formKey = "", formUri = "http://zephyrsoft.net/crashreport.jsp", mode = ReportingInteractionMode.SILENT)
public class WorkTimeTrackerApplication extends Application {
	
	@Override
	public void onCreate() {
		Logger.info("creating application");
		ACRA.init(this);
		Basics.getOrCreateInstance(getApplicationContext());
		
		Logger.info("running self-tests");
		TimeSum.test();
		
		Logger.info("handing off to super");
		super.onCreate();
	}
	
	@Override
	public void onTerminate() {
		Logger.info("terminating application");
		Basics.getOrCreateInstance(getApplicationContext()).getDao().close();
		super.onTerminate();
	}
	
	@Override
	public void onLowMemory() {
		Logger.info("low memory for application");
		super.onLowMemory();
	}
	
}
