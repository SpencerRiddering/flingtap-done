// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import com.flingtap.common.HandledException;
import com.flingtap.common.android.XmlUtils;
import com.flingtap.done.backup.BackupManagerProxy;
import com.flingtap.done.backup.BackupUtil;
import com.flingtap.done.base.R;
import com.flingtap.done.provider.Task;
import com.flingtap.done.provider.Task.ProximityAlerts;
import com.flingtap.done.provider.Task.TaskAttachments;
import com.flingtap.done.provider.Task.Tasks;
import com.google.android.maps.GeoPoint;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.*;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * Provides access to a database of tasks, task attachments, and proximity alerts.
 * 
 * 
 */
public class TaskProvider extends ContentProvider {
    private static final String TAG = "TaskProvider";

	public static final String DATABASE_NAME = "ftdone.db";
	static final int DATABASE_VERSION = 6;

    public static final String ACTION_FILTER_BITS_CHANGED = "com.flingtap.done.intent.action.FILTER_BITS_CHANGED";

	// Table Names
	static final String TASK_TABLE_NAME = "tasks";
	static final String ATTACHMENT_TABLE_NAME = "attachments";
	static final String PROXIMITY_ALERT_TABLE_NAME = "proximity_alerts";
	static final String LABELS_TABLE_NAME = "labels";
	static final String LABELED_CONTENT_TABLE_NAME = "labeled_content";
	static final String FILTER_TABLE_NAME = "filter";
	static final String FILTER_ELEMENT_TABLE_NAME = "filter_element";
	static final String TEMP_FILE_TABLE_NAME = "temp_file";
	static final String NOTIFICATION_TABLE_NAME = "notification";
	static final String COMPLETABLE_TABLE_NAME = "completable";
	static final String APP_WIDGETS_TABLE_NAME = "app_widgets";
	static final String APP_WIDGETS_DELETED_TABLE_NAME = "app_widgets_deleted";

	// Temp Table Names
	static final String MODIFIED_ID_TEMP_TABLE_NAME = "modified_id";

	private static final int SEARCH_SUGGEST = 1;
	private static final int TASKS = 2;
	private static final int TASK_ID = 3;
	private static final int ATTACHMENTS = 4;
	private static final int ATTACHMENT_ID = 5;
	private static final int PROXIMITY_ALERTS = 6;
	private static final int PROXIMITY_ALERT_ID = 7;
	private static final int LABELS = 8;
	private static final int LABEL_ID = 9;
	private static final int LABELED_CONTENT = 10;
	private static final int LABELED_CONTENT_ID = 11;
	private static final int LABELS_LABELED_CONTENT = 12;
	private static final int LABELS_LABELED_CONTENT_ID = 13;
	private static final int LABELS_TASKS = 14;
	private static final int LABELS_TASKS_ID = 15;
	private static final int TASKS_LABELS = 16;
	private static final int TASKS_LABELS_ID = 17;
	// private static final int LABELED_CONTENT_TASKS = 18;
	private static final int FILTERS = 19;
	private static final int FILTERS_ID = 20;
	private static final int FILTER_ELEMENTS = 21;
	private static final int FILTER_ELEMENTS_ID = 22;
	private static final int MULTI_TASK_ID = 23;
	private static final int MASK_DAYS_FROM_TODAYS_END = 24;
	private static final int MASK_WEEKS_FROM_THIS_WEEKS_END = 25;
	private static final int MASK_LABEL = 26;
	private static final int MASK_ALL = 27;
	private static final int FILTER_FILTER_ELEMENTS = 28;
	private static final int TASK_ATTACHMENT_PROXIMITY_ALERT_ID = 29;
	private static final int TASK_ATTACHMENT_ID = 30;
	private static final int PROXIMITY_ALERT_TASK_ATTACHMENT_ID = 31;
	// private static final int FILTER_FILTER_ELEMENTS_LABELS = 32;
	private static final int MASK_DUE = 33;
	private static final int MASK_PRIORITY = 35;
	private static final int MASK_UNLABELED = 36;
	private static final int MASK_REPOSITORY = 37;
	private static final int MASK_STATUS = 38;
	private static final int USER_APPLIED_LABEL = 39;
	private static final int USER_APPLIED_LABEL_ID = 40;
	private static final int TEMP_FILES = 41;
	private static final int TEMP_FILE_ID = 42;
	private static final int ARCHIVED_TASKS = 43;
	private static final int NOTIFICATIONS = 44;
	private static final int NOTIFICATION_ID = 45;
	private static final int TASKS_CALLMINDER = 46;
	private static final int DB_INFO_VERSION = 47;
	private static final int DB_INFO_FILE_SIZE = 48;
	private static final int DB_INFO_MAX_SIZE = 49;
	private static final int DB_INFO_PAGE_SIZE = 50;
	private static final int DB_INFO_FILE_PATH = 51;
	private static final int ARCHIVED_TASKS_ID = 52;
	private static final int UNARCHIVED_TASKS = 53;
	private static final int UNARCHIVED_TASKS_ID = 54;
	private static final int FILTER_BITS = 55;
	private static final int FILTER_BITS_ID = 56;
	private static final int SWITCH_FILTER_ID = 57;
	private static final int ACTIVE_FILTER_FILTER_ELEMENTS = 58;
	private static final int COMPLETABLE = 59;
	private static final int COMPLETABLE_ID = 60;
	private static final int SWITCH_TO_PERMANENT_FILTER_ID = 64;
	private static final int SHORTCUT_REFRESH = 65;
	private static final int APP_WIDGETS = 66;
	// private static final int APP_WIDGET_ID = 67; // Not used.
	private static final int APP_WIDGETS_DELETED = 68;
	private static final int DB_BACKUP = 69;

    // Pre-defined values used for the "_permanent" column,, ie this record is both permanent as well as the "Archive" filter.
	static final String ID_FILTER_BASIC = "1";
	public static final String ID_FILTER_ALL = "2";
	static final String ID_FILTER_ARCHIVE = "3";

	static final String ID_LABEL_ARCHIVED = "1";

	private static final UriMatcher URL_MATCHER;

	private static final String APP_TEMP_FOLDER = "temp";

	/**
     * Retrieve, creating if needed, a new application temp directly where temp files can be placed. 
	 */
	private static File getTempDir(Context context) {
		// Create the application temp folder.
        File tempFolder = context.getDir(APP_TEMP_FOLDER, Context.MODE_WORLD_READABLE); // TODO: Make sure that no secret files are stored in this folder.
		assert null != tempFolder;
		return tempFolder;
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		private Context mContext = null;

		public DatabaseHelper(Context context, String name, CursorFactory factory, int version) {
			super(context, name, factory, version);
			mContext = context;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.i(TAG, "DatabaseHelper.onCreate() called.");
			// **************
			// Tasks
			// **************
            db.execSQL("CREATE TABLE "+TASK_TABLE_NAME+" (" 
            		+ Task.Tasks._ID + " INTEGER PRIMARY KEY,"
                    + Task.TasksColumns.CREATED_DATE + " INTEGER NOT NULL," 
                    + Task.TasksColumns.MODIFIED_DATE + " INTEGER NOT NULL," 
                    + Task.TasksColumns.TASK_TITLE + " TEXT," 
                    + Task.TasksColumns.TASK_DESC + " TEXT," 
                    + Task.TasksColumns.PRIORITY + " INTEGER," 
                    + Task.TasksColumns.DUE_DATE + " INTEGER," 
                    + Task.TasksColumns.ALARM_TIME + " INTEGER,"
                    + Task.TasksColumns.ALARM_ACTIVE + " INTEGER,"
                    + Task.TasksColumns.COMPLETE + " INTEGER," 
                    + Task.TasksColumns.COMPLETION_DATE + " INTEGER," 
                    + Task.TasksColumns._FILTER_BIT + " INTEGER"
					+ ");");

			// **************
			// Attachments
			// **************
            db.execSQL("CREATE TABLE "+ATTACHMENT_TABLE_NAME+" (" 
            		+ Task.TaskAttachments._ID + " INTEGER PRIMARY KEY,"
                    + Task.TaskAttachments.TASK_ID + " INTEGER NOT NULL"
                    	+ " CONSTRAINT fk_task_id REFERENCES "+TASK_TABLE_NAME+"("+Task.Tasks._ID+") ON DELETE CASCADE," // NOTE: Entire constraint is ignored by SQLite. See trigger below for implementation. 
                    + Task.TaskAttachments._URI + " TEXT,"
                    + Task.TaskAttachments._PACKAGE + " TEXT,"
                    + Task.TaskAttachments._CLASS_NAME + " TEXT,"
                    + Task.TaskAttachments._INTENT + " TEXT,"
                    + Task.TaskAttachments.NAME + " TEXT,"
                    + Task.TaskAttachments._ICON + " BLOB,"
                    + Task.TaskAttachments._ICON_RESOURCE + " TEXT,"
                    // + " CONSTRAINT fk_attachment_parent_id REFERENCES "+ATTACHMENT_TABLE_NAME+"("+Task.TaskAttachments._ID+") ON DELETE CASCADE,"  // NOTE: Entire constraint is ignored by SQLite. See trigger below for implementation.
                   	+ Task.TaskAttachments._DELETE_INTENT + " TEXT," // Added in version 2. 
					+ "UNIQUE(" + Task.TaskAttachments.TASK_ID + ", " + Task.TaskAttachments._URI + ") );");

			// **************************************************
			// Foreign key constraint INSERT (tasks <-- attachments)
			// **************************************************
            // For constraint details see: See http://www.justatheory.com/computers/databases/sqlite/ 
            db.execSQL(	  "CREATE TRIGGER fki_tasks_attachments_id "
	            		+ "BEFORE INSERT ON "+ATTACHMENT_TABLE_NAME+" "
	            		+ "FOR EACH ROW BEGIN " 
	            		+ "SELECT CASE "
	            		+      "WHEN ((SELECT _id FROM "+TASK_TABLE_NAME+" WHERE _id = NEW._task_id) IS NULL) "
	            		+      "THEN RAISE(ABORT, 'insert on table \""+ATTACHMENT_TABLE_NAME+"\" violates foreign key constraint \"fki_tasks_attachments_id\"') "
	            		+ "END;"
	            		+ "END;");

            // **************************************************
			// Foreign key constraint UPDATE (tasks <-- attachments)
			// **************************************************
            // For constraint details see: See http://www.justatheory.com/computers/databases/sqlite/ 
            db.execSQL(  "CREATE TRIGGER fku_tasks_attachments_id "
			           + "BEFORE UPDATE ON "+ATTACHMENT_TABLE_NAME+" "
			           + "FOR EACH ROW BEGIN " 
			           +    "SELECT CASE "
			           +      "WHEN ((SELECT _id FROM "+TASK_TABLE_NAME+" WHERE _id = new._task_id) IS NULL) "
			           +      "THEN RAISE(ABORT, 'update on table \"attachments\" violates foreign key constraint \"fku_tasks_attachments_id\"') "
			           +   "END;"
			           + "END;");            

			// **************************************************
			// Cascade deletes (tasks <-- attachments)
			// **************************************************
            // For constraint details see: See http://www.justatheory.com/computers/databases/sqlite/ 
            db.execSQL(   "CREATE TRIGGER fkd_tasks_attachments_id "
			            + "BEFORE DELETE ON "+TASK_TABLE_NAME+" "
			            + "FOR EACH ROW BEGIN "
			            +     "DELETE from "+ATTACHMENT_TABLE_NAME+" WHERE "+Task.TaskAttachments.TASK_ID+" = OLD._id; "
			            + "END;");


			// *******************
			// Proximity Alerts
			// *******************
            db.execSQL("CREATE TABLE "+PROXIMITY_ALERT_TABLE_NAME+" ("  
            		+ Task.ProximityAlerts._ID + " INTEGER PRIMARY KEY,"
            		+ Task.ProximityAlerts._SELECTED_URI + " TEXT,"
                    + Task.ProximityAlerts.RADIUS + " INTEGER, " 
                    + Task.ProximityAlerts.RADIUS_UNIT + " INTEGER, " 
            		+ Task.ProximityAlerts._GEO_URI + " TEXT," 
            		+ Task.ProximityAlerts._IS_SATELLITE + " INTEGER, " 
                    + Task.ProximityAlerts._IS_TRAFFIC + " INTEGER, " 
                    + Task.ProximityAlerts._ZOOM_LEVEL + " INTEGER, " 
                    + Task.ProximityAlerts.ENABLED + " INTEGER " 
                    + ");");

            // **************************************************
			// Cascade deletes
            // **************************************************
            // For constraint details see: See http://www.justatheory.com/computers/databases/sqlite/
            // NOTE: This shouldn't be needed, but is a temp fix because attachment's _delete_intent arent' reliable.
            db.execSQL(   "CREATE TRIGGER fkd_tasks_proximity_id "
			            + "BEFORE DELETE ON "+PROXIMITY_ALERT_TABLE_NAME+" "
			            + "FOR EACH ROW BEGIN "
			            +     "DELETE from "+ATTACHMENT_TABLE_NAME+" WHERE "+Task.TaskAttachments._URI+" = '" + Task.ProximityAlerts.CONTENT_URI + "/' || OLD._id; " // Task.ProximityAlerts.ATTACHMENT_ID
					+ "END;");

			// **************
			// Labels
			// **************
            db.execSQL("CREATE TABLE "+LABELS_TABLE_NAME+" (" 
            		+ Task.Labels._ID + " INTEGER PRIMARY KEY,"
                    + Task.Labels.DISPLAY_NAME + " TEXT UNIQUE COLLATE NOCASE,"
                    + Task.Labels.DESCRIPTION + " TEXT,"
                    + Task.Labels._USER_APPLIED + " INTEGER"
					+ ");");

			// **************
			// Labeled Content
			// **************
            db.execSQL("CREATE TABLE " + LABELED_CONTENT_TABLE_NAME + " (" 
            		+ Task.LabeledContentColumns._ID + " INTEGER PRIMARY KEY,"
                    + Task.LabeledContentColumns._LABEL_ID + " INTEGER NOT NULL" 
                		+ " CONSTRAINT fk_label_id REFERENCES "+LABELS_TABLE_NAME+"("+Task.Labels._ID+") ON DELETE CASCADE,"  // NOTE: Entire constraint is ignored by SQLite. See trigger below for implementation.
                    + Task.LabeledContentColumns._CONTENT_URI + " TEXT NOT NULL," // Initially this will be a task URI.
                    + Task.LabeledContentColumns.CREATED_DATE + " INTEGER NOT NULL," 
                    + "UNIQUE ("+Task.LabeledContentColumns._LABEL_ID+","+Task.LabeledContentColumns._CONTENT_URI+")" 
                    + ");");

			// *******************************************************
			// Triggers for Labeled Content
			// *******************************************************

			// **************************************************
			// Cascade deletes (tasks <-- labeled_content)
			// **************************************************
            // For constraint details see: See http://www.justatheory.com/computers/databases/sqlite/ 
            db.execSQL(   "CREATE TRIGGER fkd_tasks_labeled_content_id "
			            + "BEFORE DELETE ON "+TASK_TABLE_NAME+" "
			            + "FOR EACH ROW BEGIN "
			            +     "DELETE from "+LABELED_CONTENT_TABLE_NAME+" WHERE "+Task.LabeledContentColumns._CONTENT_URI + " = '"+Task.Tasks.CONTENT_URI+"/' || OLD._id; "
					    + "END;");

			// *************************************************
			// Foreign key: Task.LabeledContentColumns._LABEL_ID
			// *************************************************

            // For constraint details see: See http://www.justatheory.com/computers/databases/sqlite/ 
			final String TRIGGER_LABELED_CONTENT = "labeled_content";
            db.execSQL(	  "CREATE TRIGGER fki_"+TRIGGER_LABELED_CONTENT+" "
	            		+ "BEFORE INSERT ON "+LABELED_CONTENT_TABLE_NAME+" "
	            		+ "FOR EACH ROW BEGIN " 
	            		+ "SELECT CASE "
	            		+      "WHEN ((SELECT _id FROM "+LABELS_TABLE_NAME+" WHERE _id = NEW."+Task.LabeledContentColumns._LABEL_ID+") IS NULL) "
	            		+      "THEN RAISE(ABORT, 'insert on table \""+LABELED_CONTENT_TABLE_NAME+"\" violates foreign key constraint \"fki_"+TRIGGER_LABELED_CONTENT+"\"') "
	            		+ "END;"
	            		+ "END;");

            // For constraint details see: See http://www.justatheory.com/computers/databases/sqlite/ 
            db.execSQL(  "CREATE TRIGGER fku_"+TRIGGER_LABELED_CONTENT+" "
			           + "BEFORE UPDATE ON "+LABELED_CONTENT_TABLE_NAME+" "
			           + "FOR EACH ROW BEGIN " 
			           +    "SELECT CASE "
			           +      "WHEN ((SELECT _id FROM "+LABELS_TABLE_NAME+" WHERE _id = new."+Task.LabeledContentColumns._LABEL_ID+") IS NULL) "
			           +      "THEN RAISE(ABORT, 'update on table \""+LABELED_CONTENT_TABLE_NAME+"\" violates foreign key constraint \"fku_"+TRIGGER_LABELED_CONTENT+"\"') "
			           +   "END;"
			           + "END;");            

			// Cascade deletes
            // For constraint details see: See http://www.justatheory.com/computers/databases/sqlite/ 
            db.execSQL(   "CREATE TRIGGER fkd_"+TRIGGER_LABELED_CONTENT+" "
			            + "BEFORE DELETE ON "+LABELS_TABLE_NAME+" "
			            + "FOR EACH ROW BEGIN "
			            +     "DELETE from "+LABELED_CONTENT_TABLE_NAME+" WHERE "+Task.LabeledContentColumns._LABEL_ID+" = OLD._id; "
			            + "END;");

			// **************
			// Filter
			// **************
            db.execSQL("CREATE TABLE " + FILTER_TABLE_NAME + " (" 
            		+ Task.FilterColumns._ID + " INTEGER PRIMARY KEY," 
            		+ Task.FilterColumns.DISPLAY_NAME + " TEXT UNIQUE,"
            		+ Task.FilterColumns.DESCRIPTION + " TEXT," 
            		+ Task.FilterColumns._DISPLAY_NAME_ARRAY_INDEX + " TEXT," 
                    + Task.FilterColumns._PERMANENT   + " INTEGER UNIQUE," 
                    + Task.FilterColumns._ACTIVE      + " INTEGER," 
                    + Task.FilterColumns._SELECTED      + " INTEGER UNIQUE" 
					+ ");");

			// **************
			// Filter Element
			// **************
            db.execSQL("CREATE TABLE " + FILTER_ELEMENT_TABLE_NAME + " (" 
            		+ Task.FilterElementColumns._ID + " INTEGER PRIMARY KEY,"
                    + Task.FilterElementColumns._FILTER_ID   + " INTEGER NOT NULL " 
            			+ " CONSTRAINT fk_filter_id REFERENCES "+FILTER_TABLE_NAME+"("+Task.FilterColumns._ID+") ON DELETE CASCADE,"  // NOTE: Entire constraint is ignored by SQLite. See trigger below for implementation.
                    + Task.FilterElementColumns._ACTIVE    + " INTEGER," 
                    + Task.FilterElementColumns._CONSTRAINT_URI + " TEXT," // TODO: !!! Add a composit key for _CONSTRAINT_URI and _FILTER_ID
                    + Task.FilterElementColumns._APPLY_WHEN_ACTIVE + " INTEGER," 
                    + Task.FilterElementColumns._PHASE + " INTEGER," 
                    + Task.FilterElementColumns._PARAMETERS  + " TEXT," // Value is null when this label should not be applied.
                    + Task.FilterElementColumns._ORDER + " INTEGER," // Added in version 3. 
					+ "UNIQUE(" + Task.FilterElementColumns._FILTER_ID + ", " + Task.FilterElementColumns._CONSTRAINT_URI + ") );");

			// *******************************************************
			// Triggers for Filter Element
			// *******************************************************

			// *************************************************
			// Foreign key: Task.FilterElementColumns._FILTER_ID
			// *************************************************

            // For constraint details see: See http://www.justatheory.com/computers/databases/sqlite/ 
			final String TRIGGER_FILTER_ELEMENT_FILTER_ID = "filter_element_filter_id";
            db.execSQL(	  "CREATE TRIGGER fki_"+TRIGGER_FILTER_ELEMENT_FILTER_ID+" "
	            		+ "BEFORE INSERT ON "+FILTER_ELEMENT_TABLE_NAME+" "
	            		+ "FOR EACH ROW BEGIN " 
	            		+ "SELECT CASE "
	            		+      "WHEN ((SELECT _id FROM "+FILTER_TABLE_NAME+" WHERE _id = NEW."+Task.FilterElementColumns._FILTER_ID+") IS NULL) "
	            		+      "THEN RAISE(ABORT, 'insert on table \""+FILTER_ELEMENT_TABLE_NAME+"\" violates foreign key constraint \"fki_"+TRIGGER_FILTER_ELEMENT_FILTER_ID+"\"') "
	            		+ "END;"
	            		+ "END;");

            // For constraint details see: See http://www.justatheory.com/computers/databases/sqlite/ 
            db.execSQL(  "CREATE TRIGGER fku_"+TRIGGER_FILTER_ELEMENT_FILTER_ID+" "
			           + "BEFORE UPDATE ON "+FILTER_ELEMENT_TABLE_NAME+" "
			           + "FOR EACH ROW BEGIN " 
			           +    "SELECT CASE "
			           +      "WHEN ((SELECT _id FROM "+FILTER_TABLE_NAME+" WHERE _id = new."+Task.FilterElementColumns._FILTER_ID+") IS NULL) "
			           +      "THEN RAISE(ABORT, 'update on table \""+FILTER_ELEMENT_TABLE_NAME+"\" violates foreign key constraint \"fku_"+TRIGGER_FILTER_ELEMENT_FILTER_ID+"\"') "
			           +   "END;"
			           + "END;");            

			// Cascade deletes
            // For constraint details see: See http://www.justatheory.com/computers/databases/sqlite/ 
            db.execSQL(   "CREATE TRIGGER fkd_"+TRIGGER_FILTER_ELEMENT_FILTER_ID+" "
			            + "BEFORE DELETE ON "+FILTER_TABLE_NAME+" "
			            + "FOR EACH ROW BEGIN "
			            +     "DELETE from "+FILTER_ELEMENT_TABLE_NAME+" WHERE "+Task.FilterElementColumns._FILTER_ID+" = OLD._id; "
			            + "END;");

			// **************
			// TempFile
			// **************
            db.execSQL("CREATE TABLE "+TEMP_FILE_TABLE_NAME+" (" 
            		+ Task.TempFile._ID + " INTEGER PRIMARY KEY,"
                    + Task.TempFile.DISPLAY_NAME + " TEXT,"
                    + Task.TempFile.DATA + " DATA STREAM," // TODO: !!! Remove this column from db. Data is stored in a file. The _data field is only used for ContentValues (but the value is still stored,, so some refactoring is required). 
                    + Task.TempFile.SIZE + " INTEGER," 
                    + Task.TempFile.DATE_ADDED + " INTEGER,"
                    + Task.TempFile.DATE_MODIFIED + " INTEGER,"
                    + Task.TempFile.MIME_TYPE + " TEXT,"
                    + Task.TempFile.TITLE + " TEXT,"
                    + Task.TempFile._PRESERVE_UNTIL + " INTEGER" 
                    + ");"); 
			// Create application temp folder.
			getTempDir(mContext);

			// **************
			// Notification
			// **************
            db.execSQL("CREATE TABLE "+NOTIFICATION_TABLE_NAME+" (" 
            		+ Task.Notification._ID + " INTEGER PRIMARY KEY,"
                    + Task.Notification._URI + " TEXT UNIQUE"
					+ ");");

			// **************
			// Completable
			// **************
            db.execSQL("CREATE TABLE "+COMPLETABLE_TABLE_NAME+" (" 
            		+ Task.Completable._ID + " INTEGER PRIMARY KEY,"
                    + Task.Completable.TEXT_CONTENT + " TEXT NOT NULL,"
                    + Task.Completable._COMPLETED + " INTEGER"
					+ ");");

			// Cascade deletes
            // For constraint details see: See http://www.justatheory.com/computers/databases/sqlite/ 
            // NOTE: This shouldn't be needed, but is a temp fix because attachment's _delete_intent arent' reliable. 
            db.execSQL(   "CREATE TRIGGER fkd_tasks_completable_id "
			            + "BEFORE DELETE ON "+COMPLETABLE_TABLE_NAME+" "
			            + "FOR EACH ROW BEGIN "
			            +     "DELETE from "+ATTACHMENT_TABLE_NAME+" WHERE "+Task.TaskAttachments._URI+" = '" + Task.Completable.CONTENT_URI + "/' || OLD._id; " // Task.Completable.ATTACHMENT_ID
					+ "END;");

			// **************
			// App Widgets
			// **************
			createAppWidgetTable(db);
			createAppWidgetDeletedTable(db);

			loadInitialData(db, mContext);
			loadInitialSuplimentalData(db);
		}

		private void createAppWidgetTable(SQLiteDatabase db) {
    		db.execSQL("CREATE TABLE "+APP_WIDGETS_TABLE_NAME+" (" 
    				+ Task.Tasks._ID + " INTEGER PRIMARY KEY,"
    				+ Task.AppWidgets._APP_WIDGET_ID + " INTEGER NOT NULL UNIQUE," 
                    + Task.AppWidgets._TYPE + " INTEGER NOT NULL," 
    				+ Task.AppWidgets._THEME + " INTEGER NOT NULL," 
    				+ Task.AppWidgets._TEXT_SIZE + " INTEGER NOT NULL" 
    				+ ");");
		}

		private void createAppWidgetDeletedTable(SQLiteDatabase db) {
    		db.execSQL("CREATE TABLE "+APP_WIDGETS_DELETED_TABLE_NAME+" (" 
    				+ Task.Tasks._ID + " INTEGER PRIMARY KEY,"
    				+ Task.AppWidgetsDeleted._APP_WIDGET_ID + " INTEGER NOT NULL UNIQUE" 
    				+ ");");
		}

		/**
		 * @Override
		 */
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.i(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will preserve all old data");

			// TODO: !!!! Test/Verify that the next upgrade is handled.
			switch (oldVersion) {
				case 1:
//            		db.execSQL("ALTER TABLE " + ATTACHMENT_TABLE_NAME + " ADD COLUMN " + Task.TaskAttachments._DELETE_INTENT + " BLOB");
				case 2:
//            		db.execSQL("ALTER TABLE " + FILTER_ELEMENT_TABLE_NAME + " ADD COLUMN " + Task.FilterElement._ORDER + " INTEGER" );
//            		// db.execSQL("ALTER TABLE " + FILTER_TABLE_NAME         + " MODIFY " + Task.Filter.DISPLAY_NAME + " TEXT UNIQUE" ); // SQLite3 can't alter column constraints.
//            		db.execSQL("ALTER TABLE " + FILTER_TABLE_NAME         + " ADD COLUMN " + Task.Filter.DESCRIPTION   + " TEXT" );
//            		db.execSQL("ALTER TABLE " + FILTER_TABLE_NAME         + " ADD COLUMN " + Task.Filter._SELECTED   + " INTEGER UNIQUE" );
//            		// db.execSQL("ALTER TABLE " + FILTER_TABLE_NAME         + " MODIFY " + Task.Filter._PERMANENT + " INTEGER UNIQUE" ); // SQLite3 can't alter column constraints.
//            		// db.execSQL("ALTER TABLE " + FILTER_TABLE_NAME         + " MODIFY " + Task.Filter._ACTIVE + " INTEGER" ); // SQLite3 can't alter column constraints.
//            		// db.execSQL("ALTER TABLE " + FILTER_ELEMENT_TABLE_NAME         + " MODIFY " + Task.Filter._ACTIVE + " INTEGER" ); // SQLite3 can't alter column constraints.
//            		// db.execSQL("ALTER TABLE " + FILTER_TABLE_NAME         + " UNIQUE("+Task.FilterElementColumns._FILTER_ID+", "+Task.FilterElementColumns._CONSTRAINT_URI+") " ); // SQLite3 can't alter column constraints.
//            		// db.execSQL("ALTER TABLE " + ATTACHMENT_TABLE_NAME         + " MODIFY " + Task.TaskAttachments._INTENT + " TEXT" ); // SQLite3 can't alter column constraints.
//            		// db.execSQL("ALTER TABLE " + ATTACHMENT_TABLE_NAME         + " MODIFY " + Task.TaskAttachments._DELETE_INTENT + " TEXT" ); // SQLite3 can't alter column constraints.
//            		// db.execSQL("ALTER TABLE " + ATTACHMENT_TABLE_NAME         + " DELETE COLUMN " + Task.TaskAttachments.MISC_EXTRA ); // SQLite3 can't alter column constraints.
            	case 3:
            		// Deleted _created and _modified fields from most tables.
            	case 4:
            		createAppWidgetTable(db);
            		createAppWidgetDeletedTable(db);
            	case 5:
            	case 6:
            	case 7:
            	case 8:
				case 9:
				case 10:
				case 11:
				case 12:
				case 13:
				case 14:
				case 15:
				case 16:
				case 17:
				case 18:
				case 19:
				case 20:
				case 21:
				case 22:
				case 23:
				case 24:
					break;

            	default: // TODO: !!! Come up with a better way to handle development mode. 
					if (!StaticConfig.PRODUCTION_MODE) {

						db.execSQL("DROP TABLE IF EXISTS " + TASK_TABLE_NAME);
						db.execSQL("DROP TABLE IF EXISTS " + ATTACHMENT_TABLE_NAME);
						db.execSQL("DROP TABLE IF EXISTS " + PROXIMITY_ALERT_TABLE_NAME);
						db.execSQL("DROP TABLE IF EXISTS " + LABELS_TABLE_NAME);
						db.execSQL("DROP TABLE IF EXISTS " + LABELED_CONTENT_TABLE_NAME);
						db.execSQL("DROP TABLE IF EXISTS " + FILTER_TABLE_NAME);
						db.execSQL("DROP TABLE IF EXISTS " + FILTER_ELEMENT_TABLE_NAME);
						db.execSQL("DROP TABLE IF EXISTS " + TEMP_FILE_TABLE_NAME);
						db.execSQL("DROP TABLE IF EXISTS " + NOTIFICATION_TABLE_NAME);
						db.execSQL("DROP TABLE IF EXISTS " + COMPLETABLE_TABLE_NAME);
						db.execSQL("DROP TABLE IF EXISTS " + APP_WIDGETS_TABLE_NAME);
						db.execSQL("DROP TABLE IF EXISTS " + APP_WIDGETS_DELETED_TABLE_NAME);
						onCreate(db);

						// Make sure the settings are set to the default value.
						SharedPreferences settings = mContext.getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE);
						Editor editor = settings.edit();
						editor.putBoolean(ApplicationPreference.ENABLE_ARCHIVE, ApplicationPreference.ENABLE_ARCHIVE_DEFAULT);
						editor.putBoolean(ApplicationPreference.ENABLE_LABELS, ApplicationPreference.ENABLE_LABELS_DEFAULT);
						editor.commit();
					}
			}
		}
		
		private void loadInitialSuplimentalData(SQLiteDatabase db) {
			// ****************************
			// Initial tasks
			// ****************************

			// "Read why FlingTap Done is special" task
			db.execSQL(
					"INSERT INTO " + TASK_TABLE_NAME 
			  		+ " ("+Task.TasksColumns.TASK_TITLE+","+Task.TasksColumns.TASK_DESC+","+Task.TasksColumns.CREATED_DATE+","+Task.Tasks.MODIFIED_DATE+","+Task.Tasks.PRIORITY+","+Task.Tasks.DUE_DATE+","+Task.Tasks.ALARM_ACTIVE+","+Task.TasksColumns._FILTER_BIT+","+Task.TasksColumns.COMPLETE
			  		+") VALUES ('"+mContext.getString(R.string.task_tellUsWhatYouThink)+"','"+mContext.getString(R.string.task_weWantToKnowAboutYourFlingTapDoneExperience)+"',"+System.currentTimeMillis()+","+System.currentTimeMillis()+","+Task.Tasks.PRIORITY_NONE+","+Task.Tasks.DUE_DATE_NOT_SET_LONG+","+Task.Tasks.ALARM_ACTIVE_FALSE+","+Task.TasksColumns.FILTER_IN+","+Task.TasksColumns.COMPLETE_FALSE+")");
		}
	}

	private static void loadInitialData(SQLiteDatabase db, Context context) {
		// ****************************
		// "Archived" label
		// ****************************
        db.execSQL(
        		"INSERT INTO " + LABELS_TABLE_NAME 
        		+ " ("+Task.LabelsColumns._ID+","+Task.LabelsColumns.DISPLAY_NAME+","+Task.LabelsColumns._USER_APPLIED // +","+Task.LabelsColumns.CREATED_DATE
        		+") VALUES ("+ID_LABEL_ARCHIVED+",'--Archived--',"+Task.LabelsColumns.USER_APPLIED_FALSE+")"); // +","+System.currentTimeMillis()

		// ****************************
		// "Basic" Filter
		// ****************************
		// "Basic" filter.
        db.execSQL(
        		"INSERT INTO " + FILTER_TABLE_NAME 
        		+ " ("+Task.FilterColumns._ID+","+Task.FilterColumns.DISPLAY_NAME+","+Task.FilterColumns._DISPLAY_NAME_ARRAY_INDEX+","+Task.FilterColumns._PERMANENT+","+Task.FilterColumns._ACTIVE+","+Task.FilterColumns._SELECTED //+","+Task.FilterColumns.CREATED_DATE
        		+") VALUES ("+ID_FILTER_BASIC+",'-- Basic --',2, "+ID_FILTER_BASIC+","+Task.FilterColumns.ACTIVE_TRUE_STRING+","+Task.FilterColumns.SELECTED_TRUE+")");  // +","+System.currentTimeMillis()

		// "Due Date" filter element.
		db.execSQL(
				"INSERT INTO " + FILTER_ELEMENT_TABLE_NAME 
		  		+ " ("+Task.FilterElementColumns._FILTER_ID+","+Task.FilterElementColumns._ACTIVE+","+Task.FilterElementColumns._CONSTRAINT_URI+","+Task.FilterElementColumns._APPLY_WHEN_ACTIVE+","+Task.FilterElementColumns._PHASE+","+Task.FilterElementColumns._PARAMETERS+","+Task.FilterElementColumns._ORDER // +","+Task.FilterElementColumns.CREATED_DATE
		  		+") VALUES ("+ID_FILTER_BASIC+","+Task.FilterElementColumns.ACTIVE_TRUE+","+DatabaseUtils.sqlEscapeString(Constraint.Version1.DUE_CONTENT_URI_STRING)+","+Task.FilterElementColumns.APPLY_WHEN_ACTIVE+","+Task.FilterElementColumns.PHASE_INCLUDE+","+DatabaseUtils.sqlEscapeString(Constraint.Version1.INCLUDE_NO_DUE_DATE_ITEMS+"=true&"+Constraint.Version1.ANYTIME+"=true")+","+FilterUtil.DUE_DATE_FILTER_ELEMENT_ORDER+")"); // +","+System.currentTimeMillis()

		// "Priority" filter element.
        //    No _PARAMETERS value is provided so that it will be empty and thus be OFF by default.
		db.execSQL(
				"INSERT INTO " + FILTER_ELEMENT_TABLE_NAME 
		  		+ " ("+Task.FilterElementColumns._FILTER_ID+","+Task.FilterElementColumns._ACTIVE+","+Task.FilterElementColumns._CONSTRAINT_URI+","+Task.FilterElementColumns._APPLY_WHEN_ACTIVE+","+Task.FilterElementColumns._PHASE+","+Task.FilterElementColumns._ORDER // +","+Task.FilterElementColumns.CREATED_DATE
		  		+") VALUES ("+ID_FILTER_BASIC+","+Task.FilterElementColumns.ACTIVE_TRUE+","+DatabaseUtils.sqlEscapeString(Constraint.Version1.PRIORITY_CONTENT_URI_STRING)+","+Task.FilterElementColumns.APPLY_WHEN_ACTIVE+","+Task.FilterElementColumns.PHASE_INCLUDE+","+FilterUtil.PRIORITY_FILTER_ELEMENT_ORDER+")"); // +","+System.currentTimeMillis()

		// "Status" filter element.
        //    No _PARAMETERS value is provided so that it will be empty and thus be OFF by default.
		db.execSQL(
				"INSERT INTO " + FILTER_ELEMENT_TABLE_NAME 
		  		+ " ("+Task.FilterElementColumns._FILTER_ID+","+Task.FilterElementColumns._ACTIVE+","+Task.FilterElementColumns._CONSTRAINT_URI+","+Task.FilterElementColumns._APPLY_WHEN_ACTIVE+","+Task.FilterElementColumns._PHASE+","+Task.FilterElementColumns._ORDER // +","+Task.FilterElementColumns.CREATED_DATE
		  		+") VALUES ("+ID_FILTER_BASIC+","+Task.FilterElementColumns.ACTIVE_TRUE+","+DatabaseUtils.sqlEscapeString(Constraint.Version1.STATUS_CONTENT_URI_STRING)+","+Task.FilterElementColumns.APPLY_WHEN_ACTIVE+","+Task.FilterElementColumns.PHASE_INCLUDE+","+FilterUtil.STATUS_FILTER_ELEMENT_ORDER+")"); // +","+System.currentTimeMillis()

        // ****************************
        // "All" Filter
		// ****************************

		// "All" filter.
        db.execSQL(
        		"INSERT INTO " + FILTER_TABLE_NAME 
        		+ " ("+Task.FilterColumns._ID+","+Task.FilterColumns.DISPLAY_NAME+","+Task.FilterColumns._DISPLAY_NAME_ARRAY_INDEX+","+Task.FilterColumns._PERMANENT//+","+Task.FilterColumns.CREATED_DATE
        		+") VALUES ("+ID_FILTER_ALL+",'-- All --',0,"+ID_FILTER_ALL+")");//+","+System.currentTimeMillis() 

		// "Repository" filter element.
		db.execSQL(
				"INSERT INTO " + FILTER_ELEMENT_TABLE_NAME 
		  		+ " ("+Task.FilterElementColumns._FILTER_ID+","+Task.FilterElementColumns._CONSTRAINT_URI+","+Task.FilterElement._PARAMETERS+","+Task.FilterElementColumns._APPLY_WHEN_ACTIVE+","+Task.FilterElementColumns._PHASE+","+Task.FilterElementColumns._ORDER // +","+Task.FilterElementColumns.CREATED_DATE
		  		+") VALUES ("+ID_FILTER_ALL+","+DatabaseUtils.sqlEscapeString(Constraint.Version1.REPOSITORY_CONTENT_URI_STRING)+","+DatabaseUtils.sqlEscapeString(RepositoryFilterElementDelegate.FILTER_ELEMENT_INDEX_PARAMETER+"="+Constraint.Version1.REPOSITORY_PARAM_OPTION_MAIN+"&"+Constraint.Version1.REPOSITORY_PARAM+"="+Constraint.Version1.REPOSITORY_PARAM_OPTION_MAIN)+","+Task.FilterElementColumns.APPLY_WHEN_ACTIVE+","+Task.FilterElementColumns.PHASE_EXCLUDE+","+FilterUtil.ARCHIVE_FILTER_ELEMENT_ORDER+")"); // +","+System.currentTimeMillis() 

		// ****************************
		// "Archive" Filter
		// ****************************

		// "Archive" filter.
        db.execSQL(
        		"INSERT INTO " + FILTER_TABLE_NAME 
        		+ " ("+Task.FilterColumns._ID+","+Task.FilterColumns.DISPLAY_NAME+","+Task.FilterColumns._DISPLAY_NAME_ARRAY_INDEX+","+Task.FilterColumns.DESCRIPTION+","+Task.FilterColumns._PERMANENT//+","+Task.FilterColumns.CREATED_DATE
        		+") VALUES ("+ID_FILTER_ARCHIVE+",'-- Archive --',1,'"+context.getString(R.string.allTasksInTheArchive)+"', "+ID_FILTER_ARCHIVE+")"); // +","+System.currentTimeMillis()

		// "Repository" filter element.
		db.execSQL(
				"INSERT INTO " + FILTER_ELEMENT_TABLE_NAME 
		  		+ " ("+Task.FilterElementColumns._FILTER_ID+","+Task.FilterElementColumns._CONSTRAINT_URI+","+Task.FilterElement._PARAMETERS+","+Task.FilterElementColumns._APPLY_WHEN_ACTIVE+","+Task.FilterElementColumns._PHASE+","+Task.FilterElementColumns._ORDER // +","+Task.FilterElementColumns.CREATED_DATE
		  		+") VALUES ("+ID_FILTER_ARCHIVE+","+DatabaseUtils.sqlEscapeString(Constraint.Version1.REPOSITORY_CONTENT_URI_STRING)+","+DatabaseUtils.sqlEscapeString(RepositoryFilterElementDelegate.FILTER_ELEMENT_INDEX_PARAMETER+"="+Constraint.Version1.REPOSITORY_PARAM_OPTION_ARCHIVE+"&"+Constraint.Version1.REPOSITORY_PARAM+"="+Constraint.Version1.REPOSITORY_PARAM_OPTION_ARCHIVE)+","+Task.FilterElementColumns.APPLY_WHEN_ACTIVE+","+Task.FilterElementColumns.PHASE_EXCLUDE+","+FilterUtil.ARCHIVE_FILTER_ELEMENT_ORDER+")"); // +","+System.currentTimeMillis() 
	}

	private DatabaseHelper dbHelper = null;
	
 	BackupManagerProxy mBackupManagerHolder = null;
	SharedPreferences mSharedPrefs = null;

 	private void backupDataChanged(){
 		if( null != mBackupManagerHolder && mSharedPrefs.getBoolean(ApplicationPreference.BACKUP_ENABLED, ApplicationPreference.BACKUP_ENABLED_DEFAULT)){
 			mBackupManagerHolder.dataChanged(getContext(), true);
 		}
 	}
 	
	@Override
	public boolean onCreate() {
		try{
			mSharedPrefs = getContext().getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE);		
					
		 	if( Integer.parseInt(Build.VERSION.SDK) >= 8 ){
		 		mBackupManagerHolder = BackupManagerProxy.getInstance(getContext());
		 	}
		} catch (HandledException h) { // Ignore.
		} catch (Exception exp) {
			Log.e(TAG, "ERR000KM", exp);
			ErrorUtil.handleException("ERR000KM", exp, getContext());
		}
		try {
			dbHelper = new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION);
			if (null == dbHelper.getWritableDatabase()) {
				ErrorUtil.handle("ERR00095", "null == dbHelper.getWritableDatabase()", this);
				return false;
			}
			return true;
		} catch (HandledException h) { // Ignore.
		} catch (Exception exp) {
			Log.e(TAG, "ERR00094", exp);
			ErrorUtil.handleException("ERR00094", exp, getContext());
		}
		return false;
	}

	/**
     * Handle query requests from clients.
     *
     * Admittedly, this is an obnoxiously long method. In its defense, it is very disciplined. Each case is
     *   independent of all others (ie. no fall through conditions). Simply familiarize yourself with the setup at the
     *   start of the method, the execution of the query at the end of the method, and then find the case for the url
     *   you are investigating somewhere in the middle.
     *
     * NOTE: Removed the qb.setProjectionMap(..) calls because there is no current need for projection mapping.
	 */
    public Cursor query(Uri url, String[] projection, String selection,
            String[] selectionArgs, String sort) {

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		SQLiteDatabase mDB = dbHelper.getReadableDatabase();

		// If no sort order is specified use the default
		// String orderBy=Task.Tasks.DEFAULT_SORT_ORDER;
        String orderBy=BaseColumns._ID; // TODO: !! Change to default (null). Let the caller decide how to order results. _id is not a valid sort in all cases.  
		switch (URL_MATCHER.match(url)) {
			case TASKS:
				// qb.setTables("tasks");
				qb.setTables("tasks as t");
                // qb.setProjectionMap(TASKS_LIST_PROJECTION_MAP); // NOTE: Remember that subselects included in the columns list must also be mapped.
				sort = Task.Tasks.DEFAULT_SORT_ORDER;
				break;

			case TASK_ID:
				// qb.setTables("tasks");
				qb.setTables("tasks as t");
				// qb.appendWhere("_id=" + url.getgetPathSegment(1));
				qb.appendWhere("_id=" + url.getLastPathSegment());
				break;

			case ATTACHMENTS:
				// qb.setTables(DATABASE_TABLE_NAME);
				qb.setTables("attachments as t");
                // qb.setProjectionMap(TASKS_ATTACHMENT_PROJECTION_MAP); // NOTE: Remember that subselects included in the columns list must also be mapped.
				sort = Task.TaskAttachments.DEFAULT_SORT_ORDER;
				break;

			case ATTACHMENT_ID:
				// qb.setTables(DATABASE_TABLE_NAME);
				qb.setTables("attachments as t");
				// qb.appendWhere("_id=" + url.getPathSegment(1));
				qb.appendWhere("_id=" + url.getLastPathSegment());
				break;

			case PROXIMITY_ALERTS:
				qb.setTables("proximity_alerts as t");
                // qb.setProjectionMap(PROXIMITY_ALERT_PROJECTION_MAP); // NOTE: Remember that subselects included in the columns list must also be mapped.
				break;

			case PROXIMITY_ALERT_ID:
				qb.setTables("proximity_alerts as t");
				// qb.appendWhere("_id=" + url.getPathSegment(1));
				qb.appendWhere("_id=" + url.getLastPathSegment());

				break;

        case TASK_ATTACHMENT_PROXIMITY_ALERT_ID: // TODO: ! Check whether I'm including too much join information.
				qb.setTables(TASK_TABLE_NAME + "," + ATTACHMENT_TABLE_NAME + "," + PROXIMITY_ALERT_TABLE_NAME);

				qb.appendWhere(TASK_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Tasks._ID);
				qb.appendWhere("=");
				qb.appendWhere(url.getPathSegments().get(1));

				qb.appendWhere(" AND ");

				qb.appendWhere(ATTACHMENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.TaskAttachments._ID);
				qb.appendWhere("=");
				qb.appendWhere(url.getPathSegments().get(3));

				qb.appendWhere(" AND ");

				qb.appendWhere(PROXIMITY_ALERT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.ProximityAlerts._ID);
				qb.appendWhere("=");
				qb.appendWhere(url.getLastPathSegment());

				qb.appendWhere(" AND ");

				// Join Tasks to Attachments.
				qb.appendWhere(TASK_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Tasks._ID);
				qb.appendWhere("=");
				qb.appendWhere(ATTACHMENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.TaskAttachments.TASK_ID);

				qb.appendWhere(" AND ");

				// Join Attachments To Proximity Alerts.
				qb.appendWhere("'");
				qb.appendWhere(Task.ProximityAlerts.CONTENT_URI_STRING);
				qb.appendWhere("/' || ");
				qb.appendWhere(PROXIMITY_ALERT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.ProximityAlerts._ID);
				qb.appendWhere("=");
				qb.appendWhere(ATTACHMENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.TaskAttachments._URI);

				qb.appendWhere(" AND ");

				// attachments._uri LIKE content://AUTHORITY/proximity
				qb.appendWhere(ATTACHMENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.TaskAttachments._URI);
				qb.appendWhere(" LIKE ");
			    qb.appendWhere(DatabaseUtils.sqlEscapeString(Task.ProximityAlerts.CONTENT_URI_STRING.toString())); // QUEST: Should this have a '/' on the end?

				sort = PROXIMITY_ALERT_TABLE_NAME + "." + Task.ProximityAlerts._ID;
				break;

			case TASKS_CALLMINDER:
				qb.setTables(TASK_TABLE_NAME + "," + ATTACHMENT_TABLE_NAME);

				// Join Tasks to Attachments.
				qb.appendWhere(TASK_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Tasks._ID);
				qb.appendWhere("=");
				qb.appendWhere(ATTACHMENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.TaskAttachments.TASK_ID);

				qb.appendWhere(" AND ");

				// Only not completed tasks.
				qb.appendWhere(TASK_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Tasks.COMPLETE);
				qb.appendWhere("=");
				qb.appendWhere(Task.Tasks.COMPLETE_FALSE);

				qb.appendWhere(" AND ");

				// Only tasks with the given attachment.
				qb.appendWhere(ATTACHMENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.TaskAttachments._URI);
				qb.appendWhere(" LIKE '");
			    qb.appendWhere(url.getQueryParameter(Task.TaskAttachments._URI)); // TODO: Validate this input using DatabaseUtils.sqlEscapeString(..).
				qb.appendWhere("%'");

                // Log.v(TAG, "url.getQueryParameter(Task.TaskAttachments._URI) == "+url.getQueryParameter(Task.TaskAttachments._URI));
            
				qb.appendWhere(" AND ");

				// Not Archived Tasks.
				qb.appendWhere(TASK_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Tasks._ID);
				qb.appendWhere(" NOT IN (");

				qb.appendWhere("SELECT ");
				qb.appendWhere(TASK_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Tasks._ID);
				qb.appendWhere(" FROM ");
				qb.appendWhere(LABELED_CONTENT_TABLE_NAME);
				qb.appendWhere(",");
				qb.appendWhere(LABELS_TABLE_NAME);
				qb.appendWhere(",");
				qb.appendWhere(TASK_TABLE_NAME);

				qb.appendWhere(" WHERE ");

				// Joins tasks table to labeled_content table.
				// 'content://"+AUTHORITY+"/tasks' + (tasks._id) = labeled_content._content_uri
				qb.appendWhere("'");
				qb.appendWhere(Task.Tasks.CONTENT_URI_STRING);
				qb.appendWhere("/' || ");
				qb.appendWhere(TASK_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Tasks._ID);
				qb.appendWhere("=");
				qb.appendWhere(LABELED_CONTENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.LabeledContentColumns._CONTENT_URI);

				qb.appendWhere(" AND ");

				// Joins labeled_content to labels table.
				// labeled_content._label_id = labels._id
				qb.appendWhere(LABELED_CONTENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.LabeledContentColumns._LABEL_ID);
				qb.appendWhere("=");
				qb.appendWhere(LABELS_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Labels._ID);

				qb.appendWhere(" AND ");

				// labels._id = ID_LABEL_ARCHIVED
				qb.appendWhere(LABELS_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Labels._ID);
				qb.appendWhere("="); // TODO: !!!
				qb.appendWhere(ID_LABEL_ARCHIVED);

				qb.appendWhere(")");

				break;

			case TASK_ATTACHMENT_ID:
				qb.setTables(TASK_TABLE_NAME + "," + ATTACHMENT_TABLE_NAME);

				qb.appendWhere(TASK_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Tasks._ID);
				qb.appendWhere("=");
				qb.appendWhere(url.getPathSegments().get(1));

				qb.appendWhere(" AND ");

				qb.appendWhere(ATTACHMENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.TaskAttachments._ID);
				qb.appendWhere("=");
				qb.appendWhere(url.getPathSegments().get(3));

				qb.appendWhere(" AND ");

				// Join Tasks to Attachments.
				qb.appendWhere(TASK_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Tasks._ID);
				qb.appendWhere("=");
				qb.appendWhere(ATTACHMENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.TaskAttachments.TASK_ID);

				sort = ATTACHMENT_TABLE_NAME + "." + Task.TaskAttachments._ID;
				break;

			case PROXIMITY_ALERT_TASK_ATTACHMENT_ID:

				qb.setTables(TASK_TABLE_NAME + "," + ATTACHMENT_TABLE_NAME + "," + PROXIMITY_ALERT_TABLE_NAME);

				// proximity_alerts._id=4
				qb.appendWhere(PROXIMITY_ALERT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.ProximityAlerts._ID);
				qb.appendWhere("=");
				qb.appendWhere(url.getPathSegments().get(1));

				qb.appendWhere(" AND ");

				// Join Tasks to Attachments.
				// tasks._id=attachments._task_id
				qb.appendWhere(TASK_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Tasks._ID);
				qb.appendWhere("=");
				qb.appendWhere(ATTACHMENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.TaskAttachments.TASK_ID);

				qb.appendWhere(" AND ");

				// Join Attachments To Proximity Alerts.
				qb.appendWhere("'");
				qb.appendWhere(Task.ProximityAlerts.CONTENT_URI_STRING);
				qb.appendWhere("/' || ");
				qb.appendWhere(PROXIMITY_ALERT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.ProximityAlerts._ID);
				qb.appendWhere("=");
				qb.appendWhere(ATTACHMENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.TaskAttachments._URI);

				qb.appendWhere(" AND ");

				// attachments._uri LIKE content://AUTHORITY/proximity
				qb.appendWhere(ATTACHMENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.TaskAttachments._URI);
				qb.appendWhere(" LIKE ");
    			qb.appendWhere(DatabaseUtils.sqlEscapeString(Task.ProximityAlerts.CONTENT_URI_STRING.toString()+"/%") ); // QUEST: Should this have a '/' on the end?

				sort = PROXIMITY_ALERT_TABLE_NAME + "." + Task.ProximityAlerts._ID;
				break;

			case LABELS:
				qb.setTables(LABELS_TABLE_NAME);
                // qb.setProjectionMap( TODO: add mappings ); // NOTE: Remember that subselects included in the columns list must also be mapped.
				break;

			case LABEL_ID:
				qb.setTables(LABELS_TABLE_NAME);
				qb.appendWhere(Task.Labels._ID + "=" + url.getLastPathSegment());
				break;

			case LABELED_CONTENT:
				qb.setTables(LABELED_CONTENT_TABLE_NAME);
				break;

			case LABELED_CONTENT_ID:
				qb.setTables(LABELED_CONTENT_TABLE_NAME);

				// labeled_content._id = #
				qb.appendWhere(LABELED_CONTENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.LabeledContentColumns._ID);
				qb.appendWhere("=");
				qb.appendWhere(url.getPathSegments().get(1));
				break;

			case LABELS_LABELED_CONTENT:
				// INNER JOIN people on (contact_methods.person = people._id)

				qb.setTables(LABELED_CONTENT_TABLE_NAME + "," + LABELS_TABLE_NAME);

                // qb.setProjectionMap( TODO: add mappings ); // NOTE: Remember that subselects included in the columns list must also be mapped.

				// labels._id = #
				qb.appendWhere(LABELS_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Labels._ID);
				qb.appendWhere("=");
				qb.appendWhere(url.getPathSegments().get(1));

				qb.appendWhere(" AND ");

				// Joins labeled_content to labels table.
				// labeled_content._label_id = labels._id
				qb.appendWhere(LABELED_CONTENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.LabeledContentColumns._LABEL_ID);
				qb.appendWhere("=");
				qb.appendWhere(LABELS_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Labels._ID);

				break;

			case LABELS_LABELED_CONTENT_ID:

				qb.setTables(LABELED_CONTENT_TABLE_NAME + "," + LABELS_TABLE_NAME);

                // qb.setProjectionMap( TODO: add mappings ); // NOTE: Remember that subselects included in the columns list must also be mapped.

				// labels._id = #
				qb.appendWhere(LABELS_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Labels._ID);
				qb.appendWhere("=");
				qb.appendWhere(url.getPathSegments().get(1));

				qb.appendWhere(" AND ");

				// Joins labeled_content to labels table.
				// labeled_content._label_id = labels._id
				qb.appendWhere(LABELED_CONTENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.LabeledContentColumns._LABEL_ID);
				qb.appendWhere("=");
				qb.appendWhere(LABELS_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Labels._ID);

				qb.appendWhere(" AND ");

				// labeled_content._id = #
				qb.appendWhere(LABELED_CONTENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.LabeledContentColumns._ID);
				qb.appendWhere("=");
				qb.appendWhere(url.getPathSegments().get(3));

				break;

			case LABELS_TASKS:
				// INNER JOIN people on (contact_methods.person = people._id)
				qb.setTables(LABELED_CONTENT_TABLE_NAME + "," + LABELS_TABLE_NAME + "," + TASK_TABLE_NAME);
                // qb.setProjectionMap( TODO: add mappings ); // NOTE: Remember that subselects included in the columns list must also be mapped.

				// labeled_content._content_uri LIKE content://AUTHORITY/tasks
				qb.appendWhere(LABELED_CONTENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.LabeledContentColumns._CONTENT_URI);
				qb.appendWhere(" LIKE ");
    			qb.appendWhere(DatabaseUtils.sqlEscapeString(Task.Tasks.CONTENT_URI_STRING.toString())); // QUEST: Should this have a '/' on the end?

				qb.appendWhere(" AND ");

				// Joins tasks table to labeled_content table.
				qb.appendWhere("'");
				qb.appendWhere(Task.Tasks.CONTENT_URI_STRING);
				qb.appendWhere("/' || ");
				qb.appendWhere(TASK_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Tasks._ID);
				qb.appendWhere("=");
				qb.appendWhere(LABELED_CONTENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.LabeledContentColumns._CONTENT_URI);

				qb.appendWhere(" AND ");

				// labels._id = #
				qb.appendWhere(LABELS_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Labels._ID);
				qb.appendWhere("=");
				qb.appendWhere(url.getPathSegments().get(1));

				qb.appendWhere(" AND ");

				// Joins labeled_content to labels table.
				// labeled_content._label_id = labels._id
				qb.appendWhere(LABELED_CONTENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.LabeledContentColumns._LABEL_ID);
				qb.appendWhere("=");
				qb.appendWhere(LABELS_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Labels._ID);

				break;

			case LABELS_TASKS_ID:
				qb.setTables(LABELED_CONTENT_TABLE_NAME + "," + LABELS_TABLE_NAME + "," + TASK_TABLE_NAME);

				List<String> pathSegments = url.getPathSegments();

				// tasks._id = #
				qb.appendWhere(TASK_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Tasks._ID);
				qb.appendWhere("=");
				qb.appendWhere(pathSegments.get(3));

				qb.appendWhere(" AND ");

				// labeled_content._content_uri LIKE content://AUTHORITY/tasks
				qb.appendWhere(LABELED_CONTENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.LabeledContentColumns._CONTENT_URI);
				qb.appendWhere(" LIKE ");
			qb.appendWhere(DatabaseUtils.sqlEscapeString(Task.Tasks.CONTENT_URI_STRING)); // QUEST: Should this have a '/' on the end?

				qb.appendWhere(" AND ");

				// Joins tasks table to labeled_content table.
				qb.appendWhere("'");
				qb.appendWhere(Task.Tasks.CONTENT_URI_STRING);
				qb.appendWhere("/' || ");
				qb.appendWhere(TASK_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Tasks._ID);
				qb.appendWhere("=");
				qb.appendWhere(LABELED_CONTENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.LabeledContentColumns._CONTENT_URI);

				qb.appendWhere(" AND ");

				// labels._id = #
				qb.appendWhere(LABELS_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Labels._ID);
				qb.appendWhere("=");
				qb.appendWhere(pathSegments.get(1));

				qb.appendWhere(" AND ");

				// Joins labeled_content to labels table.
				// labeled_content._label_id = labels._id
				qb.appendWhere(LABELED_CONTENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.LabeledContentColumns._LABEL_ID);
				qb.appendWhere("=");
				qb.appendWhere(LABELS_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Labels._ID);

				break;

			case TASKS_LABELS:
				// INNER JOIN people on (contact_methods.person = people._id)
				qb.setTables(LABELED_CONTENT_TABLE_NAME + "," + LABELS_TABLE_NAME + "," + TASK_TABLE_NAME);
                // qb.setProjectionMap( TODO: add mappings ); // NOTE: Remember that subselects included in the columns list must also be mapped.

				// tasks._id = #
				qb.appendWhere(TASK_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Tasks._ID);
				qb.appendWhere("=");
				qb.appendWhere(url.getPathSegments().get(1));

				qb.appendWhere(" AND ");

				// labeled_content._content_uri LIKE content://AUTHORITY/tasks
				qb.appendWhere(LABELED_CONTENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.LabeledContentColumns._CONTENT_URI);
				qb.appendWhere(" LIKE ");
    			qb.appendWhere(DatabaseUtils.sqlEscapeString(Task.Tasks.CONTENT_URI_STRING)); // QUEST: Should this have a '/' on the end?

				qb.appendWhere(" AND ");

				// Joins tasks table to labeled_content table.
				qb.appendWhere("'");
				qb.appendWhere(Task.Tasks.CONTENT_URI_STRING);
				qb.appendWhere("/' || ");
				qb.appendWhere(TASK_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Tasks._ID);
				qb.appendWhere("=");
				qb.appendWhere(LABELED_CONTENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.LabeledContentColumns._CONTENT_URI);

				qb.appendWhere(" AND ");

				// Joins labeled_content to labels table.
				// labeled_content._label_id = labels._id
				qb.appendWhere(LABELED_CONTENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.LabeledContentColumns._LABEL_ID);
				qb.appendWhere("=");
				qb.appendWhere(LABELS_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Labels._ID);

				break;

			case TASKS_LABELS_ID:

				qb.setTables(LABELED_CONTENT_TABLE_NAME + "," + LABELS_TABLE_NAME + "," + TASK_TABLE_NAME);

				pathSegments = url.getPathSegments();

				// tasks._id = #
				qb.appendWhere(TASK_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Tasks._ID);
				qb.appendWhere("=");
				qb.appendWhere(pathSegments.get(1));

				qb.appendWhere(" AND ");

				// labeled_content._content_uri LIKE content://AUTHORITY/tasks
				qb.appendWhere(LABELED_CONTENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.LabeledContentColumns._CONTENT_URI);
				qb.appendWhere(" LIKE ");
			qb.appendWhere(DatabaseUtils.sqlEscapeString(Task.Tasks.CONTENT_URI_STRING)); // QUEST: Should this have a '/' on the end?

				qb.appendWhere(" AND ");

				// Joins tasks table to labeled_content table.
				qb.appendWhere("'");
				qb.appendWhere(Task.Tasks.CONTENT_URI_STRING);
				qb.appendWhere("/' || ");
				qb.appendWhere(TASK_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Tasks._ID);
				qb.appendWhere("=");
				qb.appendWhere(LABELED_CONTENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.LabeledContentColumns._CONTENT_URI);

				qb.appendWhere(" AND ");

				// labels._id = #
				qb.appendWhere(LABELS_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Labels._ID);
				qb.appendWhere("=");
				qb.appendWhere(pathSegments.get(3));

				qb.appendWhere(" AND ");

				// Joins labeled_content to labels table.
				// labeled_content._label_id = labels._id
				qb.appendWhere(LABELED_CONTENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.LabeledContentColumns._LABEL_ID);
				qb.appendWhere("=");
				qb.appendWhere(LABELS_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.Labels._ID);

				break;

			case FILTERS:
				qb.setTables(FILTER_TABLE_NAME);
                // qb.setProjectionMap( TODO: add mappings ); // NOTE: Remember that subselects included in the columns list must also be mapped.
				break;

			case FILTERS_ID:
				qb.setTables(FILTER_TABLE_NAME);
				qb.appendWhere(Task.FilterColumns._ID + "=" + url.getLastPathSegment());
				break;

    		case TEMP_FILES: // // NOTE: Does not support selectin the temp file data.
				sort = Task.TempFile._ID;
				qb.setTables(TEMP_FILE_TABLE_NAME);
                // qb.setProjectionMap( TODO: add mappings ); // NOTE: Remember that subselects included in the columns list must also be mapped.
				break;

			case TEMP_FILE_ID:
				sort = Task.TempFile._ID;
				qb.setTables(TEMP_FILE_TABLE_NAME);
				qb.appendWhere(Task.TempFile._ID + "=" + url.getLastPathSegment());
				break;

			case FILTER_ELEMENTS:
				qb.setTables(FILTER_ELEMENT_TABLE_NAME);
                // qb.setProjectionMap( TODO: add mappings ); // NOTE: Remember that subselects included in the columns list must also be mapped.
				break;

			case FILTER_ELEMENTS_ID:
				qb.setTables(FILTER_ELEMENT_TABLE_NAME);
				qb.appendWhere(Task.FilterElementColumns._ID + "=" + url.getLastPathSegment());
				break;

			case FILTER_FILTER_ELEMENTS:
                // qb.setTables(FILTER_ELEMENT_TABLE_NAME+" JOIN "+FILTER_TABLE_NAME+" ON ("+FILTER_ELEMENT_TABLE_NAME+"."+Task.FilterElementColumns._FILTER_ID+"="+FILTER_TABLE_NAME+"."+Task.FilterColumns._ID+")" );
				qb.setTables(FILTER_ELEMENT_TABLE_NAME);
                // qb.appendWhere(FILTER_TABLE_NAME+"."+Task.FilterColumns._ID+"="+url.getPathSegments().get(1));
				qb.appendWhere(Task.FilterElement._FILTER_ID + "=" + url.getPathSegments().get(1));
                // qb.setProjectionMap( TODO: add mappings ); // NOTE: Remember that subselects included in the columns list must also be mapped.
				break;

			case ACTIVE_FILTER_FILTER_ELEMENTS:
				// INNER JOIN people on (contact_methods.person = people._id)
				qb.setTables(FILTER_ELEMENT_TABLE_NAME + " JOIN " + FILTER_TABLE_NAME + " ON (" + FILTER_ELEMENT_TABLE_NAME + "." + Task.FilterElementColumns._FILTER_ID + "=" + FILTER_TABLE_NAME + "." + Task.FilterColumns._ID + ")");
				qb.appendWhere(FILTER_TABLE_NAME + "." + Task.FilterColumns._SELECTED + "=" + Task.FilterColumns.SELECTED_TRUE);
                // qb.setProjectionMap( TODO: add mappings ); // NOTE: Remember that subselects included in the columns list must also be mapped.
				break;

			// case FILTER_FILTER_ELEMENTS_LABELS:
                //			qb.setTables(FILTER_TABLE_NAME+" JOIN "+ FILTER_ELEMENT_TABLE_NAME+" JOIN "+LABELS_TABLE_NAME+" ON ("+ FILTER_TABLE_NAME+"."+Task.FilterColumns._ID+"="+FILTER_ELEMENT_TABLE_NAME+"."+Task.FilterElementColumns._FILTER_ID+" AND "+FILTER_ELEMENT_TABLE_NAME+"."+Task.FilterElementColumns._LABEL_ID+"="+LABELS_TABLE_NAME+"."+Task.Labels._ID+")" );
                //        	qb.appendWhere(FILTER_TABLE_NAME+"."+Task.FilterColumns._ID+"="+url.getPathSegments().get(1));
                ////			qb.setProjectionMap( TODO: add mappings ); // NOTE: Remember that subselects included in the columns list must also be mapped.
                // break;

			case NOTIFICATIONS:
				qb.setTables(NOTIFICATION_TABLE_NAME);
                // qb.setProjectionMap( TODO: add mappings ); // NOTE: Remember that subselects included in the columns list must also be mapped.
				break;

			case NOTIFICATION_ID:
				qb.setTables(NOTIFICATION_TABLE_NAME);
				qb.appendWhere("_id=" + url.getLastPathSegment());
				break;

			case COMPLETABLE:
				qb.setTables(COMPLETABLE_TABLE_NAME);
                // qb.setProjectionMap(COMPLETABLE_PROJECTION_MAP); // NOTE: Remember that subselects included in the columns list must also be mapped.
				break;

			case COMPLETABLE_ID:
				qb.setTables(COMPLETABLE_TABLE_NAME);
				qb.appendWhere("_id=" + url.getLastPathSegment());
				break;

			case DB_INFO_VERSION:
				MatrixCursor matrixCursor = new MatrixCursor(new String[] { "version" });
				matrixCursor.addRow(new Integer[] { mDB.getVersion() });
				return matrixCursor;

			case DB_INFO_FILE_SIZE: // Returns the number of bytes in the file.
				File databaseFile = new File(mDB.getPath());
				matrixCursor = new MatrixCursor(new String[] { "file_size" });
				matrixCursor.addRow(new Long[] { databaseFile.length() });
				return matrixCursor;

			case DB_INFO_FILE_PATH:
				matrixCursor = new MatrixCursor(new String[] { "file_path" });
				matrixCursor.addRow(new String[] { mDB.getPath() });
				return matrixCursor;

			case DB_INFO_MAX_SIZE:
				matrixCursor = new MatrixCursor(new String[] { "max_size" });
				matrixCursor.addRow(new Long[] { mDB.getMaximumSize() });
				return matrixCursor;

			case DB_INFO_PAGE_SIZE:
				matrixCursor = new MatrixCursor(new String[] { "page_size" });
				matrixCursor.addRow(new Long[] { mDB.getPageSize() });
				return matrixCursor;

			case DB_BACKUP:
				try{
					matrixCursor = new MatrixCursor(projection);
					Object[] row = new Object[projection.length];
					for(int i=0; i < projection.length; i++){
						if( Task.BackupColumns._LAST_MODIFIED.equals(projection[i])){
							
							// Don't set lastModified if request includes a request for the full serialization 
							//   since the serialization will have a unique timestamp associated with it that we want to use (and will use).
							
							// Search for _SERIALIZATION in projection.
							boolean foundSerializationColumn = false;
							for(int j=0; j < projection.length; j++){
								if( Task.BackupColumns._SERIALIZATION.equals(projection[j])){
									foundSerializationColumn = true;
									break;
								}
							}
							// Don't add lastModified if _SERIALIZATION is present.
							if( !foundSerializationColumn ){
								row[i] = getDatabaseLastModified();
							}
						}else if (Task.BackupColumns._SERIALIZER_VERSION.equals(projection[i])){
							row[i] = BackupUtil.SERIALIZER_VERSION;
						}else if (Task.BackupColumns._INSTALL_FINGERPRINT.equals(projection[i])){
							row[i] = mBackupManagerHolder.getInstallFingerprint();
						}else if (Task.BackupColumns._VERSION.equals(projection[i])){
							row[i] = DATABASE_VERSION;
						}else if (Task.BackupColumns._SERIALIZATION.equals(projection[i])){
							long lastModified = 0;
							StringBuilder dbSerialization = new StringBuilder();
							
							lastModified = serializeDb(mDB, dbSerialization);

							row[i] = dbSerialization.toString();
							
							// Set _LAST_MODIFIED value specifically for this serializiation.
							for(int j=0; j < projection.length; j++){
								if( Task.BackupColumns._LAST_MODIFIED.equals(projection[j])){
									row[j] = lastModified;
								}
							}
						}
					}
					matrixCursor.addRow(row);
					return matrixCursor;
				} catch (HandledException he) {
				} catch (Exception e) {
					Log.e(TAG, "ERR000KB", e);
					ErrorUtil.handleException("ERR000KB", e, getContext());
				}
				return new MatrixCursor(new String[]{});
				
			case SEARCH_SUGGEST:
				// qb.setTables("tasks as t");
				qb.setTables(TASK_TABLE_NAME);

				assert url.getPathSegments().size() > 1;
                String queryText = url.getLastPathSegment().toLowerCase(); // TODO: Remove .toLowerCase().
				qb.appendWhere(TaskListCursorFactory.composeWhereClause(queryText));

                // qb.setProjectionMap(QUERY_PROJECTION_MAP); // NOTE: Remember that subselects included in the columns list must also be mapped.
        	
				selection = null;
				selectionArgs = new String[] {};
    			projection = new String[]{Task.Tasks._ID, Task.Tasks._ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, Task.Tasks.TASK_TITLE + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1}; // , Task.Tasks._ID + " AS " + SearchManager.SUGGEST_COLUMN_SHORTCUT_ID
                break;

        case SHORTCUT_REFRESH:
				qb.setTables(TASK_TABLE_NAME);

				String shortcutId = null;
				assert null != url;

				if (url.getPathSegments().size() > 1) {
					shortcutId = url.getLastPathSegment();
				}
				assert Long.parseLong(shortcutId) > 0; // Verify its a valid id.

				// qb.appendWhere(Task.Tasks._ID + "=?");
				selection = Task.Tasks._ID + "=?";
				selectionArgs = new String[] { shortcutId };

                // qb.setProjectionMap(QUERY_SUGGEST_SHORTCUT_PROJECTION_MAP); // NOTE: Remember that subselects included in the columns list must also be mapped.

				projection = new String[] { Task.Tasks._ID, Task.Tasks._ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, Task.Tasks.TASK_TITLE + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1, Task.Tasks._ID + " AS " + SearchManager.SUGGEST_COLUMN_SHORTCUT_ID };
				break;

			case ARCHIVED_TASKS_ID:
				qb.setTables(LABELED_CONTENT_TABLE_NAME + "," + LABELS_TABLE_NAME + "," + TASK_TABLE_NAME);

				// Selects the labeled_content records for archived tasks.
				// labeled_content._label_id = ID_LABEL_ARCHIVED
				qb.appendWhere(LABELED_CONTENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.LabeledContentColumns._LABEL_ID);
				qb.appendWhere("=");
				qb.appendWhere(ID_LABEL_ARCHIVED);

				qb.appendWhere(" AND ");

				// Selects the labeled_content records for the specified task.
				//labeled_content._content_uri='content://"+AUTHORITY+"/tasks/69'
				qb.appendWhere(LABELED_CONTENT_TABLE_NAME);
				qb.appendWhere(".");
				qb.appendWhere(Task.LabeledContentColumns._CONTENT_URI);
				qb.appendWhere("=");
				qb.appendWhere("'");
				qb.appendWhere(Task.Tasks.CONTENT_URI_STRING);
				qb.appendWhere("/");
				qb.appendWhere(url.getLastPathSegment());
				qb.appendWhere("'");
				break;

			case APP_WIDGETS:
				qb.setTables(APP_WIDGETS_TABLE_NAME);
				break;

			case APP_WIDGETS_DELETED:
				qb.setTables(APP_WIDGETS_DELETED_TABLE_NAME);
				break;

			// case APP_WIDGET_ID:
                // qb.setTables(APP_WIDGETS_TABLE_NAME);
                // qb.appendWhere("_id=" + url.getLastPathSegment());
                // break;

			default:
				throw new IllegalArgumentException("Unknown URL " + url);
		}

		if (!TextUtils.isEmpty(sort)) {
			orderBy = sort;
		}

		// Get the database and run the query
		Cursor c = qb.query(mDB, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
		c.setNotificationUri(getContext().getContentResolver(), url);
		return c;
	}

    public String getType(Uri url) {
    	try{
            switch (URL_MATCHER.match(url)) {
                case TASKS:
                    return Task.Tasks.CONTENT_TYPE;

                case TASK_ID:
                    return Task.Tasks.CONTENT_ITEM_TYPE;        

                case ATTACHMENTS:
                    return Task.TaskAttachments.CONTENT_TYPE;

                case ATTACHMENT_ID:
                    return Task.TaskAttachments.CONTENT_ITEM_TYPE;        

                case PROXIMITY_ALERTS:
                    return Task.ProximityAlerts.CONTENT_TYPE;

                case PROXIMITY_ALERT_ID:
                    return Task.ProximityAlerts.CONTENT_ITEM_TYPE;

                case TASK_ATTACHMENT_PROXIMITY_ALERT_ID:
                    return Task.ProximityAlerts.CONTENT_ITEM_TYPE;
                    
                case TASK_ATTACHMENT_ID:
                    return Task.TaskAttachments.CONTENT_ITEM_TYPE;        

                case PROXIMITY_ALERT_TASK_ATTACHMENT_ID:
                    return Task.TaskAttachments.CONTENT_ITEM_TYPE;        
                                    
    	        case LABELS:
    	            return Task.Labels.CONTENT_TYPE;
    	
    	        case LABEL_ID:
    	            return Task.Labels.CONTENT_ITEM_TYPE;
    	            
    	        case LABELED_CONTENT:
    	            return Task.LabeledContent.CONTENT_TYPE;
    	
    	        case LABELED_CONTENT_ID:
    	        	// TODO: ! Use the _content_uri to choose a suitable mime type (of our own making, not the actual mime type for the _content_uri) to return here.
    	        	// getLabeledContentType(url);
    	            return Task.LabeledContent.CONTENT_ITEM_TYPE;
    	            
    	        case LABELS_LABELED_CONTENT:
    	            return Task.LabeledContent.CONTENT_TYPE;
    	
    	        case LABELS_LABELED_CONTENT_ID:
    	        	// TODO: ! Use the _content_uri to choose a suitable mime type (of our own making, not the actual mime type for the _content_uri) to return here.
    	        	// getLabeledContentType(url);
    	            return Task.LabeledContent.CONTENT_ITEM_TYPE;
    	            
    	        case LABELS_TASKS:
    	            return Task.Tasks.CONTENT_TYPE;
    	
    	        case LABELS_TASKS_ID:
    	            return Task.Tasks.CONTENT_ITEM_TYPE;        
    	            
    	        case TASKS_LABELS:
    	            return Task.Labels.CONTENT_TYPE;
    	
    	        case TASKS_LABELS_ID:
    	            return Task.Labels.CONTENT_ITEM_TYPE;        
    	            
    	        case FILTERS:
    	            return Task.Filter.CONTENT_TYPE;        
    	            
    	        case FILTERS_ID:
    	            return Task.Filter.CONTENT_ITEM_TYPE;        
    	            
    	        case FILTER_ELEMENTS:
    	            return Task.FilterElement.CONTENT_TYPE;        
    	            
    	        case FILTER_ELEMENTS_ID:
    	            return Task.FilterElement.CONTENT_ITEM_TYPE;       
    	            
    	        case NOTIFICATION_ID:
    	        	return Task.Notification.CONTENT_ITEM_TYPE;       
    	        	
    	        case NOTIFICATIONS:
    	            return Task.Notification.CONTENT_TYPE;       
    	            
    	        case COMPLETABLE_ID:
    	        	return Task.Completable.CONTENT_ITEM_TYPE;       
    	        	
    	        case COMPLETABLE:
    	            return Task.Completable.CONTENT_TYPE;       

    	        default:
    	        	ErrorUtil.handle("ERR00096", "Unknown URL type" + url, this);
            }
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR00097", exp);
    		ErrorUtil.handleException("ERR00097", exp, getContext());
    	}
        throw new IllegalArgumentException("Unknown URL type" + url);
    }

	public Uri insert(Uri url, ContentValues initialValues) {
		SQLiteDatabase mDB = dbHelper.getWritableDatabase();
		long rowID;

		int match = URL_MATCHER.match(url);

		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		Uri insertedUri = null;
		switch (match) {
			case TASKS:
				insertedUri = insertTask(mDB, values);
				break;

			case ATTACHMENTS:
				insertedUri = insertAttachment(mDB, values);
				break;

			case PROXIMITY_ALERTS:
				insertedUri = insertProximityAlert(mDB, values);
				break;

			case LABELS:
				insertedUri = insertLabel(mDB, values);
				break;

			case USER_APPLIED_LABEL:
				insertedUri = insertUserAppliedLabel(mDB, values, url.getQueryParameter("filter_id"));

				break;

			case TEMP_FILES:
				insertedUri = insertTempFile(mDB, values);
				break;

			case LABELS_TASKS: // TODO: Validate that the _CONTENT_URI is a Task uri.
			case LABELS_LABELED_CONTENT:
				insertedUri = insertLabledContent(mDB, values, url, insertedUri);

				break;
			case TASKS_LABELS:
				insertedUri = insertTaskLabel(mDB, values, url, insertedUri);
				break;

			case LABELED_CONTENT:
				// Make sure that the fields are all set
				if (values.containsKey(Task.LabeledContent._CONTENT_URI) == false) {
					throw new IllegalArgumentException("You must specify Task.LabeledContent._CONTENT_URI");
				}
				if (values.containsKey(Task.LabeledContent._LABEL_ID) == false) {
					throw new IllegalArgumentException("You must specify Task.LabeledContent._LABEL_ID");
				}

				if (values.containsKey(Task.LabeledContent.CREATED_DATE) == false) {
					values.put(Task.LabeledContent.CREATED_DATE, System.currentTimeMillis());
				}

				rowID = mDB.insert(LABELED_CONTENT_TABLE_NAME, Task.LabeledContent._CONTENT_URI, values);
				if (rowID > 0) {
					insertedUri = ContentUris.withAppendedId(Task.Labels.CONTENT_URI, rowID);
	                getContext().getContentResolver().notifyChange(insertedUri, null); // TODO: Is this really needed? Contacts provider doesn't have it.
	    			updateDatabaseLastModified();
	            }
				break;
        	
			case FILTERS:
				// Make sure that the fields are all set
				if (values.containsKey(Task.FilterColumns.DISPLAY_NAME) == false) {
					if (values.containsKey(Task.FilterColumns._DISPLAY_NAME_ARRAY_INDEX) == false) {
						throw new IllegalArgumentException("You must specify _display_name or _display_name_array_index");
					}
				}
				if (values.containsKey(Task.FilterColumns._PERMANENT) == false) {
					values.putNull(Task.FilterColumns._PERMANENT);
				}
				if (values.containsKey(Task.FilterColumns._ACTIVE) == false) {
            	    values.put(Task.FilterColumns._ACTIVE, Task.FilterColumns.ACTIVE_TRUE); // User just added the filter so now they will want to configure it and pre-view the resutls.
				}

				rowID = mDB.insert(FILTER_TABLE_NAME, Task.FilterColumns.DISPLAY_NAME, values);
				if (rowID > 0) {
					insertedUri = ContentUris.withAppendedId(Task.Filter.CONTENT_URI, rowID);

					// TODO: ! De-activate any _other_ records.

					getContext().getContentResolver().notifyChange(insertedUri, null); // TODO: Is this really needed? Contacts provider doesn't have it.
					
					updateDatabaseLastModified();
				}
				break;

			case FILTER_ELEMENTS:
				insertedUri = insertFilterElement(mDB, values);
				break;

			case NOTIFICATIONS:
				insertedUri = insertNotification(mDB, values);
				break;

			case COMPLETABLE:
				insertedUri = insertCompletable(mDB, values);
				break;

			case APP_WIDGETS:
				insertedUri = insertAppWidget(mDB, values);
				break;

			case APP_WIDGETS_DELETED:
				insertedUri = insertAppWidgetDeleted(mDB, values);
				break;

	        case DB_BACKUP:
	        	insertedUri = restoreBackup(mDB, url, values); 
	        	break;
				
			default:
				throw new IllegalArgumentException("Unknown URL " + url);
		}
		if (null == insertedUri) {
			throw new SQLException("Failed to insert row into " + url);
		} else {
			return insertedUri;
		}
	}

	private Uri insertFilterElement(SQLiteDatabase mDB, ContentValues values) {
		long rowID;

		// Make sure that the fields are all set
		if (values.containsKey(Task.FilterElementColumns._FILTER_ID) == false) {
			throw new IllegalArgumentException("You must specify Task.FilterElementColumns._FILTER_ID");
		}

		if (values.containsKey(Task.FilterElementColumns._ACTIVE) == false) {
			values.put(Task.FilterElementColumns._ACTIVE, Task.FilterElementColumns.ACTIVE_TRUE);
		}

		rowID = mDB.insert(FILTER_ELEMENT_TABLE_NAME, Task.FilterElementColumns._PARAMETERS, values);
		if (rowID > 0) {
			Uri insertedUri = ContentUris.withAppendedId(Task.FilterElement.CONTENT_URI, rowID);
		    getContext().getContentResolver().notifyChange(insertedUri, null); // TODO: Is this really needed? Contacts provider doesn't have it.
			return insertedUri;
		}
		return null;
	}

	/**
	 */
	private Uri insertTaskLabel(SQLiteDatabase mDB, ContentValues values, Uri url, Uri insertedUri) {
		long rowID;
		values.put(Task.LabeledContent._CONTENT_URI, Uri.withAppendedPath(Task.Tasks.CONTENT_URI, url.getPathSegments().get(1)).toString());

		// Make sure that the fields are all set
		if (values.containsKey(Task.LabeledContent._LABEL_ID) == false) {
			throw new IllegalArgumentException("You must specify Task.LabeledContent._LABEL_ID");
		}

		if (values.containsKey(Task.LabeledContent.CREATED_DATE) == false) {
			values.put(Task.LabeledContent.CREATED_DATE, System.currentTimeMillis());
		}

		rowID = mDB.insert(LABELED_CONTENT_TABLE_NAME, Task.LabeledContent._CONTENT_URI, values);
		if (rowID > 0) {
			insertedUri = ContentUris.withAppendedId(Task.Labels.CONTENT_URI, rowID);
		    getContext().getContentResolver().notifyChange(insertedUri, null); // TODO: Is this really needed? Contacts provider doesn't have it.
			
			updateDatabaseLastModified();
			
		}
		return insertedUri;
	}

	/**
	 * TODO: Why is "Uri insertedUri" part of the signature?
	 */
	private Uri insertLabledContent(SQLiteDatabase mDB, ContentValues values, Uri url, Uri insertedUri) {
		long rowID;
		values.put(Task.LabeledContent._LABEL_ID, url.getPathSegments().get(1));

		// Make sure that the fields are all set
		if (values.containsKey(Task.LabeledContent._CONTENT_URI) == false) {
			throw new IllegalArgumentException("You must specify Task.LabeledContent._CONTENT_URI");
		}

		if (values.containsKey(Task.LabeledContent.CREATED_DATE) == false) {
			values.put(Task.LabeledContent.CREATED_DATE, System.currentTimeMillis());
		}

		rowID = mDB.insert(LABELED_CONTENT_TABLE_NAME, Task.LabeledContent._CONTENT_URI, values);
		if (rowID > 0) {
			insertedUri = ContentUris.withAppendedId(Task.Labels.CONTENT_URI, rowID);
		    getContext().getContentResolver().notifyChange(insertedUri, null); // TODO: Is this really needed? Contacts provider doesn't have it.
			
			updateDatabaseLastModified();
			
		}
		return insertedUri;
	}

	private Uri insertTask(SQLiteDatabase mDB, ContentValues values) {
		long rowID;
		Long now = Long.valueOf(System.currentTimeMillis());

		// Make sure that the fields are all set
		if (values.containsKey(Task.Tasks.TASK_TITLE) == false) {
			values.put(Task.Tasks.TASK_TITLE, "");
		}

		if (values.containsKey(Task.Tasks.TASK_DESC) == false) {
			values.put(Task.Tasks.TASK_DESC, "");
		}

		if (values.containsKey(Task.Tasks.CREATED_DATE) == false) {
			values.put(Task.Tasks.CREATED_DATE, now);
		}

		if (values.containsKey(Task.Tasks.MODIFIED_DATE) == false) {
			values.put(Task.Tasks.MODIFIED_DATE, now);
		}

		if (values.containsKey(Task.Tasks.PRIORITY) == false) {
			values.put(Task.Tasks.PRIORITY, 0); // TODO: Pull from preferences
		}

		if (values.containsKey(Task.Tasks.DUE_DATE) == false) {
			values.put(Task.Tasks.DUE_DATE, Task.Tasks.DUE_DATE_NOT_SET_LONG);
		}

		// Check if the new task requires that an alarm be set.
		if (values.containsKey(Task.Tasks.ALARM_TIME) && null != values.getAsLong(Task.Tasks.ALARM_TIME)) {
			values.put(Task.Tasks.ALARM_ACTIVE, Task.Tasks.ALARM_ACTIVE_TRUE);
		} else {
			values.put(Task.Tasks.ALARM_ACTIVE, Task.Tasks.ALARM_ACTIVE_FALSE);
		}

		if (values.containsKey(Task.Tasks.COMPLETE) == false) {
            values.put(Task.Tasks.COMPLETE, Task.Tasks.COMPLETE_FALSE); // 0% complete 
		}

        rowID = mDB.insert(TASK_TABLE_NAME, Task.Tasks.TASK_DESC, values); // TODO: Extract constants.
		if (rowID > 0) {
			Uri uri = ContentUris.withAppendedId(Task.Tasks.CONTENT_URI, rowID);
			getContext().getContentResolver().notifyChange(uri, null);

			// Add alarm to Alarm Manager if needed.
			if (values.containsKey(Task.Tasks.ALARM_ACTIVE) && Task.Tasks.ALARM_ACTIVE_TRUE.equals(values.getAsString(Task.Tasks.ALARM_ACTIVE))) {
				registerAlarm(getContext(), values.getAsLong(Task.Tasks.ALARM_TIME), ContentUris.withAppendedId(Task.Tasks.CONTENT_URI, rowID));
			}
			
			updateDatabaseLastModified();
			
			return uri;
		}

		return null;
	}

	private Uri insertAppWidget(SQLiteDatabase mDB, ContentValues values) {
		long rowID;

		// Make sure that the fields are all set
		if (values.containsKey(Task.AppWidgets._APP_WIDGET_ID) == false) {
			throw new IllegalArgumentException("You must specify Task.AppWidgets._APP_WIDGET_ID");
		}

		rowID = mDB.insert(APP_WIDGETS_TABLE_NAME, Task.AppWidgets._APP_WIDGET_ID, values);
		if (rowID > 0) {
			Uri uri = ContentUris.withAppendedId(Task.AppWidgets.CONTENT_URI, rowID);
			return uri;
		}
		return null;
	}

	private Uri insertAppWidgetDeleted(SQLiteDatabase mDB, ContentValues values) {
		long rowID;

		// Make sure that the fields are all set
		if (values.containsKey(Task.AppWidgetsDeleted._APP_WIDGET_ID) == false) {
			throw new IllegalArgumentException("You must specify Task.AppWidgetsDeleted._APP_WIDGET_ID");
		}

		rowID = mDB.insert(APP_WIDGETS_DELETED_TABLE_NAME, Task.AppWidgetsDeleted._APP_WIDGET_ID, values);
		if (rowID > 0) {
			Uri uri = ContentUris.withAppendedId(Task.AppWidgetsDeleted.CONTENT_URI, rowID);
			return uri;
		}
		return null;
	}

	private Uri insertNotification(SQLiteDatabase mDB, ContentValues values) {
		long rowID;

		rowID = mDB.insert(NOTIFICATION_TABLE_NAME, Task.Notification._URI, values);
		if (rowID > 0) {
			// Uri uri = Task.Tasks.CONTENT_URI.addId(rowID);
			Uri uri = ContentUris.withAppendedId(Task.Notification.CONTENT_URI, rowID);
			getContext().getContentResolver().notifyChange(uri, null);
			return uri;
		}
		return null;
	}

	private Uri insertCompletable(SQLiteDatabase mDB, ContentValues values) {
		long rowID;

		rowID = mDB.insert(COMPLETABLE_TABLE_NAME, Task.Completable._COMPLETED, values);
		if (rowID > 0) {
			Uri uri = ContentUris.withAppendedId(Task.Completable.CONTENT_URI, rowID);
			getContext().getContentResolver().notifyChange(uri, null);
			
			updateDatabaseLastModified();
			
			return uri;
		}
		return null;
	}

	private Uri insertAttachment(SQLiteDatabase mDB, ContentValues values) {
		long rowID;
		Long now = Long.valueOf(System.currentTimeMillis());

		// Verify fields and set any defaults.
		if (values.containsKey(Task.TaskAttachments.TASK_ID) == false) {
			throw new IllegalArgumentException("You must specify Task.TaskAttachments.TASK_ID");
		}

		rowID = mDB.insert(ATTACHMENT_TABLE_NAME, null, values);
		if (rowID > 0) {
			// ContentURI uri = Task.TaskAttachments.CONTENT_URI.addId(rowID);
			Uri uri = ContentUris.withAppendedId(Task.TaskAttachments.CONTENT_URI, rowID);
			getContext().getContentResolver().notifyChange(uri, null);
			
			updateDatabaseLastModified();
			
			return uri;
		}
		return null;
	}

	private Uri insertProximityAlert(SQLiteDatabase mDB, ContentValues values) {
		long rowID;
		Long now = Long.valueOf(System.currentTimeMillis());

		if (values.containsKey(Task.ProximityAlerts._GEO_URI) == false) {
			throw new IllegalArgumentException("You must specify Task.ProximityAlerts._GEO_URI");
		}

		if (values.containsKey(Task.ProximityAlerts.RADIUS) == false) {
			// TODO: !!! Why is this allowed to default?
            values.put(Task.ProximityAlerts.RADIUS, 3); // TODO: Make this a configurable option.
		}

		if (values.containsKey(Task.ProximityAlerts.RADIUS_UNIT) == false) {
            values.put(Task.ProximityAlerts.RADIUS_UNIT, 1); // TODO: Make this a configurable option.
		}

		if (values.containsKey(Task.ProximityAlerts.ENABLED) == false) {
			values.put(Task.ProximityAlerts.ENABLED, Task.ProximityAlerts.ENABLED_TRUE);
		}

        rowID = mDB.insert(PROXIMITY_ALERT_TABLE_NAME, null, values); // TODO: Should an entirely empty insert be allowed?,, even with default values?
		if (rowID > 0) {
			Uri uri = ContentUris.withAppendedId(Task.ProximityAlerts.CONTENT_URI, rowID);

			// ********************************************************
			// Setup proximity alert
			// ********************************************************
			if (Task.ProximityAlerts.ENABLED_TRUE.equals(values.getAsString(Task.ProximityAlerts.ENABLED))) {

				GeoPoint geoPoint = Util.createPoint(values.getAsString(Task.ProximityAlerts._GEO_URI));
				assert null != geoPoint;

				Nearminder.addOrUpdateProximityAlert(getContext(), geoPoint, values.getAsInteger(Task.ProximityAlerts.RADIUS), uri);
			}

			// ********************************************************
			// Notify others of this change
			// ********************************************************
			getContext().getContentResolver().notifyChange(uri, null);

			updateDatabaseLastModified();

			return uri;
		}

		return null;
	}

	private Uri insertUserAppliedLabel(SQLiteDatabase mDB, ContentValues values2, String filterId) {
		Uri labelUri = null;
		Uri filterElementUri = null;

		ContentValues values = new ContentValues();
		values.put(Task.LabelsColumns._USER_APPLIED, Task.LabelsColumns.USER_APPLIED_TRUE);
		values.put(Task.LabelsColumns.DISPLAY_NAME, values2.getAsString(Task.LabelsColumns.DISPLAY_NAME));
		values.put(Task.LabelsColumns.DESCRIPTION, values2.getAsString(Task.LabelsColumns.DESCRIPTION));

		mDB.beginTransaction();
		try {
			labelUri = insertLabel(mDB, values);
			if (null == labelUri) {
				return null;
			}

			if (null != filterId) {

				values.clear();
				values.put(Task.FilterElementColumns._ACTIVE, Task.FilterElementColumns.ACTIVE_TRUE);
				values.put(Task.FilterElementColumns._APPLY_WHEN_ACTIVE, Task.FilterElementColumns.APPLY_WHEN_ACTIVE);
				values.put(Task.FilterElementColumns._CONSTRAINT_URI, Constraint.Version1.LABEL_CONTENT_URI_STRING + "/" + labelUri.getLastPathSegment());
				values.put(Task.FilterElementColumns._FILTER_ID, filterId);
				values.put(Task.FilterElementColumns._PHASE, Task.FilterElementColumns.PHASE_EXPLODE);

				filterElementUri = insertFilterElement(mDB, values);
				if (null == filterElementUri) {
					return null;
				}

			}
			mDB.setTransactionSuccessful();
			// These were just created, so no change occurred,, right? Yes.
			// getContext().getContentResolver().notifyChange(labelUri, null);
			// getContext().getContentResolver().notifyChange(filterElementUri, null); 

			// But .... we still need to notify the filter element list that a new label exists.
			//     We use the filter content uri here because filter elements are children of a filter.
			getContext().getContentResolver().notifyChange(Task.Filter.CONTENT_URI, null);
			
			updateDatabaseLastModified();
			
		} finally {
			mDB.endTransaction();
		}

		return labelUri;
	}

	// TODO: Wrap this in a transaction so that the DB record is removed if the file writing fails.
	private Uri insertTempFile(SQLiteDatabase mDB, ContentValues values) {
		long rowID;
		Long now = Long.valueOf(System.currentTimeMillis());

		// Make sure that the fields are all set
		if (values.containsKey(Task.TempFile.DATE_ADDED) == false) {
			values.put(Task.TempFile.DATE_ADDED, now);
		}

		if (values.containsKey(Task.TempFile.DATA) == false) {
			throw new IllegalArgumentException("You must specify Task.TempFile._DATA");
		}

		if (values.containsKey(Task.TempFile._PRESERVE_UNTIL) == false) {
			throw new IllegalArgumentException("You must specify Task.TempFile._PRESERVE_UNTIL");
		}

		if (values.getAsLong(Task.TempFile._PRESERVE_UNTIL) == null) {
			throw new IllegalArgumentException("TempFile._PRESERVE_UNTIL must be a long value.");
		}

		if (values.containsKey(Task.TempFile.DISPLAY_NAME) == false) {
			values.put(Task.TempFile.DISPLAY_NAME, String.valueOf(now));
		}

		if (values.containsKey(Task.TempFile.SIZE) == false) {
            values.put(Task.TempFile.SIZE, -1); // TODO: Double check this default value.
		}

        rowID = mDB.insert(TEMP_FILE_TABLE_NAME, null, values);
		if (rowID > 0) {

			// Attempt to create the file.

			File tempFolder = getTempDir(getContext());
    		File tempFile = new File(tempFolder, String.valueOf(rowID)); // File name is the row ID.
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(tempFile);
				try {
					byte[] data = values.getAsByteArray(Task.TempFile.DATA);
					assert null != data;
					fos.write(data);
				} finally {
					if (null != fos) {
						fos.close();
					}
				}
			} catch (IOException e) { // TODO: Consider whether this error will propogate to the caller correctly? 
				Log.w(TAG, "ERR0009B", e);
				ErrorUtil.handleException("ERR0009B", e, getContext());
		        int count = mDB.delete(TEMP_FILE_TABLE_NAME, Task.TempFile._ID+"=?", new String[]{String.valueOf(rowID)}); // TODO: Should an entirely empty insert be allowed?,, even with default values?
		        assert 0 != count;
				return null;
			}

			// Construct URI to return.
			Uri uri = ContentUris.withAppendedId(Task.TempFile.CONTENT_URI, rowID);
			// Notify listeners of the change.
			getContext().getContentResolver().notifyChange(uri, null);
			return uri;
		}

		return null;
	}

	/**
	 * Delete a user applied label.
	 * 
	 * No label will be deleted if there is no matching filter element.
	 * 
	 * @param labelId Contains a single value for "Task.LabelsColumns._ID"
	 * @return The number of label records deleted.
	 */
	private int deleteUserAppliedLabel(SQLiteDatabase mDB, String labelId) {

        int count = mDB.delete(FILTER_ELEMENT_TABLE_NAME, Task.FilterElementColumns._CONSTRAINT_URI + "='" + Constraint.Version1.LABEL_CONTENT_URI_STRING + "/' || ?", new String[] { labelId });

		getContext().getContentResolver().notifyChange(Task.FilterElement.CONTENT_URI, null); // Too much work to notify for each deleted record.

		count = mDB.delete(LABELS_TABLE_NAME, Task.LabelsColumns._ID + "=?", new String[] { labelId });
		if (count < 1) {
			Log.w(TAG, "No label id=" + labelId + " found.");
			return 0;
		}

		return count;
	}

	/**
	 * Delete a temp file.
	 * 
	 */
	private boolean deleteTempFile(long tempFileId) {
		// Attempt to delete the file.
		File tempFolder = getTempDir(getContext());
		File tempFile = new File(tempFolder, String.valueOf(tempFileId)); // File name is the row ID.

		return tempFile.delete();
	}

	/**
	 * 
	 * @param values
	 * @return
	 */
	private Uri insertLabel(SQLiteDatabase mDB, ContentValues values) {

		// Make sure that the fields are all set
		if (values.containsKey(Task.Labels.DISPLAY_NAME) == false) {
			throw new IllegalArgumentException("You must specify Task.LabelsColumns._USER_APPLIED");
		}

		if (values.containsKey(Task.LabelsColumns._USER_APPLIED) == false) {
			throw new IllegalArgumentException("You must specify Task.LabelsColumns._USER_APPLIED");
		} else {
			int userApplied = values.getAsInteger(Task.LabelsColumns._USER_APPLIED);
			if (userApplied != 0 && userApplied != 1) {
				throw new IllegalArgumentException("Task.LabelsColumns._USER_APPLIED value is invalid.");
			}
		}

		long rowID = mDB.insert(LABELS_TABLE_NAME, Task.LabelsColumns.DESCRIPTION, values);
		if (rowID > 0) {
			Uri uri = ContentUris.withAppendedId(Task.Labels.CONTENT_URI, rowID);
			getContext().getContentResolver().notifyChange(uri, null);
			
			updateDatabaseLastModified();
			
			return uri;
		}
		return null;
	}

	@Override
	public int delete(Uri url, String where, String[] whereArgs) {
		SQLiteDatabase mDB = dbHelper.getWritableDatabase();

		int count;
		long rowId = 0;
		String segment, segment2;
		switch (URL_MATCHER.match(url)) {
			case TASKS:
				// ******************
				// Send delete pending intents
				// ******************
				sendDeleteIntentsForTasks(mDB, where, whereArgs);

				// ******************
				// Remove alarm from Alarm Manager.
				// ******************
        	    String alarmTasksQuery = Task.Tasks.ALARM_ACTIVE+"="+Task.Tasks.ALARM_ACTIVE_TRUE
        			    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
				Cursor alarmCursor = mDB.query(TASK_TABLE_NAME, new String[] { Task.Tasks._ID }, alarmTasksQuery, whereArgs, null, null, null);
				assert null != alarmCursor;
				while (alarmCursor.moveToNext()) {
					unregisterAlarm(getContext(), ContentUris.withAppendedId(Task.Tasks.CONTENT_URI, alarmCursor.getLong(0)));
				}
				alarmCursor.close();

				// ***************
				// Do the delete
				// ***************
				count = mDB.delete(TASK_TABLE_NAME, where, whereArgs);
	        	if( 0 < count ){
	    			updateDatabaseLastModified();
	        	}
				break;

			case TASK_ID:
				segment = url.getLastPathSegment();
				rowId = Long.parseLong(segment);
	        	String taskWhere = Task.Tasks._ID + "=" + segment + (!TextUtils.isEmpty(where) ? " AND (" + where
        			    + ')' : "");

				// ******************
				// Send delete pending intents
        		// TODO: !!! This doesn't handle the case where an "attachment is deleted" using the context menu on attachment but there is no attachment handler to send this delete intent.
				// --> I added a TODO below at the ATTACHMENT_ID delete spot.
				// ******************
				sendDeleteIntentsForTasks(mDB, taskWhere, whereArgs);

				// ******************
				// Remove alarm from Alarm Manager.
				// ******************
				unregisterAlarm(getContext(), url);

				// ***************
				// Do the delete
				// ***************
				count = mDB.delete(TASK_TABLE_NAME, taskWhere, whereArgs);
	        	if( 0 < count ){
	    			updateDatabaseLastModified();
	        	}
				break;

			case ATTACHMENTS:
				sendDeleteIntentsForAttachment(mDB, where, whereArgs);
				count = mDB.delete("attachments", where, whereArgs);
	        	if( 0 < count ){
	    			updateDatabaseLastModified();
	        	}
				break;

			case ATTACHMENT_ID:
				segment = url.getLastPathSegment();
				rowId = Long.parseLong(segment);
	            String whereClause = "_id="
	                + segment
	                + (!TextUtils.isEmpty(where) ? " AND (" + where
	                        + ')' : "");

				sendDeleteIntentsForAttachment(mDB, whereClause, whereArgs);

				count = mDB.delete("attachments", whereClause, whereArgs);
	        	if( 0 < count ){
	    			updateDatabaseLastModified();
	        	}
	        	// TODO: !!! sendDeleteIntent(..) here.
				break;

			case PROXIMITY_ALERTS:
				// ********************************************************
				// Remove proximity alert from LocationManager
				// TODO: Consider whether to use the following algorithm:
				// 1. Record IDs of records that will be deleted.
				// 2. Delete the records.
        	    // 3. Use recorded IDs to remove the proximity alert from the LocationManager.
				// ********************************************************
				Cursor proximityAlertCursor = mDB.query(PROXIMITY_ALERT_TABLE_NAME, new String[] { Task.ProximityAlerts._ID }, where, whereArgs, null, null, Task.ProximityAlerts._ID);
				assert null != proximityAlertCursor;
				while (proximityAlertCursor.moveToNext()) {
					Uri proximityAlertUri = ContentUris.withAppendedId(Task.ProximityAlerts.CONTENT_URI, proximityAlertCursor.getLong(0));
					Nearminder.removeProximityAlert(getContext(), proximityAlertUri);
				}
				proximityAlertCursor.close();

				// Do the delete.
				count = mDB.delete(PROXIMITY_ALERT_TABLE_NAME, where, whereArgs);
	        	if( 0 < count ){
	    			updateDatabaseLastModified();
	        	}

				break;

			case PROXIMITY_ALERT_ID:
				segment = url.getLastPathSegment();
				rowId = Long.parseLong(segment);
				count = mDB.delete(PROXIMITY_ALERT_TABLE_NAME, "_id="
                            + segment
                            + (!TextUtils.isEmpty(where) ? " AND (" + where
                                    + ')' : ""), whereArgs);

				// ********************************************************
				// Remove proximity alert from LocationMananger.
				// ********************************************************
				Uri proximityAlertUri = ContentUris.withAppendedId(Task.ProximityAlerts.CONTENT_URI, Long.parseLong(segment));
				Nearminder.removeProximityAlert(getContext(), proximityAlertUri);
				if( 0 < count ){
					updateDatabaseLastModified();
				}
				break;

			case LABELS:
				count = mDB.delete(LABELS_TABLE_NAME, where, whereArgs);
	        	if( 0 < count ){
	    			updateDatabaseLastModified();
	        	}
				break;

			case LABEL_ID:
				segment = url.getLastPathSegment();
				rowId = Long.parseLong(segment);
				count = mDB.delete(LABELS_TABLE_NAME, Task.Labels._ID + "="
                            + segment
                            + (!TextUtils.isEmpty(where) ? " AND (" + where
                                    + ')' : ""), whereArgs);
	        	if( 0 < count ){
	    			updateDatabaseLastModified();
	        	}
				break;

			case LABELED_CONTENT:
				count = mDB.delete(LABELED_CONTENT_TABLE_NAME, where, whereArgs);
	        	if( 0 < count ){
	    			updateDatabaseLastModified();
	        	}
				break;

			case LABELED_CONTENT_ID:
				segment = url.getLastPathSegment();
				rowId = Long.parseLong(segment);
				count = mDB.delete(LABELED_CONTENT_TABLE_NAME, Task.LabeledContentColumns._ID + "="
                            + segment
                            + (!TextUtils.isEmpty(where) ? " AND (" + where
                                    + ')' : ""), whereArgs);            
	        	if( 0 < count ){
	    			updateDatabaseLastModified();
	        	}
				break;

            case LABELS_LABELED_CONTENT:
				// Remove all the labled_content records for this label.
				segment = url.getPathSegments().get(1);
				rowId = Long.parseLong(segment);
				count = mDB.delete(LABELED_CONTENT_TABLE_NAME, Task.Labels._ID + "="
                            + segment
                            + (!TextUtils.isEmpty(where) ? " AND (" + where
                                    + ')' : ""), whereArgs);        
	        	if( 0 < count ){
	    			updateDatabaseLastModified();
	        	}
				break;
			case LABELS_LABELED_CONTENT_ID:
				// Remove this labled_content record.
				segment = url.getPathSegments().get(3);
				rowId = Long.parseLong(segment);
				count = mDB.delete(LABELED_CONTENT_TABLE_NAME, Task.LabeledContentColumns._ID + "="
                            + segment
                            + (!TextUtils.isEmpty(where) ? " AND (" + where
                                    + ')' : ""), whereArgs);            
	        	if( 0 < count ){
	    			updateDatabaseLastModified();
	        	}
				break;

            case FILTERS:
				count = mDB.delete(FILTER_TABLE_NAME, where, whereArgs);
	        	if( 0 < count ){
	    			updateDatabaseLastModified();
	        	}
				break;

			case FILTERS_ID:
				segment = url.getLastPathSegment();
				rowId = Long.parseLong(segment);
				if( ID_FILTER_ARCHIVE.equals(segment) || ID_FILTER_ALL.equals(segment)  ){ // Hey! Why are you trying to change the _permanent field value? 
	            	Log.e(TAG, "ERR000HP");
	            	ErrorUtil.handleException("ERR000HP", (Exception)(new Exception( )).fillInStackTrace(), getContext());
						throw new IllegalArgumentException("Can't delete permanent filters.");
				}
	            count = mDB.delete(FILTER_TABLE_NAME, Task.FilterElementColumns._ID + "="
                            + segment
                            + (!TextUtils.isEmpty(where) ? " AND (" + where
                                    + ')' : ""), whereArgs);
	        	if( 0 < count ){
	    			updateDatabaseLastModified();
	        	}
				break;

			// NOTE: Not supported, I'm lazy and it's not needed now.
			// case TEMP_FILES:
			//      count = mDB.delete(TEMP_FILE_TABLE_NAME, where, whereArgs);
			//      break;

			case TEMP_FILE_ID:
				segment = url.getLastPathSegment();
				rowId = Long.parseLong(segment);

				if (!deleteTempFile(rowId)) {
					Log.w(TAG, "Failed to delete temp file");
				}

				count = mDB.delete(TEMP_FILE_TABLE_NAME, Task.FilterElementColumns._ID + "="
                            + segment
                            + (!TextUtils.isEmpty(where) ? " AND (" + where
                                    + ')' : ""), whereArgs);
				break;

			case FILTER_ELEMENTS:
				count = mDB.delete(FILTER_ELEMENT_TABLE_NAME, where, whereArgs);
	        	if( 0 < count ){
	    			updateDatabaseLastModified();
	        	}
				break;

			case FILTER_ELEMENTS_ID:
				segment = url.getLastPathSegment();
				rowId = Long.parseLong(segment);
				count = mDB.delete(FILTER_ELEMENT_TABLE_NAME, Task.FilterElementColumns._ID + "="
                            + segment
                            + (!TextUtils.isEmpty(where) ? " AND (" + where
                                    + ')' : ""), whereArgs);
	        	if( 0 < count ){
	    			updateDatabaseLastModified();
	        	}
				break;
			case USER_APPLIED_LABEL_ID:
				segment = url.getLastPathSegment();
				rowId = Long.parseLong(segment);
				count = deleteUserAppliedLabel(mDB, segment);
	        	if( 0 < count ){
	    			updateDatabaseLastModified();
	        	}
				break;

			case ARCHIVED_TASKS:

				String olderThanDateString = url.getQueryParameter("older_than"); // TODO: ! Extract this string.
				long olderThanDateLong = -1;
				if (null != olderThanDateString) {
					olderThanDateLong = Long.parseLong(olderThanDateString);
				}

				StringBuffer archiveQuery = new StringBuffer();
				archiveQuery.append("'");
				archiveQuery.append(Task.Tasks.CONTENT_URI_STRING);
				archiveQuery.append("/' || ");
				archiveQuery.append(Task.Tasks._ID);
				archiveQuery.append(" IN (SELECT ");
				archiveQuery.append(Task.LabeledContentColumns._CONTENT_URI);
				archiveQuery.append(" FROM ");
				archiveQuery.append(LABELED_CONTENT_TABLE_NAME);
				archiveQuery.append(" WHERE ");
				if (-1 != olderThanDateLong) {
					archiveQuery.append(Task.LabeledContentColumns.CREATED_DATE);
					archiveQuery.append(" < ");
					archiveQuery.append(olderThanDateLong);
					archiveQuery.append(" AND ");
				}
				archiveQuery.append(Task.LabeledContentColumns._LABEL_ID);
				archiveQuery.append("=");
				archiveQuery.append(ID_LABEL_ARCHIVED);
				archiveQuery.append(" AND ");
				archiveQuery.append(Task.LabeledContentColumns._CONTENT_URI);
				archiveQuery.append(" LIKE '");
				archiveQuery.append(Task.Tasks.CONTENT_URI_STRING);
				archiveQuery.append("/%')");

				StringBuffer outerArchiveMaskQuery = new StringBuffer();
				outerArchiveMaskQuery.append(TASK_TABLE_NAME);
				outerArchiveMaskQuery.append(".");
				outerArchiveMaskQuery.append(Task.Tasks._ID);
				outerArchiveMaskQuery.append(" IN (SELECT ");
				outerArchiveMaskQuery.append(Task.Tasks._ID);
				outerArchiveMaskQuery.append(" FROM ");
				outerArchiveMaskQuery.append(TASK_TABLE_NAME);
				outerArchiveMaskQuery.append(" WHERE ");
				outerArchiveMaskQuery.append(archiveQuery.toString());
				outerArchiveMaskQuery.append(")");

				count = mDB.delete(TASK_TABLE_NAME, archiveQuery
                            + (!TextUtils.isEmpty(where) ? " AND (" + where
                                    + ')' : ""), whereArgs);    		

	        	if( 0 < count ){
	    			updateDatabaseLastModified();
	        	}

				break;

			case NOTIFICATIONS:
				count = mDB.delete(NOTIFICATION_TABLE_NAME, where, whereArgs);
				break;

			case NOTIFICATION_ID:
				segment = url.getLastPathSegment();
				rowId = Long.parseLong(segment);
				count = mDB.delete(NOTIFICATION_TABLE_NAME, "_id="
                            + segment
                            + (!TextUtils.isEmpty(where) ? " AND (" + where
                                    + ')' : ""), whereArgs);
				break;
			case COMPLETABLE:
				count = mDB.delete(COMPLETABLE_TABLE_NAME, where, whereArgs);
	        	if( 0 < count ){
	    			updateDatabaseLastModified();
	        	}
				break;

			case COMPLETABLE_ID:
				segment = url.getLastPathSegment();
				rowId = Long.parseLong(segment);
				count = mDB.delete(COMPLETABLE_TABLE_NAME, "_id="
                            + segment
                            + (!TextUtils.isEmpty(where) ? " AND (" + where
                                    + ')' : ""), whereArgs);
	        	if( 0 < count ){
	    			updateDatabaseLastModified();
	        	}
				break;

			case APP_WIDGETS:
				count = mDB.delete(APP_WIDGETS_TABLE_NAME, where, whereArgs);
				break;
			case APP_WIDGETS_DELETED:
				count = mDB.delete(APP_WIDGETS_DELETED_TABLE_NAME, where, whereArgs);
				break;

			default:
				throw new IllegalArgumentException("Unsupported operation \"delete\" for URL " + url);
		}
		if (count > 0) {
			getContext().getContentResolver().notifyChange(url, null);
		}
		return count;
	}

	private void sendDeleteIntentsForTasks(SQLiteDatabase mDB, String where, String[] whereArgs) {
		Cursor attachmentCursor = mDB.query(ATTACHMENT_TABLE_NAME + " JOIN " + TASK_TABLE_NAME + " USING (" + Task.Tasks._ID + ")", new String[] { Task.TaskAttachments._DELETE_INTENT }, where, whereArgs, null, null, null);
		assert null != attachmentCursor;
		Intent deleteIntent = null;
		while (attachmentCursor.moveToNext()) {
			if (attachmentCursor.isNull(0)) {
				continue;
			}
			deleteIntent = AttachmentPart.expandIntent(attachmentCursor.getString(0), getContext());
			sendDeleteIntent(deleteIntent);
		}
		attachmentCursor.close();
	}

	private void sendDeleteIntentsForAttachment(SQLiteDatabase mDB, String where, String[] whereArgs) {
		Cursor attachmentCursor = mDB.query(ATTACHMENT_TABLE_NAME, new String[] { Task.TaskAttachments._DELETE_INTENT }, where, whereArgs, null, null, null);
		assert null != attachmentCursor;
		Intent deleteIntent = null;
		while (attachmentCursor.moveToNext()) {
			if (attachmentCursor.isNull(0)) {
				continue;
			}
			deleteIntent = AttachmentPart.expandIntent(attachmentCursor.getString(0), getContext());
			sendDeleteIntent(deleteIntent);
		}
		attachmentCursor.close();
	}

	private void sendDeleteIntent(Intent deleteIntent) {
		if (null != deleteIntent) {
			deleteIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // This makes it safe, and is required because we are not in an activity.
			getContext().startActivity(deleteIntent);
		} else {
			Log.e(TAG, "ERR000FO");
			ErrorUtil.handleExceptionNotifyUser("ERR000FO", (Exception) (new Exception()).fillInStackTrace(), getContext());
		}
	}

	@Override
    public int update(Uri url, ContentValues values, String where, String[] whereArgs) {
    	SQLiteDatabase mDB = dbHelper.getWritableDatabase();

        int count;
        switch (URL_MATCHER.match(url)) {
        case TASKS:
        	
        	if( values.containsKey(Task.Tasks.ALARM_TIME) || values.containsKey(Task.Tasks.COMPLETE) ){
        		count = updateTasksWithAlarmActiveFlagHandlingLogic(mDB, where, whereArgs, values);
        	}else{
        		// Do the update
                count = mDB.update(TASK_TABLE_NAME, values, where, whereArgs);
        	}
        	if( 0 < count ){
    			updateDatabaseLastModified();
        	}
            break;

        case TASK_ID:
        	String segment = url.getLastPathSegment();

        	if( values.containsKey(Task.Tasks.ALARM_TIME) || values.containsKey(Task.Tasks.COMPLETE) ){
        		count = updateTasksWithAlarmActiveFlagHandlingLogic(mDB, "_id="
                        + segment
                        + (!TextUtils.isEmpty(where) ? " AND (" + where
                                + ')' : ""), whereArgs, values);
        	}else{
        		// Do the update
                count = mDB
                        .update("tasks", values, "_id="
                                + segment
                                + (!TextUtils.isEmpty(where) ? " AND (" + where
                                        + ')' : ""), whereArgs);
        	}
        	if( 0 < count ){
    			updateDatabaseLastModified();
        	}

            break;
            
        case MULTI_TASK_ID:
        	String taskIdInList = url.getLastPathSegment();
        	for(char c: taskIdInList.toCharArray()){
        		if( !Character.isDigit(c) && !(c==',')){
        			return 0;
        		}
        	}
        	
        	count = mDB.update(TASK_TABLE_NAME, values, "_id IN ("
        			+ taskIdInList + ")"
        			+ (!TextUtils.isEmpty(where) ? " AND (" + where
        					+ ')' : ""), whereArgs);
        	if( 0 < count ){
    			updateDatabaseLastModified();
        	}

        	break;
        	        	
        case MASK_DUE:        	
        	DateFormat df = DateFormat.getDateTimeInstance();
        	StringBuffer sb = new StringBuffer();

        	// Handle "Days from Today's end"
        	String daysFromTodaysEnd = url.getQueryParameter(Constraint.Version1.DAYS_FROM_TODAYS_END);
        	if( null != daysFromTodaysEnd ){
	        	// Retrieve the requested data "Days from today".
	        	int daysFromToday = Integer.parseInt(daysFromTodaysEnd);
	        	
	        	// Prepare a calendar for the end of the day, X days from now.
	        	Calendar cal = Calendar.getInstance(); // TODO: ! Add local here.
	        	//Log.d(TAG, "Before: " + df.format(cal.getTime()));
	        	cal.roll(Calendar.DAY_OF_YEAR, daysFromToday + 1);
	        	cal.set(Calendar.HOUR_OF_DAY, 00);
	        	cal.set(Calendar.MINUTE,      00);
	        	cal.set(Calendar.SECOND,      00);
	        	
	        	// Included or exclude.
	        	sb.append(Task.TasksColumns.DUE_DATE + " < " + String.valueOf(cal.getTimeInMillis()));
	        	//Log.d(TAG, "After: " + df.format(cal.getTime()));
        	} else{
	        	// Handle "Weeks from this week's end"
	        	String weeksFromThisWeeksEnd = url.getQueryParameter(Constraint.Version1.WEEKS_FROM_THIS_WEEKS_END);
	        	if( null != weeksFromThisWeeksEnd ){
		        	// Retrieve the requested data "Days from today".
		        	int weeksFromThisWeek = Integer.parseInt(weeksFromThisWeeksEnd);
		        	
		        	// Prepare a calendar for the end of the week, X weeks from now.
		        	Calendar cal = Calendar.getInstance(); // TODO: ! Add local here.
		        	
		        	//Log.d(TAG, "Before: " + df.format(cal.getTime()));
		        	cal.roll(Calendar.WEEK_OF_YEAR, weeksFromThisWeek + 1); // TODO: The first day of the week is controlled by the local but can also be specified with Calendar.setFirstDayOfWeek(..)
		        	cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY); // TODO: !!! Make this a preference, I thought that this was auto handled by Calendar by referring to the locale,, but now (without internet connection) I don't see how.
		        	cal.roll(Calendar.DAY_OF_YEAR, 1 );
		        	cal.set(Calendar.HOUR_OF_DAY, 00);
		        	cal.set(Calendar.MINUTE,      00);
		        	cal.set(Calendar.SECOND,      00);
		        	
		        	// Included or exclude.
		        	sb.append(Task.TasksColumns.DUE_DATE + " < " + String.valueOf(cal.getTimeInMillis()));
		        	//Log.d(TAG, "After: " + df.format(cal.getTime()));
		        	
	        	} else{
		        	// Handle "Anytime"
		        	if( null != url.getQueryParameter(Constraint.Version1.ANYTIME) ){

		        	} else if( null != url.getQueryParameter(Constraint.Version1.PAST) ){
			        	// Get Calendar for current time.
			        	Calendar cal = Calendar.getInstance(); // TODO: ! Add local here.
			        	sb.append(Task.TasksColumns.DUE_DATE + " < " + String.valueOf(cal.getTimeInMillis()));
				        	
		        	} else if( null != url.getQueryParameter(Constraint.Version1.DISABLE) ){
		        		sb.append(Task.TasksColumns.DUE_DATE + " IS NULL" ); // An impossible condition.
		        	}		        	

	        	}	
        	}
        	// Handle Tasks with no due dates.
        	if( null != url.getQueryParameter(Constraint.Version1.INCLUDE_NO_DUE_DATE_ITEMS) ){
        		if( null != url.getQueryParameter(Constraint.Version1.ANYTIME) ){ // Constraint.Version1.ANYTIME includes Task.TasksColumns.DUE_DATE_NOT_SET, so we must exclude it.

        		}else{ // Masks other than ANYTIME don't include DUE_DATE_NOT_SET and so we must include tasks with no due date using this OR clause.
	        		if( sb.length() > 0 ){
	        			sb.append(" OR ");
	        		}
		        	sb.append(Task.TasksColumns.DUE_DATE + "=" + Task.TasksColumns.DUE_DATE_NOT_SET);
        		}
		        	
        	}else{ // Don't show tasks which have no due date.
        		if( null != url.getQueryParameter(Constraint.Version1.ANYTIME) ){ // Constraint.Version1.ANYTIME includes Task.TasksColumns.DUE_DATE_NOT_SET, so we must exclude it.
            		if( sb.length() > 0 ){
	        			sb.append(" AND ");
	        		}
		        	sb.append(Task.TasksColumns.DUE_DATE + "!=" + Task.TasksColumns.DUE_DATE_NOT_SET);
        		}        		
        		if( null != url.getQueryParameter(Constraint.Version1.DISABLE) ){ // Both INCLUDE_NO_DUE_DATE_ITEMS and DISABLE are active.
        			// TODO: !!! Prevent user from doing this,, it is guaranteed to hide all tasks.
        		}
        		
        	}

        	if( sb.length() != 0 ){
	        	StringBuffer outerDueDateMaskQuery = new StringBuffer();
	        	outerDueDateMaskQuery.append(Task.Tasks._FILTER_BIT);
	        	outerDueDateMaskQuery.append("=");
	        	outerDueDateMaskQuery.append(Task.Tasks.FILTER_IN);
	        	outerDueDateMaskQuery.append(" AND ");
	        	outerDueDateMaskQuery.append(TASK_TABLE_NAME);
	        	outerDueDateMaskQuery.append(".");
	        	outerDueDateMaskQuery.append(Task.Tasks._ID);

		       	outerDueDateMaskQuery.append(" NOT ");

	        	outerDueDateMaskQuery.append(" IN (SELECT ");
	        	outerDueDateMaskQuery.append(Task.Tasks._ID);
	        	outerDueDateMaskQuery.append(" FROM ");
	        	outerDueDateMaskQuery.append(TASK_TABLE_NAME);
	        	outerDueDateMaskQuery.append(" WHERE ");
	        	outerDueDateMaskQuery.append(sb.toString());
	        	outerDueDateMaskQuery.append(")");
        	
        		count = mDB.update(TASK_TABLE_NAME, values, outerDueDateMaskQuery.toString()
        				,new String[]{});
        	}else{ // If no qualifications where included, then default to nothing.
        		count = 0;
        	}
        	break;
        	
        case MASK_PRIORITY:        	
        	String priority = url.getQueryParameter(Constraint.Version1.PRIORITY_PARAM);
        	priority = null == priority ? Constraint.Version1.PRIORITY_VALUE_NONE : priority;
    		count = mDB.update(TASK_TABLE_NAME, values, Task.Tasks.PRIORITY + "<?"
    				,new String[]{priority});
        	break;
        	
        case MASK_STATUS:        	
        	StringBuffer statusQuery = new StringBuffer();
        	String option = url.getQueryParameter(Constraint.Version1.STATUS_PARAM_OPTION);
        	option = null == option ? Constraint.Version1.STATUS_PARAM_OPTION_DEFAULT_VALUE : option;
        	if( Constraint.Version1.STATUS_PARAM_OPTION_BOTH.equals(option)){
        		// Do nothing since this will include both complete and ready tasks.
        		count = 0;
        		break;
        	}else if( Constraint.Version1.STATUS_PARAM_OPTION_COMPLETED.equals(option)){
        		statusQuery.append(Task.Tasks.COMPLETE);
        		statusQuery.append('=');
        		statusQuery.append(Task.Tasks.COMPLETE_FALSE);
        	}else if( Constraint.Version1.STATUS_PARAM_OPTION_NOT_COMPLETED.equals(option)){
        		statusQuery.append(Task.Tasks.COMPLETE);
        		statusQuery.append('=');
        		statusQuery.append(Task.Tasks.COMPLETE_TRUE);
        	}else if( Constraint.Version1.STATUS_PARAM_OPTION_DATE_RANGE.equals(option)){
            	String fromDateString = url.getQueryParameter(Constraint.Version1.STATUS_PARAM_FROM_DATE);
            	long fromDateValue = Long.parseLong(fromDateString);
            	String toDateString   = url.getQueryParameter(Constraint.Version1.STATUS_PARAM_TO_DATE);
            	long toDateValue = Long.parseLong(toDateString);
 //           	Log.v(TAG, "fromDateValue " + fromDateValue + " toDateDal "+ toDateValue);
            	if( fromDateValue > toDateValue ){
            		long tmpVal = fromDateValue;
            		fromDateValue = toDateValue;
            		toDateValue = tmpVal;
            	}
//            	Log.v(TAG, "fromDateValue " + fromDateValue + " toDateDal "+ toDateValue);
            	
        		statusQuery.append(Task.Tasks.COMPLETION_DATE);
        		statusQuery.append('<');
        		statusQuery.append(fromDateValue);
        		statusQuery.append(" OR ");
        		statusQuery.append(Task.Tasks.COMPLETION_DATE);
        		statusQuery.append('>');
        		statusQuery.append(toDateValue);
        		statusQuery.append(" OR ");
        		statusQuery.append(Task.Tasks.COMPLETION_DATE);
        		statusQuery.append(" IS NULL");
        	}

    		count = mDB.update(TASK_TABLE_NAME, values, statusQuery.toString() 
    				,new String[]{});
        	
        	break;
        	        	
        	

        case MASK_REPOSITORY:
        	String repository = url.getQueryParameter(Constraint.Version1.REPOSITORY_PARAM);
        	repository = (null == repository ? Constraint.Version1.REPOSITORY_PARAM_OPTION_DEFAULT : repository);

        	StringBuffer repositoryQuery = createRepositoryQuery(repository);

        	StringBuffer outerArchiveMaskQuery = new StringBuffer();
        	outerArchiveMaskQuery.append(Task.Tasks._FILTER_BIT);
        	outerArchiveMaskQuery.append("=");
        	outerArchiveMaskQuery.append(Task.Tasks.FILTER_IN); // Redundant, but should make the update more effecient.
        	outerArchiveMaskQuery.append(" AND ");
        	outerArchiveMaskQuery.append(TASK_TABLE_NAME);
        	outerArchiveMaskQuery.append(".");
        	outerArchiveMaskQuery.append(Task.Tasks._ID);
        	outerArchiveMaskQuery.append(" NOT IN (");
        	outerArchiveMaskQuery.append(repositoryQuery.toString());
        	outerArchiveMaskQuery.append(")");        	
        	
    		count = mDB.update(TASK_TABLE_NAME, values, outerArchiveMaskQuery.toString()
    				,new String[]{});
    		break;
        	
        case MASK_DAYS_FROM_TODAYS_END:
        	if( null == values ){
        		values = new ContentValues(); // Ensure that we have a ContentValues object.
        	}	
        	values.clear();
        	
        	// Retrieve the requested data "Days from today".
        	int daysFromToday = Integer.parseInt(url.getLastPathSegment());
        	
        	// Prepare a calendar for the end of the day, X days from now.
        	Calendar cal = Calendar.getInstance(); // TODO: ! Add local here.
        	cal.roll(Calendar.DATE, daysFromToday + 1);
        	cal.set(Calendar.HOUR_OF_DAY, 00);
        	cal.set(Calendar.MINUTE,      00);
        	cal.set(Calendar.SECOND,      00);
        	
        	count = mDB.update(TASK_TABLE_NAME, values,
        			Task.TasksColumns.DUE_DATE + " < ?"
        			+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
        			new String[]{String.valueOf(cal.getTimeInMillis())});
        	break;
        	
        case MASK_WEEKS_FROM_THIS_WEEKS_END:
        	if( null == values ){
        		values = new ContentValues();
        	}	
        	values.clear();
        	
        	// Retrieve the requested data "Days from today".
        	int weeksFromThisWeek = Integer.parseInt(url.getLastPathSegment());
        	
        	// Prepare a calendar for the end of the day, X days from now.
        	cal = Calendar.getInstance(); // TODO: ! Add local here.
        	cal.roll(Calendar.WEEK_OF_YEAR, weeksFromThisWeek + 1); // TODO: The first day of the week is controlled by the local but can also be specified with Calendar.setFirstDayOfWeek(..)
        	cal.set(Calendar.HOUR_OF_DAY, 00);
        	cal.set(Calendar.MINUTE,      00);
        	cal.set(Calendar.SECOND,      00);
        	
        	count = mDB.update(TASK_TABLE_NAME, values,
        			Task.TasksColumns.DUE_DATE + " < ?"
        			+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
        			new String[]{String.valueOf(cal.getTimeInMillis())});
        	break;            
        	
        case MASK_LABEL:
        	String enabled = url.getQueryParameter(Constraint.Version1.LABEL_PARAM_ENABLED);
        	enabled = (null == enabled ? Constraint.Version1.LABEL_PARAM_ENABLED_VALUE_TRUE : enabled);
        	if( Constraint.Version1.LABEL_PARAM_ENABLED_VALUE_TRUE.equals(enabled) ){
        		
        		String labelId = url.getLastPathSegment();

                // SELECT * FROM TASKS WHERE
                //   _id IN (SELECT tasks._id FROM tasks, labeled_content WHERE 'content://"+Task.AUTHORITY+"/tasks/' || tasks._id = labeled_content._content_uri
                //   AND labeled_content._label_id = {Given labeldId} )
                final String whereClause = TASK_TABLE_NAME +"."+ Task.Tasks._ID +" IN (SELECT " +TASK_TABLE_NAME +"."+ Task.Tasks._ID
                        + " FROM " + TASK_TABLE_NAME + "," + LABELED_CONTENT_TABLE_NAME
                        + " WHERE '" + Task.Tasks.CONTENT_URI_STRING + "/' || " + TASK_TABLE_NAME +"."+ Task.Tasks._ID + " = "+LABELED_CONTENT_TABLE_NAME+"."+Task.LabeledContentColumns._CONTENT_URI+ " AND "
                        + LABELED_CONTENT_TABLE_NAME + "." + Task.LabeledContentColumns._LABEL_ID + " = " + labelId + ")"
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");

                count = mDB.update(TASK_TABLE_NAME,
                        values,
                        whereClause,
                        whereArgs);
	        		
        	}else{
        		count = 0;
        	}
        	break;            
        	
        case MASK_UNLABELED:
        	enabled = url.getQueryParameter(Constraint.Version1.UNLABELED_PARAM_ENABLED);
        	enabled = (null == enabled ? Constraint.Version1.UNLABELED_PARAM_ENABLED_VALUE_FALSE : enabled);
        	if( Constraint.Version1.UNLABELED_PARAM_ENABLED_VALUE_TRUE.equals(enabled) ){
        		Log.v(TAG, "MASK_UNLABELED called");
        		// SELECT * FROM TASKS WHERE
        		//    _id NOT IN  (SELECT tasks._id FROM tasks, labeled_content WHERE 'content://"+Task.AUTHORITY+"/tasks/' || tasks._id = labeled_content._content_uri 
        		//	  AND labeled_content._label_id NOT NULL)
        		final String whereUnClause = TASK_TABLE_NAME +"."+ Task.Tasks._ID +
        		" NOT IN (SELECT " +TASK_TABLE_NAME +"."+ Task.Tasks._ID 
        		+  " FROM " + TASK_TABLE_NAME + "," + LABELED_CONTENT_TABLE_NAME 
        		+  " WHERE '" + Task.Tasks.CONTENT_URI_STRING + "/' || " + TASK_TABLE_NAME +"."+ Task.Tasks._ID + " = "+LABELED_CONTENT_TABLE_NAME+"."+Task.LabeledContentColumns._CONTENT_URI+ " AND ("  // Join Tasks and LabeledContent
//        		+       LABELED_CONTENT_TABLE_NAME + "." + Task.LabeledContentColumns._LABEL_ID + " NOT NULL " // (tasks with a label)
        		+       LABELED_CONTENT_TABLE_NAME + "." + Task.LabeledContentColumns._LABEL_ID + " != " + ID_LABEL_ARCHIVED // tasks without the archive label (but have a user applied label)
        		+       " OR " 
        		+       LABELED_CONTENT_TABLE_NAME + "." + Task.LabeledContentColumns._ID + " IN ("
        		 
        		// Get all label_content _id's which have both an archive label as well as an user applied label.
        		+ 		  " SELECT "+ Task.LabeledContentColumns._ID 
        		+         " FROM "  + LABELED_CONTENT_TABLE_NAME 
        		+         " WHERE " + Task.LabeledContentColumns._LABEL_ID + " == "+ID_LABEL_ARCHIVED+" AND "    
        		+                     Task.LabeledContentColumns._CONTENT_URI + " IN (" // "IN" indicates when a task is labeled both as archived and with a user applied label.
        		+ 				" SELECT "+ Task.LabeledContentColumns._CONTENT_URI
        		+               " FROM "  + LABELED_CONTENT_TABLE_NAME 
        		+               " WHERE " + Task.LabeledContentColumns._LABEL_ID + " != " + ID_LABEL_ARCHIVED
        		+  ") ) ) )"
        		        		
        		+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
        		
        		count = mDB.update(TASK_TABLE_NAME,
        				values, 
        				whereUnClause, 
        				whereArgs); 
        	}else{
        		count = 0;
        	}
        	break;            
        	
        case MASK_ALL:
        	
        	count = mDB.update(TASK_TABLE_NAME, values, where, whereArgs);
        	break;            
        	
        case ATTACHMENTS:
        	// TODO: !!! When _intent and _delete_intent are queried, the returned Intent currently is serialized as a byte array using our internal serialization protocol. This will not be a valid value to external applications (or our own applicaion). Should replace the queried bytes with an actual Intent object,, or something... 
            count = mDB.update("attachments", values, where, whereArgs);
        	if( 0 < count ){
    			updateDatabaseLastModified();
        	}
            break;

        case ATTACHMENT_ID:
            segment = url.getLastPathSegment();
            count = mDB
                    .update("attachments", values, "_id="
                            + segment
                            + (!TextUtils.isEmpty(where) ? " AND (" + where
                                    + ')' : ""), whereArgs);
        	if( 0 < count ){
    			updateDatabaseLastModified();
        	}
            break;            
            
        case PROXIMITY_ALERTS:
        	if( values.containsKey(Task.ProximityAlerts.ENABLED) || values.containsKey(Task.ProximityAlerts._GEO_URI) || values.containsKey(Task.ProximityAlerts.RADIUS)){ 
            	count = updateProximityAlertAndDependencies(mDB, values, where, whereArgs);
        	}else{
                count = mDB.update(PROXIMITY_ALERT_TABLE_NAME, values, where, whereArgs);
        	}
        	if( 0 < count ){
    			updateDatabaseLastModified();
        	}
            break;
            
        case PROXIMITY_ALERT_ID:
        	segment = url.getLastPathSegment();
        	if( values.containsKey(Task.ProximityAlerts.ENABLED) || values.containsKey(Task.ProximityAlerts._GEO_URI) || values.containsKey(Task.ProximityAlerts.RADIUS)){ 
            	count = updateProximityAlertAndDependencies(mDB, values, "_id="
                        + segment
                        + (!TextUtils.isEmpty(where) ? " AND (" + where
                                + ')' : ""), whereArgs);
        	}else{
        		count = mDB
        		.update(PROXIMITY_ALERT_TABLE_NAME, values, "_id="
        				+ segment
        				+ (!TextUtils.isEmpty(where) ? " AND (" + where
        						+ ')' : ""), whereArgs);
        	}
        	if( 0 < count ){
    			updateDatabaseLastModified();
        	}
            break;

        case LABELS:
            count = mDB.update(LABELS_TABLE_NAME, values, where, whereArgs);
        	if( 0 < count ){
    			updateDatabaseLastModified();
        	}
            break;

        case LABEL_ID:
            segment = url.getLastPathSegment();
            count = mDB
                    .update(LABELS_TABLE_NAME, values, Task.Labels._ID + "="
                            + segment
                            + (!TextUtils.isEmpty(where) ? " AND (" + where
                                    + ')' : ""), whereArgs);
        	if( 0 < count ){
    			updateDatabaseLastModified();
        	}
            break;

        case FILTERS:
            count = mDB.update(FILTER_TABLE_NAME, values, where, whereArgs);
        	if( 0 < count ){
    			updateDatabaseLastModified();
        	}
            break;

        case FILTERS_ID:
        	if( values.containsKey(Task.Filter._PERMANENT)){ // Hey! Why are you trying to change the _permanent field value? 
    			Log.e(TAG, "ERR000HQ");
    			ErrorUtil.handleException("ERR000HQ", (Exception)(new Exception( )).fillInStackTrace(), getContext());
    			// TODO: !!! Consider throwing an error here.
        	}
            segment = url.getLastPathSegment();
            count = mDB
                    .update(FILTER_TABLE_NAME, values, Task.FilterColumns._ID + "="
                            + segment
                            + (!TextUtils.isEmpty(where) ? " AND (" + where
                                    + ')' : ""), whereArgs);
        	if( 0 < count ){
    			updateDatabaseLastModified();
        	}
            break;
        case SWITCH_FILTER_ID:
            segment = url.getLastPathSegment();
            mDB.beginTransaction();
            try{
            	values = new ContentValues(1);
            	
            	// Ensure that target filter is active.
            	values.put(Task.Filter._ACTIVE, Task.Filter.ACTIVE_TRUE);
            	mDB.update(FILTER_TABLE_NAME, values, Task.Filter._ID+"=?", new String[]{segment});
            	
            	// Clear out any existing active flag.
            	values.clear();            	
            	values.putNull(Task.Filter._SELECTED);
            	mDB.update(FILTER_TABLE_NAME, values, null, null);
            	
            	// Set the new active flag.
            	values.clear();
            	values.put(Task.Filter._SELECTED, Task.Filter.SELECTED_TRUE);
            	count = mDB.update(FILTER_TABLE_NAME, values, Task.Filter._ID + "="
            			+ segment
            			+ (!TextUtils.isEmpty(where) ? " AND (" + where
            					+ ')' : ""), whereArgs);
            	
            	// Make sure that a new filter was set.
            	if( count == 1 ){
            		mDB.setTransactionSuccessful();
            		getContext().getContentResolver().notifyChange(Task.Filter.CONTENT_URI, null);
            	}
            	if( 0 < count ){
        			updateDatabaseLastModified();
            	}
            }finally{
            	mDB.endTransaction();
            }
            break;
            
        case SWITCH_TO_PERMANENT_FILTER_ID: 
            segment = url.getLastPathSegment();
            mDB.beginTransaction();
            try{
            	values = new ContentValues(1);
            	
            	// Ensure that target filter is active.
            	values.put(Task.Filter._ACTIVE, Task.Filter.ACTIVE_TRUE);
            	mDB.update(FILTER_TABLE_NAME, values, Task.Filter._PERMANENT+"=?", new String[]{segment});

            	
            	// Clear out any existing active flag.
            	values.clear();
            	values.putNull(Task.Filter._SELECTED);
            	mDB.update(FILTER_TABLE_NAME, values, null, null);
            	
            	// Set the new active flag.
            	values.clear();
            	values.put(Task.Filter._SELECTED, Task.Filter.SELECTED_TRUE);
            	count = mDB.update(FILTER_TABLE_NAME, values, Task.Filter._PERMANENT + "="
            			+ segment
            			+ (!TextUtils.isEmpty(where) ? " AND (" + where
            					+ ')' : ""), whereArgs);
            	
            	// Make sure that a new filter was set.
            	if( count == 1 ){
            		mDB.setTransactionSuccessful();
            		getContext().getContentResolver().notifyChange(Task.Filter.CONTENT_URI, null);
            	}
            	if( 0 < count ){
        			updateDatabaseLastModified();
            	}
            }finally{
            	mDB.endTransaction();
            }
        	break;

        case FILTER_ELEMENTS:
            count = mDB.update(FILTER_ELEMENT_TABLE_NAME, values, where, whereArgs);
        	if( 0 < count ){
    			updateDatabaseLastModified();
        	}
            break;

        case FILTER_ELEMENTS_ID:
            segment = url.getLastPathSegment();
            count = mDB
                    .update(FILTER_ELEMENT_TABLE_NAME, values, Task.FilterElementColumns._ID + "="
                            + segment
                            + (!TextUtils.isEmpty(where) ? " AND (" + where
                                    + ')' : ""), whereArgs);
        	if( 0 < count ){
    			updateDatabaseLastModified();
        	}
            break;

		case TEMP_FILES: // NOTE: Does not support updating the temp file data.
            count = mDB.update(TEMP_FILE_TABLE_NAME, values, where, whereArgs);
			break;

        case TEMP_FILE_ID: // NOTE: Does not support updating the temp file data.
            segment = url.getLastPathSegment();
            count = mDB
                    .update(TEMP_FILE_TABLE_NAME, values, Task.TempFile._ID + "="
                            + segment
                            + (!TextUtils.isEmpty(where) ? " AND (" + where
                                    + ')' : ""), whereArgs);
        	break;            
            
        case NOTIFICATIONS:
            count = mDB.update(NOTIFICATION_TABLE_NAME, values, where, whereArgs);
            break;

        case NOTIFICATION_ID:
            segment = url.getLastPathSegment();
            count = mDB
                    .update(NOTIFICATION_TABLE_NAME, values, "_id="
                            + segment
                            + (!TextUtils.isEmpty(where) ? " AND (" + where
                                    + ')' : ""), whereArgs);
            break;
            
        case COMPLETABLE:
            count = mDB.update(COMPLETABLE_TABLE_NAME, values, where, whereArgs);
        	if( 0 < count ){
    			updateDatabaseLastModified();
        	}
            break;

        case COMPLETABLE_ID:
            segment = url.getLastPathSegment();
            count = mDB
                    .update(COMPLETABLE_TABLE_NAME, values, "_id="
                            + segment
                            + (!TextUtils.isEmpty(where) ? " AND (" + where
                                    + ')' : ""), whereArgs);
        	if( 0 < count ){
    			updateDatabaseLastModified();
        	}
            break;

        // May require more code to map changes to both tables.
        case USER_APPLIED_LABEL:
            count = mDB.update(LABELS_TABLE_NAME, values, where, whereArgs);
    		getContext().getContentResolver().notifyChange(Task.FilterElement.CONTENT_URI, null); // Too much work to notify for each updated record. 
        	if( 0 < count ){
    			updateDatabaseLastModified();
        	}
            break;
        	
        // May require more code to map changes to both tables.
        case USER_APPLIED_LABEL_ID:
            segment = url.getLastPathSegment();
            count = mDB
                    .update(LABELS_TABLE_NAME, values, "_id="
                            + segment
                            + (!TextUtils.isEmpty(where) ? " AND (" + where
                                    + ')' : ""), whereArgs);
    		getContext().getContentResolver().notifyChange(Task.FilterElement.CONTENT_URI, null); // Too much work to notify for each updated record. 
        	if( 0 < count ){
    			updateDatabaseLastModified();
        	}
            break;

        case ARCHIVED_TASKS_ID:
        	// Note: Both completed and not-completed (abandoned) tasks can be archived.  
    		String taskId = url.getLastPathSegment();
    		count = archiveTasks(mDB, Task.Tasks._ID+"=?", new String[]{String.valueOf(taskId)});
        	if( 0 < count ){
    			updateDatabaseLastModified();
        	}
    		break;
    		
    	case ARCHIVED_TASKS:
    		// Only completed tasks which are not already in the archive are archived.

    		// TODO: !!! Consider whether this is the right behavior.
    		// NOTE: Tasks that are not visible due to the filter settings are included in the consideration. 

        	// _filter_bit=1 AND tasks._id IN (
        	//   SELECT _id FROM tasks where {archiveQuery}
        	// )
        	StringBuffer query = new StringBuffer();
        	query.append(TASK_TABLE_NAME);
        	query.append(".");
        	query.append(Task.Tasks._ID);
        	query.append(" NOT IN (");
        	query.append(createRepositoryQuery(Constraint.Version1.REPOSITORY_PARAM_OPTION_ARCHIVE));
        	query.append(")");        	    		

    		count = archiveTasks(mDB, Task.Tasks.COMPLETE+"=? AND "+query, new String[]{Task.Tasks.COMPLETE_TRUE});
    		
        	if( 0 < count ){
    			updateDatabaseLastModified();
        	}
    		
            break;
        case UNARCHIVED_TASKS_ID:
        	// Note: Both completed and not-completed (abandoned) tasks can be archived.  
    		String theTaskId = url.getLastPathSegment();
    		count = unarchiveTasks(mDB, Task.Tasks._ID+"=?", new String[]{String.valueOf(theTaskId)});
        	if( 0 < count ){
    			updateDatabaseLastModified();
        	}
    		break;
    		
    	case UNARCHIVED_TASKS:
    		// Only tasks which are in the archive are unarchived.

    		// TODO: !!! Consider whether this is the right behavior.
    		// NOTE: Tasks that are not visible due to the filter settings are included in the consideration. 
        	
        	StringBuffer theQuery = new StringBuffer();
        	theQuery.append(TASK_TABLE_NAME);
        	theQuery.append(".");
        	theQuery.append(Task.Tasks._ID);
        	theQuery.append(" IN (");
        	theQuery.append(createRepositoryQuery(Constraint.Version1.REPOSITORY_PARAM_OPTION_ARCHIVE));
        	theQuery.append(")");        	    		
    		
    		count = unarchiveTasks(mDB, theQuery.toString(), null);
        	if( 0 < count ){
    			updateDatabaseLastModified();
        	}

            break;
    	case FILTER_BITS:
//    		Log.v(TAG, "FILTER_BITS");
    		count = filterBits(mDB, null);
    		break;
    	case FILTER_BITS_ID:
//    		Log.v(TAG, "FILTER_BITS_ID");
    		count = filterBits(mDB, url.getLastPathSegment());
    		break; 
    	case DB_BACKUP:
    		mBackupManagerHolder.datbaseBackedUp(values.getAsLong(Task.BackupColumns._LAST_BACKED_UP));
    		count = 1;
    		break;
    		
    		
        default:
            throw new IllegalArgumentException("Unknown URL " + url);
        }

        if( 0 < count ){
        	getContext().getContentResolver().notifyChange(url, null);
        }

        return count;
    }

	// TODO: !! The count value here is not correct. I'm not sure exactly how to measure the number changed, or if it is even relavant.
	private int filterBits(SQLiteDatabase mDB, String taskId) {
		int count;
		mDB.beginTransaction();
		try {

			// **************************************
			// Set all appropriate filter bits.
			// **************************************
			ContentValues initContentValues = new ContentValues(1);
			ContentValues setFilterBitsContentValues = new ContentValues(1);
			Uri filterElementUri = null;

			String where = null;
			String[] params = null;
			if (null != taskId) {
				// TODO: !!! Pass the _id value in as a parameter instead so it will work with external filters. 
				where = TASK_TABLE_NAME + "." + Tasks._ID + "=?";
				params = new String[] { taskId };
			}

			// **************************************
			// Initialize.
			// **************************************
			// LABEL_CHANGE
			Uri filterJoinUri = Uri.parse("content://" + Task.AUTHORITY + "/selected_filter/filter_elements");

			Cursor filterJoinCursor = getContext().getContentResolver().query(
					filterJoinUri,
					new String[] { Task.FilterElementColumns._CONSTRAINT_URI, Task.FilterElementColumns._PARAMETERS },
					"(("+TaskProvider.FILTER_ELEMENT_TABLE_NAME+"."+Task.FilterElementColumns._ACTIVE+"=? AND "+Task.FilterElementColumns._APPLY_WHEN_ACTIVE+"=?) OR ("+TaskProvider.FILTER_ELEMENT_TABLE_NAME+"."+ Task.FilterElementColumns._ACTIVE+"!=? AND "+Task.FilterElementColumns._APPLY_WHEN_ACTIVE+"=?)) AND "+Task.FilterElementColumns._PHASE+"=?",
					new String[] { Task.FilterElementColumns.ACTIVE_TRUE, Task.FilterElementColumns.APPLY_WHEN_ACTIVE, Task.FilterElementColumns.ACTIVE_TRUE, Task.FilterElementColumns.APPLY_WHEN_NOT_ACTIVE, Task.FilterElementColumns.PHASE_EXPLODE },
					TaskProvider.FILTER_ELEMENT_TABLE_NAME + "." + Task.FilterElementColumns._ID);
			assert null != filterJoinCursor;
			if( ! filterJoinCursor.moveToFirst() ){ // Are all the explosion filter elements not active?
				// **************************************
				// Initialize - Set all filter bits to on.
				// **************************************
				initContentValues.put(Task.Tasks._FILTER_BIT, Task.Tasks.FILTER_IN);
				count = getContext().getContentResolver().update(Constraint.Version1.ALL_CONTENT_URI, initContentValues, where, params);
			} else {
				// **************************************
				// Initialize - Set all filter bits to off.
				// **************************************
				initContentValues.put(Task.Tasks._FILTER_BIT, Task.Tasks.FILTER_OUT);
				getContext().getContentResolver().update(Constraint.Version1.ALL_CONTENT_URI, initContentValues, where, params);

				// **************************************
				// Explode all possible filter bits.
				// **************************************
				setFilterBitsContentValues.put(Task.Tasks._FILTER_BIT, Task.Tasks.FILTER_IN);
				count = 0;
				do {
					filterElementUri = Uri.parse(filterJoinCursor.getString(0) + (null == filterJoinCursor.getString(1) ? "" : "?" + filterJoinCursor.getString(1)));
					count += getContext().getContentResolver().update(filterElementUri, setFilterBitsContentValues, where, params);
					//Log.v(TAG, filterElementUri.toString() + "update count == " + count);
				} while (filterJoinCursor.moveToNext());
			}
			filterJoinCursor.close();

			// **************************************
			// Restrict.
			// **************************************
			// LABEL_CHANGE
			filterJoinCursor = getContext().getContentResolver().query(
					filterJoinUri,
					new String[]{Task.FilterElementColumns._CONSTRAINT_URI, Task.FilterElementColumns._PARAMETERS, Task.FilterElementColumns._PHASE}, 
					"(("+TaskProvider.FILTER_ELEMENT_TABLE_NAME+"."+Task.FilterElementColumns._ACTIVE+"=? AND "+Task.FilterElementColumns._APPLY_WHEN_ACTIVE+"=?) OR ("+TaskProvider.FILTER_ELEMENT_TABLE_NAME+"."+Task.FilterElementColumns._ACTIVE+"!=? AND "+Task.FilterElementColumns._APPLY_WHEN_ACTIVE+"=?)) AND ("+Task.FilterElementColumns._PHASE+"=? OR "+Task.FilterElementColumns._PHASE+"=?)",
					new String[] { Task.FilterElementColumns.ACTIVE_TRUE, Task.FilterElementColumns.APPLY_WHEN_ACTIVE, Task.FilterElementColumns.ACTIVE_TRUE, Task.FilterElementColumns.APPLY_WHEN_NOT_ACTIVE, Task.FilterElementColumns.PHASE_EXCLUDE, Task.FilterElementColumns.PHASE_INCLUDE },
					TaskProvider.FILTER_ELEMENT_TABLE_NAME + "." + Task.FilterElementColumns._ID);
			assert null != filterJoinCursor;

			// **************************************
			// Include.
			// **************************************
			setFilterBitsContentValues.put(Task.Tasks._FILTER_BIT, Task.Tasks.FILTER_OUT);
			while (filterJoinCursor.moveToNext()) {
				filterElementUri = Uri.parse(filterJoinCursor.getString(0) + (null == filterJoinCursor.getString(1) ? "" : "?" + filterJoinCursor.getString(1)));
				boolean inclusive = Task.FilterElementColumns.PHASE_INCLUDE.equals(filterJoinCursor.getString(2));
				filterElementUri = filterElementUri.buildUpon().appendQueryParameter(Constraint.Version1.PARAM_INCLUSIVE, inclusive ? "true" : "false").build();
				//    	getContentResolver().update(filterElementUri, setFilterBitsContentValues, Task.Tasks._FILTER_BIT+"=?", new String[]{Task.Tasks.FILTER_IN});
				getContext().getContentResolver().update(filterElementUri, setFilterBitsContentValues, where, params);
				//Log.v(TAG, filterElementUri.toString() + "update count == " + count);
			}
			filterJoinCursor.close();

			mDB.setTransactionSuccessful();

			// Notify interested parties that filter bits have changed.
			sendFilterBitsChangedBroadcast(getContext().getApplicationContext());
		} finally {
			mDB.endTransaction();
		}
		FilterBitsUpdateTimestamp filterBitsUpdatedTimestamp = new FilterBitsUpdateTimestamp(getContext());
		try {
			filterBitsUpdatedTimestamp.ensureExists();
			filterBitsUpdatedTimestamp.update();		
		} catch (HandledException he) {
		} catch (Exception e) {
			Log.e(TAG, "ERR000KZ", e);
			ErrorUtil.handleException("ERR000KZ", e, getContext());
		}
		return count;
	}

	/**
	 * 
     * TODO: !!! Prevent user from editing task (including adding attachments, and use of ACTION_ATTACH) when task is marked complete or when the task is archived.
	 * @param mDB
	 * @param values
	 * @param where
	 * @param whereArgs
	 * @return
	 */
	private int updateProximityAlertAndDependencies(SQLiteDatabase mDB, ContentValues values, String where,
			String[] whereArgs) {
		int count;
		mDB.beginTransaction();
		try {
			// ********************************************************
			// Record the modified record IDs for use later.
			// ********************************************************
			String tempTableSql = "CREATE TEMP TABLE " + MODIFIED_ID_TEMP_TABLE_NAME + " AS SELECT " + Task.ProximityAlerts._ID + " FROM " + PROXIMITY_ALERT_TABLE_NAME + " WHERE " + where;
			if (null == whereArgs) {
				mDB.execSQL(tempTableSql);
			} else {
				mDB.execSQL(tempTableSql, whereArgs);
			}

			// ********************************************************
			// Do the update.
			// ********************************************************
			count = mDB.update(PROXIMITY_ALERT_TABLE_NAME, values, where, whereArgs);

			// ********************************************************
			// Setup proximity alert
			// ********************************************************
			if (count > 0) {

				// ********************************************************
				// Set ENABLED flags
				// ********************************************************
				ContentValues proximityAlertCv = new ContentValues(1);

				// ****************************
				// Set ENABLED_TRUE flags
				// ****************************
				proximityAlertCv.put(Task.ProximityAlerts.ENABLED, Task.ProximityAlerts.ENABLED_TRUE);
				int updateCountActive = 0;

				StringBuffer qualifiedWhereActive = new StringBuffer();

				// Wrap entire UPDATE WHERE clause with an IN (..) clause to avoid a "no such column" error for tasks._id (I don't know why).
				qualifiedWhereActive.append(PROXIMITY_ALERT_TABLE_NAME + "." + Task.ProximityAlerts._ID);
				qualifiedWhereActive.append(" IN (");
				qualifiedWhereActive.append("SELECT " + PROXIMITY_ALERT_TABLE_NAME + "." + Task.ProximityAlerts._ID + " FROM ");
				qualifiedWhereActive.append(PROXIMITY_ALERT_TABLE_NAME + " AS p, ");
				qualifiedWhereActive.append(TASK_TABLE_NAME + " AS t, ");
				qualifiedWhereActive.append(ATTACHMENT_TABLE_NAME + " AS a ");
				qualifiedWhereActive.append(" WHERE ");

//				qualifiedWhereActive.append(PROXIMITY_ALERT_TABLE_NAME + "." + Task.ProximityAlerts._ID); 
				qualifiedWhereActive.append("p." + Task.ProximityAlerts._ID);
				qualifiedWhereActive.append(" IN (");
				qualifiedWhereActive.append("SELECT " + MODIFIED_ID_TEMP_TABLE_NAME + "." + Task.ProximityAlerts._ID + " FROM " + MODIFIED_ID_TEMP_TABLE_NAME);
				qualifiedWhereActive.append(") AND ");

				// Join Tasks to Attachments.
				// qualifiedWhereActive.append(TASK_TABLE_NAME);
				qualifiedWhereActive.append("t");
				qualifiedWhereActive.append(".");
				qualifiedWhereActive.append(Task.Tasks._ID);
				qualifiedWhereActive.append("=");
				// qualifiedWhereActive.append(ATTACHMENT_TABLE_NAME);
				qualifiedWhereActive.append("a");
				qualifiedWhereActive.append(".");
				qualifiedWhereActive.append(Task.TaskAttachments.TASK_ID);

				qualifiedWhereActive.append(" AND ");

				// Join Attachments To Proximity Alerts.
				qualifiedWhereActive.append("'");
				qualifiedWhereActive.append(Task.ProximityAlerts.CONTENT_URI_STRING);
				qualifiedWhereActive.append("/' || ");
				// qualifiedWhereActive.append(PROXIMITY_ALERT_TABLE_NAME);
				qualifiedWhereActive.append("p");
				qualifiedWhereActive.append(".");
				qualifiedWhereActive.append(Task.ProximityAlerts._ID);
				qualifiedWhereActive.append("=");
				// qualifiedWhereActive.append(ATTACHMENT_TABLE_NAME);
				qualifiedWhereActive.append("a");
				qualifiedWhereActive.append(".");
				qualifiedWhereActive.append(Task.TaskAttachments._URI);

				qualifiedWhereActive.append(" AND ");

				// Only not-completed tasks.
				// qualifiedWhereActive.append(TASK_TABLE_NAME);
				qualifiedWhereActive.append("t");
				qualifiedWhereActive.append(".");
				qualifiedWhereActive.append(Task.Tasks.COMPLETE);
				qualifiedWhereActive.append("=");
				qualifiedWhereActive.append(Task.Tasks.COMPLETE_FALSE);

				qualifiedWhereActive.append(" AND ");

				// Check if task is archived here.
				// qualifiedWhereActive.append(TASK_TABLE_NAME);
				qualifiedWhereActive.append("t");
				qualifiedWhereActive.append(".");
				qualifiedWhereActive.append(Task.Tasks._ID);
				qualifiedWhereActive.append(" NOT IN (");
				qualifiedWhereActive.append(createRepositoryQuery(Constraint.Version1.REPOSITORY_PARAM_OPTION_ARCHIVE));
				qualifiedWhereActive.append(")");

				qualifiedWhereActive.append(")");

				// Do the update
				updateCountActive = mDB.update(PROXIMITY_ALERT_TABLE_NAME, proximityAlertCv, qualifiedWhereActive.toString(), null);

				// ****************************
				// Set ENABLED_FALSE flags
				// ****************************
				proximityAlertCv.put(Task.ProximityAlerts.ENABLED, Task.ProximityAlerts.ENABLED_FALSE);
				int updateCountNotActive = 0;

				StringBuffer qualifiedWhereNotActive = new StringBuffer();

				// Wrap entire UPDATE WHERE clause with an IN (..) clause to avoid a "no such column" error for tasks._id (I don't know why).
				qualifiedWhereNotActive.append(PROXIMITY_ALERT_TABLE_NAME + "." + Task.ProximityAlerts._ID);
				qualifiedWhereNotActive.append(" IN (");
				qualifiedWhereNotActive.append("SELECT " + PROXIMITY_ALERT_TABLE_NAME + "." + Task.ProximityAlerts._ID + " FROM ");
				qualifiedWhereNotActive.append(PROXIMITY_ALERT_TABLE_NAME + " AS p, ");
				qualifiedWhereNotActive.append(TASK_TABLE_NAME + " AS t, ");
				qualifiedWhereNotActive.append(ATTACHMENT_TABLE_NAME + " AS a ");
				qualifiedWhereNotActive.append(" WHERE ");

				
//				qualifiedWhereNotActive.append(PROXIMITY_ALERT_TABLE_NAME + "."); 
				qualifiedWhereNotActive.append("p.");
				qualifiedWhereNotActive.append(Task.ProximityAlerts._ID);
				qualifiedWhereNotActive.append(" IN (");
				qualifiedWhereNotActive.append("SELECT " + MODIFIED_ID_TEMP_TABLE_NAME + "." + Task.ProximityAlerts._ID + " FROM " + MODIFIED_ID_TEMP_TABLE_NAME);
				qualifiedWhereNotActive.append(") AND ");

				// Join Tasks to Attachments.
				// qualifiedWhereNotActive.append(TASK_TABLE_NAME);
				qualifiedWhereNotActive.append("t");
				qualifiedWhereNotActive.append(".");
				qualifiedWhereNotActive.append(Task.Tasks._ID);
				qualifiedWhereNotActive.append("=");
				// qualifiedWhereNotActive.append(ATTACHMENT_TABLE_NAME);
				qualifiedWhereNotActive.append("a");
				qualifiedWhereNotActive.append(".");
				qualifiedWhereNotActive.append(Task.TaskAttachments.TASK_ID);

				qualifiedWhereNotActive.append(" AND ");

				// Join Attachments To Proximity Alerts.
				qualifiedWhereNotActive.append("'");
				qualifiedWhereNotActive.append(Task.ProximityAlerts.CONTENT_URI_STRING);
				qualifiedWhereNotActive.append("/' || ");
				// qualifiedWhereNotActive.append(PROXIMITY_ALERT_TABLE_NAME);
				qualifiedWhereNotActive.append("p");
				qualifiedWhereNotActive.append(".");
				qualifiedWhereNotActive.append(Task.ProximityAlerts._ID);
				qualifiedWhereNotActive.append("=");
				// qualifiedWhereNotActive.append(ATTACHMENT_TABLE_NAME);
				qualifiedWhereNotActive.append("a");
				qualifiedWhereNotActive.append(".");
				qualifiedWhereNotActive.append(Task.TaskAttachments._URI);

				qualifiedWhereNotActive.append(" AND (");

				// Only not-completed tasks.
				// qualifiedWhereNotActive.append(TASK_TABLE_NAME);
				qualifiedWhereNotActive.append("t");
				qualifiedWhereNotActive.append(".");
				qualifiedWhereNotActive.append(Task.Tasks.COMPLETE);
				qualifiedWhereNotActive.append("=");
				qualifiedWhereNotActive.append(Task.Tasks.COMPLETE_TRUE);

				qualifiedWhereNotActive.append(" OR ");

				// Check if task is archived here.
				// qualifiedWhereNotActive.append(TASK_TABLE_NAME);
				qualifiedWhereNotActive.append("t");
				qualifiedWhereNotActive.append(".");
				qualifiedWhereNotActive.append(Task.Tasks._ID);
				qualifiedWhereNotActive.append(" IN (");
				qualifiedWhereNotActive.append(createRepositoryQuery(Constraint.Version1.REPOSITORY_PARAM_OPTION_ARCHIVE));
				qualifiedWhereNotActive.append("))");

				qualifiedWhereNotActive.append(")");

				// Do the update
				updateCountNotActive = mDB.update(PROXIMITY_ALERT_TABLE_NAME, proximityAlertCv, qualifiedWhereNotActive.toString(), null);

				assert count == updateCountActive + updateCountNotActive;

				// ****************************
				// Query the modified records.
				// ****************************
				SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
				qb.setTables(PROXIMITY_ALERT_TABLE_NAME + " JOIN " + MODIFIED_ID_TEMP_TABLE_NAME + " USING (" + Task.ProximityAlerts._ID + ")");
				Cursor proximityAlertCursor = qb.query(mDB, new String[] { Task.ProximityAlerts._ID, Task.ProximityAlerts._GEO_URI, Task.ProximityAlerts.RADIUS, Task.ProximityAlerts.ENABLED }, null, null, null, null, null);
				assert null != proximityAlertCursor;

				GeoPoint geoPoint = null;
				Uri proximityAlertUri = null;

				// ****************************
				// For each record, update/create or remove the proximity alert.
				// ****************************
				while (proximityAlertCursor.moveToNext()) {

					proximityAlertUri = ContentUris.withAppendedId(Task.ProximityAlerts.CONTENT_URI, proximityAlertCursor.getLong(0));
					assert null != proximityAlertUri;

					if( (values.containsKey(Task.ProximityAlerts.ENABLED) && Task.ProximityAlerts.ENABLED_TRUE.equals(values.getAsString(Task.ProximityAlerts.ENABLED))) || 
							Task.ProximityAlerts.ENABLED_TRUE.equals(proximityAlertCursor.getString(3))){

						if (values.containsKey(Task.ProximityAlerts._GEO_URI)) {
							geoPoint = Util.createPoint(values.getAsString(Task.ProximityAlerts._GEO_URI));
						} else {
							geoPoint = Util.createPoint(proximityAlertCursor.getString(1));
						}
						assert null != geoPoint;

						Nearminder.addOrUpdateProximityAlert(getContext(), geoPoint, values.getAsInteger(Task.ProximityAlerts.RADIUS), proximityAlertUri);
					} else { // Task.ProximityAlerts.ENABLED_FALSE
		                // TODO: !!! Change the Neaminder.java notification code to use mDB directly.
						Nearminder.removeProximityAlert(getContext(), proximityAlertUri);
					}
				}
				proximityAlertCursor.close();
			}

			mDB.setTransactionSuccessful();
		} finally {
			mDB.execSQL("DROP TABLE IF EXISTS " + MODIFIED_ID_TEMP_TABLE_NAME);
			mDB.endTransaction();
		}
		return count;
	}

	/**
     * TODO: ! Add a recovery method which will re-initialize all alarms in case of an error.
	 * 
     * 1. Start transaction
     * 2. Select all ids from the requested set.
     * 3. Perform the requested update 
     * 4. Update the alarm active flag (split update into two parts)
     * 5. Use the list of IDs to register/unregister alarms in AlarmManager.  
     * 6. end transaction
	 */
	private int updateTasksWithAlarmActiveFlagHandlingLogic(SQLiteDatabase mDB, String where, String[] whereArgs,
			ContentValues values) {
		int count;

		mDB.beginTransaction();
		try {
			// ********************************************************
			// Record the modified task IDs for use later.
			// ********************************************************
			String tempTableSql = "CREATE TEMP TABLE " + MODIFIED_ID_TEMP_TABLE_NAME + " AS SELECT " + Task.Tasks._ID + " FROM " + TASK_TABLE_NAME + " WHERE " + where;
			if (null == whereArgs) {
				mDB.execSQL(tempTableSql);
			} else {
				mDB.execSQL(tempTableSql, whereArgs);
			}

			// *****************************
			// Perform the requested update.
			// *****************************
			count = mDB.update(TASK_TABLE_NAME, values, where, whereArgs);

			if (count > 0) {

				long currentTime = System.currentTimeMillis();

				// ****************************
				// Set ALARM_ACTIVE_TRUE flags
				// ****************************
				ContentValues alarmActiveCv = new ContentValues(1);
				alarmActiveCv.put(Task.Tasks.ALARM_ACTIVE, Task.Tasks.ALARM_ACTIVE_TRUE);
				int updateCountActive = 0;

				// If the complete flag will be true
				// or
				// If the alarm time will be null or in the past
				if ((values.containsKey(Task.Tasks.COMPLETE) && Task.Tasks.COMPLETE_TRUE.equals(values.getAsString(Task.Tasks.COMPLETE)))// will
																																			// be
																																			// ...
						|| (values.containsKey(Task.Tasks.ALARM_TIME) && (null == values.get(Task.Tasks.ALARM_TIME) || values.getAsLong(Task.Tasks.ALARM_TIME) <= currentTime))) {
					// No need to update database with ALARM_ACTIVE_TRUE
				} else {

					StringBuffer qualifiedWhereActive = new StringBuffer();
					qualifiedWhereActive.append(Task.Tasks._ID);
					qualifiedWhereActive.append(" IN (");
					qualifiedWhereActive.append("SELECT " + Task.Tasks._ID + " FROM " + MODIFIED_ID_TEMP_TABLE_NAME);
					qualifiedWhereActive.append(") AND ");
					qualifiedWhereActive.append(Task.Tasks.COMPLETE);
					qualifiedWhereActive.append("=");
					qualifiedWhereActive.append(Task.Tasks.COMPLETE_FALSE);
					qualifiedWhereActive.append(" AND ");
					qualifiedWhereActive.append(Task.Tasks.ALARM_TIME);
					qualifiedWhereActive.append(" > ");
					qualifiedWhereActive.append(currentTime);
					qualifiedWhereActive.append(" AND ");
					// Check if task is _not_ archived here.
					qualifiedWhereActive.append(Task.Tasks._ID);
					qualifiedWhereActive.append(" NOT IN (");
					qualifiedWhereActive.append(createRepositoryQuery(Constraint.Version1.REPOSITORY_PARAM_OPTION_ARCHIVE));
					qualifiedWhereActive.append(")");

					updateCountActive = mDB.update(TASK_TABLE_NAME, alarmActiveCv, qualifiedWhereActive.toString(), null);
				}

				// ****************************
				// Set ALARM_ACTIVE_FALSE flags
				// ****************************
				alarmActiveCv.put(Task.Tasks.ALARM_ACTIVE, Task.Tasks.ALARM_ACTIVE_FALSE);

				int updateCountNotActive = 0;
				StringBuffer qualifiedWhereNotActive = new StringBuffer();
				qualifiedWhereNotActive.append(Task.Tasks._ID);
				qualifiedWhereNotActive.append(" IN (");
				qualifiedWhereNotActive.append("SELECT " + Task.Tasks._ID + " FROM " + MODIFIED_ID_TEMP_TABLE_NAME);
				qualifiedWhereNotActive.append(") AND (");
				if (values.containsKey(Task.Tasks.COMPLETE) && Task.Tasks.COMPLETE_TRUE.equals(values.getAsString(Task.Tasks.COMPLETE))) {
					qualifiedWhereNotActive.append(Task.Tasks.COMPLETE);
					qualifiedWhereNotActive.append("=");
					qualifiedWhereNotActive.append(Task.Tasks.COMPLETE_TRUE);
					qualifiedWhereNotActive.append(" OR ");
				}
				if (values.containsKey(Task.Tasks.ALARM_TIME) && (null == values.get(Task.Tasks.ALARM_TIME) || values.getAsLong(Task.Tasks.ALARM_TIME) <= currentTime)) {
					qualifiedWhereNotActive.append(Task.Tasks.ALARM_TIME);
					qualifiedWhereNotActive.append(" IS NULL ");
					qualifiedWhereNotActive.append(" OR ");
					qualifiedWhereNotActive.append(Task.Tasks.ALARM_TIME);
					qualifiedWhereNotActive.append(" <= ");
					qualifiedWhereNotActive.append(currentTime);
					qualifiedWhereNotActive.append(" OR ");
				}
				// Check if task is archived here.
				qualifiedWhereNotActive.append(Task.Tasks._ID);
				qualifiedWhereNotActive.append(" IN (");
				qualifiedWhereNotActive.append(createRepositoryQuery(Constraint.Version1.REPOSITORY_PARAM_OPTION_ARCHIVE));
				qualifiedWhereNotActive.append("))");

				updateCountNotActive = mDB.update(TASK_TABLE_NAME, alarmActiveCv, qualifiedWhereNotActive.toString(), null);

				// ****************************
				// Register/Unregister alarm from Alarm Manager.
				// ****************************
				Cursor taskCursor = mDB.query(TASK_TABLE_NAME + " JOIN " + MODIFIED_ID_TEMP_TABLE_NAME + " USING (" + Task.Tasks._ID + ")", new String[] { Task.Tasks.ALARM_ACTIVE, Task.Tasks._ID, Task.Tasks.ALARM_TIME }, null, null, null, null, null);
				assert null != taskCursor;
				// assert taskCursor.getCount() == updateCountNotActive +
				// updateCountActive; // False assumption. Some alarm_active
				// flags may not change even after an update.
				assert count >= updateCountNotActive + updateCountActive;
				assert taskCursor.getCount() == count;

				while (taskCursor.moveToNext()) {
					if (Task.Tasks.ALARM_ACTIVE_TRUE.equals(taskCursor.getString(0))) {
						registerAlarm(getContext(), taskCursor.getLong(2), ContentUris.withAppendedId(Task.Tasks.CONTENT_URI, taskCursor.getLong(1)));
					} else {
						unregisterAlarm(getContext(), ContentUris.withAppendedId(Task.Tasks.CONTENT_URI, taskCursor.getLong(1)));
					}
				}
				taskCursor.close();

				//**************************************************************
				// ********
				// Activate/Deactivate Proximity alert(s).
				//**************************************************************
				// ********
				ContentValues cvProximityAlert = new ContentValues();

				// ******************************
				// Prepare SQL
				// ******************************
				StringBuffer proximityAlertBaseWhere = new StringBuffer();

				proximityAlertBaseWhere.append(Task.ProximityAlerts._ID);
				proximityAlertBaseWhere.append(" IN ( SELECT ");
				proximityAlertBaseWhere.append(PROXIMITY_ALERT_TABLE_NAME);
				proximityAlertBaseWhere.append(".");
				proximityAlertBaseWhere.append(Task.ProximityAlerts._ID);
				proximityAlertBaseWhere.append(" FROM ");
				proximityAlertBaseWhere.append(PROXIMITY_ALERT_TABLE_NAME + "," + TASK_TABLE_NAME + "," + ATTACHMENT_TABLE_NAME);
				proximityAlertBaseWhere.append(" WHERE ");

				proximityAlertBaseWhere.append(TASK_TABLE_NAME);
				proximityAlertBaseWhere.append(".");
				proximityAlertBaseWhere.append(Task.Tasks._ID);
				proximityAlertBaseWhere.append(" IN (");
				proximityAlertBaseWhere.append("SELECT " + Task.Tasks._ID + " FROM " + MODIFIED_ID_TEMP_TABLE_NAME);
				proximityAlertBaseWhere.append(")");

				proximityAlertBaseWhere.append(" AND ");

				// Join Tasks to Attachments.
				proximityAlertBaseWhere.append(TASK_TABLE_NAME);
				proximityAlertBaseWhere.append(".");
				proximityAlertBaseWhere.append(Task.Tasks._ID);
				proximityAlertBaseWhere.append("=");
				proximityAlertBaseWhere.append(ATTACHMENT_TABLE_NAME);
				proximityAlertBaseWhere.append(".");
				proximityAlertBaseWhere.append(Task.TaskAttachments.TASK_ID);

				proximityAlertBaseWhere.append(" AND ");

				// Join Attachments To Proximity Alerts.
				proximityAlertBaseWhere.append("'");
				proximityAlertBaseWhere.append(Task.ProximityAlerts.CONTENT_URI_STRING);
				proximityAlertBaseWhere.append("/' || ");
				proximityAlertBaseWhere.append(PROXIMITY_ALERT_TABLE_NAME);
				proximityAlertBaseWhere.append(".");
				proximityAlertBaseWhere.append(Task.ProximityAlerts._ID);
				proximityAlertBaseWhere.append("=");
				proximityAlertBaseWhere.append(ATTACHMENT_TABLE_NAME);
				proximityAlertBaseWhere.append(".");
				proximityAlertBaseWhere.append(Task.TaskAttachments._URI);

				// ******************************
				// Update Task.ProximityAlerts.ENABLED to ENABLED_TRUE
				// ******************************

				// If the complete flag will be true
				if (values.containsKey(Task.Tasks.COMPLETE) && Task.Tasks.COMPLETE_TRUE.equals(values.getAsString(Task.Tasks.COMPLETE))) {
					// No need to update database with
					// ProximityAlerts.ENABLED_TRUE
				} else {
					cvProximityAlert.put(Task.ProximityAlerts.ENABLED, Task.ProximityAlerts.ENABLED_TRUE);

					StringBuffer proximityAlertActiveWhere = new StringBuffer();

					proximityAlertActiveWhere.append(" AND ");

					proximityAlertActiveWhere.append(TASK_TABLE_NAME);
					proximityAlertActiveWhere.append(".");
					proximityAlertActiveWhere.append(Task.Tasks.COMPLETE);
					proximityAlertActiveWhere.append("=");
					proximityAlertActiveWhere.append(Task.Tasks.COMPLETE_FALSE);

					proximityAlertActiveWhere.append(" AND ");

					// Check if task is _not_ archived here.
					proximityAlertActiveWhere.append(TASK_TABLE_NAME);
					proximityAlertActiveWhere.append(".");
					proximityAlertActiveWhere.append(Task.Tasks._ID);
					proximityAlertActiveWhere.append(" NOT IN (");
					proximityAlertActiveWhere.append(createRepositoryQuery(Constraint.Version1.REPOSITORY_PARAM_OPTION_ARCHIVE));
					proximityAlertActiveWhere.append(")");

					proximityAlertActiveWhere.append(")");

					mDB.update(PROXIMITY_ALERT_TABLE_NAME, cvProximityAlert, proximityAlertBaseWhere.toString() + proximityAlertActiveWhere.toString(), null);
				}

				// ******************************
				// Update Task.ProximityAlerts.ENABLED to ENABLED_TRUE
				// ******************************
				cvProximityAlert.put(Task.ProximityAlerts.ENABLED, Task.ProximityAlerts.ENABLED_FALSE);

				StringBuffer proximityAlertNotActiveWhere = new StringBuffer();

				proximityAlertNotActiveWhere.append(" AND ");

				proximityAlertNotActiveWhere.append("(");

				proximityAlertNotActiveWhere.append(TASK_TABLE_NAME);
				proximityAlertNotActiveWhere.append(".");
				proximityAlertNotActiveWhere.append(Task.Tasks.COMPLETE);
				proximityAlertNotActiveWhere.append("=");
				proximityAlertNotActiveWhere.append(Task.Tasks.COMPLETE_TRUE);

				proximityAlertNotActiveWhere.append(" OR ");

				// Check if task is _not_ archived here.
				proximityAlertNotActiveWhere.append(TASK_TABLE_NAME);
				proximityAlertNotActiveWhere.append(".");
				proximityAlertNotActiveWhere.append(Task.Tasks._ID);
				proximityAlertNotActiveWhere.append(" IN (");
				proximityAlertNotActiveWhere.append(createRepositoryQuery(Constraint.Version1.REPOSITORY_PARAM_OPTION_ARCHIVE));
				proximityAlertNotActiveWhere.append(")");

				proximityAlertNotActiveWhere.append("))");

				mDB.update(PROXIMITY_ALERT_TABLE_NAME, cvProximityAlert, proximityAlertBaseWhere.toString() + proximityAlertNotActiveWhere.toString(), null);

				// ******************************
				// Add/Remove the proximity alert from the LocationManager
				// ******************************
				Cursor proximityAlertCursor = mDB.query(PROXIMITY_ALERT_TABLE_NAME, new String[] { Task.ProximityAlerts._ID, Task.ProximityAlerts.ENABLED, Task.ProximityAlerts._GEO_URI, Task.ProximityAlerts.RADIUS }, proximityAlertBaseWhere.toString() + ")", null, null, null, null);
				assert null != proximityAlertCursor;
				while (proximityAlertCursor.moveToNext()) {
					Uri proximityAlertUri = ContentUris.withAppendedId(Task.ProximityAlerts.CONTENT_URI, proximityAlertCursor.getLong(0));
					if (Task.ProximityAlerts.ENABLED_TRUE.equals(proximityAlertCursor.getString(1))) {
						GeoPoint geoPoint = Util.createPoint(proximityAlertCursor.getString(2));
						assert null != geoPoint;
						Nearminder.addOrUpdateProximityAlert(getContext(), geoPoint, proximityAlertCursor.getInt(3), proximityAlertUri);
					} else {
						Nearminder.removeProximityAlert(getContext(), proximityAlertUri);
					}
				}
				proximityAlertCursor.close();

			}

			mDB.setTransactionSuccessful();
		} finally {
			mDB.execSQL("DROP TABLE IF EXISTS " + MODIFIED_ID_TEMP_TABLE_NAME);
			mDB.endTransaction();
		}

		return count;
	}

	/**
	 * Currently only works for archived (or all non-archived) tasks.
	 * 
	 * @param repository
	 * @return
	 */
	private StringBuffer createRepositoryQuery(String repository) {
		StringBuffer archiveQuery = new StringBuffer();

		archiveQuery.append("SELECT ");
		archiveQuery.append(TASK_TABLE_NAME + "." + Task.Tasks._ID);
		archiveQuery.append(" FROM ");
		archiveQuery.append(TASK_TABLE_NAME);
		archiveQuery.append(" WHERE ");
		archiveQuery.append("'");
		archiveQuery.append(Task.Tasks.CONTENT_URI_STRING);
		archiveQuery.append("/' || ");
		archiveQuery.append(TASK_TABLE_NAME + "." + Task.Tasks._ID);
		if (!Constraint.Version1.REPOSITORY_PARAM_OPTION_ARCHIVE.equals(repository)) {
			archiveQuery.append(" NOT");
		}
		archiveQuery.append(" IN (SELECT ");
		archiveQuery.append(Task.LabeledContentColumns._CONTENT_URI);
		archiveQuery.append(" FROM ");
		archiveQuery.append(LABELED_CONTENT_TABLE_NAME);
		archiveQuery.append(" WHERE ");
		archiveQuery.append(Task.LabeledContentColumns._LABEL_ID);
		archiveQuery.append("=");
		archiveQuery.append(ID_LABEL_ARCHIVED);
		archiveQuery.append(" AND ");
		archiveQuery.append(Task.LabeledContentColumns._CONTENT_URI);
		archiveQuery.append(" LIKE '");
		archiveQuery.append(Task.Tasks.CONTENT_URI_STRING);
		archiveQuery.append("/%')");
		return archiveQuery;
	}

	/**
	 * @param mDB
	 * @param where2
	 * @param whereParams
	 * @return
	 */
	private int archiveTasks(SQLiteDatabase mDB, String where2, String[] whereParams) {
		int count2 = 0;
		mDB.beginTransaction();
		try {
			Cursor archiveableTasksCursor = mDB.query(
					TASK_TABLE_NAME, 
					new String[]{Task.Tasks._ID}, 
					where2, 
					whereParams, 
					null,
					null,
					Task.Tasks._ID);

			Uri insertLabeledContentUri = Uri.parse("content://" + Task.AUTHORITY + "/labels/" + TaskProvider.ID_LABEL_ARCHIVED + "/labeled_content");
			if (archiveableTasksCursor.getCount() > 0) {
				ContentValues cv = new ContentValues();

				// **********************************************************************
				// For each task:
				// - Add a LabeledContentColumns record
				// - Unregister Alarm
				// - Deactivate Alarm in DB.
				// - Deactiveate Proximity Alert in DB
				// - Unregister Proximity Alent Location Manager.
				// **********************************************************************
				while (archiveableTasksCursor.moveToNext()) {
					cv.clear();
					Uri taskUri = ContentUris.withAppendedId(Task.Tasks.CONTENT_URI, archiveableTasksCursor.getLong(0));
					cv.put(Task.LabeledContentColumns._CONTENT_URI, taskUri.toString());
					Uri resultUri = insertLabledContent(mDB, cv, insertLabeledContentUri, null);
					assert null != resultUri;

					// ******************************
					// Unregister alarm from AlarmMananger
					// ******************************
					unregisterAlarm(getContext(), taskUri);

					// ******************************
					// Deactivate alarm in database
					// ******************************
					ContentValues cvAlarm = new ContentValues();
					cvAlarm.put(Task.TasksColumns.ALARM_ACTIVE, Task.TasksColumns.ALARM_ACTIVE_FALSE);
					int resultCount = mDB.update(TASK_TABLE_NAME, cvAlarm, Task.Tasks._ID + "=?", new String[] { archiveableTasksCursor.getString(0) });
					assert 1 == resultCount;

					// **********************************************************************
					// Deactivate Proximity alert(s).
					// **********************************************************************

					String proximityAlertWhere = proximityAlertQuery(archiveableTasksCursor.getLong(0));

					// ******************************
					// Update Task.ProximityAlerts.ENABLED to ENABLED_FALSE
					// ******************************
					ContentValues cvProximityAlert = new ContentValues();
					cvProximityAlert.put(Task.ProximityAlerts.ENABLED, Task.ProximityAlerts.ENABLED_FALSE);

					mDB.update(PROXIMITY_ALERT_TABLE_NAME, cvProximityAlert, proximityAlertWhere, null);

					// ******************************
					// Remove the proximity alert from the LocationManager
					// ******************************
					Cursor proximityAlertCursor = mDB.query(PROXIMITY_ALERT_TABLE_NAME, new String[] { Task.ProximityAlerts._ID }, proximityAlertWhere, null, null, null, null);
					assert null != proximityAlertCursor;
					while (proximityAlertCursor.moveToNext()) {
						Uri proximityAlertUri = ContentUris.withAppendedId(Task.ProximityAlerts.CONTENT_URI, proximityAlertCursor.getLong(0));
						Nearminder.removeProximityAlert(getContext(), proximityAlertUri);
					}
					proximityAlertCursor.close();

					count2++;
				}
				assert count2 == archiveableTasksCursor.getCount();
				if (count2 != archiveableTasksCursor.getCount()) {
					// TODO: ! Report error.
					Log.e(TAG, "Failed to insert archive labels.");
				}
			}
			archiveableTasksCursor.close();

			mDB.setTransactionSuccessful();
		} finally {
			mDB.endTransaction();
		}
		return count2;
	}

	/**
	 * @param mDB
	 * @param where2
	 * @param whereParams
	 * @return
	 */
	private int unarchiveTasks(SQLiteDatabase mDB, String where2, String[] whereParams) {
		int count2 = 0;
		int count = 0;
		mDB.beginTransaction();
		try {
			Cursor unarchiveableTasksCursor = mDB.query(
					TASK_TABLE_NAME, 
					new String[]{Task.Tasks._ID, Task.Tasks.ALARM_ACTIVE, Task.Tasks.ALARM_TIME, Task.Tasks.COMPLETE}, 
					where2, 
					whereParams, 
					null,
					null,
					Task.Tasks._ID);

			Uri insertLabeledContentUri = Uri.parse("content://" + Task.AUTHORITY + "/labels/" + TaskProvider.ID_LABEL_ARCHIVED + "/labeled_content");
			if (unarchiveableTasksCursor.getCount() > 0) {
				ContentValues cv = new ContentValues();

				// **********************************************************************
				// For each task:
				// - Remove the LabeledContentColumns record
				// - Register Alarm (if needed)
				// - Activate Alarm in DB (if needed)
				// - Activeate Proximity Alert in DB (if needed)
				// - Register Proximity Alent Location Manager (if needed)
				// **********************************************************************
				while (unarchiveableTasksCursor.moveToNext()) {
					cv.clear();

					// Remove the LabeledContentColumns record
					Uri taskUri = ContentUris.withAppendedId(Task.Tasks.CONTENT_URI, unarchiveableTasksCursor.getLong(0));
					count = mDB.delete(LABELED_CONTENT_TABLE_NAME, 
							Task.LabeledContentColumns._CONTENT_URI+"=? AND " + Task.LabeledContent._LABEL_ID + "=?", 
							new String[]{taskUri.toString(), TaskProvider.ID_LABEL_ARCHIVED});
					if (count == 0) {
						// TODO: !!! Handle error.
						// Event.onEvent(name)
					}
					count2 += count;

					if (unarchiveableTasksCursor.getInt(3) == Task.Tasks.COMPLETE_FALSE_INT) {

						if (!unarchiveableTasksCursor.isNull(2) && System.currentTimeMillis() < unarchiveableTasksCursor.getLong(2)) {
							// ******************************
							// Activate alarm in database
							// ******************************
							ContentValues cvAlarm = new ContentValues();
							cvAlarm.put(Task.TasksColumns.ALARM_ACTIVE, Task.TasksColumns.ALARM_ACTIVE_TRUE);
							count = mDB.update(TASK_TABLE_NAME, cvAlarm, Task.Tasks._ID + "=?", new String[] { unarchiveableTasksCursor.getString(0) });
							assert 1 == count;

							if (count == 0) {
								// TODO: !!! Handle error.
								// Event.onEvent(name)
							} else {
								// ******************************
								// Register alarm from AlarmMananger
								// ******************************
								registerAlarm(getContext(), unarchiveableTasksCursor.getLong(2), taskUri);
							}
						}

						
						
						// **********************************************************************
						// Activate Proximity alert(s).
						// **********************************************************************

						// ******************************
						// Prepare SQL
						// ******************************
						String proximityAlertWhere = proximityAlertQuery(unarchiveableTasksCursor.getLong(0));

						// ******************************
						// Update Task.ProximityAlerts.ENABLED to ENABLED_TRUE
						// ******************************
						ContentValues cvProximityAlert = new ContentValues();
						cvProximityAlert.put(Task.ProximityAlerts.ENABLED, Task.ProximityAlerts.ENABLED_TRUE);

						mDB.update(PROXIMITY_ALERT_TABLE_NAME, cvProximityAlert, proximityAlertWhere, null);

						// ******************************
						// Add the proximity alert to the LocationManager
						// ******************************

						Cursor proximityAlertCursor = mDB.query(PROXIMITY_ALERT_TABLE_NAME, new String[] { Task.ProximityAlerts._ID, Task.ProximityAlerts._GEO_URI, Task.ProximityAlerts.RADIUS }, proximityAlertWhere, null, null, null, null);
						assert null != proximityAlertCursor;
						while (proximityAlertCursor.moveToNext()) {
							Uri proximityAlertUri = ContentUris.withAppendedId(Task.ProximityAlerts.CONTENT_URI, proximityAlertCursor.getLong(0));
							GeoPoint geoPoint = Util.createPoint(proximityAlertCursor.getString(1));
							assert null != geoPoint;

							Nearminder.addOrUpdateProximityAlert(getContext(), geoPoint, proximityAlertCursor.getInt(2), proximityAlertUri);
						}
						proximityAlertCursor.close();

					}

				}
			}
			unarchiveableTasksCursor.close();

			mDB.setTransactionSuccessful();
		} finally {
			mDB.endTransaction();
		}
		return count2;
	}

	private String proximityAlertQuery(long taskId) {
		StringBuffer proximityAlertWhere = new StringBuffer();

		proximityAlertWhere.append(Task.ProximityAlerts._ID);
		proximityAlertWhere.append(" IN ( SELECT ");
		proximityAlertWhere.append(PROXIMITY_ALERT_TABLE_NAME);
		proximityAlertWhere.append(".");
		proximityAlertWhere.append(Task.ProximityAlerts._ID);
		proximityAlertWhere.append(" FROM ");
		proximityAlertWhere.append(PROXIMITY_ALERT_TABLE_NAME + "," + TASK_TABLE_NAME + "," + ATTACHMENT_TABLE_NAME);
		proximityAlertWhere.append(" WHERE ");

		proximityAlertWhere.append(TASK_TABLE_NAME);
		proximityAlertWhere.append(".");
		proximityAlertWhere.append(Task.Tasks._ID);
		proximityAlertWhere.append(" = ");
		proximityAlertWhere.append(String.valueOf(taskId));

		proximityAlertWhere.append(" AND ");

		// Join Tasks to Attachments.
		proximityAlertWhere.append(TASK_TABLE_NAME);
		proximityAlertWhere.append(".");
		proximityAlertWhere.append(Task.Tasks._ID);
		proximityAlertWhere.append("=");
		proximityAlertWhere.append(ATTACHMENT_TABLE_NAME);
		proximityAlertWhere.append(".");
		proximityAlertWhere.append(Task.TaskAttachments.TASK_ID);

		proximityAlertWhere.append(" AND ");

		// Join Attachments To Proximity Alerts.
		proximityAlertWhere.append("'");
		proximityAlertWhere.append(Task.ProximityAlerts.CONTENT_URI_STRING);
		proximityAlertWhere.append("/' || ");
		proximityAlertWhere.append(PROXIMITY_ALERT_TABLE_NAME);
		proximityAlertWhere.append(".");
		proximityAlertWhere.append(Task.ProximityAlerts._ID);
		proximityAlertWhere.append("=");
		proximityAlertWhere.append(ATTACHMENT_TABLE_NAME);
		proximityAlertWhere.append(".");
		proximityAlertWhere.append(Task.TaskAttachments._URI);

		proximityAlertWhere.append(")");
		return proximityAlertWhere.toString();
	}

	/**
	 */
	public static void registerAlarm(Context context, long time, Uri taskUri) {
		AlarmManager aManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		registerAlarm(context, time, taskUri, aManager);
	}

	/**
	 * Register an intent with the AlarmMananger.
	 */
	public static void registerAlarm(Context context, long time, Uri taskUri, AlarmManager aManager) {
		Log.v(TAG, "registerAlarm(..) called.");
		assert null != aManager;

		NotificationUtil.removeNotification(context, taskUri); // Remove existing alarm, if any. 

		Intent alarmIntent = new Intent(TaskEditorAlarmIntentReceiver.ALARM_ACTION, taskUri);

		PendingIntent pIntent = PendingIntent.getBroadcast(context, -1, alarmIntent, 0);
		assert pIntent != null;
		aManager.set(AlarmManager.RTC_WAKEUP, time, pIntent); // USER_REPORTED_BUG: (Unconfirmed) This alarm doesn't always result in the Intent receiver receiving an intent. May be delayed by some period of time or be missed if the process has been killed.
	}

	/**
	 * UnRegister an intent with the AlarmMananger.
	 * Also, remove any notification that exists for this alarm.
	 */
	public static void unregisterAlarm(Context context, Uri taskUri) {

		NotificationUtil.removeNotification(context, taskUri);

		AlarmManager aManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		Intent alarmIntent = new Intent(TaskEditorAlarmIntentReceiver.ALARM_ACTION, taskUri);
		PendingIntent pIntent = PendingIntent.getBroadcast(context, -1, alarmIntent, PendingIntent.FLAG_NO_CREATE);
		if (null != pIntent) {
			aManager.cancel(pIntent);
		} else {
			// Unregistering an alarm that is not registered is not an error.
		}
	}

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
		try {
			switch (URL_MATCHER.match(uri)) {
				case TEMP_FILE_ID:
					Cursor cursor = query(
						uri, 
						new String[]{Task.TempFile._ID}, 
						null, 
						null, 
						Task.TempFile._ID);
					assert null != cursor;
					try {
						if (!cursor.moveToFirst()) {
							Log.i(TAG, "File not found");
							throw new FileNotFoundException();
						}
						try {
							File theFile = new File(getTempDir(getContext()) + "/" + cursor.getString(0));
							Log.v(TAG, "theFile==" + theFile.toString());
							int theMode; // TODO: ! Reconsider whether you want to allow writable mode.
							if ("r".equals(mode)) {
								theMode = ParcelFileDescriptor.MODE_READ_ONLY;
							} else if ("rw".equals(mode)) {
								theMode = ParcelFileDescriptor.MODE_READ_WRITE;
							} else if ("w".equals(mode)) {
								theMode = ParcelFileDescriptor.MODE_WRITE_ONLY;
							} else {
								throw new IllegalArgumentException("Must specify 'r', 'w', or 'rw'");
							}

							ParcelFileDescriptor pfd = ParcelFileDescriptor.open(theFile, theMode);
							assert null != pfd;

							return pfd;
						} catch (FileNotFoundException e) {
							Log.e(TAG, "ERR00098 File not found. ", e);
							ErrorUtil.handleException("ERR00098", e, getContext());
							throw new FileNotFoundException();
						}
					} finally {
						cursor.close();
					}
					// break;
				default:
					Log.e(TAG, "openFile not supported for uri " + uri);
					ErrorUtil.handle("ERR0009A", "openFile not supported for uri " + uri, this);
			}
		} catch (FileNotFoundException fnfe) { // Ignore.
			throw fnfe;
		} catch (HandledException h) { // Ignore.
		} catch (Exception exp) {
			Log.e(TAG, "ERR00099", exp);
			ErrorUtil.handleException("ERR00099", exp, getContext());
		}
		throw new IllegalArgumentException("openFile not supported for uri " + uri);
	}

	// TODO: !!! Extract these uri paths into constants so we know where they are being used (and to get a little re-use).
	// TODO: !!! Provide 2 constant string values, one for the actual path used below, and one which contains ? replacement tokens, and maybe even a utility method for filling it out. 
	static {
		URL_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URL_MATCHER.addURI(Task.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
		URL_MATCHER.addURI(Task.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
        URL_MATCHER.addURI(Task.AUTHORITY, "search_suggest_shortcut",   SHORTCUT_REFRESH); // TODO: Replace with SearchManager.SUGGEST_URI_PATH_SHORTCUT when android:minSdkVersion is moved to >= 4. 
        URL_MATCHER.addURI(Task.AUTHORITY, "search_suggest_shortcut/*", SHORTCUT_REFRESH); // TODO: Replace with SearchManager.SUGGEST_URI_PATH_SHORTCUT when android:minSdkVersion is moved to >= 4. 
		URL_MATCHER.addURI(Task.AUTHORITY, "tasks", TASKS);
		URL_MATCHER.addURI(Task.AUTHORITY, "tasks/#", TASK_ID);
		URL_MATCHER.addURI(Task.AUTHORITY, "tasks/#,*", MULTI_TASK_ID);

		URL_MATCHER.addURI(Task.AUTHORITY, "attachments", ATTACHMENTS);
		URL_MATCHER.addURI(Task.AUTHORITY, "attachments/#", ATTACHMENT_ID);
		URL_MATCHER.addURI(Task.AUTHORITY, "proximity", PROXIMITY_ALERTS);
		URL_MATCHER.addURI(Task.AUTHORITY, "proximity/#", PROXIMITY_ALERT_ID);
        URL_MATCHER.addURI(Task.AUTHORITY, "tasks/#/attachments/#/proximity/#", TASK_ATTACHMENT_PROXIMITY_ALERT_ID); // Queries only.
        URL_MATCHER.addURI(Task.AUTHORITY, "tasks/#/attachments/#", TASK_ATTACHMENT_ID); // Queries only.
        URL_MATCHER.addURI(Task.AUTHORITY, "proximity/#/tasks/attachments", PROXIMITY_ALERT_TASK_ATTACHMENT_ID); // Queries only.

		URL_MATCHER.addURI(Task.AUTHORITY, "labels", LABELS);
		URL_MATCHER.addURI(Task.AUTHORITY, "labels/#", LABEL_ID);
		URL_MATCHER.addURI(Task.AUTHORITY, "labels/#/labeled_content", LABELS_LABELED_CONTENT);
		URL_MATCHER.addURI(Task.AUTHORITY, "labels/#/labeled_content/#", LABELS_LABELED_CONTENT_ID);
		URL_MATCHER.addURI(Task.AUTHORITY, "labeled_content", LABELED_CONTENT);
		URL_MATCHER.addURI(Task.AUTHORITY, "labeled_content/#", LABELED_CONTENT_ID);
		// Supported for Query only.
		URL_MATCHER.addURI(Task.AUTHORITY, "labels/#/tasks", LABELS_TASKS);
		// Supported for Query only.
		URL_MATCHER.addURI(Task.AUTHORITY, "labels/#/tasks/#", LABELS_TASKS_ID);
		// Supported for Query only.
		URL_MATCHER.addURI(Task.AUTHORITY, "tasks/#/labels", TASKS_LABELS);
		// Supported for Query only.
		URL_MATCHER.addURI(Task.AUTHORITY, "tasks/#/labels/#", TASKS_LABELS_ID);
		// Need
        //URL_MATCHER.addURI(Task.AUTHORITY, "labeled_content/tasks", LABELED_CONTENT_TASKS);
		URL_MATCHER.addURI(Task.AUTHORITY, "filters", FILTERS);
		URL_MATCHER.addURI(Task.AUTHORITY, "filters/#", FILTERS_ID);
        URL_MATCHER.addURI(Task.AUTHORITY, "switch_filter/#", SWITCH_FILTER_ID); // Updates only.
        URL_MATCHER.addURI(Task.AUTHORITY, "switch_to_permanent_filter/#", SWITCH_TO_PERMANENT_FILTER_ID); // Updates only.

//        URL_MATCHER.addURI(Task.AUTHORITY, "switch_default_category/#", SWITCH_DEFAULT_CATEGORY_ID); // Updates only.

        URL_MATCHER.addURI(Task.AUTHORITY, "selected_filter/filter_elements", ACTIVE_FILTER_FILTER_ELEMENTS); // Queries only.
        URL_MATCHER.addURI(Task.AUTHORITY, "filters/#/filter_elements", FILTER_FILTER_ELEMENTS); // Queries only.
//        URL_MATCHER.addURI(Task.AUTHORITY, "filters/#/filter_elements/labels", FILTER_FILTER_ELEMENTS_LABELS);
		URL_MATCHER.addURI(Task.AUTHORITY, "filter_elements", FILTER_ELEMENTS);
		URL_MATCHER.addURI(Task.AUTHORITY, "filter_elements/#", FILTER_ELEMENTS_ID);

        URL_MATCHER.addURI(Task.AUTHORITY, "tasks/callminder", TASKS_CALLMINDER); // Only supports queries

		URL_MATCHER.addURI(Task.AUTHORITY, "user_applied_label", USER_APPLIED_LABEL);
		URL_MATCHER.addURI(Task.AUTHORITY, "user_applied_label/#", USER_APPLIED_LABEL_ID);

		URL_MATCHER.addURI(Task.AUTHORITY, "temp_file", TEMP_FILES);
		URL_MATCHER.addURI(Task.AUTHORITY, "temp_file/#", TEMP_FILE_ID);

		// TODO: Explain the behavior of these and other special URIs.
        URL_MATCHER.addURI(Task.AUTHORITY, "archive_tasks", ARCHIVED_TASKS); // Only update and delete supported. 
        URL_MATCHER.addURI(Task.AUTHORITY, "archive_tasks/#", ARCHIVED_TASKS_ID); // Only update and query supported. update will apply the archive label. query returns the task record it exists and it is archived. 

		// TODO: Explain the behavior of these and other special URIs.
        URL_MATCHER.addURI(Task.AUTHORITY, "unarchive_tasks", UNARCHIVED_TASKS); // Only update supported. 
        URL_MATCHER.addURI(Task.AUTHORITY, "unarchive_tasks/#", UNARCHIVED_TASKS_ID); // Only update supported.

		// TODO: Explain the behavior of these and other special URIs.
        URL_MATCHER.addURI(Task.AUTHORITY, "filter_bits", FILTER_BITS);      // Only update supported. 
        URL_MATCHER.addURI(Task.AUTHORITY, "filter_bits/#", FILTER_BITS_ID); // Only update supported.

		URL_MATCHER.addURI(Task.AUTHORITY, "notification", NOTIFICATIONS);
		URL_MATCHER.addURI(Task.AUTHORITY, "notification/#", NOTIFICATION_ID);

		URL_MATCHER.addURI(Task.AUTHORITY, "completable", COMPLETABLE);
		URL_MATCHER.addURI(Task.AUTHORITY, "completable/#", COMPLETABLE_ID);

//        URL_MATCHER.addURI(Task.AUTHORITY, "tasks/"+Constraint.CONSTRAINT+"/"+Constraint.Version1.VERSION+"/"+Constraint.Version1.DUE+"/"+Constraint.Version1.DAYS_FROM_TODAYS_END+"/#",      MASK_DAYS_FROM_TODAYS_END);
//        URL_MATCHER.addURI(Task.AUTHORITY, "tasks/"+Constraint.CONSTRAINT+"/"+Constraint.Version1.VERSION+"/"+Constraint.Version1.DUE+"/"+Constraint.Version1.WEEKS_FROM_THIS_WEEKS_END+"/#", MASK_WEEKS_FROM_THIS_WEEKS_END);

		URL_MATCHER.addURI(Task.AUTHORITY, "tasks/" + Constraint.CONSTRAINT + "/" + Constraint.Version1.VERSION + "/" + Constraint.Version1.DUE, MASK_DUE);
		URL_MATCHER.addURI(Task.AUTHORITY, "tasks/" + Constraint.CONSTRAINT + "/" + Constraint.Version1.VERSION + "/" + Constraint.Version1.PRIORITY, MASK_PRIORITY);
		URL_MATCHER.addURI(Task.AUTHORITY, "tasks/" + Constraint.CONSTRAINT + "/" + Constraint.Version1.VERSION + "/" + Constraint.Version1.STATUS, MASK_STATUS);
		URL_MATCHER.addURI(Task.AUTHORITY, "tasks/" + Constraint.CONSTRAINT + "/" + Constraint.Version1.VERSION + "/" + Constraint.Version1.REPOSITORY, MASK_REPOSITORY);
		URL_MATCHER.addURI(Task.AUTHORITY, "tasks/" + Constraint.CONSTRAINT + "/" + Constraint.Version1.VERSION + "/" + Constraint.Version1.LABEL + "/#", MASK_LABEL);
		URL_MATCHER.addURI(Task.AUTHORITY, "tasks/" + Constraint.CONSTRAINT + "/" + Constraint.Version1.VERSION + "/" + Constraint.Version1.UNLABELED, MASK_UNLABELED);
		URL_MATCHER.addURI(Task.AUTHORITY, "tasks/" + Constraint.CONSTRAINT + "/" + Constraint.Version1.VERSION + "/" + Constraint.Version1.ALL, MASK_ALL);
		// content:// Task.AUTHORITY /tasks/ mask / 1 / all

		// Database information.
		URL_MATCHER.addURI(Task.AUTHORITY, "db_info/version", DB_INFO_VERSION);
		URL_MATCHER.addURI(Task.AUTHORITY, "db_info/file_size", DB_INFO_FILE_SIZE);
		URL_MATCHER.addURI(Task.AUTHORITY, "db_info/max_size", DB_INFO_MAX_SIZE);
		URL_MATCHER.addURI(Task.AUTHORITY, "db_info/page_size", DB_INFO_PAGE_SIZE);
		URL_MATCHER.addURI(Task.AUTHORITY, "db_info/file_path", DB_INFO_FILE_PATH);
		
		URL_MATCHER.addURI(Task.AUTHORITY, "db/backup", DB_BACKUP);
		
		// Widget informations
		URL_MATCHER.addURI(Task.AUTHORITY, "app_widgets", APP_WIDGETS);
//        URL_MATCHER.addURI(Task.AUTHORITY, "app_widgets/#", APP_WIDGET_ID);  // Not used.
		URL_MATCHER.addURI(Task.AUTHORITY, "app_widgets_deleted", APP_WIDGETS_DELETED);
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if (null != dbHelper) {
			dbHelper.close();
		}
	}

	/**
	 * @param context
	 */
	public static void sendFilterBitsChangedBroadcast(Context context) {
		//Log.v(TAG, "sendFilterBitsChangedBroadcast(..) called.");
		context.sendBroadcast(new Intent(TaskProvider.ACTION_FILTER_BITS_CHANGED));
	}

	/**
	 * Serializes the database using the built in Android
	 * org.xmlpull.v1.XmlSerializer object.
	 * 
	 * See also:
	 * http://developer.android.com/reference/org/xmlpull/v1/XmlSerializer.html
	 * 
	 */
	private long serializeDb(SQLiteDatabase mDB, StringBuilder dbSerialization) {
		Log.v(TAG, "serializeDb(..) called.");
		mDB.beginTransaction();
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();

			// May want to use storage (external or internal) rather than
			// running the risk of
			// running out of memory.
			// File xmlfile = new
			// File(Environment.getExternalStorageDirectory()+"/serialize.xml");
			// File xmlfile = new
			// File(Environment.getDataDirectory()+"/serialize.xml");

			// Begin serialization.
			XmlSerializer serializer = Xml.newSerializer();
			serializer.setOutput(bos, "UTF-8");

			BackupUtil.addHeader(serializer);

			// tasks
			BackupUtil.addTableData(mDB, serializer, TASK_TABLE_NAME, new String[] { Task.Tasks._ID, 				Task.Tasks.TASK_TITLE, 		   Task.Tasks.TASK_DESC, 		  Task.Tasks.DUE_DATE, 			 Task.Tasks.CREATED_DATE, 		Task.Tasks.MODIFIED_DATE, 	   Task.Tasks.PRIORITY, 		  Task.Tasks.ALARM_TIME, 		 Task.Tasks.ALARM_ACTIVE, 		Task.Tasks.COMPLETE, 		   Task.Tasks.COMPLETION_DATE }, 
					                                                  new int[]    { BackupUtil.COLUMN_TYPE_STRING, BackupUtil.COLUMN_TYPE_STRING, BackupUtil.COLUMN_TYPE_STRING, BackupUtil.COLUMN_TYPE_STRING, BackupUtil.COLUMN_TYPE_STRING, BackupUtil.COLUMN_TYPE_STRING, BackupUtil.COLUMN_TYPE_STRING, BackupUtil.COLUMN_TYPE_STRING, BackupUtil.COLUMN_TYPE_STRING, BackupUtil.COLUMN_TYPE_STRING, BackupUtil.COLUMN_TYPE_STRING }, 
					                                                  null, 
					                                                  null,
					                                                  Task.Tasks._ID);

			// attachments
			BackupUtil.addTableData(mDB, serializer, ATTACHMENT_TABLE_NAME, new String[] { Task.TaskAttachments._ID, 	  Task.TaskAttachments.TASK_ID,  Task.TaskAttachments._URI,     Task.TaskAttachments._PACKAGE, Task.TaskAttachments._CLASS_NAME, Task.TaskAttachments._INTENT,   Task.TaskAttachments.NAME,     Task.TaskAttachments._ICON,  Task.TaskAttachments._ICON_RESOURCE, Task.TaskAttachments._DELETE_INTENT }, 
																			new int[]    { BackupUtil.COLUMN_TYPE_STRING, BackupUtil.COLUMN_TYPE_STRING, BackupUtil.COLUMN_TYPE_STRING, BackupUtil.COLUMN_TYPE_STRING, BackupUtil.COLUMN_TYPE_STRING,    BackupUtil.COLUMN_TYPE_STRING,  BackupUtil.COLUMN_TYPE_STRING, BackupUtil.COLUMN_TYPE_BLOB, BackupUtil.COLUMN_TYPE_STRING, 	  BackupUtil.COLUMN_TYPE_STRING },
																			null, 
																			null, 
																			Task.TaskAttachments._ID);

			// proximity_alerts
			BackupUtil.addTableData(mDB, serializer, PROXIMITY_ALERT_TABLE_NAME, new String[] { Task.ProximityAlerts._ID, Task.ProximityAlerts._SELECTED_URI, Task.ProximityAlerts.RADIUS, Task.ProximityAlerts.RADIUS_UNIT, Task.ProximityAlerts.ENABLED, Task.ProximityAlerts._GEO_URI,
					Task.ProximityAlerts._IS_SATELLITE, Task.ProximityAlerts._IS_TRAFFIC, Task.ProximityAlerts._ZOOM_LEVEL }, 
					null,
					null, 
					null, 
					Task.ProximityAlerts._ID);

			// labels
			BackupUtil.addTableData(mDB, serializer, LABELS_TABLE_NAME, new String[] { Task.Labels._ID, Task.Labels.DISPLAY_NAME, Task.Labels.DESCRIPTION, Task.Labels._ID }, 
					null, 
					null, // Task.Labels._USER_APPLIED + " IS ?", 
					null, // new String[]{Task.Labels.USER_APPLIED_TRUE}, 
					Task.Labels._ID);

			// labeled_content
			BackupUtil.addTableData(mDB, serializer, LABELED_CONTENT_TABLE_NAME, new String[] { Task.LabeledContent._ID, Task.LabeledContent._LABEL_ID, Task.LabeledContent._CONTENT_URI, Task.LabeledContent.CREATED_DATE }, 
					null, 
					null, 
					null, 
					Task.LabeledContent._ID);

			// filter
			BackupUtil.addTableData(mDB, serializer, FILTER_TABLE_NAME, new String[] { Task.Filter._ID, Task.Filter.DISPLAY_NAME, Task.Filter._DISPLAY_NAME_ARRAY_INDEX, Task.Filter.DESCRIPTION, Task.Filter._PERMANENT, Task.Filter._ACTIVE, Task.Filter._SELECTED }, 
					null, 
					null, // Task.Filter._PERMANENT+" IS NULL", 
					null, 
					Task.Filter._ID);

			// filter_element
			BackupUtil.addTableData(mDB, serializer, FILTER_ELEMENT_TABLE_NAME, new String[] { Task.FilterElement._ID, Task.FilterElement._FILTER_ID, Task.FilterElement._ACTIVE, Task.FilterElement._PARAMETERS, Task.FilterElement._PHASE, Task.FilterElement._APPLY_WHEN_ACTIVE,
					Task.FilterElement._CONSTRAINT_URI, Task.FilterElement._ORDER }, 
					null, 
					null, // Task.FilterElement._FILTER_ID + " IN (SELECT " + Task.Filter._ID + " FROM " + FILTER_TABLE_NAME + " WHERE " + Task.Filter._PERMANENT+" IS NULL)", 
					null, 
					Task.FilterElement._ID);

			// completable
			BackupUtil.addTableData(mDB, serializer, COMPLETABLE_TABLE_NAME, new String[] { Task.Completable._ID, Task.Completable.TEXT_CONTENT, Task.Completable._COMPLETED }, 
					null, 
					null, 
					null, 
					Task.Completable._ID);

			BackupUtil.addFooter(serializer);

			serializer.flush();

			// write xml data into the FileOutputStream
			dbSerialization.append(bos.toString("UTF-8"));

			// finally we close the file stream
			bos.close();

			// No changes, so no need to commit.
		} catch (Exception e) {
			Log.e(TAG, "ERR000K0", e);
			ErrorUtil.handleExceptionAndThrow("ERR000K0", e, getContext());
		} finally {
			mDB.endTransaction();
		}
		return getDatabaseLastModified();
	}

	/**
	 * Constructs a collection of operations to be peformed on the database to restore the snapshot.
	 * 
	 * The restore only occurrs when there is no problem parsing.
	 * 
	 * The structure used to hold the operations is a hashtable with table name keys pointing to collections of ContentValue objects.   
	 * 
	 * HashMap
	 *   TableName --> ArrayList of ContentValue objects. 
	 *   
	 * @param mDB
	 * @param url
	 * @param values
	 * @return
	 */
	private Uri restoreBackup(SQLiteDatabase mDB, Uri url, ContentValues values) {
		
		// TODO: When database version increments, values.getAsInteger(Task.Backup._VERSION)
		//       must be checked where the older snapshot of the datbase would conflict with 
		//       the newer scheme. 
		
		//Log.v(TAG, "restoreBackup(..) called. url = " + url.toString() + " value == " + values.toString());
		
		// Verify that we aren't restoring backup of current database. (Possible because Backup is implemented as an add-on and thus can be installed and uninstalled independently of FlingTap Done).			
		try{
	        if( values.containsKey(Task.Backup._INSTALL_FINGERPRINT) && 
	        		values.getAsLong(Task.Backup._INSTALL_FINGERPRINT) == mBackupManagerHolder.getInstallFingerprint() ){
	        	Log.i(TAG, "Restore data has same fingerprint as current install and so is an exact backup of the current data. Ignoring it.");
	        	// Attempting to restore backup of the same data from an earlier time.

				// Report our current stats to Backup Restore Agent.
				return prepareRestoreReturnUri();
	        }else{
	        	Log.i(TAG, "Restore data has different fingerprint as current install and so will be restored.");
	        }
		} catch (Exception e) {
			Log.e(TAG, "ERR000KW", e);
			ErrorUtil.handleExceptionAndThrow("ERR000KW", e, getContext());
		}

		Uri returnUri = null;
		try{
			
			String serializedDb = values.getAsString(Task.Backup._SERIALIZATION);
			HashMap<String, ArrayList<ContentValues>> operationMap = new HashMap<String, ArrayList<ContentValues>>();
			try {
				XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
				parser.setInput(new ByteArrayInputStream(serializedDb.getBytes("UTF-8")), "UTF-8");

				XmlUtils.beginDocument(parser, BackupUtil.TAG_DATASET);

				String tableName = null;
				ArrayList<String> columns = null;
				ContentValues cv = null;
				int valueCounter = -1;
				boolean withinRow = false;
				boolean withinValue = false;
				boolean withinColumn = false;
				
				int eventType = parser.getEventType();
				while (eventType != XmlPullParser.END_DOCUMENT) {

					if (eventType == XmlPullParser.START_TAG) {
						// System.out.println("Start tag " + parser.getName());
						if (BackupUtil.TAG_TABLE.equals(parser.getName())) {
							// Start table.
							tableName = parser.getAttributeValue(null, BackupUtil.TAG_TABLE_ATTR_NAME);
							columns = new ArrayList<String>();
						}else if (BackupUtil.TAG_COLMN.equals(parser.getName())) {						
							assert null != tableName;
							withinColumn = true;
							valueCounter = -1;
						}else if (BackupUtil.TAG_ROW.equals(parser.getName())) {
							withinRow = true;
							cv = new ContentValues();
							valueCounter = -1;
						}else if (BackupUtil.TAG_VALUE.equals(parser.getName()) && withinRow ) {
							withinValue = true;
							valueCounter++;
							if( valueCounter >= columns.size() ){ // TODO: !!!!! Consider not aborting in this condition.
								// Error!!!
								Log.e(TAG, "ERR000K2");
								ErrorUtil.handle("ERR000K2", "table "+tableName+" valueCounter {"+valueCounter+"} > columns.size() {"+columns.size()+"}" , this);
								// Don't abort, just keep trying to move forward.
							}
						}
						
						
					} else if (eventType == XmlPullParser.END_TAG) {
						// System.out.println("End tag " + parser.getName());
						if (BackupUtil.TAG_TABLE.equals(parser.getName())) {
							// End table.
							tableName = null;
							columns = null;
							cv = null;
						}else if (BackupUtil.TAG_COLMN.equals(parser.getName())) {
							withinColumn = false;
							cv = null;
						}else if (BackupUtil.TAG_ROW.equals(parser.getName())) {
							withinRow = false;
							valueCounter = -1;
							
							if( !operationMap.containsKey(tableName) ){
								operationMap.put(tableName, new ArrayList<ContentValues>());	
							}
							if( cv != null ){
								operationMap.get(tableName).add(cv);
							}
							
							cv = null;
						}else if (BackupUtil.TAG_VALUE.equals(parser.getName()) ) { 
							
							if( withinValue && 
									valueCounter >= 0 && 
									valueCounter < columns.size() &&
									!cv.containsKey(columns.get(valueCounter))){
								cv.put(columns.get(valueCounter), "");
							}						
							withinValue = false;
						}else if (withinRow && BackupUtil.TAG_NULL.equals(parser.getName()) ) { 
							valueCounter++;
						}
					} else if (eventType == XmlPullParser.TEXT) {
						// System.out.println("Text " + parser.getText());
						if( withinValue && valueCounter >= 0 && valueCounter < columns.size() ){
							cv.put(columns.get(valueCounter), parser.getText());
						}else if( withinColumn ){
							columns.add(parser.getText());
						}

					}
					
					eventType = parser.next();
				} 

			} catch (Exception e) {
				Log.e(TAG, "ERR000K1", e);
				ErrorUtil.handleExceptionAndThrow("ERR000K1", e, getContext());
			}
			
			try{
				mDB.beginTransaction();
				
				// **********************************************************
				// Shift IDs.
				// **********************************************************
				
				final long topTaskId = BackupUtil.selectTopTableId(mDB, TASK_TABLE_NAME, Task.Tasks._ID);
				BackupUtil.shiftTableIds(operationMap, TASK_TABLE_NAME, Task.Tasks._ID, topTaskId);
				
				final long topAttachmentId = BackupUtil.selectTopTableId(mDB, ATTACHMENT_TABLE_NAME, Task.TaskAttachments._ID);
				BackupUtil.shiftTableIds(operationMap, ATTACHMENT_TABLE_NAME, Task.TaskAttachments._ID, topAttachmentId);
				
				final long topProximityAlertId = BackupUtil.selectTopTableId(mDB, PROXIMITY_ALERT_TABLE_NAME, Task.ProximityAlerts._ID);
				BackupUtil.shiftTableIds(operationMap, PROXIMITY_ALERT_TABLE_NAME, Task.ProximityAlerts._ID, topProximityAlertId);
				
				final long topLabelsId = BackupUtil.selectTopTableId(mDB, LABELS_TABLE_NAME, Task.Labels._ID);
				BackupUtil.shiftTableIds(operationMap, LABELS_TABLE_NAME, Task.Labels._ID, topLabelsId);
				
				final long topLabeledContentId = BackupUtil.selectTopTableId(mDB, LABELED_CONTENT_TABLE_NAME, Task.LabeledContent._ID);
				BackupUtil.shiftTableIds(operationMap, LABELED_CONTENT_TABLE_NAME, Task.LabeledContent._ID, topLabeledContentId);
								
				final long topFilterId = BackupUtil.selectTopTableId(mDB, FILTER_TABLE_NAME, Task.Filter._ID);
				BackupUtil.shiftTableIds(operationMap, FILTER_TABLE_NAME, Task.Filter._ID, topFilterId);

				final long topFilterElementId = BackupUtil.selectTopTableId(mDB, FILTER_ELEMENT_TABLE_NAME, Task.FilterElement._ID);
				BackupUtil.shiftTableIds(operationMap, FILTER_ELEMENT_TABLE_NAME, Task.FilterElement._ID, topFilterElementId);

				final long topCompletableId = BackupUtil.selectTopTableId(mDB, COMPLETABLE_TABLE_NAME, Task.Completable._ID);
				BackupUtil.shiftTableIds(operationMap, COMPLETABLE_TABLE_NAME, Task.Completable._ID, topCompletableId);

				// **********************************
				// Shift foriegn key ids columns (including foriegn key IDs in URIs columns).
				// **********************************
				
				// Shift Filter IDs in _filter_id of filter elements.
				BackupUtil.shiftTableIds(operationMap, FILTER_ELEMENT_TABLE_NAME, Task.FilterElement._FILTER_ID, topFilterId);
				
				// Shift Task IDs in Labeled Content URIs. 
				BackupUtil.shiftTableUriIds(operationMap, LABELED_CONTENT_TABLE_NAME, Task.LabeledContent._CONTENT_URI, Task.AUTHORITY, Task.Tasks.TWIG, topTaskId);

				//   Shift labeled_content records except those pointing at stock labels (because stock labels aren't shifted because we re-use the current databases stock labels..
				BackupUtil.shiftTableIdsExceptWhen(operationMap, LABELED_CONTENT_TABLE_NAME, Task.LabeledContent._LABEL_ID, topLabelsId, Task.LabeledContent._LABEL_ID, ID_LABEL_ARCHIVED);
				
				
				// Shift Label ID in _CONSTRAINT_URI (_uri) URI of filter element.
				BackupUtil.shiftTableUriIds(operationMap, FILTER_ELEMENT_TABLE_NAME, Task.FilterElement._CONSTRAINT_URI, Task.AUTHORITY, Task.Labels.BASE_PATH_FOR_FILTER_ELEMENTS, topLabelsId);
				
				// Shift _task_id IDs in Attachment records.
				BackupUtil.shiftTableIds(operationMap, ATTACHMENT_TABLE_NAME, Task.TaskAttachments.TASK_ID, topTaskId);

				// **********************************
				// Shift URIs in Intents.
				// **********************************
				
				// Shift _uri URIs in Attachment records. 
				BackupUtil.shiftTableUriIds(operationMap, ATTACHMENT_TABLE_NAME, Task.TaskAttachments._URI, Task.AUTHORITY, Task.Tasks.TWIG, topTaskId);
				BackupUtil.shiftTableUriIds(operationMap, ATTACHMENT_TABLE_NAME, Task.TaskAttachments._URI, Task.AUTHORITY, Task.Completable.TWIG, topCompletableId);
				BackupUtil.shiftTableUriIds(operationMap, ATTACHMENT_TABLE_NAME, Task.TaskAttachments._URI, Task.AUTHORITY, Task.ProximityAlerts.TWIG, topProximityAlertId);
				BackupUtil.shiftTableUriIds(operationMap, ATTACHMENT_TABLE_NAME, Task.TaskAttachments._URI, Task.AUTHORITY, Task.Labels.BASE_PATH_FOR_FILTER_ELEMENTS, topProximityAlertId);
				
				// Shift _intent URIs in Attachment records. 
				BackupUtil.shiftTableUriIds(operationMap, ATTACHMENT_TABLE_NAME, Task.TaskAttachments._INTENT, Task.AUTHORITY, Task.Tasks.TWIG, topTaskId);
				BackupUtil.shiftTableUriIds(operationMap, ATTACHMENT_TABLE_NAME, Task.TaskAttachments._INTENT, Task.AUTHORITY, Task.Completable.TWIG, topCompletableId);
				BackupUtil.shiftTableUriIds(operationMap, ATTACHMENT_TABLE_NAME, Task.TaskAttachments._INTENT, Task.AUTHORITY, Task.ProximityAlerts.TWIG, topProximityAlertId);
				BackupUtil.shiftTableUriIds(operationMap, ATTACHMENT_TABLE_NAME, Task.TaskAttachments._INTENT, Task.AUTHORITY, Task.Labels.BASE_PATH_FOR_FILTER_ELEMENTS, topProximityAlertId);
				
				// Shift _delete_intent URIs in Attachment records. 
				BackupUtil.shiftTableUriIds(operationMap, ATTACHMENT_TABLE_NAME, Task.TaskAttachments._DELETE_INTENT, Task.AUTHORITY, Task.Tasks.TWIG, topTaskId);
				BackupUtil.shiftTableUriIds(operationMap, ATTACHMENT_TABLE_NAME, Task.TaskAttachments._DELETE_INTENT, Task.AUTHORITY, Task.Completable.TWIG, topCompletableId);
				BackupUtil.shiftTableUriIds(operationMap, ATTACHMENT_TABLE_NAME, Task.TaskAttachments._DELETE_INTENT, Task.AUTHORITY, Task.ProximityAlerts.TWIG, topProximityAlertId);
				BackupUtil.shiftTableUriIds(operationMap, ATTACHMENT_TABLE_NAME, Task.TaskAttachments._DELETE_INTENT, Task.AUTHORITY, Task.Labels.BASE_PATH_FOR_FILTER_ELEMENTS, topProximityAlertId);

				// Not needed because _SELECTED_URI is always a contact (as of 1/17/2011).
				//BackupUtil.shiftTableUriIds(operationMap, PROXIMITY_ALERT_TABLE_NAME, Task.ProximityAlerts._SELECTED_URI, Contacts.**, Task.Labels.BASE_PATH_FOR_FILTER_ELEMENTS, ???);
				
				// ***********************************
				// Remove "Stock" records from operationMap.
				// ***********************************
				
				// Remove stock filters.
				ArrayList<ContentValues> stockFilters = BackupUtil.removeEntriesWhenPresent(operationMap, FILTER_TABLE_NAME, Task.Filter._PERMANENT);
				// Remove filter elements of stock filters.
				BackupUtil.removeEntriesWhereMatch(operationMap, FILTER_ELEMENT_TABLE_NAME, Task.FilterElement._FILTER_ID, stockFilters, Task.Filter._ID);

				// Remove stock labels.
				// Assume that destination database contains said labels.
				BackupUtil.removeEntriesWhenEquals(operationMap, LABELS_TABLE_NAME, Task.Labels._USER_APPLIED, Task.Labels.USER_APPLIED_FALSE);

				
				// **********************************************************
				// Re-assign duplicate labels in restore to current label. 
				// **********************************************************
				BackupUtil.reassignDuplicateLabels(mDB, LABELS_TABLE_NAME, operationMap.get(LABELS_TABLE_NAME), operationMap.get(LABELED_CONTENT_TABLE_NAME), operationMap.get(FILTER_ELEMENT_TABLE_NAME));
				
				// **********************************************************
				// Re-name restore labels with duplicate name. 
				// **********************************************************
				BackupUtil.renameFilters(getContext().getResources(), mDB, FILTER_TABLE_NAME, operationMap.get(FILTER_TABLE_NAME));
				
				// **********************************************************
				// Restore
				// **********************************************************
				
				// Restore Tasks table.
				BackupUtil.restoreTable(mDB, operationMap, TASK_TABLE_NAME, Tasks.TASK_TITLE);
				
				// Restore Attachments table.
				BackupUtil.restoreTableWithByteArrayColumns(mDB, operationMap, ATTACHMENT_TABLE_NAME, TaskAttachments.NAME, new String[]{TaskAttachments._ICON});

				// Restore proximity alerts table.
				BackupUtil.restoreTable(mDB, operationMap, PROXIMITY_ALERT_TABLE_NAME,  ProximityAlerts._IS_SATELLITE);
				
				// Restore Labels table.
				BackupUtil.restoreTable(mDB, operationMap, LABELS_TABLE_NAME, Task.Labels.DESCRIPTION);
				
				// Restore Labeled content table.
				BackupUtil.restoreTable(mDB, operationMap, LABELED_CONTENT_TABLE_NAME, Task.LabeledContent._CONTENT_URI);
				
				// Restore filtered table.
				BackupUtil.restoreTable(mDB, operationMap, FILTER_TABLE_NAME, Task.FilterColumns.DISPLAY_NAME);
				
				// Restore filter element table.
				BackupUtil.restoreTable(mDB, operationMap, FILTER_ELEMENT_TABLE_NAME, Task.FilterElementColumns._PARAMETERS);
				
				// Restore Completeable table.
				BackupUtil.restoreTable(mDB, operationMap, COMPLETABLE_TABLE_NAME, Task.Completable._COMPLETED);

				// Ignore Temp File table.
				// Restore data shouldn't have any temp file records.

				// Ignore Notification table.

				returnUri = prepareRestoreReturnUri();
				
				mDB.setTransactionSuccessful();
			} catch (Exception e) {
				Log.e(TAG, "ERR000K3", e);
				ErrorUtil.handleExceptionAndThrow("ERR000K3", e, getContext());
			}finally{
				mDB.endTransaction();
			}

			try{
				// Request a database backup since we just added data to the database.
				updateDatabaseLastModified();
				
				// See above call to SetupBroadcastReceiver.clearNotificationsAlarmsAndProximityAlerts for explanation. 
				SetupBroadcastReceiver.setupNotificationsAlarmsAndProximityAlerts(getContext(), false);
				
				// Ensure filter bits are set correctly. 
				filterBits(mDB, null);
			} catch (Exception e) {
				Log.e(TAG, "ERR000K7", e);
				ErrorUtil.handleExceptionAndThrow("ERR000K7", e, getContext());
			}
			
		} catch (HandledException he) {
		} catch (Exception e) {
			Log.e(TAG, "ERR000KC", e);
			ErrorUtil.handleException("ERR000KC", e, getContext());
		}

		return returnUri;
	}

	private Uri prepareRestoreReturnUri() {
		Uri.Builder returnUriBuilder = Task.Backup.CONTENT_URI.buildUpon();
		returnUriBuilder.appendQueryParameter(Task.Backup._SERIALIZER_VERSION, BackupUtil.SERIALIZER_VERSION); 
		returnUriBuilder.appendQueryParameter(Task.Backup._LAST_MODIFIED, String.valueOf(getDatabaseLastModified()));
		Uri returnUri = returnUriBuilder.build();
		return returnUri;
	}

	private void updateDatabaseLastModified(){
		backupDataChanged();
	}
	
	private long getDatabaseLastModified(){
		return mBackupManagerHolder.getDatabaseLastModified();
	}
	
}
