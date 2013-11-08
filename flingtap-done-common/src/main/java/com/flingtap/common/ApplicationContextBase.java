// Licensed under the Apache License, Version 2.0

package com.flingtap.common;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Process;
import android.util.Log;

public abstract class ApplicationContextBase extends Application {
	private final static String TAG = "ApplicationContext";
	
	private static final int CONCEPTION_VERSION = 0;
	
	protected abstract void onFirstRun();
	
	protected abstract boolean onNewVersion(int actualVersion, int recordedVersion);

	protected abstract void onDowngrade(int actualVersion, int recordedVersion);
	
	protected abstract void onCreation();
	
	protected abstract void onException(Exception exp);
	
	protected abstract void onError(Error err);
	
	protected abstract boolean killOnDowngrade();

	/**
	 */
	public void onCreate()  {
		try{
			super.onCreate();
			
			onCreation();
			
			SharedPreferences settings = getSharedPreferences(ApplicationPreferenceBase.NAME, Context.MODE_PRIVATE); 
			assert null != settings;
	
			// Check whether the package has been upgraded.
			int recordedVersion = settings.getInt(ApplicationPreferenceBase.APPLICATION_VERSION, CONCEPTION_VERSION);
						
			if( CONCEPTION_VERSION == recordedVersion ){ // Run only the very first time. 
				// These alarms/services are normally started from on-startup and on-reinstall broadcasts.
				onFirstRun();
			}

			int actualVersion;
			try {
				PackageManager pm = getPackageManager();
				assert null != pm;
				PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
				actualVersion = pi.versionCode;
				
				if( actualVersion == recordedVersion ){ // Same version
					// Move on...
				}else if( (actualVersion > recordedVersion) ){ // New Version.
					// Log.i(TAG, "(actualVersion > recordedVersion) is " + "("+actualVersion+" > "+recordedVersion+")");
					
					if( onNewVersion(actualVersion, recordedVersion) ){
						// *******************************************************
						// Update version in preferences.
						// *******************************************************					
						SharedPreferences.Editor ed = settings.edit();
						assert null != ed;
						
						// Update the application version in the shared preferences.
						ed.putInt(ApplicationPreferenceBase.APPLICATION_VERSION, actualVersion);
						boolean commit2 = ed.commit();
						//	        if( !commit2 ){
						//				Log.e(TAG, "Failed to upgrade shared preferences.");
						//				throw new RuntimeException("Failed to upgrade shared preferences.");
						//	        }
						
					}
				}else if( actualVersion < recordedVersion ){ // Old version.
					try{
						onDowngrade(actualVersion, recordedVersion);
					}finally{
						if( killOnDowngrade() ){
							// TODO: !!! Consider how to handle this situation more gracefully.
							Process.sendSignal(Process.myPid(), Process.SIGNAL_KILL); // This prevents the application from launching, strange from user's perspective.
						}
					}
				}
			} catch (NameNotFoundException e) {
				try{
					onException(e); // TODO: !!! Why not just handle the error here to make the subclass even more simple.
				}finally{
					Log.i(TAG, "Failed to find the application version so prompting for EULA just to make sure.");
					SharedPreferences.Editor ed2 = settings.edit();
					assert null != ed2;
					ed2.putBoolean(ApplicationPreferenceBase.EULA_ACCEPTANCE_REQUIRED, true);
					boolean commit = ed2.commit();
				}
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			onException(exp);
		}catch(ThreadDeath td){
			onError(td);
		}catch(OutOfMemoryError oome){
			onError(oome);
		}  		
	}
}
