// Licensed under the Apache License, Version 2.0

package com.flingtap.done.addon.backup;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.flingtap.common.ErrorUtil;
import com.flingtap.common.HandledException;
import com.flingtap.done.backup.export.BackupPreferenceConstants;
import com.flingtap.done.provider.Task;

public class BackupRestoreAgent extends BackupAgent {
	private static final String TAG = "BackupRestoreAgent";
	
	private static final String ENTITY_KEY_SERIALIZER_VERSION 	= "serializer_version";
	private static final String ENTITY_KEY_DB 					= "db";
	private static final String ENTITY_KEY_DB_VERSION			= "db_version";
	private static final String ENTITY_KEY_SDK_VERSION 			= "sdk_version";
	private static final String ENTITY_KEY_DEVICE_MODEL 		= "device_model";
	private static final String ENTITY_KEY_INSTALL_FINGERPRINT	= "install_fingerprint";
		
	@Override
	public void onCreate() {
		super.onCreate();
//		try{
//		} catch (HandledException e) {
//		} catch (Exception e) {
//			Log.e(TAG, "ERR000KV", e);
//          ErrorUtil.handleException("ERR000KV", e, this);
//		}
	}
	
	@Override
	public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException {
		Log.v(TAG, "onBackup(..) called.");
		try{
			try{
				SharedPreferences settings = getSharedPreferences(BackupPreferenceConstants.NAME, Context.MODE_PRIVATE); 
				if( settings.getBoolean(BackupPreferenceConstants.BACKUP_ENABLED, BackupPreferenceConstants.BACKUP_ENABLED_DEFAULT) ){
					Log.w(TAG, "Backup requested but backups are disabled in FlingTap Done.");
					return;
				}
				
				//Log.v(TAG, "onBackup(..) called.");
				if( null == oldState ){ // No prior state exists.
					// Be safe and do a backup.
					doBackup(data, newState);
					return;
				}
				FileInputStream instream = new FileInputStream(oldState.getFileDescriptor());
				try{
					DataInputStream in = new DataInputStream(instream);
					try{
						
						int serializationVersion = in.readInt(); // First int is the serialization version.
						switch(serializationVersion){
							case 1:
								long stateModified = in.readLong();
								
								long dbLastModified = findDbLastModified();
								if( 0 == dbLastModified ){
									// Db last modified timestamp not found.
									// Be safe and do a backup.
									doBackup(data, newState);
								}
								if (stateModified == dbLastModified) {
									// Don't back up because the file hasn't changed
									return;
								}
								// The file has been modified, so do a backup
								// Or the time on the device changed, so be safe and do a backup
								
								doBackup(data, newState);
								break;
							default:
								// Unknown serialization version.
								// Be safe and do a backup.
								doBackup(data, newState);
						}
					}finally{
						if( null != in ){
							in.close();
						}
					}
				}finally{
					if( null != instream ){
						instream.close();
					}
				}
				
			} catch (IOException e) {
				// Unable to read state file... be safe and do a backup
				doBackup(data, newState);
			}
			
		} catch (HandledException e) {
		} catch (Exception e) {
			Log.e(TAG, "ERR000KR", e);
            ErrorUtil.handleException("ERR000KR", e, this);
		}

		
		Log.v(TAG, "onBackup(..) completed.");
	}

	
	private long findDbLastModified(){
		
		Cursor cursor = getContentResolver().query(Task.Backup.CONTENT_URI, new String[]{Task.BackupColumns._LAST_MODIFIED}, null, null, null);
		if( null == cursor ){
			return 0;
		}
		try{
			if( cursor.moveToFirst() ){
				return cursor.getLong(0); 
			}else{
				return 0;
			}
		}finally{
			cursor.close();
		}
	}
	
	private void doBackup(BackupDataOutput data, ParcelFileDescriptor newState){
		Log.v(TAG, "doBackup(..) called.");
		
		try {
			// write the current data to data to back it up
			// Construct body
			String dbSerialization = null;
			int serializerVersion = 0;
			int dbVersion = 0;
			long lastModified = 0;
			long installFingerprint = 0;
			
			Log.v(TAG, "Fetching backup data.");
			Cursor cursor = getContentResolver().query(Task.Backup.CONTENT_URI, 
					new String[]{
					Task.Backup._SERIALIZER_VERSION,// 0
					Task.Backup._VERSION, 			// 1
					Task.Backup._LAST_MODIFIED,		// 2
					Task.Backup._SERIALIZATION,		// 3
					Task.Backup._INSTALL_FINGERPRINT// 4
				}, null, null, null);
			if( null == cursor ){
				if( BackupRestoreAgent.doesPackageExist(this, "com.flingtap.done.base") ){
					Log.e(TAG, "ERR000KP");
					throw new HandledException(); // Should have already been documented at this point.
				}else{
					Log.e(TAG, "Backup requested but FlingTap Done not installed.");
					throw new HandledException(); // Should have already been documented at this point.
				}
			}
			try{
				if( cursor.moveToFirst() ){
					serializerVersion = cursor.getInt(cursor.getColumnIndexOrThrow(Task.BackupColumns._SERIALIZER_VERSION)); 
					dbVersion = cursor.getInt(cursor.getColumnIndexOrThrow(Task.BackupColumns._VERSION)); 
					lastModified = cursor.getLong(cursor.getColumnIndexOrThrow(Task.BackupColumns._LAST_MODIFIED)); 
					dbSerialization = cursor.getString(cursor.getColumnIndexOrThrow(Task.BackupColumns._SERIALIZATION)); 
					int fingerPrintColumnIndex = cursor.getColumnIndex(Task.Backup._INSTALL_FINGERPRINT);
					if( -1 != fingerPrintColumnIndex ){
						installFingerprint = cursor.getLong(fingerPrintColumnIndex); 
					}
				}else{
					Log.e(TAG, "ERR000KQ");
					throw new HandledException(); // Should have already been documented at this point.
				}
			}finally{
				if( null != cursor ){
					cursor.close();
				}
			}
			Log.v(TAG, "serializing backup.");
			//Log.v(TAG, "serializerVersion="+serializerVersion+" dbVersion="+dbVersion+" lastModified="+lastModified+" dbSerialization="+(null==dbSerialization));
			if( 	0 != serializerVersion &&
					null != dbSerialization && 
					0 != dbVersion &&
					0 < lastModified){
				// Write serializer version.
				byte[] serializerVersionBytes = ByteBuffer.allocate(4).putInt(serializerVersion).array(); // Java integers are 32 bits (aka 4 bytes).
				data.writeEntityHeader(ENTITY_KEY_SERIALIZER_VERSION, serializerVersionBytes.length); 
				data.writeEntityData(serializerVersionBytes, serializerVersionBytes.length); 

				// Write SDK version.
				byte[] sdkVersionBytes = ByteBuffer.allocate(4).putInt(Build.VERSION.SDK_INT).array();
				data.writeEntityHeader(ENTITY_KEY_SDK_VERSION, sdkVersionBytes.length); 
				data.writeEntityData(sdkVersionBytes, sdkVersionBytes.length); 				
				
				// Write device model.
				byte[] deviceModelBytes = Build.MODEL.getBytes("UTF-8");
				data.writeEntityHeader(ENTITY_KEY_DEVICE_MODEL, deviceModelBytes.length); 
				data.writeEntityData(deviceModelBytes, deviceModelBytes.length); 
				
				// Write db version.
				byte[] dbVersionBytes = ByteBuffer.allocate(4).putInt(dbVersion).array(); 
				data.writeEntityHeader(ENTITY_KEY_SERIALIZER_VERSION, dbVersionBytes.length);   
				data.writeEntityData(dbVersionBytes, dbVersionBytes.length); 
				
				// Write Database entity.
				byte[] serializedDbBytes = dbSerialization.getBytes("UTF-8");
				data.writeEntityHeader(ENTITY_KEY_DB, serializedDbBytes.length);  
				data.writeEntityData(serializedDbBytes, serializedDbBytes.length); 
				
				// Write Install Fingerprint.
				byte[] installFingerprintBytes = ByteBuffer.allocate(8).putLong(installFingerprint).array();
				data.writeEntityHeader(ENTITY_KEY_INSTALL_FINGERPRINT, installFingerprintBytes.length); 
				data.writeEntityData(installFingerprintBytes, installFingerprintBytes.length); 				

			}else{
				Log.e(TAG, "ERR000KG");
	            ErrorUtil.handle("ERR000KG", "", this);
				throw new HandledException();
			}
			
			// Step 3. Write a representation of the current data
			Log.v(TAG, "writing state file.");
			writeStateFile(newState, serializerVersion, lastModified);

			// Step 4. Update our database with the new backup timestamp.
			Log.v(TAG, "Updating backup timestamp.");
			ContentValues cv = new ContentValues(1);
			cv.put(Task.BackupColumns._LAST_BACKED_UP, System.currentTimeMillis());
			int count = getContentResolver().update(Task.Backup.CONTENT_URI, 
					cv, 
					null, 
					null);
			if( count != 1 ){
				Log.e(TAG, "ERR000KO");
	            ErrorUtil.handle("ERR000KO", "", this);
			}
			
		} catch (HandledException e) {
		} catch (IOException e) {
			Log.e(TAG, "ERR000K9", e);
            ErrorUtil.handleException("ERR000K9", e, this);
		}
	}
	
	
	@Override
	public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
		try{
			Log.v(TAG, "onRestore(..) called.");

			ContentValues values = new ContentValues();
			Uri.Builder updateUri = Task.Backup.CONTENT_URI.buildUpon();
	        // We should only see one entity in the data stream, but the safest
	        // way to consume it is using a while() loop
	        while (data.readNextHeader()) {
	            String key = data.getKey();
	            int dataSize = data.getDataSize();
	            
	            if (ENTITY_KEY_SERIALIZER_VERSION.equals(key)) {
	                byte[] byteBuf = new byte[dataSize];
	                data.readEntityData(byteBuf, 0, dataSize);
	        		values.put(Task.Backup._SERIALIZER_VERSION, ByteBuffer.wrap(byteBuf).getInt());
	        		//Log.v(TAG, "ENTITY_KEY_SERIALIZER_VERSION == " + ByteBuffer.wrap(byteBuf).getInt());
	        		
	            }else if(ENTITY_KEY_DB.equals(key)) {
	                byte[] byteBuf = new byte[dataSize];
	                data.readEntityData(byteBuf, 0, dataSize);
	        		values.put(Task.Backup._SERIALIZATION, new String(byteBuf, "UTF-8"));
	        		//Log.v(TAG, "ENTITY_KEY_DB == " + new String(byteBuf, "UTF-8"));
	        		
	            }else if(ENTITY_KEY_SDK_VERSION.equals(key)) {
	                byte[] byteBuf = new byte[dataSize];
	                data.readEntityData(byteBuf, 0, dataSize);
	                updateUri.appendQueryParameter(Task.Backup.PARAM_SDK_VERSION, String.valueOf(ByteBuffer.wrap(byteBuf).getInt()));
	                //Log.v(TAG, "ENTITY_KEY_SDK_VERSION == " + ByteBuffer.wrap(byteBuf).getInt());
	                
	            }else if(ENTITY_KEY_DEVICE_MODEL.equals(key)) {
	                byte[] byteBuf = new byte[dataSize];
	                data.readEntityData(byteBuf, 0, dataSize);
	                updateUri.appendQueryParameter(Task.Backup.PARAM_DEVICE_MODEL, new String(byteBuf, "UTF-8"));
	                //Log.v(TAG, "ENTITY_KEY_DEVICE_MODEL == " + new String(byteBuf, "UTF-8"));
	                
	            }else if(ENTITY_KEY_DB_VERSION.equals(key)) {
	                byte[] byteBuf = new byte[dataSize];
	                data.readEntityData(byteBuf, 0, dataSize);
	        		values.put(Task.Backup._VERSION, ByteBuffer.wrap(byteBuf).getInt());
	        		//Log.v(TAG, "ENTITY_KEY_DB_VERSION == " + ByteBuffer.wrap(byteBuf).getInt());

	            }else if(ENTITY_KEY_INSTALL_FINGERPRINT.equals(key)) {
	                byte[] byteBuf = new byte[dataSize];
	                data.readEntityData(byteBuf, 0, dataSize);
	        		values.put(Task.Backup._INSTALL_FINGERPRINT, ByteBuffer.wrap(byteBuf).getLong());
	                //Log.v(TAG, "ENTITY_KEY_INSTALL_FINGERPRINT == " + ByteBuffer.wrap(byteBuf).getLong());
	                
	            }else {
	                // Curious!  This entity is data under a key we do not
	                // understand how to process.  Just skip it.
	                data.skipEntityData();
	    			Log.e(TAG, "ERR000K8: Unknown backup entity: " + key);
	                ErrorUtil.handle("ERR000K8", "Unknown backup entity: " + key, this);
	            }
	        }
			
			Uri responseUri = getContentResolver().insert(updateUri.build(), values);
			
			int serializerVersion = Integer.parseInt(responseUri.getQueryParameter(Task.Backup._SERIALIZER_VERSION));

			long lastModified = Long.parseLong(responseUri.getQueryParameter(Task.Backup._LAST_MODIFIED));
			
			if( 0 != serializerVersion ){
				// The last thing to do is write the state blob that describes the
				// app's data as restored from backup.
				writeStateFile(newState, serializerVersion, lastModified);
			}else{
				throw new IOException("Failed to restore data.");
			}
			
			Log.v(TAG, "onRestore(..) completed.");
		} catch (HandledException e) {
		} catch (Exception e) {
			Log.e(TAG, "ERR000KA", e);
            ErrorUtil.handleException("ERR000KA", e, this);
		}

	}
	
    /**
     * Write out the new state file. 
     */
    void writeStateFile(ParcelFileDescriptor stateFile, int serializerVersion, long lastModified) throws IOException {        		
		FileOutputStream outstream = new FileOutputStream(stateFile.getFileDescriptor());
		DataOutputStream out = new DataOutputStream(outstream);
		
		// Header fields.
		out.writeInt(serializerVersion); // serialization version.
		out.writeLong(lastModified); // DB last modified timestamp.
        
    }

    // TODO: Duplicated in AddonUtil.
    private static boolean doesPackageExist(Context context, String packageName){
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
