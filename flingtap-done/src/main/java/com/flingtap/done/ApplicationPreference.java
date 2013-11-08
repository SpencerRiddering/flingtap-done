// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.flingtap.done.backup.export.BackupPreferenceConstants;

import android.net.Uri;
import android.provider.Settings;

public class ApplicationPreference {

	public static final String NAME = "com.flingtap.done.application_preferences";
	
	public static final String APPLICATION_VERSION = "application_version";
	public static final String APPLICATION_NEW_VERSION = "application_new_version";
	
//	public static final String ALARM_OFFSET_FROM_DUEDATE = "alarm_offset_from_duedate";
//	public static final boolean ALARM_OFFSET_FROM_DUEDATE_DEFAULT = false;
	
//	public static final String SHOW_DUE_DATE_YEAR = "show_due_date_year";
//	public static final boolean SHOW_DUE_DATE_YEAR_DEFAULT = false;
	
	public static final String UPDATE_ADDRESS_PROMPT_KEY = "contacts_prompt_user_for_updating_address";
	public static final boolean UPDATE_ADDRESS_PROMPT_DEFAULT = false;

	// Contact method is never updated now.
//	public static final String UPDATE_ADDRESS_ACTION_KEY = "contacts_always_update_or_not";
//	public static final String UPDATE_ADDRESS_VALUE_ALWAYS = "always";
//	public static final String UPDATE_ADDRESS_VALUE_NEVER = "never";
//	public static final String UPDATE_ADDRESS_VALUE_DEFALUT = UPDATE_ADDRESS_VALUE_ALWAYS;

	public static final String ENABLE_ARCHIVE = "enable_archive";
	public static final boolean ENABLE_ARCHIVE_DEFAULT = false;

	public static final String CLEAR_ARCHIVE = "clear_archive";
	
	public static final String ARCHIVE_RETAIN_DAYS = "archive_retain_days";
	public static final int ARCHIVE_RETAIN_DAYS_DEFAULT = 30;

	
	public static final String COMPLETED_TASKS_CATEGORY = "completed_tasks"; // Boolean
	public static final String AUTO_ARCHIVE_COMPLETED = "auto_archive_completed"; // Boolean
	public static final boolean AUTO_ARCHIVE_COMPLETED_DEFAULT = false;
	public static final String AUTO_DELETE_COMPLETED  = "auto_delete_completed"; // Boolean
	public static final boolean AUTO_DELETE_COMPLETED_DEFAULT  = false;
	
	public static final String  FEATURE_INTRODUCTION_NEEDED = "feature_introduction_needed";
	public static final boolean FEATURE_INTRODUCTION_NEEDED_DEFAULT = true;

	
	public static final String EULA_ACCEPTANCE_REQUIRED = "eula_acceptance_required";
	public static final boolean EULA_ACCEPTANCE_REQUIRED_DEFAULT = true;
	
	public static final String TEMP_FILE_HOUSEKEEPER_LAST_RUN = "temp_file_housekeeper_last_run";
	
	public static final String ENABLE_LABELS = "enable_labels";
	public static final boolean ENABLE_LABELS_DEFAULT = false;
	
//	public static final String ENABLE_BUCKETS = "enable_buckets";
//	public static final boolean ENABLE_BUCKETS_DEFAULT = false;
//	public static final String EDIT_BUCKETS = "edit_buckets";

	public static final String ALARM_RINGTONE = "alarm_ringtone";
	public static final Uri ALARM_RINGTONE_DEFAULT = Settings.System.DEFAULT_NOTIFICATION_URI;
	
	public static final String  ALARM_VIBRATE = "alarm_vibrate";
	public static final boolean ALARM_VIBRATE_DEFAULT = true;
	
	public static final String  ALARM_FLASH = "alarm_flash";
	public static final boolean ALARM_FLASH_DEFAULT = true;

	public static final String NEARMINDER_RINGTONE = "nearminder_ringtone";
	public static final Uri    NEARMINDER_RINGTONE_DEFAULT = Settings.System.DEFAULT_NOTIFICATION_URI;
	
	public static final String  NEARMINDER_VIBRATE = "nearminder_vibrate";
	public static final boolean NEARMINDER_VIBRATE_DEFAULT = true;
	
	public static final String  NEARMINDER_FLASH = "nearminder_flash";
	public static final boolean NEARMINDER_FLASH_DEFAULT = true;

	// NOTE: Can't specify whether to proximity alert will use GPS :(
//	public static final String  NEVER_USE_GPS_FOR_NEARMINDERS = "never_use_gps_for_nearminders";
//	public static final boolean NEVER_USE_GPS_FOR_NEARMINDERS_DEFAULT = true;

	public static final String  CALLMINDER_ENABLED = "callminder_enabled";
	public static final boolean CALLMINDER_ENABLED_DEFAULT = false;
	
	public static final String CALLMINDER_RINGTONE = "callminder_ringtone";
	public static final Uri    CALLMINDER_RINGTONE_DEFAULT = Settings.System.DEFAULT_NOTIFICATION_URI;
	
	public static final String  CALLMINDER_VIBRATE = "callminder_vibrate";
	public static final boolean CALLMINDER_VIBRATE_DEFAULT = true;
	
	public static final String  CALLMINDER_FLASH = "callminder_flash";
	public static final boolean CALLMINDER_FLASH_DEFAULT = true;
	
	public static final String UPDATE_REQUIRED_DEADLINE = "update_required_deadline"; // Not configurable by user (set by downloading the versions.xml from server). 
	public static final String UPDATE_REQUIRED_URI = "update_required_uri"; // Not configurable by user (set by downloading the versions.xml from server). Used as a key for requesting that a new version be checked for from the preferenes screen.

	
	public static final String  CURRENT_FEATURE_LICENSES = "current_feature_licenses";
	public static final int     CURRENT_FEATURE_LICENSES_DEFAULT = 0;
	
	public static final String  DEFAULT_EDITOR_TAB = "default_editor_tab";
	public static final String  DEFAULT_EDITOR_TAB_DEFAULT = TaskEditorTabActivity.TAB_LABEL_DETAILS;
	
	public static final String  BACKUP_ENABLED = BackupPreferenceConstants.BACKUP_ENABLED; // Do not change this or Backup Addon will break.
	public static final boolean BACKUP_ENABLED_DEFAULT = BackupPreferenceConstants.BACKUP_ENABLED_DEFAULT;  // Do not change this or Backup Addon will break.
	
	public static final String  BACKUP_STATUS = "backup_status";
	public static final String  BACKUP_GROUP = "backup_group";
}
