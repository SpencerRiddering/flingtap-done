// Licensed under the Apache License, Version 2.0

package com.flingtap.done;


import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.flingtap.common.HandledException;
import com.flingtap.done.backup.BackupManagerProxy;
import com.flingtap.done.backup.BackupNotifications;
import com.flingtap.done.backup.BackupStatusPreference;
import com.flingtap.done.backup.BackupUtil;
import com.flingtap.done.backup.PrivacySettingsUtil;
import com.flingtap.done.base.R;
import com.flingtap.done.provider.Task;
import com.flingtap.done.util.AddonUtil;
import com.flingtap.done.util.LicenseUtil;
import com.tomgibara.android.veecheck.Veecheck;

/**
 * REMEMBER: When disabling a feature, make sure to reset the system state for that feature (so no data becomes permanently inaccessable).
 * 
 *  
 *   
 *  
 * @author spencer
 *
 */
public class ApplicationPreferenceActivity extends PreferenceActivity implements OnPreferenceChangeListener, OnPreferenceClickListener {
	
	private static final String TAG = "ApplicationPreferenceActivity";
	
    public static final int HELP_ID = Menu.FIRST + 10;
    
    private static final String REDIRECT_URI = "com.flingtap.done.application_preferences_activity.redirect_uri";
    
    private static final String NOTIFY_URI_BACKUP_NOT_SUPPORTED = "http://flingtap.com/done/backup/notify/not_supported";
    private static final String NOTIFY_URI_BACKUP_DISABLED_IN_PRIVACY_SETTINGS = "http://flingtap.com/done/backup/notify/disabled_in_privacy_settings";
    private static final String NOTIFY_URI_BACKUP_ENABLED = "http://flingtap.com/done/backup/notify/enabled";

    private CheckBoxPreference labelsEnabledPref = null;
    private CheckBoxPreference archiveEnabledPref = null;
    private CheckBoxPreference callminderEnabledPref = null;
    private Preference clearArchiveNowPref = null;
//    private Preference defaultCategoryPref = null;
    private Preference updateRequiredUri = null;
//    private Preference bucketsEnabledPref = null;
//    private Preference editBucketsPref = null;
    private ListPreference defaultEditorTabPref = null;
    private CheckBoxPreference backupEnabledPref = null;
    
    
	public static final String ACTION_TOGGLE_LABELS  = "com.flingtap.done.intent.action.TOGGLE_LABELS";
	public static final String ACTION_TOGGLE_ARCHIVE = "com.flingtap.done.intent.action.TOGGLE_ARCHIVE";
	public static final String ACTION_TOGGLE_BACKUP = "com.flingtap.done.intent.action.TOGGLE_BACKUP";
//	public static final String ACTION_TOGGLE_BUCKETS  = "com.flingtap.done.intent.action.TOGGLE_BUCKETS";
	public static final String ACTION_REDIRECT = "com.flingtap.done.intent.action.REDIRECT";
    
	public static final String EXTRA_TOGGLE_VALUE    = "com.flingtap.done.intent.extra.TOGGLE_VALUE";
	
//	private static final int REQUEST_CHANGE_DEFAULT_CATEGORY_CODE = 1;
	
	public static Intent createToggleLabelsIntent(boolean value){
		Intent togglePreferenceIntent = new Intent(ACTION_TOGGLE_LABELS);
		togglePreferenceIntent.putExtra(ApplicationPreferenceActivity.EXTRA_TOGGLE_VALUE, value);
		return togglePreferenceIntent;
	}
	
	public static Intent createToggleArchiveIntent(boolean value){
		Intent togglePreferenceIntent = new Intent(ACTION_TOGGLE_ARCHIVE);
		togglePreferenceIntent.putExtra(ApplicationPreferenceActivity.EXTRA_TOGGLE_VALUE, value);
		return togglePreferenceIntent;
	}
	
	public static Intent createToggleBackupIntent(boolean value){
		Intent togglePreferenceIntent = new Intent(ACTION_TOGGLE_BACKUP);
		togglePreferenceIntent.putExtra(ApplicationPreferenceActivity.EXTRA_TOGGLE_VALUE, value);
		return togglePreferenceIntent;
	}
	
	public static Intent getLaunchIntent(Context context){
		Intent prefIntent = new Intent(context, ApplicationPreferenceActivity.class);
		return prefIntent;
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	try{
	        //Log.v(TAG, "onCreate(..) called.");
			SessionUtil.onSessionStart(this);
	
	        // Explicitly set the preferences file name.
	        PreferenceManager prefMan = getPreferenceManager(); 
	        prefMan.setSharedPreferencesName(ApplicationPreference.NAME);
	        
	        // Load the preferences from an XML resource
	        addPreferencesFromResource(R.xml.application_preferences);
	        
			
			Intent intent = getIntent();
			String action = intent.getAction();
			
			if( ACTION_TOGGLE_LABELS.equals(action) ){
				if( !intent.getExtras().containsKey(EXTRA_TOGGLE_VALUE) ){
					setResult(RESULT_CANCELED);
					finish();
					return;
				}
				labelsEnabledPref = (CheckBoxPreference)findPreference(ApplicationPreference.ENABLE_LABELS);
				if( labelsEnabledPref.isChecked() != intent.getBooleanExtra(EXTRA_TOGGLE_VALUE, false) ){
					changeLabelsPreference(intent.getBooleanExtra(EXTRA_TOGGLE_VALUE, false), false);
				}
				setResult(RESULT_OK);
				finish();
				return;
			}else if( ACTION_TOGGLE_ARCHIVE.equals(action) ){
				if( !intent.getExtras().containsKey(EXTRA_TOGGLE_VALUE) ){
					setResult(RESULT_CANCELED);
					finish();
					return;
				}
				archiveEnabledPref = (CheckBoxPreference)findPreference(ApplicationPreference.ENABLE_ARCHIVE);
				if( archiveEnabledPref.isChecked() != intent.getBooleanExtra(EXTRA_TOGGLE_VALUE, false) ){
					changeArchivePreference(intent.getBooleanExtra(EXTRA_TOGGLE_VALUE, false), false);
				}
				setResult(RESULT_OK);
				finish();
				return;
//			}else if( ACTION_TOGGLE_BUCKETS.equals(action) ){
//				if( !intent.getExtras().containsKey(EXTRA_TOGGLE_VALUE) ){
//					setResult(RESULT_CANCELED);
//					finish();
//					return;
//				}
//				CheckBoxPreference bucketsEnabledPref = (CheckBoxPreference)findPreference(ApplicationPreference.ENABLE_BUCKETS);
//				if( bucketsEnabledPref.isChecked() != intent.getBooleanExtra(EXTRA_TOGGLE_VALUE, false) ){
//					changeBucketsPreference(intent.getBooleanExtra(EXTRA_TOGGLE_VALUE, false), false);
//				}
//				setResult(RESULT_OK);
//				finish();
//				return;
			}else if( ACTION_TOGGLE_BACKUP.equals(action) ){
				if( !intent.getExtras().containsKey(EXTRA_TOGGLE_VALUE) ){
					setResult(RESULT_CANCELED);
					finish();
					return;
				}
		        backupEnabledPref = (CheckBoxPreference)findPreference(ApplicationPreference.BACKUP_ENABLED);
				if( backupEnabledPref.isChecked() != intent.getBooleanExtra(EXTRA_TOGGLE_VALUE, false) ){
					if( changeBackupEnabledPreference(intent.getBooleanExtra(EXTRA_TOGGLE_VALUE, false), false) ){
						// No listener is attached yet so update the preference value ourselves.
						backupEnabledPref.setChecked(intent.getBooleanExtra(EXTRA_TOGGLE_VALUE, false));
					}
				}
				setResult(RESULT_OK);
				finish();
				return;
				
			}else if( ACTION_REDIRECT.equals(action) ){
				Intent theIntent = getIntent();
				if( BackupNotifications.needsClear(theIntent) ){
					BackupNotifications.clear(this, theIntent);
				}
				Intent privacySettingsIntent = theIntent.getParcelableExtra(REDIRECT_URI);
				if( null != privacySettingsIntent ){
					privacySettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(privacySettingsIntent);
					finish();
					return;
				}
			}else{
				if( BackupNotifications.needsClear(getIntent()) ){
					BackupNotifications.clear(this, getIntent());
				}
			}
			Event.onEvent(Event.VIEW_APPLICATION_PREFERENCES, null); // Map<String, String> parameters = new HashMap<String, String>();
			
    		

	        
	
	        
	        labelsEnabledPref = (CheckBoxPreference)findPreference(ApplicationPreference.ENABLE_LABELS);
	        labelsEnabledPref.setOnPreferenceChangeListener(this);
	        
	        archiveEnabledPref = (CheckBoxPreference)findPreference(ApplicationPreference.ENABLE_ARCHIVE);
	        archiveEnabledPref.setOnPreferenceChangeListener(this);
	        
	        clearArchiveNowPref = findPreference(ApplicationPreference.CLEAR_ARCHIVE);
	        clearArchiveNowPref.setOnPreferenceClickListener(this);

//	        bucketsEnabledPref = findPreference(ApplicationPreference.ENABLE_BUCKETS);
//	        bucketsEnabledPref.setOnPreferenceChangeListener(this);
//	        	        
//	        editBucketsPref = findPreference(ApplicationPreference.EDIT_BUCKETS);
//	        editBucketsPref.setOnPreferenceClickListener(this);
	        
//	        defaultCategoryPref = findPreference(ApplicationPreference.DEFAULT_BUCKET);
//	        defaultCategoryPref.setOnPreferenceClickListener(this);	        
	        
	        callminderEnabledPref = (CheckBoxPreference)findPreference(ApplicationPreference.CALLMINDER_ENABLED);
	        callminderEnabledPref.setOnPreferenceChangeListener(this);	        
	        
	        updateRequiredUri = findPreference(ApplicationPreference.UPDATE_REQUIRED_URI);
	        updateRequiredUri.setOnPreferenceClickListener(this);	        
	        
	        defaultEditorTabPref = (ListPreference)findPreference(ApplicationPreference.DEFAULT_EDITOR_TAB);
	        defaultEditorTabPref.setOnPreferenceChangeListener(this);
	        defaultEditorTabPref.setOnPreferenceClickListener(this);	        
//			defaultEditorTabPref.setDefaultValue(ApplicationPreference.DEFAULT_EDITOR_TAB_DEFAULT);
			updateDefaultEditorTabSummary(defaultEditorTabPref.getValue());
	        
	        
	        PreferenceCategory completedTaskCategory = (PreferenceCategory)findPreference(ApplicationPreference.COMPLETED_TASKS_CATEGORY);
	        if( archiveEnabledPref.isChecked() ){
	        	makeArchiveCompletedPreferenceActive(this, completedTaskCategory);
	        }else{
	        	makeDeleteCompletedPreferenceActive(this, completedTaskCategory);
	        }
	        
	        backupEnabledPref = (CheckBoxPreference)findPreference(ApplicationPreference.BACKUP_ENABLED);
	        if( Integer.parseInt(Build.VERSION.SDK) >= 8 || // Build.VERSION_CODES.FROYO
	        		AddonUtil.doesPackageHaveSameSignature(this, SharedConstant.ADDON_BACKUP) ){ // Or Backup addon is installed. 
	        	backupEnabledPref.setOnPreferenceChangeListener(this);
	        }else{ // Prior to FROYO then remove the preference group.
	        	PreferenceGroup backupPrefGroup = (PreferenceGroup)findPreference(ApplicationPreference.BACKUP_GROUP);
	        	getPreferenceScreen().removePreference(backupPrefGroup);
	        }
	        
	        
		// TODO: Remove this code. I don't want to give user choice to "not check for updates" or change update URI.        
		//        // Load the preferences from an Intent
		//        Intent veecheckIntent = new Intent(this, UpdatePreferences.class);
		////        ComponentName cn = new ComponentName(StaticConfig.PACKAGE_NAME, "com.flingtap.done.UpdatePreferences");
		////        Intent veecheckIntent = new Intent("android.intent.action.MAIN");
		////        veecheckIntent.setComponent(cn);
		//        addPreferencesFromIntent(veecheckIntent);
	        
		        
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0000A", exp);
			ErrorUtil.handleExceptionFinish("ERR0000A", exp, this);
		}
		
    }
    
	private static void makeArchiveCompletedPreferenceActive(Context context, PreferenceCategory completedTaskCategory) {
        completedTaskCategory.removeAll();
		CheckBoxPreference completedTaskPreference = new CheckBoxPreference(context);
		completedTaskPreference.setKey(ApplicationPreference.AUTO_ARCHIVE_COMPLETED);
		completedTaskPreference.setDefaultValue(ApplicationPreference.AUTO_ARCHIVE_COMPLETED_DEFAULT);
		completedTaskPreference.setTitle(R.string.automaticallyArchive);
		completedTaskPreference.setSummary(R.string.completedTasksWillBeArchivedAfterMidnightEachDay);
		completedTaskCategory.addPreference(completedTaskPreference);
	}
	private static void makeDeleteCompletedPreferenceActive(Context context, PreferenceCategory completedTaskCategory) {
        completedTaskCategory.removeAll();
		CheckBoxPreference completedTaskPreference = new CheckBoxPreference(context);
		completedTaskPreference.setKey(ApplicationPreference.AUTO_DELETE_COMPLETED);
		completedTaskPreference.setDefaultValue(ApplicationPreference.AUTO_DELETE_COMPLETED_DEFAULT);
		completedTaskPreference.setTitle(R.string.automaticallyDelete);
		completedTaskPreference.setSummary(R.string.completedTasksWillBeDeletedAfterMidnightEachDay);
		completedTaskCategory.addPreference(completedTaskPreference);
	}

	
//	@Override
//	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//	  	super.onActivityResult(requestCode, resultCode, data);
//	  	try{
//	  		//Log.v(TAG, "onActivityResult(..) called.");
//	  		if( resultCode == RESULT_CANCELED ){
//	  			return;
//	  		}
//	  		switch(requestCode){
//	  			case REQUEST_CHANGE_DEFAULT_CATEGORY_CODE:
//	  				BucketUtil.switchDefaultCategory(this, String.valueOf(ContentUris.parseId(data.getData())) );
//	  				break;
//	  		}
//	  		
//		}catch(HandledException h){ // Ignore.
//		}catch(Exception exp){
//			Log.e(TAG, "ERR000H5", exp);
//			ErrorUtil.handleExceptionNotifyUser("ERR000H5", exp, this);
//		}  
//	}
	
//    @Override
//    protected void onPause() {
//    	super.onPause();
//    	
//    	/**
//    	 *  Copyright 2009 Tom Gibara
//    	 *  
//    	 *  Licensed under the Apache License, Version 2.0 (the "License"); 
//    	 *  you may not use this file except in compliance with the License. 
//    	 *  You may obtain a copy of the License at 
//    	 *  
//    	 *  	http://www.apache.org/licenses/LICENSE-2.0 
//    	 *  
//    	 *  Unless required by applicable law or agreed to in writing, software 
//    	 *  distributed under the License is distributed on an "AS IS" BASIS, 
//    	 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
//    	 *  See the License for the specific language governing permissions and 
//    	 *  limitations under the License. 
//    	 */		
//    	// ************************************************************************
//    	//reschedule the checking in case the user has changed anything
//		sendBroadcast(new Intent(Veecheck.getRescheduleAction(this)));
//    	// ************************************************************************
//
//    }    
//    @Override
//    protected void onResume() {
//    	super.onResume();
////		  onContentChanged();
////  		ComponentName comp = new ComponentName(this.getPackageName(), getClass().getName());
////			startActivity(new Intent().setComponent(comp)); 
//
//    }
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//    	super.onActivityResult(requestCode, resultCode, data);
//    	Log.v(TAG, "onActivityResult(..) called.");
//    	if( RESULT_OK == resultCode ){
//    		  onContentChanged();
//    		ComponentName comp = new ComponentName(this.getPackageName(), getClass().getName());
//			startActivity(new Intent().setComponent(comp)); 
//    	}
//    }
//    boolean restarting = false;
//    @Override
//    protected void onNewIntent(Intent intent) {
//    	super.onNewIntent(intent);
//    	Log.v(TAG, "onNewIntent(..) called.");
//    	if( restarting ){
//    		restarting = false;
//    	}else{
////        	onContentChanged();
//        	startActivity(getIntent());
////        	getListView().invalidate();
////        	getListView().postInvalidate();
//    		restarting = true;
//    	}
//    }
    
	private static void toggleArchiveFilterElementsActiveness(Context context, boolean active) {
		ContentValues cv = new ContentValues(1);
		if( active ){
			cv.put(Task.FilterElementColumns._ACTIVE, Task.FilterElementColumns.ACTIVE_TRUE);
		}else{
			cv.putNull(Task.FilterElementColumns._ACTIVE);
		}
		int count = context.getContentResolver().update(Task.FilterElement.CONTENT_URI, cv, Task.FilterElementColumns._CONSTRAINT_URI+"=?", new String[]{Constraint.Version1.REPOSITORY_CONTENT_URI_STRING});
		assert 1 == count;
	}

	private static void toggleUnlabeledFilterElementActiveness(Context context, boolean active) {
		ContentValues cv = new ContentValues(1);
		if( active ){
			cv.put(Task.FilterElementColumns._ACTIVE, Task.FilterElementColumns.ACTIVE_TRUE);
		}else{
			cv.putNull(Task.FilterElementColumns._ACTIVE);
		}
		int count = context.getContentResolver().update(Task.FilterElement.CONTENT_URI, cv, Task.FilterElementColumns._CONSTRAINT_URI+"=?", new String[]{Constraint.Version1.UNLABELED_CONTENT_URI_STRING});
		// assert 1 == count; // This may be 0 for filters not created by user (ARCHIVE, ALL, etc..).
	}	

//	private static void toggleArchiveFilterActiveness(Context context, boolean active) {
//		ContentValues cv = new ContentValues(1);
//		cv.put(Task.Filter._ACTIVE, active?Task.Filter.ACTIVE_TRUE:Task.Filter.ACTIVE_FALSE);
//		int count = context.getContentResolver().update(Task.FilterElement.CONTENT_URI, cv, Task.Filter._PERMANENT+"=?", new String[]{TaskProvider.ID_FILTER_ARCHIVE});
//		assert 1 == count;
//		if( 1 != count ){
//			Log.e(TAG, "ERR000EY");
//			ErrorUtil.handleNotifyUser("ERR000EY", "", context, context);										
//		}
//		
//	}

	private static void togglePermanentFilterActiveness(Context context, String filterPermanentId, boolean active) {
		ContentValues cv = new ContentValues(1);
		if( active ){
			cv.put(Task.Filter._ACTIVE, Task.Filter.ACTIVE_TRUE);
		}else{
			cv.putNull(Task.Filter._ACTIVE);
		}
		int count = context.getContentResolver().update(Task.Filter.CONTENT_URI, cv, Task.Filter._PERMANENT+"=?", new String[]{filterPermanentId});
		assert 1 == count;
		if( 1 != count ){
			Log.e(TAG, "ERR000EZ");
			ErrorUtil.handleExceptionNotifyUser("ERR000EZ",(Exception)(new Exception("filterPermanentId="+filterPermanentId+",active="+active)).fillInStackTrace(), context);										
		}
		
	}
	
//	private void addStandardLabels(){
//	    // ****************************
//	    // "Work" label
//	    // ****************************
//		LabelUtil.createUserAppliedLabel(this, getString(R.string.label_display_name_work), null);
//
//	    // ****************************
//	    // "Personal" label
//	    // ****************************
//		LabelUtil.createUserAppliedLabel(this, getString(R.string.label_display_name_personal), null);		
//		
////		Uri workFilterUri = FilterUtil.createFilter(this, getString(R.string.label_display_name_work));
////	    // ****************************
////	    // "Work" label
////	    // ****************************
////		LabelUtil.createUserAppliedLabel(this, getString(R.string.label_display_name_work), workFilterUri.getLastPathSegment());
//////	    ContentValues values = new ContentValues();
//////		values.put(Task.LabelsColumns.DISPLAY_NAME, getString(R.string.label_display_name_work));
//////		Uri workUri = getContentResolver().insert(Uri.parse("content://"+Task.AUTHORITY+"/user_applied_label"), values);
//////		assert null != workUri;
////		
////		
//////	    ContentValues values = new ContentValues();
//////	    values.put(Task.LabelsColumns.DISPLAY_NAME,  getText(R.string.label_display_name_work).toString());
//////	    values.put(Task.LabelsColumns._USER_APPLIED, Task.LabelsColumns.USER_APPLIED_TRUE);
//////	    values.put(Task.LabelsColumns.CREATED_DATE,  System.currentTimeMillis());
//////	    Uri workLabelUri = getContentResolver().insert(Task.Labels.CONTENT_URI, values);
//////	    long workLabelId = ContentUris.parseId(workLabelUri);
//////	    
//////		// "Work" filter element.
//////		if( -1 != workLabelId ){
//////		    values.clear();
//////		    values.put(Task.FilterElementColumns._FILTER_ID, TaskProvider.ID_FILTER_BASIC);
//////		    values.put(Task.FilterElementColumns._ACTIVE, Task.FilterElementColumns.ACTIVE_TRUE);
//////		    values.put(Task.FilterElementColumns._CONSTRAINT_URI, DatabaseUtils.sqlEscapeString(Constraint.Version1.LABEL_CONTENT_URI_STRING+"/"+workLabelId));
//////		    values.put(Task.FilterElementColumns._APPLY_WHEN_ACTIVE, Task.FilterElementColumns.APPLY_WHEN_ACTIVE);
//////		    values.put(Task.FilterElementColumns._PHASE, Task.FilterElementColumns.PHASE_EXPLODE);
//////		    values.put(Task.FilterElementColumns.CREATED_DATE, System.currentTimeMillis());
//////		    getContentResolver().insert(Task.FilterElement.CONTENT_URI, values);
//////		}
////		
////	    // ****************************
////	    // "Personal" label
////	    // ****************************
////		Uri personalFilterUri = FilterUtil.createFilter(this, getString(R.string.label_display_name_personal));
////		LabelUtil.createUserAppliedLabel(this, getString(R.string.label_display_name_personal), personalFilterUri.getLastPathSegment());
//////		values.clear();
//////		values.put(Task.LabelsColumns.DISPLAY_NAME, getString(R.string.label_display_name_personal));
//////		Uri personalUri = getContentResolver().insert(Uri.parse("content://"+Task.AUTHORITY+"/user_applied_label"), values);
//////		assert null != personalUri;
////		
//////	    values = new ContentValues();
//////	    values.put(Task.LabelsColumns.DISPLAY_NAME,  getText(R.string.label_display_name_personal).toString());
//////	    values.put(Task.LabelsColumns._USER_APPLIED, Task.LabelsColumns.USER_APPLIED_TRUE);
//////	    values.put(Task.LabelsColumns.CREATED_DATE,  System.currentTimeMillis());
//////	    Uri personalLabelUri = getContentResolver().insert(Task.Labels.CONTENT_URI, values);
//////	    long personalLabelId = ContentUris.parseId(personalLabelUri);
//////
//////		// "Personal" filter element.
//////		if( -1 != personalLabelId ){
//////		    values.clear();
//////		    values.put(Task.FilterElementColumns._FILTER_ID, TaskProvider.ID_FILTER_BASIC);
//////		    values.put(Task.FilterElementColumns._ACTIVE, Task.FilterElementColumns.ACTIVE_TRUE);
//////		    values.put(Task.FilterElementColumns._CONSTRAINT_URI, DatabaseUtils.sqlEscapeString(Constraint.Version1.LABEL_CONTENT_URI_STRING+"/"+personalLabelId));
//////		    values.put(Task.FilterElementColumns._APPLY_WHEN_ACTIVE, Task.FilterElementColumns.APPLY_WHEN_ACTIVE);
//////		    values.put(Task.FilterElementColumns._PHASE, Task.FilterElementColumns.PHASE_EXPLODE);
//////		    values.put(Task.FilterElementColumns.CREATED_DATE, System.currentTimeMillis());
//////		    getContentResolver().insert(Task.FilterElement.CONTENT_URI, values);
//////		}
//			
//	    
//	}

	// Conditions: 
	//   1. Archive on, Labels on --> Archive on
	// * 2. Archive on, Labels on -->             Labels on
	// * 3. Archive on            -->                       (Basic filter) !! Doesn't include archive or unlabeled filter element.
	//   4.             Labels on --> 						(Basic filter)
	//   5. Archive on            --> Archive on, Labels on
	//   6.             Labels on --> Archive on, Labels on
	//   7.                       --> Archive on
	//   8.                       -->             Labels on
	public static void disableArchive(Context context) {
		// Delete archived tasks.
		context.getContentResolver().delete(Uri.parse("content://"+Task.AUTHORITY+"/archive_tasks"), null, null);
		
		SharedPreferences settings = context.getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE);
		boolean enableLabels = settings.getBoolean(ApplicationPreference.ENABLE_LABELS, ApplicationPreference.ENABLE_LABELS_DEFAULT);
//		boolean enableBuckets = settings.getBoolean(ApplicationPreference.ENABLE_BUCKETS, ApplicationPreference.ENABLE_BUCKETS_DEFAULT);

		if( enableLabels 
//				|| enableBuckets
				){
			// Switch to "All" filter.
			FilterUtil.switchSelectedToPermanantFilter(context, TaskProvider.ID_FILTER_ALL);	
			
			// Deactivate "Archive" Filter.
			togglePermanentFilterActiveness(context, TaskProvider.ID_FILTER_ARCHIVE, false);
			
			// Deactivate all archive filter elements. 
			toggleArchiveFilterElementsActiveness(context, false);

		}else{ // Switch to single basic filter.

// No need for this because switchSelectedToPermanantFilter(..) now does this for us.
//			// Activate "Basic" Filter.
//			toggleFilterActiveness(context, TaskProvider.ID_FILTER_BASIC, true);
			
			// Switch to "Basic" filter.
			FilterUtil.switchSelectedToPermanantFilter(context, TaskProvider.ID_FILTER_BASIC);	
			
			// Delete all non-permanent filters
			context.getContentResolver().delete(Task.Filter.CONTENT_URI, Task.Filter._PERMANENT+" IS NULL", null);
			
			// Deactivate "Archive" Filter.
			togglePermanentFilterActiveness(context, TaskProvider.ID_FILTER_ARCHIVE, false);
			
			// Deactivate "All" Filter.
			togglePermanentFilterActiveness(context, TaskProvider.ID_FILTER_ALL, false);
		}
		
		// Update "All" filter description.
		updateAllFilterDescription(context, null);
		
		// Re-apply filter bits.
		FilterUtil.applyFilterBits(context);
		
//		// If the repository is set to "archived" then change it to the default repository.
//		// TODO: !! The following code should be refactored and centralized into RepositoryFilterElementDelegate, or maybe moved into the content provider.
//		Cursor filterElementCursor = context.getContentResolver().query(Task.FilterElement.CONTENT_URI, new String[]{Task.FilterElement._PARAMETERS}, Task.FilterElementColumns._CONSTRAINT_URI+"=? AND "+Task.FilterElementColumns._PARAMETERS+"=?", new String[]{"content://"+Task.AUTHORITY+'/'+Constraint.Version1.REPOSITORY_URI_PATTERN_STRING, RepositoryFilterElementDelegate.getArchiveRepositoryFilterElementParameters()}, null);
//		assert null != filterElementCursor;
//		if( filterElementCursor.moveToFirst() ){
//			// Need to reset the repository to the default repository.
//			ContentValues cv = new ContentValues(1);
//			cv.put(Task.FilterElement._PARAMETERS, RepositoryFilterElementDelegate.getDefaultRepositoryFilterElementParameters());
//			int count = context.getContentResolver().update(Task.FilterElement.CONTENT_URI, cv, Task.FilterElementColumns._CONSTRAINT_URI+"=?", new String[]{"content://"+Task.AUTHORITY+'/'+Constraint.Version1.REPOSITORY_URI_PATTERN_STRING});
//			assert 1 == count;
//			// Re-apply filter bits in light of the repository change.
//			FilterUtil.applyFilterBits(context);
//		}
//		filterElementCursor.close();
	}

//	public static void disableBuckets(Context context) {
//
//    	BucketUtil.deleteAllBuckets(context); // Delete all buckets, except for default mBucket.
//    	BucketUtil.nullifyAllTaskBucketReferences(context); // Needed to nullify the _bucket field for tasks that 
//		
//    	
//		SharedPreferences settings = context.getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE);
//		boolean enableLabels = settings.getBoolean(ApplicationPreference.ENABLE_LABELS, ApplicationPreference.ENABLE_LABELS_DEFAULT);
//		boolean enableArchive = settings.getBoolean(ApplicationPreference.ENABLE_ARCHIVE, ApplicationPreference.ENABLE_ARCHIVE_DEFAULT);
//
//		if( enableLabels || enableArchive ){
//			
//// Not needed because there is no "Buckets" filter.			
////			// Switch to "All" filter.
////			FilterUtil.switchSelectedFilter(context, TaskProvider.ID_FILTER_ALL);	
//
//		}else{ // Switch to single basic filter.
//			
//			// Delete all non-permanent filters
//			context.getContentResolver().delete(Task.Filter.CONTENT_URI, Task.Filter._PERMANENT+" IS NULL", null);
//			
//			// Activate "Basic" Filter.
//			toggleFilterActiveness(context, TaskProvider.ID_FILTER_BASIC, true);
//			
//			// Switch to "Basic" filter.
//			FilterUtil.switchSelectedFilter(context, TaskProvider.ID_FILTER_BASIC);	
//		}
//		
//		// Re-apply filter bits.
//		FilterUtil.applyFilterBits(context);
//		
//	}
	
	
	private static void updateAllFilterDescription(Context context, String description) {
		ContentValues cv = new ContentValues(1);
		cv.put(Task.Filter.DESCRIPTION, description);
		int count = context.getContentResolver().update(Task.Filter.CONTENT_URI, cv, Task.Filter._PERMANENT+"=?", new String[]{TaskProvider.ID_FILTER_ALL});
		if( count != 1 ){
			Log.e(TAG, "ERR000HO");
			ErrorUtil.handleExceptionNotifyUser("ERR000HO", (Exception)(new Exception(  )).fillInStackTrace(), context);			
		}
	}
	
	
	// Conditions: 
	// * 1. Archive on, Labels on --> Archive on
	//   2. Archive on, Labels on -->             Labels on
	//   3. Archive on            -->                       (Basic filter) !! Doesn't include archive or unlabeled filter element.
	// * 4.             Labels on --> 						(Basic filter) !! Doesn't include archive or unlabeled filter element.
	//   5. Archive on            --> Archive on, Labels on
	//   6.             Labels on --> Archive on, Labels on
	//   7.                       --> Archive on
	//   8.                       -->             Labels on
	public static void disableLabels(Context context) {
		
		// Delete all _user-applied_ labels.
		context.getContentResolver().delete(Task.Labels.CONTENT_URI, Task.Labels._USER_APPLIED+"=?", new String[]{Task.Labels.USER_APPLIED_TRUE});

		// Delete all labeled content mappings (except for archived label mapping). 
		// TODO: !!! Consider adding a "user-applied" flag to the labeled-content table so we have a more generic way to exclude mappings like the archive label mapping. OR select all the user-applied label IDs and then join it to the labeled-content table when you do the delete.
		context.getContentResolver().delete(Task.LabeledContent.CONTENT_URI, Task.LabeledContentColumns._LABEL_ID+"!=?", new String[]{TaskProvider.ID_LABEL_ARCHIVED});    	

		
		SharedPreferences settings = context.getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE);
		boolean enableArchive = settings.getBoolean(ApplicationPreference.ENABLE_ARCHIVE, ApplicationPreference.ENABLE_ARCHIVE_DEFAULT);
//		boolean enableBuckets = settings.getBoolean(ApplicationPreference.ENABLE_BUCKETS, ApplicationPreference.ENABLE_BUCKETS_DEFAULT);

		if( enableArchive 
//				|| enableBuckets 
				){			
			// Deactivate all unlabeled filter elements. 
			toggleUnlabeledFilterElementActiveness(context, false);
			
			// Delete all _label_ filter elements.
			context.getContentResolver().delete(Task.FilterElement.CONTENT_URI, Task.FilterElementColumns._CONSTRAINT_URI+" LIKE '" + Constraint.Version1.LABEL_CONTENT_URI_STRING + "/%'", null);
		}else{ // Switch to single basic filter.
			
// No need for this because switchSelectedToPermanantFilter(..) now does this for us.			
//			// Activate "Basic" Filter.
//			toggleFilterActiveness(context, TaskProvider.ID_FILTER_BASIC, true);
			
			// Switch to "Basic" filter.
			FilterUtil.switchSelectedToPermanantFilter(context, TaskProvider.ID_FILTER_BASIC);			
			
			// Delete all non-permanent filters
			context.getContentResolver().delete(Task.Filter.CONTENT_URI, Task.Filter._PERMANENT+" IS NULL", null);
			
			// Deactivate "All" Filter.
			togglePermanentFilterActiveness(context, TaskProvider.ID_FILTER_ALL, false);
		}

		// Re-apply filter bits.
		FilterUtil.applyFilterBits(context);		
	}
	@Override
	protected void onResume() {
		super.onResume();
		try{
			SessionUtil.onSessionStart(this);
			BackupStatusPreference statusPref = (BackupStatusPreference)findPreference(ApplicationPreference.BACKUP_STATUS);
			if( null != statusPref ){
				statusPref.updateStatus();
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000KS", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000KS", exp, ApplicationPreferenceActivity.this);										
		}

	}

	@Override
	protected void onPause() {
		super.onPause();
		SessionUtil.onSessionStop(this);
	}
	
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		super.onCreateOptionsMenu(menu);
//		try{
//
//
//		}catch(HandledException h){ // Ignore.
//		}catch(Exception exp){
//			Log.e(TAG, "ERR0000N", exp);
//			ErrorUtil.handleExceptionNotifyUser("ERR0000N", exp, ApplicationPreferenceActivity.this);										
//		}
//		return true;
//	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		try{
			MenuItem helpMenuItem = menu.findItem(HELP_ID);
			if( null == helpMenuItem ){
				helpMenuItem = menu.add(0, HELP_ID, 5, R.string.option_help);
				helpMenuItem.setAlphabeticShortcut('h');
//			helpMenuItem.setIcon(R.drawable.help);
				helpMenuItem.setIcon(android.R.drawable.ic_menu_help);
				
			}
			Intent helpIntent = null;
			if( ArchiveUtil.isArchiveEnabled(ApplicationPreferenceActivity.this) ){
				helpIntent = StaticDisplayActivity.createIntent(this, R.layout.help_settings_archive, R.string.help_settings); // TODO: !!! Is this title resource correct?
			}else{
				helpIntent = StaticDisplayActivity.createIntent(this, R.layout.help_settings_delete, R.string.help_settings);  // TODO: !!! Is this title resource correct?
			}
			assert null != helpIntent;
			helpMenuItem.setIntent(helpIntent);
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000DM", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000DM", exp, ApplicationPreferenceActivity.this);										
		}

		
		return true;
	}

	private static final int DIALOG_CLEAR_ARCHIVE_ID   = 50;
	private static final int DIALOG_DISABLE_ARCHIVE_ID = 51;
	private static final int DIALOG_DISABLE_LABELS_ID  = 52;
//	private static final int DIALOG_DISABLE_BUCKETS_ID = 53;
	private static final int DIALOG_MINDERS_ADDON_REQUIRED_ID   	= 54;
	private static final int DIALOG_ORGANIZER_ADDONO_REQUIRED_ID   	= 55;
	private static final int DIALOG_BACKUP_ADDON_REQUIRED_ID   		= 56;
	private static final int DIALOG_BACKUP_NOT_SUPPORTED_ID   		= 57;
	private static final int DIALOG_BACKUP_DISABLED_IN_PRIVACY_SETTINGS_ID   		= 58;
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = super.onCreateDialog(id);
		try{
			if(null != dialog){
				return dialog;
			}
			switch(id){
				case DIALOG_DISABLE_ARCHIVE_ID:
					// TODO: !! Only do this check if there actually are tasks that will be deleted.
					
					dialog = new AlertDialog.Builder(ApplicationPreferenceActivity.this)
					.setTitle(R.string.dialog_deleteArchivedTasks)
					.setMessage(R.string.dialog_areYouSureYouWantToDeleteAllArchivedTaskAndDisableTheArchive)
					.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface arg0, int arg1) {
							try{
								Toast.makeText(ApplicationPreferenceActivity.this, R.string.toast_noArchivedTasksWereDeleted, Toast.LENGTH_SHORT).show();
							}catch(HandledException h){ // Ignore.
							}catch(Exception exp){
								Log.e(TAG, "ERR0000J", exp);
								ErrorUtil.handleExceptionNotifyUser("ERR0000J", exp, ApplicationPreferenceActivity.this);										
							}  		
						}
					})
					.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface arg0, int arg1) {
							try{
								actuallyDisableArchive();
								Toast.makeText(ApplicationPreferenceActivity.this, R.string.toast_allArchivedTasksDeleted, Toast.LENGTH_SHORT).show();

							}catch(HandledException h){ // Ignore.
							}catch(Exception exp){
								Log.e(TAG, "ERR0000E", exp);
								ErrorUtil.handleExceptionNotifyUser("ERR0000E", exp, ApplicationPreferenceActivity.this);										
							}   		
						}


					})
					.setOnCancelListener(new DialogInterface.OnCancelListener(){
						public void onCancel(DialogInterface arg0) {
							try{
								Toast.makeText(ApplicationPreferenceActivity.this, R.string.toast_noArchivedTasksWereDeleted, Toast.LENGTH_SHORT).show();
							}catch(HandledException h){ // Ignore.
							}catch(Exception exp){
								Log.e(TAG, "ERR0000K", exp);
								ErrorUtil.handleExceptionNotifyUser("ERR0000K", exp, ApplicationPreferenceActivity.this);										
							}  		
						}
					})
					.create();

					break;
//				case DIALOG_DISABLE_BUCKETS_ID:
//					dialog = new AlertDialog.Builder(ApplicationPreferenceActivity.this)
//					.setTitle(R.string.disable_buckets_dialog_title)
//					.setMessage(R.string.disable_buckets_dialog_message)
//					.setNegativeButton(R.string.disable_buckets_dialog_button_cancel, new DialogInterface.OnClickListener(){
//						public void onClick(DialogInterface arg0, int arg1) {
//							try{
//								Toast.makeText(ApplicationPreferenceActivity.this, R.string.disable_buckets_negative_toast, Toast.LENGTH_SHORT).show();
//							}catch(HandledException h){ // Ignore.
//							}catch(Exception exp){
//								Log.e(TAG, "ERR000HB", exp);
//								ErrorUtil.handleExceptionNotifyUser("ERR000HB", exp, ApplicationPreferenceActivity.this);										
//							}  		
//						}
//					})
//					.setPositiveButton(R.string.disable_buckets_dialog_button_ok, new DialogInterface.OnClickListener(){
//						public void onClick(DialogInterface arg0, int arg1) {
//							try{
//								actuallyDisableBuckets();
//								Toast.makeText(ApplicationPreferenceActivity.this, R.string.disable_buckets_positive_toast, Toast.LENGTH_SHORT).show();
//
//							}catch(HandledException h){ // Ignore.
//							}catch(Exception exp){
//								Log.e(TAG, "ERR000HC", exp);
//								ErrorUtil.handleExceptionNotifyUser("ERR000HC", exp, ApplicationPreferenceActivity.this);										
//							}   		
//						}
//
//
//					})
//					.setOnCancelListener(new DialogInterface.OnCancelListener(){
//						public void onCancel(DialogInterface arg0) {
//							try{
//								Toast.makeText(ApplicationPreferenceActivity.this, R.string.disable_buckets_negative_toast, Toast.LENGTH_SHORT).show();
//							}catch(HandledException h){ // Ignore.
//							}catch(Exception exp){
//								Log.e(TAG, "ERR000HD", exp);
//								ErrorUtil.handleExceptionNotifyUser("ERR000HD", exp, ApplicationPreferenceActivity.this);										
//							}  		
//						}
//					})
//					.create();
//
//					break;
					
				case DIALOG_DISABLE_LABELS_ID:
					// TODO: !! Only do this check if there actually are applied labels that will be deleted.
					
					dialog = new AlertDialog.Builder(ApplicationPreferenceActivity.this)
					.setTitle(R.string.dialog_areYouSure)
					.setMessage(R.string.dialog_whenDisablingLabelsAllExistingLabelsAreDeleted)
					.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface arg0, int arg1) {
							try{
								Toast.makeText(ApplicationPreferenceActivity.this, R.string.toast_noLabelsWereDeleted, Toast.LENGTH_SHORT).show();
							}catch(HandledException h){ // Ignore.
							}catch(Exception exp){
								Log.e(TAG, "ERR0000H", exp);
								ErrorUtil.handleExceptionNotifyUser("ERR0000H", exp, ApplicationPreferenceActivity.this);										
							}  		
						}
					})
					.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface arg0, int arg1) {
							try{
								actuallyDisableLabels();
								Toast.makeText(ApplicationPreferenceActivity.this, R.string.toast_allLabelsDeleted, Toast.LENGTH_SHORT).show();
							}catch(HandledException h){ // Ignore.
							}catch(Exception exp){
								Log.e(TAG, "ERR0000D", exp);
								ErrorUtil.handleExceptionNotifyUser("ERR0000D", exp, ApplicationPreferenceActivity.this);										
							} 		
						}

					})
					.setOnCancelListener(new DialogInterface.OnCancelListener(){
						public void onCancel(DialogInterface arg0) {
							try{
								Toast.makeText(ApplicationPreferenceActivity.this, R.string.toast_noLabelsWereDeleted, Toast.LENGTH_SHORT).show();
							}catch(HandledException h){ // Ignore.
							}catch(Exception exp){
								Log.e(TAG, "ERR0000I", exp);
								ErrorUtil.handleExceptionNotifyUser("ERR0000I", exp, ApplicationPreferenceActivity.this);										
							}  		
						}
					})
					.create();
					break;
				
				case DIALOG_CLEAR_ARCHIVE_ID:
					dialog = new AlertDialog.Builder(ApplicationPreferenceActivity.this)
						.setTitle(R.string.dialog_confirmDeletion)
						.setMessage(R.string.dialog_deleteAllArchivedTasks)
						.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener(){
							public void onClick(DialogInterface arg0, int arg1) {
								try{
									Toast.makeText(ApplicationPreferenceActivity.this, R.string.toast_noArchivedTasksWereDeleted, Toast.LENGTH_SHORT).show();
								}catch(HandledException h){ // Ignore.
								}catch(Exception exp){
									Log.e(TAG, "ERR0000L", exp);
									ErrorUtil.handleExceptionNotifyUser("ERR0000L", exp, ApplicationPreferenceActivity.this);										
								}  		
							}
						})
						.setPositiveButton(R.string.button_delete, new DialogInterface.OnClickListener(){
							public void onClick(DialogInterface arg0, int arg1) {
								try{
									Event.onEvent(Event.CLEAR_ARCHIVE, null); // Map<String, String> parameters = new HashMap<String, String>();
									getContentResolver().delete(Uri.parse("content://"+Task.AUTHORITY+"/archive_tasks"), null, null);
									Toast.makeText(ApplicationPreferenceActivity.this, R.string.toast_allArchivedTasksDeleted, Toast.LENGTH_SHORT).show();
								}catch(HandledException h){ // Ignore.
								}catch(Exception exp){
									Log.e(TAG, "ERR0000F", exp);
									ErrorUtil.handleExceptionNotifyUser("ERR0000F", exp, ApplicationPreferenceActivity.this);										
								}  		
	
							}
						})
						.setOnCancelListener(new DialogInterface.OnCancelListener(){
							public void onCancel(DialogInterface arg0) {
								try{
									Toast.makeText(ApplicationPreferenceActivity.this, R.string.toast_noArchivedTasksWereDeleted, Toast.LENGTH_SHORT).show();
								}catch(HandledException h){ // Ignore.
								}catch(Exception exp){
									Log.e(TAG, "ERR0000M", exp);
									ErrorUtil.handleExceptionNotifyUser("ERR0000M", exp, ApplicationPreferenceActivity.this);										
								}  		
							}
						})
						.create();
	
					break;
				case DIALOG_MINDERS_ADDON_REQUIRED_ID:
					dialog = AddonRequiredDialogPart.onCreateDialog(
							ApplicationPreferenceActivity.this, 
							R.string.dialog_addOnNeeded, 
							R.string.dialog_thisFeatureIsPartOfTheMindersAddOnAndMustBeDownloadedSeparately, 
							Uri.parse("http://market.android.com/search?q=pname:"+SharedConstant.ADDON_MINDERS)); // FIXME: Change URI to market://search?q= and test on 1.5 phones.
					break;
				case DIALOG_ORGANIZER_ADDONO_REQUIRED_ID:
					dialog = AddonRequiredDialogPart.onCreateDialog(
							ApplicationPreferenceActivity.this, 
							R.string.dialog_addOnNeeded, 
							R.string.dialog_thisFeatureIsPartOfTheOrganizerAddOnAndMustBeDownloadedSeparately, 
							Uri.parse("http://market.android.com/search?q=pname:"+SharedConstant.ADDON_ORGANIZERS));
					break;
				case DIALOG_BACKUP_ADDON_REQUIRED_ID:
					dialog = AddonRequiredDialogPart.onCreateDialog(
							ApplicationPreferenceActivity.this, 
							R.string.dialog_addOnNeeded, 
							R.string.dialog_thisFeatureIsPartOfTheBackupAddOnAndMustBeDownloadedSeparately, 
							Uri.parse("market://search?q=pname:"+SharedConstant.ADDON_BACKUP));
					break;
				case DIALOG_BACKUP_NOT_SUPPORTED_ID:
					dialog = new AlertDialog.Builder(ApplicationPreferenceActivity.this)
							.setTitle(R.string.dialog_backup_not_supported_title)
							.setMessage(R.string.dialog_backup_not_supported_message)
							.setPositiveButton(R.string.dialog_backup_not_supported_btn_positive, new DialogInterface.OnClickListener(){
								public void onClick(DialogInterface dialog, int whichButton) {
									// Notify user that backup is not supported.
									Intent theIntent = new Intent(Intent.ACTION_VIEW);
									theIntent.addFlags( Intent.FLAG_ACTIVITY_NO_HISTORY );
									theIntent.setData(Uri.parse("market://search?q=pname:"+SharedConstant.ADDON_BACKUP));
									startActivity(theIntent);
								}

							})
							.setNegativeButton(R.string.dialog_backup_not_supported_btn_negative, null)
							.create();
					break;
				case DIALOG_BACKUP_DISABLED_IN_PRIVACY_SETTINGS_ID:
					dialog = new AlertDialog.Builder(ApplicationPreferenceActivity.this)
							.setTitle(R.string.dialog_backup_not_enabled_in_settings_title)
							.setMessage(R.string.dialog_backup_not_enabled_in_settings_message)
							.setPositiveButton(R.string.dialog_backup_not_enabled_in_settings_btn_positive, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									// Redirect user to privacy settings.
							    	startActivity(BackupUtil.getPrivacySettingsLaunchIntent());
								}
							})
							.setNegativeButton(R.string.dialog_backup_not_enabled_in_settings_btn_negative, null)
							.create();
					break;
					
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000CE", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000CE", exp, ApplicationPreferenceActivity.this);										
		}
		return dialog;
	}

	public boolean onPreferenceChange(Preference preference, Object newValue) {
		try{
			if( preference == labelsEnabledPref ){
				if( LicenseUtil.hasLicense(this, LicenseUtil.FEATURE_LABELS) ){
					return changeLabelsPreference((Boolean)newValue, true);
				}else{
					// Pop-up dialog sales pitch.
					showDialog(DIALOG_ORGANIZER_ADDONO_REQUIRED_ID);
					//Toast.makeText(this, "Buy the organizer add-on", Toast.LENGTH_SHORT).show();
				}
			}else if( preference == archiveEnabledPref ){
				if( LicenseUtil.hasLicense(this, LicenseUtil.FEATURE_ARCHIVING) ){
					return changeArchivePreference((Boolean)newValue, true);
				}else{
					// Pop-up dialog sales pitch.
					showDialog(DIALOG_ORGANIZER_ADDONO_REQUIRED_ID);
					//Toast.makeText(this, "Buy the organizer add-on", Toast.LENGTH_SHORT).show();
				}
			}else if( preference == callminderEnabledPref ){
				if( LicenseUtil.hasLicense(this, LicenseUtil.FEATURE_CALLMINDER) ){
					return changeCallminderPreference((Boolean)newValue);
				}else{
					// Pop-up dialog sales pitch.
					showDialog(DIALOG_MINDERS_ADDON_REQUIRED_ID);
					//Toast.makeText(this, "Buy the minder add-on", Toast.LENGTH_SHORT).show();
				}
			}else if( preference == defaultEditorTabPref ){
				updateDefaultEditorTabSummary((String)newValue);
				return true;
//			}else if( preference == bucketsEnabledPref ){
//				return changeBucketsPreference((Boolean)newValue, true);
			}else if( preference == backupEnabledPref ){
				return changeBackupEnabledPreference((Boolean)newValue, true);
			}
			
			
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000DR", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000DR", exp, ApplicationPreferenceActivity.this);										
		}  		
		return false;
	}


	private void updateDefaultEditorTabSummary(String value){
		String currentValue = null;
		if( null == value || TaskEditorTabActivity.TAB_LABEL_DETAILS.equals(value) ){
			currentValue = getResources().getStringArray(R.array.defaultEditorTabEntries)[0];
		}else{
			currentValue = getResources().getStringArray(R.array.defaultEditorTabEntries)[1];
		}
//		String currentValue = defaultEditorTabPref.getEntry().toString();
//		if( null == currentValue ){
//			currentValue = getResources().getStringArray(R.array.defaultEditorTabEntries)[0];
//		}
		defaultEditorTabPref.setSummary(TextUtils.expandTemplate(getResources().getString(R.string.defaultTab_NAME), currentValue));
		
	}

	
	public boolean changeLabelsPreference(boolean newValue, boolean prompt) {
		// If attempting to disable labels ...
		if( !newValue ){
			if( prompt ){
				showDialog(DIALOG_DISABLE_LABELS_ID);
				return false;
			}else{
				actuallyDisableLabels();
				return true;
			}
		}else{
			enableLabels(this);
			Preference labelsEnabledPref = findPreference(ApplicationPreference.ENABLE_LABELS);
			((CheckBoxPreference)labelsEnabledPref).setChecked(true);
			return true;
		}
	}

	public boolean changeArchivePreference(boolean newValue, boolean prompt) {
		// If attempting to disable labels ...
		if( !newValue ){
			if( prompt ){
				showDialog(DIALOG_DISABLE_ARCHIVE_ID);
				return false;
			}else{
				actuallyDisableArchive();
				return true;
			}
		}else{
			enableArchive(this);
//			Preference archiveEnabledPref = findPreference(ApplicationPreference.ENABLE_ARCHIVE);
			((CheckBoxPreference)archiveEnabledPref).setChecked(true);
			return true;
		}
	}

	public boolean changeCallminderPreference(boolean enable) {

		MonitorPhoneStateService.sendMessage(this, enable);
		
		return true;
	}

	public boolean changeBackupEnabledPreference(boolean enable, boolean prompt) {
		boolean backupAddonInstalled = AddonUtil.doesPackageHaveSameSignature(this, SharedConstant.ADDON_BACKUP);
		
		if( enable ){
			if( !backupAddonInstalled ){ 
				// Pop-up dialog sales pitch.
				if( prompt ){
					showDialog(DIALOG_BACKUP_ADDON_REQUIRED_ID);
				}else{
					Log.e(TAG, "ERR000KN");
					ErrorUtil.handle("ERR000KN", "?", ApplicationPreferenceActivity.this);										
				}
				return false;
			}else if( Integer.parseInt(Build.VERSION.SDK) < 8 ){ // Build.VERSION_CODES.FROYO
				Log.i(TAG, "FlingTap Done Backup requires Android 2.2");
				// Tell user that their phone doesn't support backups.
				handleNotSupportedCondition(prompt);
				return false;
			}
			if( PrivacySettingsUtil.BACKUP_ENABLED_TRUE == Settings.Secure.getInt(getContentResolver(), PrivacySettingsUtil.BACKUP_ENABLED_KEY, PrivacySettingsUtil.BACKUP_ENABLED_FALSE) ){
				// Backups are enabled. Yeah!!!!!

				// Request a backup.
				BackupManagerProxy.getInstance(this).dataChanged(this, true);

				if( prompt ){
					// Toast user.
					Toast.makeText(this, R.string.toast_backup_enabled, Toast.LENGTH_LONG).show();
				}else{
					// Notify user.
					BackupNotifications.notify(this, NOTIFY_URI_BACKUP_ENABLED, R.string.notify_backup_enabled_title, R.string.notify_backup_enabled_description);
				}
			}else{ // Backup is not enabled in settings,, but user is trying to enable it.
				Log.i(TAG, "Backups not enabled in Android privacy settings. Check if service is provisioned.");
				if( PrivacySettingsUtil.BACKUP_PROVISIONED_TRUE == Settings.Secure.getInt(getContentResolver(), PrivacySettingsUtil.BACKUP_PROVISIONED_KEY,PrivacySettingsUtil.BACKUP_PROVISIONED_FALSE) ){

					// Tell user that they need to enable backup in settings. Open dialog.
					if( prompt ){
						showDialog(DIALOG_BACKUP_DISABLED_IN_PRIVACY_SETTINGS_ID);
					}else{
						// Notify user that backup is disabled by in privacy settings.
						//   Intent sends the user back to ApplicationPreferencesActivity so that the notification can be cleared prior to 
						//   redirecting the user to the privacy settings activity.
						Intent appPrefIntent = ApplicationPreferenceActivity.getLaunchIntent(this);
						appPrefIntent.setAction(ACTION_REDIRECT);
						
						Intent privacySettingsIntent = BackupUtil.getPrivacySettingsLaunchIntent();
						
						appPrefIntent.putExtra(REDIRECT_URI, privacySettingsIntent);
						BackupNotifications.notify(this, NOTIFY_URI_BACKUP_DISABLED_IN_PRIVACY_SETTINGS, R.string.notify_backup_not_enabled_in_privacy_settings_title, R.string.notify_backup_not_enabled_in_privacy_settings_message, appPrefIntent);
					}
					return false;
				}else{
					// Tell user that their phone doesn't support backups.
					Log.i(TAG, "A backup service is not provisioned on your device.");
					handleNotSupportedCondition(prompt);
					return false;
				}
			}
		}else{ // Disable backup.
			// BackupPreference will handle updating its state accordingly.
			// Backups will automatically stop because this preference just became disabled.
		}
		
		return true;
	}

	private void handleNotSupportedCondition(boolean prompt) {
		if( prompt ){
			showDialog(DIALOG_BACKUP_NOT_SUPPORTED_ID);
		}else{
			// Notify user that backup is not supported.
			Intent appPrefIntent = ApplicationPreferenceActivity.getLaunchIntent(this);
			appPrefIntent.setAction(ACTION_REDIRECT);

			Intent marketIntent = new Intent(Intent.ACTION_VIEW);
			marketIntent.addFlags( Intent.FLAG_ACTIVITY_NO_HISTORY );
			marketIntent.setData(Uri.parse("market://search?q=pname:"+SharedConstant.ADDON_BACKUP));
			
			appPrefIntent.putExtra(REDIRECT_URI, marketIntent);
			BackupNotifications.notify(this, NOTIFY_URI_BACKUP_NOT_SUPPORTED, R.string.notify_backup_not_supported_title, R.string.notify_backup_not_supported_message, appPrefIntent);
		}
	}
	
//	public boolean changeBucketsPreference(boolean enable, boolean prompt) {
//		if( !enable ){ // Disabled ?
//			if( prompt ){
//				showDialog(DIALOG_DISABLE_BUCKETS_ID);
//				return false;
//			}else{
//				actuallyDisableBuckets();
//				return true;
//			}
//		}else{
//			enableBuckets(this);
//			Preference bucketsEnabledPref = findPreference(ApplicationPreference.ENABLE_BUCKETS);
//			((CheckBoxPreference)bucketsEnabledPref).setChecked(true);
//			return true;
//		}
//	}
	
	// Conditions: 
	//   1. Archive on, Labels on --> Archive on
	//   2. Archive on, Labels on -->             Labels on
	//   3. Archive on            -->                       (Basic filter) !! Doesn't include archive or unlabeled filter element.
	//   4.             Labels on --> 						(Basic filter)
	//   5. Archive on            --> Archive on, Labels on
	// * 6.             Labels on --> Archive on, Labels on
	// * 7.                       --> Archive on
	//   8.                       -->             Labels on
	public static void enableArchive(PreferenceActivity context) {
		if( !LicenseUtil.hasLicense(context, LicenseUtil.FEATURE_ARCHIVING)){
			ErrorUtil.handle("ERR000J2", "Attempt to enable feature with no license", context);
			return;
		}

		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(Event.TOGGLE_ARCHIVE__ENABLED, Boolean.TRUE.toString());
		Event.onEvent(Event.TOGGLE_ARCHIVE, parameters); 		
		
		boolean enableLabels = LabelUtil.isLabelsEnabled(context);
//		boolean enableBuckets = BucketUtil.isBucketsEnabled(context);
		
		if( enableLabels 
//				|| enableBuckets 
				){ // Labels are already enabled.
			

		}else{ // No features were enabeled.
			
			// Deactivate "Basic" filter.
			togglePermanentFilterActiveness(context, TaskProvider.ID_FILTER_BASIC, false);
		}
		
		// Update "All" filter description.
		updateAllFilterDescription(context, context.getString(R.string.allNonArchivedTasks) );

		// Activate all archive filter elements. 
		// TODO: !!! Shouldn't togglePermanentFilterActiveness(..) part of this method?
		toggleArchiveFilterElementsActiveness(context, true);
		
		// Activate "Archive" Filter. 
		togglePermanentFilterActiveness(context, TaskProvider.ID_FILTER_ARCHIVE, true);
		
		// Switch selected filter to "All" filter.
		FilterUtil.switchSelectedToPermanantFilter(context, TaskProvider.ID_FILTER_ALL);			
		
		makeArchiveCompletedPreferenceActive(context, (PreferenceCategory)context.findPreference(ApplicationPreference.COMPLETED_TASKS_CATEGORY));
		
		// Re-apply filter bits.
		FilterUtil.applyFilterBits(context);
		

	}


	// Conditions: 
	//   1. Archive on, Labels on --> Archive on
	//   2. Archive on, Labels on -->             Labels on
	//   3. Archive on            -->                       (Basic filter) !! Doesn't include archive or unlabeled filter element.
	//   4.             Labels on --> 						(Basic filter)
	// * 5. Archive on            --> Archive on, Labels on
	//   6.             Labels on --> Archive on, Labels on
	//   7.                       --> Archive on
	// * 8.                       -->             Labels on
	private static void enableLabels(Context context) {
		if( !LicenseUtil.hasLicense(context, LicenseUtil.FEATURE_LABELS)){
			ErrorUtil.handle("ERR000J1", "Attempt to enable feature with no license", context);
			return;
		}
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(Event.TOGGLE_LABELS__ENABLED, Boolean.TRUE.toString());
		Event.onEvent(Event.TOGGLE_LABELS, parameters); 		
		
		boolean enableArchive = ArchiveUtil.isArchiveEnabled(context);
//		boolean enableBuckets = BucketUtil.isBucketsEnabled(context);
		
		
		if( enableArchive 
//				|| enableBuckets
				){ // Archive is already enabled.
			

		}else{ // No features were enabeled.
			// Deactivate "Basic" filter.
			togglePermanentFilterActiveness(context, TaskProvider.ID_FILTER_BASIC, false);
			
		}
// 		// Activate "All" filter.
//		togglePermanentFilterActiveness(context, TaskProvider.ID_FILTER_ALL, true);
		
		// Activate all unlabeled filter elements. 
		toggleUnlabeledFilterElementActiveness(context, true);

		FilterUtil.switchSelectedToPermanantFilter(context, TaskProvider.ID_FILTER_ALL);			
		
		// Re-apply filter bits.
		FilterUtil.applyFilterBits(context);
		
	}
	


//	public static void enableBuckets(PreferenceActivity context) {
//		Map<String, String> parameters = new HashMap<String, String>();
//		parameters.put(Event.TOGGLE_BUCKETS__ENABLED, Boolean.TRUE.toString());
//		Event.onEvent(Event.TOGGLE_BUCKETS, parameters); 		
//		
//		boolean enableLabels = LabelUtil.areLabelsEnabled(context);
//		boolean enableArchive = ArchiveUtil.isArchiveEnabled(context);
//		if( enableLabels || enableArchive ){ // Labels or Archive are already enabled.
//			
//
//		}else{ // No features were enabeled.
//			
//			// Deactivate "Basic" filter.
//			togglePermanentFilterActiveness(context, TaskProvider.ID_FILTER_BASIC, false);
//			
//			// Activate "All" filter.
//			togglePermanentFilterActiveness(context, TaskProvider.ID_FILTER_ALL, true);
//			
//		}
//		// Switch selected filter to "All" filter.
//		FilterUtil.switchSelectedFilter(context, TaskProvider.ID_FILTER_ALL);			
//		
//		// Re-apply filter bits.
//		FilterUtil.applyFilterBits(context);
//		
//
//	}	

	private static Uri findAllFilter(Context context) {
		Uri theUri = null;
		// Make "All" filter selected.
		//   Assumes that All filter _id is lowest among all filters. 
		Cursor filterCursor = context.getContentResolver().query(Task.Filter.CONTENT_URI, new String[]{Task.Filter._ID}, null, null, Task.Filter._ID);
		assert null != filterCursor;
		if( filterCursor.moveToFirst() ){ // "All" filter.
			theUri = Uri.withAppendedPath(Task.Filter.CONTENT_URI, filterCursor.getString(0));
		}else{
			// TODO: !!! Log this error.
		}
		filterCursor.close();
		return theUri;
	}

	public boolean onPreferenceClick(Preference preference) {
		try{
			if( preference == clearArchiveNowPref ){
				showDialog(DIALOG_CLEAR_ARCHIVE_ID);
				return true;
			}else if( preference == updateRequiredUri ){
				Intent intent = new Intent(Veecheck.getCheckAction(ApplicationPreferenceActivity.this), Uri.parse(StaticConfig.VEECHECK_CHECK_URI));
				ApplicationPreferenceActivity.this.startService(intent);
				Toast.makeText(ApplicationPreferenceActivity.this, R.string.toast_checkingForUpdate, Toast.LENGTH_SHORT).show(); // TODO: !! Add a proper progress dialog.
				return true;
//			}else if( preference == defaultEditorTabPref ){
//				if(null == defaultEditorTabPref.getValue() || TaskEditorTabActivity.TAB_LABEL_DETAILS.equals(defaultEditorTabPref.getValue())){
//					defaultEditorTabPref.setValueIndex(0);
//				}else{
//					defaultEditorTabPref.setValueIndex(1);
//				}
//				return true;
//			}else if( preference == editBucketsPref ){
//				BucketList.launchActivity(this);
//				return true;
				
//			}else if( preference == defaultCategoryPref ){
//				Cursor selectdIdCursor = getContentResolver().query(Task.Bucket.CONTENT_URI, new String[]{Task.Bucket._ID}, Task.Bucket._DEFAULT+"=?", new String[]{String.valueOf(Task.Bucket.DEFAULT_TRUE)}, null);
//				assert null != selectdIdCursor;
//				if( !selectdIdCursor.moveToFirst() ){
//					Log.e(TAG, "ERR000H7");
//					ErrorUtil.handleNotifyUser("ERR000H7", "", this, this);										
//				}
//				startActivityForResult(BucketChooser.createLaunchIntent(this, getText(R.string.category_chooser_title_default), selectdIdCursor.getLong(0)), REQUEST_CHANGE_DEFAULT_CATEGORY_CODE);
//				selectdIdCursor.close();
//				return true;
			}
			
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000DS", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000DS", exp, ApplicationPreferenceActivity.this);										
		}  		
		return false;
	}

	private void actuallyDisableLabels() {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(Event.TOGGLE_LABELS__ENABLED, Boolean.FALSE.toString());
		Event.onEvent(Event.TOGGLE_LABELS, parameters); 		
		
		disableLabels(ApplicationPreferenceActivity.this);
		Preference labelsEnabledPref = findPreference(ApplicationPreference.ENABLE_LABELS);
		((CheckBoxPreference)labelsEnabledPref).setChecked(false);
	}
	
	private void actuallyDisableArchive() {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(Event.TOGGLE_ARCHIVE__ENABLED, Boolean.FALSE.toString());
		Event.onEvent(Event.TOGGLE_ARCHIVE, parameters); 		
		
		disableArchive(ApplicationPreferenceActivity.this);
//		Preference archiveEnabledPref = findPreference(ApplicationPreference.ENABLE_ARCHIVE);
		((CheckBoxPreference)archiveEnabledPref).setChecked(false);
		makeDeleteCompletedPreferenceActive(this, (PreferenceCategory)findPreference(ApplicationPreference.COMPLETED_TASKS_CATEGORY));
	}
	
//	private void actuallyDisableBuckets() {
//		Map<String, String> parameters = new HashMap<String, String>();
//		parameters.put(Event.TOGGLE_BUCKETS__ENABLED, Boolean.FALSE.toString());
//		Event.onEvent(Event.TOGGLE_BUCKETS, parameters); 
//		
//		disableBuckets(ApplicationPreferenceActivity.this);
//		Preference bucketsEnabledPref = findPreference(ApplicationPreference.ENABLE_BUCKETS);
//		((CheckBoxPreference)bucketsEnabledPref).setChecked(false);
//	}
}
