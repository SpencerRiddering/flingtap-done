// Licensed under the Apache License, Version 2.0

package com.flingtap.done.util;

import com.flingtap.done.ApplicationPreference;
import com.flingtap.done.ErrorUtil;
import com.flingtap.done.SharedConstant;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

/**
 * Utilities for checking and getting purchased license info.
 */
public class LicenseUtil {
	private static final String TAG = "LicenseUtil";

    // Name used in <meta-data ...> tag for uniquely identifying application features.
	private static final String META_FEATURE_LICENSES = "FEATURE_LICENSES";

    // Identifier for each application feature which can be unlocked via purchase.
    public static final int FEATURE_NEARMINDER 	= 1; // 0001
    public static final int FEATURE_CALLMINDER 	= 2; // 0010
    public static final int FEATURE_LABELS 		= 4; // 0100
    public static final int FEATURE_ARCHIVING 	= 8; // 1000

    /**
     * Check the feature licenses located in the license package's Manifest.
     *
     * Example:
     * 	<application>
     *		...
     * 		<meta-data android:name="FEATURE_LICENSES" android:value="3" />
     *	</application>
     */
	public static boolean hasAnyLicense(Context context){

		try{
			PackageManager pm = context.getPackageManager();
			ApplicationInfo appInfo = null;			

			// Check SharedConstant.ADDON_MINDERS
			if( AddonUtil.doesPackageHaveSameSignature(context, SharedConstant.ADDON_MINDERS) ){ 
				appInfo = pm.getApplicationInfo(SharedConstant.ADDON_MINDERS, PackageManager.GET_META_DATA);
				if ( null != appInfo ){
					
					// Check if any bits are on.
					if( appInfo.metaData.getInt( META_FEATURE_LICENSES ) > 0 ){
						return true;
					}
				}
			}

			
			// Check SharedConstant.ADDON_ORGANIZERS
			if( AddonUtil.doesPackageHaveSameSignature(context, SharedConstant.ADDON_ORGANIZERS) ){
				appInfo = pm.getApplicationInfo(SharedConstant.ADDON_ORGANIZERS, PackageManager.GET_META_DATA);
				if ( null != appInfo ){
					
					// Check if any bits are on.
					if( appInfo.metaData.getInt( META_FEATURE_LICENSES ) > 0 ){
						return true;
					}
				}
			}
			
		}catch(Exception exp){
			Log.e(TAG, "ERR000J3", exp);
			ErrorUtil.handleException("ERR000J3", exp, context);
		}
		return false;
	}


    /**
     * Check whether a specific license exists.
     * @param context
     * @param feature Constants from LicenseUtil.FEATURE_*
     * @return true if license user has license.
     */
	public static boolean hasLicense(Context context, int feature ){
		
		try{
			String thePackage = null;
			switch(feature){
				case FEATURE_NEARMINDER:
				case FEATURE_CALLMINDER:
					if( AddonUtil.doesPackageHaveSameSignature(context, SharedConstant.ADDON_MINDERS) ){ 
						thePackage = SharedConstant.ADDON_MINDERS;
					}
					break;
				case FEATURE_LABELS:
				case FEATURE_ARCHIVING:
					if( AddonUtil.doesPackageHaveSameSignature(context, SharedConstant.ADDON_ORGANIZERS) ){
						thePackage = SharedConstant.ADDON_ORGANIZERS;
					}
					break;
			}
			if( null == thePackage ){
				return false;
			}
			
			// Check the feature licenses located in the license package's Manifest.
			// Example: 
			// 	<application>
			//		...
			// 		<meta-data android:name="FEATURE_LICENSES" android:value="3" />
			//	</application>
			PackageManager pm = context.getPackageManager();
			ApplicationInfo appInfo = pm.getApplicationInfo(thePackage, PackageManager.GET_META_DATA);
			
			if ( null != appInfo ){
				int licenses = appInfo.metaData.getInt( META_FEATURE_LICENSES );
				//Log.d(TAG, "Licenses value is: " + licenses );
				
				// Does feature bit exists?
				return ((licenses & feature) == feature);
				
			}
		}catch(Exception exp){
			Log.e(TAG, "ERR000IZ", exp);
			ErrorUtil.handleException("ERR000IZ", exp, context);
		}
		return false;
	}

    /**
     * Update license state within SharedPreferences.
     * @param context
     */
	public static void updateCurrentFeatureLicenseSetting(Context context){
		int currentLicenses = 0;
		try{
			SharedPreferences settings = context.getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE); 
			
			PackageManager pm = context.getPackageManager();
			
			// ********************************
			// SharedConstant.ADDON_MINDERS
			// ********************************
			ApplicationInfo appInfo = null;
			try{
				appInfo = pm.getApplicationInfo(SharedConstant.ADDON_MINDERS, PackageManager.GET_META_DATA);
				
				if ( null != appInfo ){
					int licenses = appInfo.metaData.getInt( META_FEATURE_LICENSES );
					// Log.d(TAG, "Licenses value is: " + licenses );
					
					// OR bits together.
					currentLicenses = licenses | currentLicenses;
				}
			}catch(NameNotFoundException nnfe){
				// The add-on isn't installed. Not a problem.
			}
			
			
			// ********************************
			// SharedConstant.ADDON_MINDERS
			// ********************************
			try{
				appInfo = pm.getApplicationInfo(SharedConstant.ADDON_ORGANIZERS, PackageManager.GET_META_DATA);

				if ( null != appInfo ){
					int licenses = appInfo.metaData.getInt( META_FEATURE_LICENSES );
					// Log.d(TAG, "Licenses value is: " + licenses );
					
					// OR all the bits together.
					currentLicenses = licenses | currentLicenses;
				}
				
			}catch(NameNotFoundException nnfe){
				// The add-on isn't installed. Not a problem.
			}
			
			Editor currentFeatureLicenseSettings = settings.edit();
			currentFeatureLicenseSettings.putInt(ApplicationPreference.CURRENT_FEATURE_LICENSES, currentLicenses);
			currentFeatureLicenseSettings.commit();
			
		}catch(Exception exp){
			Log.e(TAG, "ERR000J0", exp);
			ErrorUtil.handleException("ERR000J0", exp, context);
		}
	}
}
