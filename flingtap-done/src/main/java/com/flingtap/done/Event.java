// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.util.HashMap;
import java.util.Map;

import com.flurry.android.FlurryAgent;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class Event {

	private Event(){};
	
	public static final void onEvent(String name){
		FlurryAgent.onEvent(name, null); 
	}	
	public static final void onEvent(String name, Map<String,String> params){
		FlurryAgent.onEvent(name, params); 
	}	

	public static final String STATIC_DISPLAY = "Static Display";

	public static final String TASK_EDIT_ARCHIVE_OPTIONS_MENU_ITEM_CLICKED = "Task Edit Archive Options Menu Item Clicked";
	public static final String TASK_EDIT_ARCHIVE_BUTTON_BAR_BUTTON_CLICKED = "Task Edit Archive Button Bar Button Clicked";

	public static final String TASK_LIST_CALL_BUTTON_CLICKED 	= "Task List Call Button Clicked"; 
	public static final String TASK_LIST_ITEM_CLICKED 	= "Task List Item Clicked"; 
	
	
	public static final String TASK_LIST_FILTER_OPTIONS_MENU_ITEM_CLICKED = "Task List Filter Options Menu Item Clicked";  

	// TODO: !!!! Condense these menu events down by making the specific menu item a property.
	public static final String TASK_LIST_OPTIONS_MENU = "Task List Options Menu";
	public static final String TASK_LIST_OPTIONS_MENU__CHOICE = "Choice";
	public static final String TASK_LIST_OPTIONS_MENU__CHOICE__FEATURE_INTRODUCTION = "Feature Introduction";
	public static final String TASK_LIST_UNARCHIVE_CONTEXT_MENU_ITEM_CLICKED = "Task List Unarchive Context Menu Item Clicked";
	public static final String TASK_LIST_ARCHIVE_CONTEXT_MENU_ITEM_CLICKED  = "Task List Archive Context Menu Item Clicked";
	public static final String TASK_LIST_ARCHIVE_OPTIONS_MENU_ITEM_CLICKED  = "Task List Archive Options Menu Item Clicked";
	public static final String TASK_LIST_LABELS_CONTEXT_MENU_ITEM_CLICKED 	= "Task List Labels Context Menu Item Clicked"; 
	public static final String TASK_LIST_SEARCH_SERVICES_CONTEXT_MENU_ITEM_CLICKED 	= "Task List Search Services Context Menu Item Clicked"; 
	public static final String TASK_LIST_MARK_CONTEXT_MENU_ITEM_CLICKED 	= "Task List Mark Context Menu Item Clicked"; 
	public static final String TASK_LIST_VIEW_CONTEXT_MENU_ITEM_CLICKED 	= "Task List View Context Menu Item Clicked"; 
	public static final String TASK_LIST_EDIT_CONTEXT_MENU_ITEM_CLICKED 	= "Task List Edit Context Menu Item Clicked"; 
	public static final String TASK_LIST_DELETE_OPTIONS_MENU_ITEM_CLICKED 	= "Task List Delete Options Menu Item Clicked"; 
	public static final String TASK_LIST_DELETE_CONTEXT_MENU_ITEM_CLICKED 	= "Task List Delete Context Menu Item Clicked"; 
	public static final String TASK_LIST_DELETE_KEY_CLICKED 	            = "Task List Delete Key Clicked"; 
	public static final String TASK_LIST_NEW_TASK_OPTIONS_MENU_ITEM_CLICKED = "Task List New Task Options Menu Item Clicked";
	public static final String TASK_LIST_HELP_OPTIONS_MENU_ITEM_CLICKED 	= "Task List Help Options Menu Item Clicked"; 
	public static final String TASK_LIST_ABOUT_OPTIONS_MENU_ITEM_CLICKED 	= "Task List About Options Menu Item Clicked"; 
	public static final String TASK_LIST_PREFERENCES_OPTIONS_MENU_ITEM_CLICKED = "Task List Preferences Options Menu Item Clicked"; 
	public static final String VIEW_WEBSITE  = "View Website";
	public static final String VIEW_WEBSITE__URL  = "URL";
	public static final String VIEW_WEBSITE__NAME  = "Name";
	public static final String VIEW_WEBSITE__NAME__BLOG  = "Blog";
	public static final String VIEW_WEBSITE__NAME__DONATE  = "Donate";
	public static final String VIEW_WEBSITE__NAME__FORUM  = "Forum";
	public static final String VIEW_WEBSITE__NAME__ADDONS  = "Add-ons";
	public static final String VIEW_WEBSITE__NAME__TUTORIALS  = "Tutorials";

	public static final String FILTER_LIST_HELP_OPTIONS_MENU_ITEM_CLICKED 	= "Filter List Help Options Menu Item Clicked";

	public static final String TASK_ATTACHMENT_LIST_ARCHIVE_BUTTON_BAR_BUTTON_CLICKED = "Task Attachment List Archive Button Bar Button Clicked";

	public static final String VIEW_TASK_LIST 	= "View Task List"; 
	public static final String ATTACH_TASK    	= "Attach Task"; 
	public static final String PICK_TASK    	= "Pick Task"; 
	public static final String GET_TASK_CONTENT = "Get Task Content"; 
	public static final String CREATE_TASK_SHORTCUT	= "Create Task Shortcut"; 
	
	public static final String DELETE_TASK = "View Task Viewer"; 
	
	public static final String ALARM_OCCURRED = "Alarm Occurred"; 
	
	public static final String VIEW_TASK_INSERT = "View Task Insert"; 
	public static final String VIEW_TASK_VIEWER = "View Task Viewer"; 
	public static final String VIEW_TASK_EDITOR = "View Task Editor"; 
	public static final String VIEW_TASK_EDITOR_HELP = "View Task Editor Help"; 

	public static final String VIEW_TASK_ATTACHMENT_LIST_TAB = "View Task Attachments List Tab"; 
	public static final String VIEW_TASK_ATTACHMENT_LIST_TAB_HELP = "View Task Attachments List Tab Help"; 
	
	public static final String SELECT_POSTAL_CONTACT_METHOD = "Select Postal Contact Method"; 
	
	public static final String SELECT_AREA = "Select Area"; // TODO: Add events for the wizard steps, and the resulting choice (cancel, ok, etc..)
	
	public static final String SEARCH_SERVICES_OPTIONS_MENU_ITEM = "Search Services Options Menu Item";
	public static final String TAP_PLACEMARK_IN_SEARCH_SERVICES = "Tap Placemark in Search Services"; // TODO: Add parameter for North-America/Asia/Europe/etc.. or maybe country.
	public static final String CHOOSE_LOCATION_FOR_SEARCH_SERVICES = "Choose Location for Search Services"; // TODO: Add parameter for North-America/Asia/Europe/etc.. or maybe country.
	public static final String LAUNCH_MAP_FOR_SEARCH_SERVICES = "Launch Map for Search Services"; // TODO: Add parameter for North-America/Asia/Europe/etc.. or maybe country.

	public static final String OPEN_STATUS_FILTER_ELEMENT = "Open Status Filter Element";
	public static final String EDIT_STATUS_FILTER_ELEMENT  = "Edit Status Filter Element";

	public static final String OPEN_REPOSITORY_FILTER_ELEMENT = "Open Repository Filter Element";
	public static final String EDIT_REPOSITORY_FILTER_ELEMENT  = "Edit Repository Filter Element";
	
	public static final String OPEN_PRIORITY_FILTER_ELEMENT = "Open Priority Filter Element";
	public static final String EDIT_PRIORITY_FILTER_ELEMENT = "Edit Priority Filter Element";

	public static final String OPEN_DUE_DATE_FILTER_ELEMENT = "Open Due Date Filter Element";
	public static final String EDIT_DUE_DATE_FILTER_ELEMENT = "Edit Due Date Filter Element";
	
	public static final String ADD_ATTACHMENT_OPTIONS_MENU_ITEM = "Add Attachment Options Menu Item";
	public static final String TASK_ATTACHMENT_LIST_ADD_ATTACHMENT_BUTTON_BAR_BUTTON_CLICKED = "Task Attachment List Add Attachment Button Bar Button Clicked";

	public static final String TASK_LIST_KEYWORD_SEARCH = "Task List Keyword Search"; // TODO: Add this event.
	
	public static final String NEARMINDER_OCCURRED = "Nearminder Occurred"; // Add parameter for North-America/Asia/Europe/etc.. or maybe country.

	public static final String RENAME_NEARMINDER = "Rename Nearminder"; // TODO: Not easy to implement now. Consider this issue further when you have a moment.
	public static final String DELETE_NEARMINDER = "Delete Nearminder";
	public static final String EDIT_NEARMINDER   = "Edit Nearminder";
	public static final String VIEW_NEARMINDER = "View Nearminder";
	

	public static final String NEARMINDER_ATTACHMENT_HANDLER_VIEW_CONTEXT_MENU_ITEM_CLICKED   = "Nearminder Attachment Handler View Context Menu Item Clicked";
	public static final String NEARMINDER_ATTACHMENT_HANDLER_EDIT_CONTEXT_MENU_ITEM_CLICKED   = "Nearminder Attachment Handler Edit Context Menu Item Clicked";
	public static final String NEARMINDER_ATTACHMENT_HANDLER_RENAME_CONTEXT_MENU_ITEM_CLICKED = "Nearminder Attachment Handler Rename Context Menu Item Clicked"; 
	public static final String NEARMINDER_ATTACHMENT_HANDLER_DELETE_CONTEXT_MENU_ITEM_CLICKED = "Nearminder Attachment Handler Delete Context Menu Item Clicked";
	public static final String NEARMINDER_ATTACHMENT_HANDLER_EDIT_OPTIONS_MENU_ITEM_CLICKED   = "Nearminder Attachment Handler Edit Options Menu Item Clicked";
	
		
	public static final String CREATE_COMPLETABLE = "Create Completable";  
	public static final String EDIT_COMPLETABLE   = "Edit Completable"; 
	public static final String DELETE_COMPLETABLE = "Delete Completable";  
	
	public static final String COMPLETABLE_ATTACHMENT_HANDLER_EDIT_CONTEXT_MENU_ITEM_CLICKED   = "Completable Attachment Handler Edit Context Menu Item Clicked";
	public static final String COMPLETABLE_ATTACHMENT_HANDLER_DELETE_CONTEXT_MENU_ITEM_CLICKED = "Completable Attachment Handler Delete Context Menu Item Clicked";
	public static final String COMPLETABLE_ATTACHMENT_HANDLER_LIST_ITEM_CLICKED   = "Completable Attachment Handler List Item Clicked";
	
	public static final String OUTGOING_CALLMINDER = "Outgoing Callminder";
	
	public static final String INCOMING_CALLMINDER = "Incoming Callminder";
	public static final String INCOMING_CALLMINDER__NUMBER_OF_CONTACTS = "Number of Contacts";  
	
	
	public static final String ABOUT_FEEDBACK_BUTTON_CLICKED = "About Feedback Button Clicked";
	
	public static final String VIEW_ABOUT = "View About";

	public static final String VIEW_FILTER = "View Filter"; // TODO: Add more properties when multiple filters are allowed. 
	// TODO: !!! Add events for filter elements.

	public static final String FILTER_ELEMENT_LIST_ADD_LABEL_OPTIONS_MENU_ITEM_CLICKED = "Filter Element List Add Label Options Menu Item Clicked";  
	public static final String FILTER_ELEMENT_LIST_RENAME_LABEL_CONTEXT_MENU_ITEM_CLICKED = "Filter Element List Rename Label Context Menu Item Clicked";  
	public static final String FILTER_ELEMENT_LIST_DELETE_LABEL_CONTEXT_MENU_ITEM_CLICKED = "Filter Element List Delete Label Context Menu Item Clicked";  
	
	public static final String EDIT_TASK_LABELS = "Edit Task Labels";
	
	public static final String CREATE_LABEL = "Create Label";  
	public static final String RENAME_LABEL = "Rename Label"; 
	public static final String DELETE_LABEL = "Delete Label";  
	
	public static final String CREATE_FILTER = "Create Filter";  
	public static final String RENAME_FILTER = "Rename Filter"; 
	public static final String DELETE_FILTER = "Delete Filter";  

	public static final String VIEW_APPLICATION_PREFERENCES = "View Application Preferences";
	
	public static final String LAUNCH_ATTACHMENT = "Launch Attachment";

	public static final String ADD_ATTACHMENT = "Add Attachment";
	// TODO: !!! Add info for image size.
	
	public static final String RENAME_ATTACHMENT = "Rename Attachment";

	public static final String REMOVE_ATTACHMENT = "Remove Attachment";
	
	public static final String TOGGLE_LABELS   = "Toggle Labels";
	public static final String TOGGLE_LABELS__ENABLED  = "Enabled"; // Boolean. 
	
	public static final String TOGGLE_ARCHIVE  = "Toggle Archive"; 
	public static final String TOGGLE_ARCHIVE__ENABLED  = "Enabled";// Boolean. 
	public static final String CLEAR_ARCHIVE   = "Clear Archive";

	public static final String ARCHIVE_TASKS                  = "Archive Task";
	public static final String ARCHIVE_TASKS__NUMBER_OF_TASKS = "Number of Tasks";

	public static final String UNARCHIVE_TASKS                  = "Unarchive Task";
	public static final String UNARCHIVE_TASKS__NUMBER_OF_TASKS = "Number of Tasks";
	
	public static final String CONFIGURE_ARCHIVE_RETENTION_POLICY = "Configure Archive Retention Policy"; // TODO: !!! Add call to Event.onEvent(..) for this event with parameters.

	public static final String COMPLETED_TASK_HOUSEKEEPING = "Completed Task Housekeeping"; // TODO: !!! Implement this
	public static final String ARCHIVE_TASK_HOUSEKEEPING = "Archive Task Housekeeping";
	
	public static final String ARCHIVE_COMPLETED_TASK_HOUSEKEEPING = "Archive Completed Task Housekeeping";
	public static final String DELETE_COMPLETED_TASK_HOUSEKEEPING = "Delete Completed Task Housekeeping";
	
	public static final String EDIT_TASK_DELETE_OPTIONS_MENU_ITEM_CLICKED = "Edit Task Delete Options Menu Item Clicked"; 
	
	public static final String EDIT_TASK                     = "Edit Task"; 
	public static final String EDIT_TASK_ADD_LABEL_BUTTON    = "Edit Task Add Label Button"; 
	public static final String EDIT_TASK_ADD_LABEL_TEXT_BOX  = "Edit Task Add Label Text Box"; 
	public static final String EDIT_TASK_DELETE_LABEL_BUTTON = "Edit Task Delete Label Button"; 
	public static final String EDIT_TASK_EDIT_PRIORITY       = "Edit Task Priority"; 
	public static final String EDIT_TASK_EDIT_DUE_DATE       = "Edit Task Due Date"; 
	public static final String EDIT_TASK_EDIT_ALARM          = "Edit Task Alarm"; 

	public static final String TASK_VIEW_EDIT_OPTIONS_MENU_ITEM_CLICKED   = "Task View Edit Options Menu Item Clicked";
	
	public static final String DELETE_TASKS                  = "Delete Task"; 
	public static final String DELETE_TASKS__NUMBER_OF_TASKS = "Number of Tasks";

	public static final String SEND_FEEDBACK   = "Send Feedback";


	
	// TODO: !!! Consider just not not adding a parameter instead of explicitly giving it a name like "None" or "Unknown".
	public static Map<String,String> prepareIntentParams(Context context, Map<String,String> parameters, Intent intent){
		parameters.put("Scheme", null == intent.getScheme()?"Unknown":intent.getScheme());
		Uri uri = intent.getData();
		parameters.put("Authority", null==uri?"None":(null==uri.getAuthority()?"None":uri.getAuthority()));
		String type = intent.getType();
		if( null == type && null != uri ){
			type = context.getContentResolver().getType(uri);
		}
		parameters.put("MIME type", type==null?"Unknown":type);
		parameters.put("Action", intent.getAction()==null?"None":intent.getAction());
		parameters.put("Component", intent.getComponent()==null?"None":intent.getComponent().flattenToShortString());
		
		return parameters;
	}
	
	// TODO: !!! Consider just not not adding a parameter instead of explicitly giving it a name like "None" or "Unknown". See how the current system works.
	public static Map<String,String> prepareUriParams(Context context, Map<String,String> parameters, Uri uri){
		parameters.put("Authority", null==uri?"None":(null==uri.getAuthority()?"None":uri.getAuthority()));
		String type = context.getContentResolver().getType(uri);
		parameters.put("MIME type", type==null?"Unknown":type);
		return parameters;
	}	
	// TODO: !!! Consider whether this is needed. See above for details.
	public static Map<String,String> prepareMissingIntentParams(Context context, Map<String,String> parameters){
		parameters.put("Scheme",    "Intent missing");
		parameters.put("Authority", "Intent missing");
		parameters.put("MIME type", "Intent missing");
		parameters.put("Action",    "Intent missing");
		parameters.put("Component", "Intent missing");
		return parameters;
	}
}
