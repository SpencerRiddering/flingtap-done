// Licensed under the Apache License, Version 2.0

package com.flingtap.done.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

/**
 * Various utility functions related to Add-ons.
 */
public class AddonUtil {

	// 4 milliseconds.
    public static boolean doesPackageHaveSameSignature(Context context, String productPackage){ 
        int compareSignatures = context.getPackageManager().checkSignatures(context.getPackageName(), productPackage);
        return PackageManager.SIGNATURE_MATCH == compareSignatures;
    }

    // TODO: Duplicated in BackupRestoreAgent.
    public static boolean doesPackageExist(Context context, String packageName){
    	try {
			PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
			if( null == info ){
				return false;
			}
			return true;
		} catch (NameNotFoundException e) {
			return false;
		}
    }
    
}
