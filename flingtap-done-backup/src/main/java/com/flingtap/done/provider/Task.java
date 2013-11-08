// Licensed under the Apache License, Version 2.0

package com.flingtap.done.provider;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Convenience definitions for TaskProvider
 */
public final class Task {
	
	private Task(){}
	
	public static final String AUTHORITY = "com.flingtap.done.taskprovider"; 

	
    /**
     * This table contains tasks.
     */
    public static final class Tasks implements BaseColumns, TasksColumns {
    	
    	private Tasks(){}
    	
        /**
         * The content:// style URL for this table.
         * 10 + 27 + 16 + 1 = 54 characters
         */
    	public static final String TWIG = "/tasks";
        public static final String CONTENT_URI_STRING = "content://"+AUTHORITY+TWIG;
        public static final Uri CONTENT_URI	= Uri.parse(CONTENT_URI_STRING);

        
        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of tasks.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.flingtap.done.task";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single task.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.flingtap.done.task";         
        
        /**
         * The default sort order for this table
         * 
         * TODO: !! the _ID is ambiguous when tasks is joined with other tables (like attachments) see: TaskList.createDefaultFilteredTasksForPersonCursor
         */
        public static final String DEFAULT_SORT_ORDER = DUE_DATE + " ASC, " + PRIORITY +  " DESC, " + _ID +  " DESC"; 
        public static final String DEFAULT_SORT_ORDER_JOIN_VERSION = DUE_DATE + " ASC, " + PRIORITY +  " DESC"; 

        
    }
    
    /**
     * Columns from the Tasks table that other tables join into themselves.
     */
    public static interface TasksColumns  {
        /**
         * The title of the task
         * <P>Type: TEXT</P>
         */
        public static final String TASK_TITLE = "title";

        /**
         * The task itself
         * <P>Type: TEXT</P>
         */
        public static final String TASK_DESC = "description"; 

        /**
         * The timestamp for when the task was created
         * <P>Type: INTEGER (long)</P>
         */
        public static final String CREATED_DATE = "_created";

        /**
         * The timestamp for when the task was last modified.
         * 
         * TODO: !!! Need to update this anytime an attachment is added or removed.
         * <P>Type: INTEGER (long)</P>
         */
        public static final String MODIFIED_DATE = "_modified";
        
        /**
         * The priority for when the task
         * <P>Type: INTEGER (long)</P>
         */
        public static final String PRIORITY = "priority";
        public static final String PRIORITY_NONE 	= "0";
        public static final String PRIORITY_LOW 	= "1";
        public static final String PRIORITY_MEDIUM 	= "2";
        public static final String PRIORITY_HIGH 	= "3";
        
        /**
         * The due date for when the task
         * <P>Type: INTEGER (long)</P>
         */
        public static final String DUE_DATE = "due";        
		public static final String DUE_DATE_NOT_SET  = "9223372036854775807";
		public static final long   DUE_DATE_NOT_SET_LONG  = Long.MAX_VALUE;
        
//        /**
//         * The number of days prior to the due date the alarm will be fired.
//         * <P>Type: INTEGER (long)</P>
//         */
//        public static final String ALARM_DAYS_PRIOR = "alarm_days_prior";        
        
        /**
         * The alarm time for when the task.
         * <P>Type: INTEGER (long)</P>
         */
        public static final String ALARM_TIME = "alarm_time";        
        public static final Object ALARM_TIME_NOT_SET = null;        
        
        /**
         * A flag indicating that the alarm should be fired when appropriate.
         * <P>Type: INTEGER (long)</P>
         */
        public static final String ALARM_ACTIVE = "alarm_active";        
		public static final String ALARM_ACTIVE_TRUE  = "1";
		public static final String ALARM_ACTIVE_FALSE = "0";

        /**
         * The completion indicator for when the task
         * <P>Type: INTEGER (long)</P>
         */
        public static final String COMPLETE = "complete";        
        
		public static final String COMPLETE_TRUE  = "1";
		public static final int COMPLETE_TRUE_INT  = 1;
		public static final String COMPLETE_FALSE = "0";
		public static final int COMPLETE_FALSE_INT = 0;
		
        /**
         * The date when the task was completed or null if it has not been completed yet..
         * 
         * Date is a Java long value from the System.currentTimeMillis() method.
         * 
         * <P>Type: INTEGER (long)</P>
         */
        public static final String COMPLETION_DATE = "completion_date";        

        /**
         * A flag indicating whether the current filter includes or excludes this task.
         * 
         * Possible values: FILTER_IN and FILTER_OUT.
         * 
         * <P>Type: INTEGER (long)</P>
         */
        public static final String _FILTER_BIT = "_filter_bit";        
    	
		public static final String FILTER_IN  = "1";
		public static final String FILTER_OUT = "0";
		
//        /**
//         * A temporary staging storage spot for OR ing multiple filter elements before AND them to _filter_bit.
//         * 
//         * Possible values: FILTER_IN and FILTER_OUT.
//         * 
//         * <P>Type: INTEGER (long)</P>
//         */
//        public static final String _AND_BIT = "_and_bit";        

    }
    
    /**
     * This table contains attachments.
     * 
     * TODO: ! Split out a TaskAttachmentsColumns interface.
     * TODO: ! Define a DEFAULT_SORT_ORDER      
     */
    public static final class TaskAttachments implements BaseColumns {
    	
    	private TaskAttachments(){}
    	
        /**
         * The content:// style URL for this table
         */
        public static final String CONTENT_URI_STRING = "content://"+AUTHORITY+"/attachments";
        public static final Uri CONTENT_URI = Uri.parse(CONTENT_URI_STRING);
        
        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of attachments.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.flingtap.done.attachment";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single attachment.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.flingtap.done.attachment";          
        
        
       /**
         * The task id foreign key
         * <P>Type: INTEGER</P>
         */
        public static final String TASK_ID = "_task_id";

        /**
         * The URI referred to by this attachment record.
         * <P>Type: STRING</P>
         */
        public static final String _URI = "_uri";        
        
        /**
         * The package referred to by this attachment record.
         * <P>Type: STRING</P>
         */
        public static final String _PACKAGE = "_package";        
        
        /**
         * The class name referred to by this attachment record.
         * <P>Type: STRING</P>
         */
        public static final String _CLASS_NAME = "_class_name";        

        /**
         * The default sort order for this table
         * TODO: Reconsider this sort order.
         */
        public static final String DEFAULT_SORT_ORDER = _ID + " DESC";

        /**
         * The intent used to launch the attachment.
         * <P>Type: BLOB</P>
         */
        public static final String _INTENT = "_intent";

        /**
         * The pending intent sent when the attachment is deleted.
         * <P>Type: BLOB</P>
         */
        public static final String _DELETE_INTENT = "_delete_intent";
        
        /**
         * The name displayed to the user for this attachment.
         * <P>Type: STRING</P>
         */
        public static final String NAME = "name";
        
        /**
         * The icon displayed to the user for this attachment.
         * 
         * Note: May be null.
         * 
         * <P>Type: BLOB</P>
         */
        public static final String _ICON = "_icon";
        
        /**
         * The icon displayed to the user for this attachment.
         * 
         * Note: May be null.
         * 
         * <P>Type: BLOB</P>
         */
        public static final String _ICON_RESOURCE = "_icon_resource";
        
    }
    
    /**
     * This table contains proximity alerts.
     * 
     * TODO: Split out a ProximityAlertsColumns interface.
     */
    public static final class ProximityAlerts implements BaseColumns {

    	private ProximityAlerts(){}
    	
        /**
         * The content:// style URL for this table
         */
    	public static final String TWIG = "/proximity";
        public static final String CONTENT_URI_STRING = "content://"+AUTHORITY+TWIG;
        public static final Uri CONTENT_URI	= Uri.parse(CONTENT_URI_STRING);
        
        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of proximity alerts.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.flingtap.done.proximity";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single proximity alert.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.flingtap.done.proximity";

        /**
         * The selected resource used when creating this proximity alert.
         * 
         * <P>Type: TEXT</P>
         */
        public static final String _SELECTED_URI = "_selected_uri";

        /**
         * The radius around the location.
         * <P>Type: INTEGER</P>
         * 
         * Note: This value is the Fifteen minus the square root of what is displayed by ProximityAlertCoordinator.
         *       displayedValue = (databaseValue + 15) ^ 2
         *       
         */
        public static final String RADIUS = "radius";
        
        /**
         * The unit of the radius around the location.
         * <P>Type: INTEGER</P>
         * 
         * NOTE: If RADIUS_UNIT changes, then RADIUS should be included in the update so that the proximity alert is updated correctly. I'm not sure how RADIUS_UNIT could change and RADIUS not change, but I need to document this detail somewhere,, so here it is.
         */
        public static final String RADIUS_UNIT = "radius_unit";

        /**
         * The enabled flag for the proximity alert.
         * <P>Type: INTEGER</P>
         */
        public static final String ENABLED = "enabled";
        public static final String ENABLED_TRUE = "1";
        public static final String ENABLED_FALSE = "0";
        
        /**
         * The GEO URI of the given point
         * <P>Type: TEXT </P>
         */
        public static final String _GEO_URI = "_geo_uri";
        
        /**
         * The is_satellite enabled flag of the given point
         * <P>Type: BOOLEAN (long)</P>
         */
        public static final String _IS_SATELLITE = "_is_satellite";
        
        /**
         * The is_traffic enabled flag of the given point
         * <P>Type: BOOLEAN (long)</P>
         */
        public static final String _IS_TRAFFIC = "_is_traffic";
        
        /**
         * The zoom level of the given point
         * <P>Type: INTEGER (int)</P>
         */
        public static final String _ZOOM_LEVEL = "_zoom_level";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = _ID + " DESC";
    }

    
    /**
     * This table contains labels.
     */
    public static final class Labels implements BaseColumns, LabelsColumns {
    	
    	private Labels(){}
    	
    	public static final String BASE_PATH_FOR_FILTER_ELEMENTS = "/tasks/mask/1/label"; // TODO: !!! Duplicated elsewhere.
    	

    	
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/labels");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of labels.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.flingtap.done.label";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single label.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.flingtap.done.label";        
        
        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = DISPLAY_NAME + " DESC";
        
        

        /**
         * A subdirectory of a single label that contains all tasks that the label is applied to.
         * 
         */
        public static final class Tasks implements BaseColumns, TasksColumns, LabeledContentColumns {
        	
        	private Tasks(){}
        	
			/**
			 * The directory twig for this sub-table.
			 */
			public static final String CONTENT_DIRECTORY = "task";

//	      /**
//	      * The default sort order for this table.
//	      */
//	     public static final String DEFAULT_SORT_ORDER = "data ASC";
			
        }
        
    }
    
    /**
     * Columns from the Labels table that other tables join into themselves.
     */
    public static interface LabelsColumns extends BaseColumns {
    	
        /**
         * The label's display name.
         * <P>Type: TEXT</P>
         */
        public static final String DISPLAY_NAME = "display_name";
        
        /**
         * The label's description.
         * <P>Type: TEXT</P>
         */
        public static final String DESCRIPTION = "description";
        
        
        /**
         * Indicates whether this user can manually apply this label.
         * 
         * <P>Type: INTEGER</P>
         */
        public static final String _USER_APPLIED = "_user_applied";

        public static final String USER_APPLIED_TRUE = "1";
        public static final String USER_APPLIED_FALSE = "0";

    }
    
    
    /**
     * This table contains labeled content.
     */
    public static class LabeledContent implements BaseColumns, LabeledContentColumns {
    	
    	private LabeledContent(){}
    	
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/labeled_content");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of labels.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.flingtap.done.labeled_content";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single label.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.flingtap.done.labeled_content";       
    }
    
    /**
     * Columns from the LabeledContent table that other tables join into themselves.
     */
    public static interface LabeledContentColumns extends BaseColumns {

		/**
		 * The label's _id.
		 * 
		 * <P>Type: INTEGER</P>
		 */
		public static final String _LABEL_ID = "_label_id";
		
		/**
		 * The content's URI.
		 * 
		 * This value may be null. 
		 * 
		 * <P>Type: TEXT</P>
		 */
		public static final String _CONTENT_URI = "_content_uri";
		        
		/**
		 * The timestamp for when the proximity alert was created
		 * <P>Type: INTEGER (long)</P>
		 */
		public static final String CREATED_DATE = "_created";    	
    }
    
    /**
     * This table contains filters.
     */
    public static class Filter implements BaseColumns, FilterColumns {
    	
    	private Filter(){}
    	
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/filters");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of filters.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.flingtap.done.filter";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single filter.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.flingtap.done.filter";     
    	
    }
    
    
    /**
     * Columns from the Filter table that other tables join into themselves.
     */
    public static interface FilterColumns extends BaseColumns {
    	
		/**
		 * The filter's display name.
		 * 
		 * <P>Type: STRING</P>
		 */
		public static final String DISPLAY_NAME = "_display_name";

		/**
		 * The filter's description.
		 * 
		 * <P>Type: STRING</P>
		 */
		public static final String DESCRIPTION = "_description";
		
		
		/**
		 * The filter's display name array index.
		 * 
		 * <P>Type: INTEGER</P>
		 */
		public static final String _DISPLAY_NAME_ARRAY_INDEX = "_display_name_array_index";
		
				
		/**
		 * A flag indicating whether the record is permanent and so should not be removable.
		 * Also indicates that the child filter elements should not be editable as well.
		 * 
		 * Note, values > 0 indicate not only that the filter is permanent, but also the value is a unique ID used to activate, deactivate the filter. 
		 *   
		 * See: PERMANENT_FALSE and other defined values (ID_FILTER_ARCHIVE, ID_FILTER_BASIC, ID_FILTER_ALL, etc..) for possible values.
		 * 
		 * <P>Type: INTEGER</P>
		 */
		public static final String _PERMANENT = "_permanent";
//		public static final int PERMANENT_FALSE = 0;
		        
		/**
		 * A flag indicating whether the this filter record is active.
		 * 
		 * Active only means the user may select it. Being active does not indicate that the filter elements should be applied.
		 * 
		 * See: ACTIVE_TRUE for possible values.
		 * 
		 * <P>Type: INTEGER</P>
		 */
		public static final String _ACTIVE = "_active";
		public static final int ACTIVE_TRUE  = 1;  
		public static final String ACTIVE_TRUE_STRING  = "1"; 
		
		/**
		 * A flag indicating whether this filter record is selected.
		 * 
		 * See: SELECTED_TRUE for possible values.
		 * 
		 * <P>Type: INTEGER</P>
		 */
		public static final String _SELECTED = "_selected";
		public static final int SELECTED_TRUE  = 1;

    }
    
    /**
     * This table contains filter elements.
     * 
     * i.e. What labels are part of a specific filter (and whether the label includes or excludes content).
     */
    public static class FilterElement implements BaseColumns, FilterElementColumns {
    	
    	private FilterElement(){}
    	
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/filter_elements");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of filters.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.flingtap.done.filter_element";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single filter.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.flingtap.done.filter_element";     
    	
    }
    
    
    /**
     * Columns from the FilterElement table that other tables join into themselves.
     */
    public static interface FilterElementColumns extends BaseColumns {

		/**
		 * The filters's _id.
		 * 
		 * Foreign key.
		 * 
		 * <P>Type: INTEGER</P>
		 */
		public static final String _FILTER_ID = "_filter_id";

		/**
		 * A flag indicating whether the label is active or not.
		 * 
		 * See: ACTIVE_TRUE and ACTIVE_FALSE for possible values.
		 * 
		 * <P>Type: INTEGER</P>
		 */
		public static final String _ACTIVE = "_active";
		public static final String ACTIVE_TRUE  = "1";
//		public static final String ACTIVE_FALSE = "0";

		/**
		 * Parameters for the label.
		 * 
		 * Sometimes the label URI is not adequate to describe the labels behavior. 
		 *   This is true in situations where the label can be configured or where the label has several modes or options.
		 * 
		 * The value will be appended to the URI as query params.
		 * 
		 * <P>Type: TEXT</P>
		 */
		public static final String _PARAMETERS = "_parameters";

		
        /**
         * Indicates which phase of the filter application this label should be applied in.
         * 
         * <P>Type: INTEGER</P>
         */
        public static final String _PHASE = "_phase";

        public static final String PHASE_EXPLODE = "0";
        public static final String PHASE_EXCLUDE = "1";
        public static final String PHASE_INCLUDE = "2";
        
        /**
         * Indicates when this label should be applied based on the _active property of the ... field.
         * 
         * <P>Type: INTEGER</P>
         */
        public static final String _APPLY_WHEN_ACTIVE = "_apply_when_active";

        public static final String APPLY_WHEN_ACTIVE = "1";
        public static final String APPLY_WHEN_NOT_ACTIVE = "0";

        /**
         * The label's constraint URI.
         * 
         * TODO: !!! Prevent Activities from using an Authority that is not their own.
         * 
         * <P>Type: TEXT</P>
         */
        public static final String _CONSTRAINT_URI = "_uri";

		/**
		 * The display order .
		 * <P>Type: INTEGER</P>
		 */
		public static final String _ORDER = "_order";    	
        
    }

	/**
	  * This table contains filter joins.
	  */
	public static class DueDateFilter implements BaseColumns {
	 	
	 	private DueDateFilter(){}
	 	
	    /**
	     * The content:// style URL for this table
	     */
	    public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/task/filter/due_date");
	
	    /**
	     * The MIME type of {@link #CONTENT_URI} providing a directory of filters.
	     */
	    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.flingtap.done.task.filter.due_date";
	
	    /**
	     * The MIME type of a {@link #CONTENT_URI} sub-directory of a single filter.
	     */
	    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.flingtap.done.task.filter.due_date";     
	 	
	 }

	
    /**
     * This table contains data normally placed into temporary files.
     */
    public static class TempFile implements BaseColumns, android.provider.MediaStore.MediaColumns, TempFileColumns { // OpenableColumns, 
    	
    	private TempFile(){}
    	
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/temp_file");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of temp files.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.flingtap.done.temp_file";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single temp file.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.flingtap.done.temp_file";     
    	
    }
    
    
    /**
     * Columns from the TempFile table that other tables join into themselves.
     */
    public static interface TempFileColumns  {


		/**
		 * Preserve until the given date. 
		 * 
		 * The date (milliseconds since 1970) that the file can be deleted.
		 * 
		 * <P>Type: INTEGER</P>
		 */
		public static final String _PRESERVE_UNTIL = "_preserve_until";
    }	
	
    
    /**
     * This table is used to allocate unique notification IDs.
     * 
     * Field _id is used as the notification id
     * 
     */
    public static class Notification implements BaseColumns, NotificationColumns { 
    	
    	private Notification(){}
    	
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/notification");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notification.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.flingtap.done.notification";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single notification.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.flingtap.done.notification";     

        
        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = BaseColumns._ID; 
        
    }
    
    /**
     * Columns from the NotificationId table that other tables join into themselves.
     */
    public static interface NotificationColumns  {

        /**
         * The URI referred to by this Notification record.
         * 
         * <P>Type: STRING</P>
         * 
         * TODO: ! Use this notification URI to replace the ID placed in the Notification intents.  
         * TODO: ! Also, Use the notification id from the intent to dismiss the notification (starts by looking up the URI and then finding the notif id)
         */
        public static final String _URI = "_uri";        
    }
    
    
    /**
     * This table is used to store completable attachment items.
     * 
     */
    public static class Completable implements BaseColumns, CompletableColumns { 
    	
    	private Completable(){}
    	
    	
    	public static final String TWIG = "/completable";
    	
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+TWIG);

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of completables.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.flingtap.done.completable";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single completable.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.flingtap.done.completable";     

        
        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = BaseColumns._ID; 
        
    }
    
    /**
     * Columns from the Completable table that other tables join into themselves.
     */
    public static interface CompletableColumns  {

        /**
         * Text describing what could be completed.
         * 
         * <P>Type: STRING</P>
         */
        public static final String TEXT_CONTENT = "text_content";        

        
        /**
         * The timestamp for when the notification was created
         * 
         * <P>Type: INTEGER (int)</P>
         */
        public static final String _COMPLETED = "_completed";
        // public static final int COMPLETED_FALSE = 0; // Uncompleted items are just null.
        public static final int COMPLETED_TRUE  = 1;
        
    }

    public static final class AppWidgets implements BaseColumns, AppWidgetColumns {
    	
    	private AppWidgets(){}
    	
        /**
         * The content:// style URL for this table.
         */
        public static final String CONTENT_URI_STRING = "content://"+AUTHORITY+"/app_widgets";
        public static final Uri CONTENT_URI	= Uri.parse(CONTENT_URI_STRING);

        
        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of tasks.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.flingtap.done.app_widget";

    }

    /**
     * Columns from the App Widgets table that other tables join into themselves.
     */
    public static interface AppWidgetColumns  {

        /**
         * The app widget's ID.
         * <P>Type: INTEGER</P>
         */
        public static final String _APP_WIDGET_ID = "_app_widget_id";
        
        /**
         * The app widget's type.
         * <P>Type: INTEGER</P>
         */
        public static final String _TYPE = "_type";
        public static final int TYPE_PENDING_LIST = 1;
        
        /**
         * The app widget's theme.
         * <P>Type: INTEGER</P>
         */
        public static final String _THEME = "_theme";
        public static final int THEME_PENDING_LIGHT = 1;
        public static final int THEME_PENDING_DARK = 2;
        
        /**
         * The app widget's theme.
         * <P>Type: INTEGER</P>
         */
        public static final String _TEXT_SIZE = "_text_size";
        public static final int TEXT_SIZE_SMALL = 1;
        public static final int TEXT_SIZE_MEDIUM = 2;
        public static final int TEXT_SIZE_LARGE = 3;
        public static final int TEXT_SIZE_JUMBO = 4;

    }
    
    public static final class AppWidgetsDeleted implements BaseColumns, AppWidgetDeletedColumns {
    	
    	private AppWidgetsDeleted(){}
    	
        /**
         * The content:// style URL for this table.
         */
        public static final String CONTENT_URI_STRING = "content://"+AUTHORITY+"/app_widgets_deleted";
        public static final Uri CONTENT_URI	= Uri.parse(CONTENT_URI_STRING);

        
        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of deleted app widgets.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.flingtap.done.app_widget_deleted";

    }
    
    /**
     * Columns from the App Widgets table that other tables join into themselves.
     */
    public static interface AppWidgetDeletedColumns  {

        /**
         * The app widget's ID.
         * <P>Type: INTEGER</P>
         */
        public static final String _APP_WIDGET_ID = "_app_widget_id";
    }
    
    
    
    /**
     * Columns used for serializing and de-serializing database for the backup.  
     */
    public static interface BackupColumns {

        /**
         * The serialization of the database using an XML encoding.
         * <P>Type: STRING</P>
         */
        public static final String _SERIALIZATION = "serialization";

        /**
         * The database serialization version.
         * <P>Type: INTEGER</P>
         */
        public static final String _SERIALIZER_VERSION = "_serializer_version";

        /**
         * The database version.
         * <P>Type: INTEGER</P>
         */
        public static final String _VERSION = "_version";

        /**
         * The date and time the database was last modified.
         * <P>Type: LONG</P>
         */
        public static final String _LAST_MODIFIED = "_last_modified"; 
                
        /**
         * The date and time the database was last backed up.
         * <P>Type: LONG</P>
         */
        public static final String _LAST_BACKED_UP = "_last_backed_up"; 
        
        /**
         * The FlingTap Done install instance fingerprint. 
         * <P>Type: LONG</P>
         */
        public static final String _INSTALL_FINGERPRINT = "_install_fingerprint"; 
    }
    
    public static final class Backup implements BackupColumns {
    	
    	private Backup(){}
    	
        /**
         * The content:// style URL for this table.
         */
        public static final String CONTENT_URI_STRING = "content://"+AUTHORITY+"/db/backup";
        public static final Uri CONTENT_URI	= Uri.parse(CONTENT_URI_STRING);
        
//        /**
//         * The MIME type of {@link #CONTENT_URI} providing a directory of backups.
//         */
//        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.flingtap.done.db.backup";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single backup.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.flingtap.done.db.backup";         
        
        
        
        /**
         * SDK Version URI parameter.
         */
        public static final String PARAM_SDK_VERSION = "sdk_version";
        
        /**
         * Device Model URI parameter.
         */
        public static final String PARAM_DEVICE_MODEL = "device_model";
                
    }
    
    
    
}
