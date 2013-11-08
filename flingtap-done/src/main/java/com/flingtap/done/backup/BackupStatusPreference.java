// Licensed under the Apache License, Version 2.0

package com.flingtap.done.backup;

import android.content.Context;
import android.preference.Preference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.flingtap.common.HandledException;
import com.flingtap.done.ErrorUtil;
import com.flingtap.done.base.R;

/**
 * Displays the "Backup" add-on status.
 * 
 * Assumes that the "Backup" add-on is installed.
 *
 */
public class BackupStatusPreference extends Preference {

	private static final String TAG = "BackupStatusPreference";
	
	private static final int STATUS_GRAY  = 0;
	private static final int STATUS_GREEN = 1;
	private static final int STATUS_RED   = 2;
	
	public BackupStatusPreference(Context context) {
		super(context);
	}

	public BackupStatusPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public BackupStatusPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	private static final int TWENTY_FOUR_HOURS = 24*60*60*1000;
	private ImageView statusImage = null;

	@Override
	protected void onBindView(View view) {
		try{
			statusImage = (ImageView)view.findViewById(R.id.preference_backup_status_image);
			updateStatus();
			// ***** Update status and statusImage must come before onBindView(..) otherwise the preference view doesn't update correctly.
			
			super.onBindView(view);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000KT", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000KT", exp, getContext());										
		}
	}
	
	public void updateStatus(){
		boolean enabled = isEnabled();
		if( null == statusImage ){
			return;
		}
		if( !enabled ){ // Are backups disabled preferences?
			statusImage.setImageLevel(STATUS_GRAY); // Off (gray)
			setTitle(R.string.app_pref_status_backup_title__off);
			setSummary(R.string.app_pref_status_backup_summary__off);
			return;
		}
		// Are backups disabled in the user's Privacy Settings?
		if(PrivacySettingsUtil.BACKUP_ENABLED_FALSE == Settings.Secure.getInt(getContext().getContentResolver(), PrivacySettingsUtil.BACKUP_ENABLED_KEY, PrivacySettingsUtil.BACKUP_ENABLED_FALSE)){
			// Yes, disabled.
			statusImage.setImageLevel(STATUS_RED); // Error (red)
			setTitle(R.string.app_pref_status_backup_title__privacy_settings);
			setSummary(R.string.app_pref_status_backup_summary__privacy_settings); 
			return;
		}

		BackupManagerProxy backupManager = BackupManagerProxy.getInstance(getContext());
		long lastDbModifiedTimestamp = backupManager.getDatabaseLastModified();
		long lastBackedupTimestamp = backupManager.getDatbaseBackedUp();
		 
		if( lastBackedupTimestamp == BackupManagerProxy.DEFAULT_TIMESTAMP ){ // if backups never have happened ...
			if( (System.currentTimeMillis() > lastDbModifiedTimestamp + TWENTY_FOUR_HOURS) ){ // Requested backup hasn't happened in 24 hours.
				statusImage.setImageLevel(STATUS_RED); // Error (red)
				setTitle(R.string.app_pref_status_backup_title__unsupported);
				setSummary(R.string.app_pref_status_backup_summary__unsupported);
			}else{ // No backup yet but its been less than 24 hours since the last db modification.
				statusImage.setImageLevel(STATUS_GRAY); // Off (gray)
				setTitle(R.string.app_pref_status_backup_title__waiting);
				setSummary(R.string.app_pref_status_backup_summary__waiting);
			}
		}else if(lastBackedupTimestamp >= lastDbModifiedTimestamp ){ // A backup has occurred, so did the backup happen after the db modification?  
			// Yes, so everything is good.
			statusImage.setImageLevel(STATUS_GREEN); // On (green)
			setTitle(R.string.app_pref_status_backup_title__on);
			setSummary(R.string.app_pref_status_backup_summary__on);
		}else{ // Backup has occurred in past but hasn't happened since the last db modification.
			
			if( (System.currentTimeMillis() > lastDbModifiedTimestamp + TWENTY_FOUR_HOURS) ){ // Requested backup but hasn't happened in 24 hours.
				// No, not disabled.
				statusImage.setImageLevel(STATUS_RED); // Error (red)
				setTitle(R.string.app_pref_status_backup_title__out_of_date);
				setSummary(R.string.app_pref_status_backup_summary__out_of_date);
				
			}else{ // Backup has happened in past and its been less than 24 hours since the last db modification so in general we are in good shape.
				statusImage.setImageLevel(STATUS_GREEN); // On (green)
				setTitle(R.string.app_pref_status_backup_title__on);
				setSummary(R.string.app_pref_status_backup_summary__on);
			}
		}
	}	
}
