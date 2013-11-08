// Licensed under the Apache License, Version 2.0

 package com.flingtap.done.backup;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlSerializer;

import com.flingtap.common.HandledException;
import com.flingtap.done.ErrorUtil;
import com.flingtap.done.TaskProvider;
import com.flingtap.done.base.R;
import com.flingtap.done.provider.Task;
import com.flingtap.done.provider.Task.Tasks;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;


/**
 * Consolidates various backup functions for packing and unpacking the database to/from xml.
 *
 * FYI: Here is how you trigger the backup service:
 *   http://hi-android.info/src/com/android/providers/settings/SettingsProvider.java.html
 *     http://forum.xda-developers.com/showthread.php?p=6374548
 *     backup_transport=com.google
 *     backup_provisioned=1
 *     backup_enabled=1
 *     backup_auto_restore
 *     Starting activity: Intent { act=android.intent.action.MAIN cmp=com.android.settings/.PrivacySettings }
 */
public class BackupUtil {

	private static final String TAG = "BackupUtil";
	
	public static final String SERIALIZER_VERSION = "1";
	
	public static final String QUERY_PARAM_SERIALIZER_VERSION = "serializer_version";
	public static final String QUERY_PARAM_LAST_UPDATE = "last_update";

	
	public static final String TAG_DATASET = "dataset";
	public static final String TAG_TABLE = "table";
	public static final String TAG_TABLE_ATTR_NAME = "name";
	public static final String TAG_ROW = "row";
	public static final String TAG_COLMN = "column";
	public static final String TAG_VALUE = "value";
	public static final String TAG_NULL = "null";
	
	public static void addHeader(XmlSerializer serializer) throws IOException {
	    // <?xml declaration with encoding from output stream and standalone flag.
	    serializer.startDocument(null, null);

	    // Enable indentation option.
	    // FIXME: Comment this line:  
	    //serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
	    
	    // Add root tag.
	    serializer.startTag(null, TAG_DATASET);
	}

	public static void addFooter(XmlSerializer serializer) throws IOException {
	    serializer.endTag(null, TAG_DATASET);
	    
	    serializer.endDocument();
	}

	public static final int COLUMN_TYPE_STRING = 0;
	public static final int COLUMN_TYPE_BLOB   = 1;
	
	public static void addTableData(SQLiteDatabase mDB, XmlSerializer serializer, String tableName, String[] columnNames, int[] columnTypes, String selection, String[] selectionArgs, String orderedBy) throws IllegalArgumentException, IllegalStateException, IOException {
		Cursor cursor = mDB.query(tableName, columnNames, selection, selectionArgs, null, null, orderedBy);
		assert null != cursor;
		try{
			if( cursor.moveToFirst()){
			    serializer.startTag(null, TAG_TABLE);
			    serializer.attribute(null, TAG_TABLE_ATTR_NAME, tableName);
			    
				// Add column names
				for(int i=0; i < columnNames.length; i++){
				    serializer.startTag(null, TAG_COLMN);
				    serializer.text(columnNames[i]);
				    serializer.endTag(null, TAG_COLMN);
				}
				
				// Add row of column values
				do{
					serializer.startTag(null, TAG_ROW);
					// Add column values
					for(int i=0; i < columnNames.length; i++){
						if( cursor.isNull(i) ){
						    serializer.startTag(null, TAG_NULL).endTag(null, TAG_NULL);
						}else{
						    if( null != columnTypes && COLUMN_TYPE_BLOB == columnTypes[i] ){
								try {
									Class base64Class = Class.forName("android.util.Base64");
									Method encodeMethod = base64Class.getMethod("encodeToString", byte[].class, int.class);
							    	writeValue(serializer, (String)encodeMethod.invoke(null, cursor.getBlob(i), 2));
								} catch (Exception e) {
									Log.e(TAG, "ERR000KH", e);
									ErrorUtil.handleExceptionAndThrow("ERR000KH", e);
								}
						    }else{
						    	writeValue(serializer, cursor.getString(i));
						    }
						}
					}
					serializer.endTag(null, TAG_ROW);
				} while(cursor.moveToNext());
				
			    serializer.endTag(null, TAG_TABLE);
			}
		}finally{
			if( null != cursor ){
				cursor.close();
			}
		}
	}
	private static void writeValue(XmlSerializer serializer, String value) throws IllegalArgumentException, IllegalStateException, IOException  {
		if( value.length() > 0 ){
			serializer.startTag(null, TAG_VALUE).text(value).endTag(null, TAG_VALUE);
		}else{
			serializer.startTag(null, TAG_VALUE).endTag(null, TAG_VALUE);
		}
	}
	
	public static ArrayList<ContentValues> removeEntriesWhenPresent(HashMap<String, ArrayList<ContentValues>> operationMap, String tableName, String idColumnName) {
		ArrayList<ContentValues> restoreOperations = operationMap.get(tableName);
		if( null == restoreOperations ){
			return null;
		}
		ArrayList<ContentValues> removeOperations = new ArrayList<ContentValues>();
		for(ContentValues restoreCv: restoreOperations){
			if( restoreCv.containsKey(idColumnName)){
				removeOperations.add(restoreCv);
			}
		}
		for(ContentValues removeCv: removeOperations){
			restoreOperations.remove(removeCv);
		}
		return removeOperations;
	}
	
	public static ArrayList<ContentValues> removeEntriesWhenEquals(HashMap<String, ArrayList<ContentValues>> operationMap, String tableName, String idColumnName, String compareValue) {
		ArrayList<ContentValues> restoreOperations = operationMap.get(tableName);
		if( null == restoreOperations || null == compareValue ){
			return null;
		}
		ArrayList<ContentValues> removeOperations = new ArrayList<ContentValues>();
		for(ContentValues restoreCv: restoreOperations){
			if( restoreCv.containsKey(idColumnName) && compareValue.equals(restoreCv.getAsString(idColumnName))){
				removeOperations.add(restoreCv);
			}
		}
		for(ContentValues removeCv: removeOperations){
			restoreOperations.remove(removeCv);
		}
		return removeOperations;
	}
	
	
	public static void removeEntriesWhereMatch(HashMap<String, ArrayList<ContentValues>> operationMap, String tableName, String idColumnName, ArrayList<ContentValues> compareContentValues, String compareColumnName) {
		ArrayList<ContentValues> restoreOperations = operationMap.get(tableName);
		if( null == restoreOperations ){
			return;
		}
		ArrayList<ContentValues> removeOperations = new ArrayList<ContentValues>();
		for(ContentValues restoreCv: restoreOperations){
			for(ContentValues compareCv: compareContentValues){
				if( restoreCv.containsKey(idColumnName) && compareCv.containsKey(compareColumnName)){
					if( restoreCv.getAsLong(idColumnName) == compareCv.getAsLong(compareColumnName)){
						removeOperations.add(restoreCv);
					}
				}
			}
		}
		for(ContentValues removeCv: removeOperations){
			restoreOperations.remove(removeCv);
		}
	}

	
	public static void restoreTable(SQLiteDatabase mDB, HashMap<String, ArrayList<ContentValues>> operationMap, String tableName, String nullColumnHack) {
		ArrayList<ContentValues> restoreOperations = operationMap.get(tableName);
		if( null == restoreOperations ){
			return;
		}
		for(ContentValues restoreCv: restoreOperations){
			if( -1 == mDB.insert(tableName, nullColumnHack, restoreCv) ){
				Log.e(TAG, "ERR000KI");
				ErrorUtil.handle("ERR000KI", "", null);
				throw new HandledException("ERR000KI");
			}
		}
	}

	public static void cleanTable(SQLiteDatabase mDB, String tableName) {
		mDB.delete(tableName, null, null);
	}
	
	public static long selectTopTableId(SQLiteDatabase mDB, String tableName, String tableIdColumn) {
		long topTaskId = 1;
		Cursor cursor = mDB.query(tableName, new String[]{tableIdColumn}, null, null, null, null, tableIdColumn + " DESC LIMIT 1");
		assert null != cursor;
		try{
			if( cursor.moveToFirst() ){
				topTaskId = cursor.getLong(0);
			}
		}finally{
			cursor.close();
		}
		return topTaskId;
	}

	public static void shiftTableIds(HashMap<String, ArrayList<ContentValues>> operationMap, String tableName, String idColumnName, long topTableId) {
		ArrayList<ContentValues> restoreOperations = operationMap.get(tableName);
		if( null == restoreOperations ){
			return;
		}
		for(ContentValues restoreCv: restoreOperations){
			restoreCv.put(idColumnName, restoreCv.getAsLong(idColumnName) + topTableId);
		}
	}

	public static void shiftTableIdsExceptWhen(HashMap<String, ArrayList<ContentValues>> operationMap, String tableName, String idColumnName, long topTableId, String exceptionColumn, String exceptionValue) {
		ArrayList<ContentValues> restoreOperations = operationMap.get(tableName);
		if( null == restoreOperations || null == exceptionValue ){
			return;
		}
		for(ContentValues restoreCv: restoreOperations){
			if( !exceptionValue.equals(restoreCv.getAsString(exceptionColumn)) ){
				restoreCv.put(idColumnName, restoreCv.getAsLong(idColumnName) + topTableId);
			}
		}
	}

	
	public static void shiftTableUriIds(HashMap<String, ArrayList<ContentValues>> operationMap, String tableName, String idColumnName, String authority, String path, long topTableId) {
		ArrayList<ContentValues> restoreOperations = operationMap.get(tableName);
		if( null == restoreOperations ){
			return;
		}
		for(ContentValues restoreCv: restoreOperations){
			if( restoreCv.containsKey(idColumnName) && null != restoreCv.getAsString(idColumnName)){
				
				Uri uri = Uri.parse(restoreCv.getAsString(idColumnName));
				if( "content".equalsIgnoreCase(uri.getScheme()) && authority.equals(uri.getAuthority()) && uri.getPath().substring(0, uri.getPath().lastIndexOf('/')).equals(path)){
					Uri.Builder newUri = uri.buildUpon().path(uri.getPath().substring(0, uri.getPath().lastIndexOf('/'))).appendPath(String.valueOf(Long.parseLong(uri.getPath().substring(uri.getPath().lastIndexOf('/')+1)) + topTableId));

//					Log.v("URI", uri.getPath().substring(uri.getPath().lastIndexOf('/')+1));
//					Log.v("URI", String.valueOf(Long.parseLong(uri.getPath().substring(uri.getPath().lastIndexOf('/')+1)) + topTableId));

					restoreCv.put(idColumnName, newUri.build().toString());
				}
			}
		}
	}
	
	public static void restoreTableWithByteArrayColumns(SQLiteDatabase mDB, HashMap<String, ArrayList<ContentValues>> operationMap, String tableName, String nullColumnHack, String[] byteArrayColumns) {
		ArrayList<ContentValues> restoreOperations = operationMap.get(tableName);
		if( null == restoreOperations ){
			return;
		}
		try {
			Class base64Class = Class.forName("android.util.Base64");
			Method encodeMethod = base64Class.getMethod("decode", new Class[]{String.class, int.class});
			for(ContentValues restoreCv: restoreOperations){
				for(String byteArrayColumn: byteArrayColumns ){
					if( null != byteArrayColumn && restoreCv.containsKey(byteArrayColumn) ){
						restoreCv.put(byteArrayColumn, (byte[])encodeMethod.invoke(null, restoreCv.getAsString(byteArrayColumn), 0) );
					}
				}
				if( -1 == mDB.insert(tableName, nullColumnHack, restoreCv) ){
					Log.e(TAG, "ERR000K4");
					ErrorUtil.handle("ERR000K4", "Failed to insert restore record for table " + tableName + " contentValues " +  restoreCv.toString(), null);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "ERR000K5", e);
			ErrorUtil.handleExceptionAndThrow("ERR000K5", e);
		}
		
	}
		
	public static Intent getPrivacySettingsLaunchIntent(){
    	return new Intent("android.settings.PRIVACY_SETTINGS"); // android.provider.Settings.ACTION_PRIVACY_SETTINGS Since API level 5
	}
	
	/**
	 * Re-assigns the label references in restore label content records to the current label where an overlap occurs. <br/>
	 * <br/>
	 * NOTE: There is no special handling of the ARCHIVE label other than to simply ignore it. Since The ARCHIVE label always has ID "1" simply removing the restore Archive label (done elsewhere) should suffice since the restore labeled content records will point to the same ID.<br/>
	 *  
	 * @param mDB
	 * @param labelTableName
	 * @param labelRestoreOperations
	 * @param labeledContentRestoreOperations
	 */
	public static void reassignDuplicateLabels(SQLiteDatabase mDB, String labelTableName, ArrayList<ContentValues> labelRestoreOperations, ArrayList<ContentValues> labeledContentRestoreOperations, ArrayList<ContentValues> restoreFilterElementOperations) { // , String labelTableName, String labeledContentTableName
		// Create current LabelName -> LabelId map.
		HashMap<String, String> currentLabelNameToIdMap = new HashMap<String, String>();
		// _user_applied labels need no special handling.  
		Cursor cursor = mDB.query(labelTableName, new String[]{Task.Labels._ID, Task.Labels.DISPLAY_NAME}, Task.Labels._USER_APPLIED+"==?", new String[]{Task.Labels.USER_APPLIED_TRUE}, null, null, null);
		assert null != cursor;
		try{
			while( cursor.moveToNext() ){
				currentLabelNameToIdMap.put(cursor.getString(1).toLowerCase(), cursor.getString(0));
			}
		}finally{
			cursor.close();
		}
		reassignDuplicateLabels(currentLabelNameToIdMap, labelRestoreOperations, labeledContentRestoreOperations, restoreFilterElementOperations);
	}
	
	public static void reassignDuplicateLabels(HashMap<String, String> currentLabelNameToIdMap, ArrayList<ContentValues> labelRestoreOperations, ArrayList<ContentValues> labeledContentRestoreOperations, ArrayList<ContentValues> restoreFilterElementOperations) {

		// List of label records to remove from restore data.
		ArrayList<ContentValues> removeLabelContentValues = new ArrayList<ContentValues>();

		// For each restore label ...
		for(ContentValues labelRestoreCv: labelRestoreOperations){
			String restoreLabelName = labelRestoreCv.getAsString(Task.Labels.DISPLAY_NAME);
			if( null != restoreLabelName ){
				restoreLabelName = restoreLabelName.toLowerCase(); // Ignore case.
				// If restore label is already defined in current labels then ..
				if( currentLabelNameToIdMap.containsKey(restoreLabelName) ){
					
					String restoreLabelId = labelRestoreCv.getAsString(Task.Labels._ID); // Will be replaced with current label ID where found.
					String currentLabelId = currentLabelNameToIdMap.get(restoreLabelName);

					// For each restore labeled content record ... 
					for(ContentValues labeledContentRestoreCv: labeledContentRestoreOperations){
						// Replace any reference (labelId) to the restore label ID 
						//   with the equivalent current label ID.  
						
						// Does restore LabeledContent._LABEL_ID == restore Label._ID being switched?
						String labeledContentRestoreLabelId = labeledContentRestoreCv.getAsString(Task.LabeledContent._LABEL_ID);
						if( null != labeledContentRestoreLabelId ){
							if( labeledContentRestoreLabelId.equals(restoreLabelId) ){
								labeledContentRestoreCv.put(Task.LabeledContent._LABEL_ID, currentLabelId);
							}
						}
					}
					
					// For each restore filter element record ... 
					for(ContentValues restoreFilterElementRestoreCv: restoreFilterElementOperations){
						// Replace any restore FilterElement._uri reference to duplicate restore labelId with with current label id.   
						//   with the equivalent current label ID.  
						
						// Does ID in restore FILTER_ELEMENT._uri == restore Label._ID being switched?
						String restoreFilterElementUri = restoreFilterElementRestoreCv.getAsString(Task.FilterElement._CONSTRAINT_URI);
						if( null != restoreFilterElementUri ){
							if( restoreFilterElementUri.startsWith("content://com.flingtap.done.taskprovider/tasks/mask/1/label/") ){ // TODO: !!!! find constants for this URI. (Task.Labels.BASE_PATH_FOR_FILTER_ELEMENTS)
								String restoreFilterElementUriId = Uri.parse(restoreFilterElementUri).getLastPathSegment();
								if( restoreFilterElementUriId.equals(restoreLabelId) ){
									restoreFilterElementRestoreCv.put(Task.FilterElement._CONSTRAINT_URI, "content://com.flingtap.done.taskprovider/tasks/mask/1/label/"+currentLabelId);
								}
							}
						}
					}
					
					// Prepare to remove the restore label.
					removeLabelContentValues.add(labelRestoreCv);
				}
			}
		}
		
		// Actually remove label records from restore data.
		for(ContentValues removeCv: removeLabelContentValues){
			labelRestoreOperations.remove(removeCv);
		}
	}
	
	
	/**
	 * 
	 * @param mDB
	 * @param filterTableName
	 * @param filterRestoreOperations
	 */
	public static void renameFilters(Resources resources, SQLiteDatabase mDB, String filterTableName, ArrayList<ContentValues> filterRestoreOperations) { 
		// Create list of (non-stock) filter names.
		ArrayList<String> currentFilterNames = new ArrayList<String>();
		Cursor cursor = mDB.query(filterTableName, new String[]{Task.Filter.DISPLAY_NAME}, Task.Filter._PERMANENT+" IS NULL", null, null, null, null);
		assert null != cursor;
		try{
			while( cursor.moveToNext() ){
				currentFilterNames.add(cursor.getString(0));
			}
		}finally{
			cursor.close();
		}
		renameFilters(resources, currentFilterNames, filterRestoreOperations);
	}
	public static void renameFilters(Resources resources, ArrayList<String> currentFilterNames, ArrayList<ContentValues> filterRestoreOperations){ 
		// For each restore filter CV ...
		for(ContentValues restoreFilterCV: filterRestoreOperations){
			// If display name exists.
			if( restoreFilterCV.containsKey(Task.Filter.DISPLAY_NAME) ){
				String restoreFilterName = restoreFilterCV.getAsString(Task.Filter.DISPLAY_NAME);
				// For each current filter name ...
				for(String currentFilterName: currentFilterNames){
					// If restore filter name == current filter name ...
					if( restoreFilterName.equalsIgnoreCase(currentFilterName)){
						// Rename restore filter name.
						restoreFilterCV.put(Task.Filter.DISPLAY_NAME, restoreFilterName + " " + resources.getString(R.string.append_restored)); // TODO: !!! string.xml resource should specify a placeholder for the original filter name so it can be placed appropriately relative to the new text.
					}
				}
			}
		}
	}
	
	
}
