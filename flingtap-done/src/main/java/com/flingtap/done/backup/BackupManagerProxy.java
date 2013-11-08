// Licensed under the Apache License, Version 2.0

package com.flingtap.done.backup;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import android.content.Context;
import android.util.Log;

import com.flingtap.done.ApplicationContext;
import com.flingtap.done.ErrorUtil;
import com.flingtap.done.SharedConstant;

public class BackupManagerProxy {
	private final String TAG = "BackupManagerProxy";

	public static final long DEFAULT_TIMESTAMP = 0;
	
	// TODO: !!! Consider moving these time-stamps into a database so it can be included in transactions.
	private File dbLastModified = null; // Last time database was modified in a way that needs to be backed up.
	                                    // Since we request a backup every time the database is modified (as mentioned above)
	                                    //   this is also the time when we last requested a backup.
	private File dbBackedUp = null;     // The last time the database was backed up.
	private Method dataChangedMethod = null;
 	private static BackupManagerProxy mBackupManagerProxy = null;
 	private boolean initialized = false;
 	private Object[] packageArg = new Object[]{SharedConstant.ADDON_BACKUP};
	private File installFingerprintFile = null; // A UID (well unique enough) of the FlingTap Done installation which
												// informs us when a backup was sourced from the same database instance
												// that the backup is being restored onto. 
 	
 	public static synchronized BackupManagerProxy getInstance(Context context){
 		if( null == mBackupManagerProxy ){
 			mBackupManagerProxy = new BackupManagerProxy(context);
 		}
 		return mBackupManagerProxy;
 	}
 	
	private BackupManagerProxy(Context context) {
		try {
			Class backupManagerClass = Class.forName("android.app.backup.BackupManager");
			Constructor backupManagerConstructor = backupManagerClass.getDeclaredConstructor(Context.class);
			dataChangedMethod = backupManagerClass.getMethod("dataChanged", String.class);

			dbLastModified = new File(context.getDir("misc", Context.MODE_PRIVATE), "ftdone.modified"); 
			if( !dbLastModified.exists() ){
				dbLastModified.getParentFile().mkdirs();
				dbLastModified.createNewFile();  
				// dbLastModified.setLastModified(DEFAULT_TIMESTAMP); // Will be updated in dataChanged(..)
				dataChanged(context, true); // Request a backup.
			}
			
			dbBackedUp     = new File(context.getDir("misc", Context.MODE_PRIVATE), "ftdone.backedup"); 
			if( !dbBackedUp.exists() ){
				dbBackedUp.getParentFile().mkdirs();
				dbBackedUp.createNewFile();  
				dbBackedUp.setLastModified(DEFAULT_TIMESTAMP);
			}
			
			//Log.d("TEST", "dbLastModified = " + dbLastModified.toString());
			//Log.d("TEST", "dbBackedUp = "+ dbBackedUp.toString());
			
			installFingerprintFile = new File(context.getDir("misc", Context.MODE_PRIVATE), "ftdone.installfingerprint"); 
			if( !installFingerprintFile.exists() ){
				installFingerprintFile.getParentFile().mkdirs();
				installFingerprintFile.createNewFile();  
				installFingerprintFile.setLastModified(System.currentTimeMillis()); 
			}

			initialized = true;
		} catch (Exception e) {
			Log.e(TAG, "ERR000KE", e);
			ErrorUtil.handleException("ERR000KE", e, context);
			dataChangedMethod = null;
		}
	}
	
	public void dataChanged(Context context, boolean requestBackup){
		try {
			if( initialized ){
				if( dbLastModified.setLastModified(System.currentTimeMillis()) ){
					if( requestBackup ){
						// dataChangedMethod.invoke(backupManagerObj, (Object[]) null); // Request backup from BackupManager.
						dataChangedMethod.invoke(null, packageArg); // Request backup from BackupManager.
					}
				}else{
					Log.e(TAG, "ERR000KL");
					ErrorUtil.handle("ERR000KL", "", context);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "ERR000KD", e);
			ErrorUtil.handleException("ERR000KD", e, context);
		}
	}
	
	public long getDatabaseLastModified(){
		return dbLastModified.lastModified();
	}

	public void datbaseBackedUp(long timestamp){
		dbBackedUp.setLastModified(timestamp);
	}
	public long getDatbaseBackedUp(){
		return dbBackedUp.lastModified();
	}
	public long getInstallFingerprint(){
		return installFingerprintFile.lastModified();
	}
	
}
