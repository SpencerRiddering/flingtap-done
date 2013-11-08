// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Contacts;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.animation.Animation;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.flingtap.common.HandledException;
import com.flingtap.common.Timestamp.TimestampException;
import com.flingtap.done.TaskListAdapter.ViewHolder;
import com.flingtap.done.base.R;
import com.flingtap.done.provider.Task;
import com.flingtap.done.provider.Task.Tasks;
import com.flingtap.done.util.Constants;
import com.flingtap.done.util.LicenseUtil;
import com.flingtap.done.util.UriInfo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * Displays a list of all tasks according to the active filter.
 * 
 */
public class TaskList extends ListActivity implements OnCheckedChangeListener, View.OnClickListener, View.OnKeyListener{
	private final static String TAG = "TaskList";

	// The different distinct states the activity can be run in.
	public static final int STATE_VIEW 			= 0;
	public static final int STATE_ATTACH 		= 1;
	public static final int STATE_PICK 			= 2;
	public static final int STATE_GET_CONTENT 	= 3;
	public static final int STATE_CREATE_SHORTCUT=4;

	int mState = STATE_VIEW;	
    protected Uri mURI; // TODO: Replace this with Task.Tasks.CONTENT_URI
    protected Uri mAttachUri;
    protected boolean mSearching = false;
	
    // Menu item Ids
    private static final int DEFAULT_ID= Menu.FIRST; // Used when we only care about error conditions.
    private static final int VIEW_ID   = Menu.FIRST + 1;
    private static final int EDIT_ID   = Menu.FIRST + 2;
    private static final int INSERT_ID = Menu.FIRST + 3;
    private static final int MARK_ID   = Menu.FIRST + 4;
    private static final int SEARCH_ID = Menu.FIRST + 5;
    private static final int PREFERENCES_ID 			= Menu.FIRST + 7;
    private static final int SEARCH_UI_ID 				= Menu.FIRST + 8;
    private static final int FILTER_ID 					= Menu.FIRST + 9;
    private static final int HELP_ID 					= Menu.FIRST + 10;
    private static final int ABOUT_ID 					= Menu.FIRST + 11;
//    private static final int FEEDBACK_ID 				= Menu.FIRST + 12;
    private static final int DELETE_COMPLETED_ID 		= Menu.FIRST + 13;
    private static final int DELETE_ID 					= Menu.FIRST + 14;
    private static final int LABEL_ID 					= Menu.FIRST + 15; 
    private static final int ARCHIVE_ID 				= Menu.FIRST + 16;
    private static final int UNARCHIVE_ID 				= Menu.FIRST + 17;
    private static final int ADD_ONS_ID 				= Menu.FIRST + 18;
    private static final int TUTORIALS_ID 				= Menu.FIRST + 19;
    private static final int FEATURE_INTRODUCTION_ID 	= Menu.FIRST + 21;
    private static final int SET_AS_ID	 				= Menu.FIRST + 22;
    private static final int OPTION_BLOG_ID				= Menu.FIRST + 23;
    private static final int OPTION_SUPPORT_ID			= Menu.FIRST + 24;
    private static final int OPTION_DONATE_ID			= Menu.FIRST + 25;

    private static final int INSERT_ATTACHMENT_REQUEST_CODE = 2;
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 3;

	// Dialog IDs
	protected final static int APPLY_LABELS_DIALOG_ID = 50;	
	protected final static int DELETE_TASK_DIALOG_ID  = 51;	
	protected final static int SPEECH_RESULT_DIALOG_ID  = 53;	
	
	private boolean mIsArchiveEnabled = false;
    private boolean mIsArchiveActive = false;

//	private AdView mAdView = null;
    private boolean mIsLicensed = false;
    
	private FilterBitsRefreshReceiver mFilterBitsRefreshReceiver = new FilterBitsRefreshReceiver();
    
    /**
     * The columns we are interested in from the database
     */
    public static final String[] PROJECTION = new String[] {
            Task.Tasks._ID, 		// 0
            Task.Tasks.TASK_TITLE,  // 1
            Task.Tasks.TASK_DESC,   // 2
            Task.Tasks.DUE_DATE, 	// 3
            Task.Tasks.COMPLETE, 	// 4
            Task.Tasks.PRIORITY,	// 5
//            "(SELECT COUNT(1) FROM attachments AS a WHERE t._id = a._task_id  AND a."+Task.TaskAttachments.URI_TYPE+"='"+android.provider.Contacts.People.CONTENT_URI+"') AS contact_count", // 6
//            "(SELECT COUNT(1) FROM attachments) AS z", // 6
//            "(SELECT _task_id FROM attachments WHERE tasks._id = attachments._task_id  AND attachments."+Task.TaskAttachments.URI_TYPE+"='"+android.provider.Contacts.People.CONTENT_URI+"') AS contact_count", // 6
//            "(SELECT COUNT(1) FROM attachments AS a WHERE t._id = a._task_id  AND a."+Task.TaskAttachments.URI_TYPE+"='"+com.flingtap.done.provider.GeoMap.GeoMapBookmarks.CONTENT_URI+"') AS location_count", // 7
//            "(SELECT COUNT(1) FROM attachments AS a WHERE t._id = a._task_id  AND a."+Task.TaskAttachments.URI_TYPE+"='"+android.provider.Browser.BOOKMARKS_URI+"') AS net_count", // 8
//            Task.Tasks.ALARM_TIME, // 9
    		};
    public static final int CURSOR_TASK_ID 			= 0;
    public static final int CURSOR_TASK_TITLE 		= 1;
    public static final int CURSOR_TASK_DESC 		= 2;
    public static final int CURSOR_TASK_DUE_DATE 	= 3;
    public static final int CURSOR_TASK_COMPLETE 	= 4;
    public static final int CURSOR_TASK_PRIORITY 	= 5;
//    public static final int CURSOR_TASK_BUCKET	= 6;
//    public static final int CURSOR_TASK_CONTACT_COUNT 	= 6;
//    public static final int CURSOR_TASK_LOCATION_COUNT 	= 7;
//    public static final int CURSOR_TASK_NET_COUNT 	= 8;
//    public static final int CURSOR_TASK_ALARM 	= 9;
    
    /**
     * Cursor which holds list of all tasks
     */
    protected Cursor mCursor;
    
    
	protected static final String[] ATTACHMENTS_PROJECTION = new String[] {
		com.flingtap.done.provider.Task.TaskAttachments._ID, 			// 0
		com.flingtap.done.provider.Task.TaskAttachments.TASK_ID, 		// 1
		com.flingtap.done.provider.Task.TaskAttachments._URI, 			// 2
};
    protected Cursor mAttachmentCursor;
    public static final int CURSOR_ATTACH_ID 			= 0;
    public static final int CURSOR_ATTACH_TASK_ID 		= 1;
    public static final int CURSOR_ATTACH_URI 			= 2;

    protected MenuItem menuItemMarkDone;
	protected static final String SELECTED_INDEX_KEY = "TaskList.SELECTED_INDEX_KEY";
	protected static final String SELECTED_ID_KEY = "TaskList.SELECTED_ID_KEY";
	protected static final String SELECTED_URI_KEY = "TaskList.SELECTED_URI_KEY";
	protected static final String SPEECH_MATCHES_KEY = "TaskList.SPEECH_MATCHES_KEY";
    
    private ArrayList<Integer> mTaskIds = null;
    public static final String EXTRA_QUERY_PERSON_ID = "com.flingtap.done.extra.EXTRA_QUERY_FILTER_TASK_IDS"; // When receiving a phone call, this extra is used to restrict the tasks to show the user relavent tasks for that call. No longer an interger. Now a String to accomidate the UUID lookup_key from ContactsContract Android 2.x series.
	private TaskListCursorFactory cursorFactory = null;

	private boolean isCallminder = false;
	private int selectedIndex = -1; 

    protected void onNewIntent(Intent intent){
    	super.onNewIntent(intent);
    	try{
    		//Log.v(TAG, "onNewIntent(..) called.");
    		setIntent(intent); // TODO: !! Not sure about this change. Trying to fix rotation resumption during callminder (filtered activity is stopped, but then original activity assumes start because of singleTop designation).
    		doSetupWithIntent(intent);
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR00088", exp);
    		ErrorUtil.handleExceptionFinish("ERR00088", exp, this);
    	}
    }

    String mActivityTitle = null;
    String mActivityTitleDefault = null;
    
    protected void doSetupWithIntent(final Intent intent) {
		isCallminder = false;
		cursorFactory.setTaskIds(null); // How is it possible that cursorFactory is null here? (I got an error saying so.

		mActivityTitle = null;
		
		// Do some setup based on the action being performed.
		final String action = intent.getAction();
		if (null == action || Intent.ACTION_MAIN.equals(action)) { // When launched from the home screen the action is null.
			// Requested to main: set that state, and the data being edited.
			mState = STATE_VIEW;
			mURI = Task.Tasks.CONTENT_URI;
			mSearching = false;
			// Assume good outcome.
			setResult(RESULT_OK);
			
			handler.sendEmptyMessageDelayed(MSG_START_PHONE_STATE_MONITOR, 5000);
			
		} else if (Intent.ACTION_VIEW.equals(action)) {
			// Requested to view: set that state, and the data being viewed.
			mState = STATE_VIEW;
			mURI = intent.getData();

			if( -1 != ContentUris.parseId(mURI) ){
				// Received search result intent from the SearchManager.
				// Since the user has selected a specific task from the suggestion list
				//  we should forward the user to view that task immediately.
				Intent viewTaskIntent = new Intent(intent);
				viewTaskIntent.setComponent(null);
				viewTaskIntent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
				startActivity(viewTaskIntent);
				finish();
				return;
			}

			mSearching = false;
			
		} else if(Intent.ACTION_ATTACH_DATA.equals(action) ) {
			// FIXME: Shoulddisplay different title.
			// Requested to attach: set that state, and the data being attached.
			mState = STATE_ATTACH;
			mURI = Task.Tasks.CONTENT_URI;
			
			// FIXME: Doesn't survive rotations.
			mAttachUri = intent.getData();
			mSearching = false;
			
		}else if( Intent.ACTION_PICK.equals(action) ){
			// Requested to pick: set that state, and the data being attached.
			mState = STATE_PICK;
			mURI = intent.getData();
			mSearching = false;
			
		}else if( Intent.ACTION_CREATE_SHORTCUT.equals(action) ){
			// Requested to create a shortcut
			mState = STATE_CREATE_SHORTCUT;
			mURI = Task.Tasks.CONTENT_URI;
			mSearching = false;
			
		}else if( Intent.ACTION_GET_CONTENT.equals(action) ){
			// Requested to get content: set that state, and the data being attached.
			mState = STATE_GET_CONTENT;
			mURI = intent.getData();
			mSearching = false;
			
		} else if( Intent.ACTION_SEARCH.equals(action)) {  
			// Log.v(TAG, "ACTION_SEARCH called");
			// FIXME: Search parameters don't survive rotation.
			
	        // mState = ; // Leave the state the way it was before we started searching.
			mSearching = true;
			mURI = intent.getData();
			if(null == mURI){
				mURI = Task.Tasks.CONTENT_URI;
			}
			
			if( null != mURI.getQueryParameter(EXTRA_QUERY_PERSON_ID) ){ // Caller included a person id.
				
				CancelNotificationBroadcastReceiver.cancelNotification(this, intent);
				
				Cursor taskIdForPersonCursor = null;
    			taskIdForPersonCursor = createDefaultFilteredTasksForPersonCursor(this, intent.getData().getQueryParameter(EXTRA_QUERY_PERSON_ID), new String[]{Task.TaskAttachments.TASK_ID});

				if( taskIdForPersonCursor.getCount() > 0 ){ 
					mTaskIds = new ArrayList<Integer>();
					while(taskIdForPersonCursor.moveToNext()){
						mTaskIds.add(taskIdForPersonCursor.getInt(0));
					}
					cursorFactory.setTaskIds(mTaskIds);
				}
				taskIdForPersonCursor.close();
				isCallminder = true;
				
				Cursor personCursor = null;
				
				final int sdkVersion = Integer.parseInt( Build.VERSION.SDK ); // Build.VERSION.SDK_INT was introduced after API level 3 and so is not compatible with 1.5 devices.
				
				if( 5 > sdkVersion ){ // Anrdoid 1.x series code.
					personCursor = getContentResolver().query(Uri.withAppendedPath(Contacts.People.CONTENT_URI, intent.getData().getQueryParameter(EXTRA_QUERY_PERSON_ID)), new String[]{Contacts.People.DISPLAY_NAME}, null, null, null); 
				}else{ // Android 2.x series code.
					personCursor = getContentResolver().query(Uri.withAppendedPath(Uri.parse("content://com.android.contacts/contacts/lookup"), intent.getData().getQueryParameter(EXTRA_QUERY_PERSON_ID)), new String[]{Contacts.People.DISPLAY_NAME}, null, null, null);  // TODO: !!! Is this the right way to look this up in Android 2.x series?
				}

				assert null != personCursor;
				assert personCursor.getCount() == 1;
				personCursor.moveToFirst();
				String personName = personCursor.getString(0);
				personCursor.close();
				
				mActivityTitle = TextUtils.expandTemplate(getString(R.string.activity_taskListCOLONX), TextUtils.expandTemplate(getString(R.string.intent_contactEQUALSX), personName)).toString();
			}
			
		} else {
			// Whoops, unknown action! Bail.
			Log.e(TAG, "ERR0008P Unknown action "+action);
			ErrorUtil.handleExceptionNotifyUserFinish("ERR0008P", (Exception)(new Exception( action )).fillInStackTrace(), this);
			return;
		}
		assert mState != -1;
		assert mState == STATE_VIEW || mState == STATE_ATTACH || mState == STATE_PICK || mState == STATE_GET_CONTENT || mState == STATE_CREATE_SHORTCUT;
		assert mURI != null;
		assert mURI.equals(Task.Tasks.CONTENT_URI);
        //Log.d(TAG, "mState == " + mState);
        //Log.d(TAG, "mURI == " + mURI);
        //Log.d(TAG, "mSearching == " + mSearching);
		
        if( null != mCursor ){
        	stopManagingCursor(mCursor);
        	mCursor.close();
        }
        
        if( mSearching ){
        	String searchText = intent.getStringExtra(SearchManager.QUERY);
        	//Log.v(TAG, "searchText == " + searchText);
        	
	        mCursor = cursorFactory.runQuery(searchText);
        }else{
        	mActivityTitle = null;
        	mCursor = cursorFactory.runQuery(null);
        }       
//    	if( STATE_VIEW == mState ){
//    		if( !mIsLicensed ){
//    			mAdView.setVisibility(View.VISIBLE);
//    		}
//    	}else{
//    		if( null != mAdView ){
//    			mAdView.setVisibility(View.GONE);
//    		}
//    	}

        mAdapter = doSetupWithCursor(mCursor);
        setListAdapter(mAdapter);

        mAdapter.setFilterQueryProvider(cursorFactory);
    }
    
    private ResourceCursorAdapter mAdapter = null;
	private TaskListAdapter.TaskListFilter filter = null;
    
    /**
     * Create a cursor contains tasks which refer to the given person and which are not complete and not in the archive.
     * 
     * @param context
     * @param personId
     * @param columns
     * @return
     */
	public static Cursor createDefaultFilteredTasksForPersonCursor(Context context, String personId, String[] columns) {

		final int sdkVersion = Integer.parseInt( Build.VERSION.SDK ); // Build.VERSION.SDK_INT was introduced after API level 3 and so is not compatible with 1.5 devices.
		
		Uri callminderTasksUri = null;
		if( 5 > sdkVersion ){ // Android 1.x series code.
			callminderTasksUri = Uri.parse("content://"+Task.AUTHORITY+"/tasks/callminder").buildUpon().appendQueryParameter(Task.TaskAttachments._URI,Uri.withAppendedPath(Contacts.People.CONTENT_URI, personId).toString()).build(); 
		}else{ // Android 2.x series code.
			callminderTasksUri = Uri.parse("content://"+Task.AUTHORITY+"/tasks/callminder").buildUpon().appendQueryParameter(Task.TaskAttachments._URI,Uri.withAppendedPath(Uri.parse("content://com.android.contacts/contacts/lookup"), personId).toString()).build(); 
		}
		// Log.i(TAG, "Task URI " + callminderTasksUri.toString());
		
		Cursor taskIdForPersonCursor = context.getContentResolver().query(
				callminderTasksUri, columns, null, 
				null, 
				Task.Tasks.DEFAULT_SORT_ORDER_JOIN_VERSION);
			assert null != taskIdForPersonCursor;
		return taskIdForPersonCursor;
	}
    
    protected ResourceCursorAdapter doSetupWithCursor(Cursor cursor){
    	// TODO: !!! Why can't I just re-use the same Adapter? 
    	TaskListAdapter adapter = new TaskListAdapter(
    			this,
    			R.layout.task_list_item, 
    			cursor,
    			this);
        return adapter;
    }
    
	public void onClick(View v) {
		if (v.getId() == R.id.task_list_speak_item_button) {
        	startVoiceRecognitionActivity();
		}
	}
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        try{
        	SessionUtil.onSessionStart(this);

        	this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
			
    		//Log.v(TAG, "onCreate(..) called.");
	        // Allow user to use menu shortcuts without opening menu.
		    setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL );
		    
		    setContentView(R.layout.task_list);
		    
		    final Intent intent = getIntent();
		    
			cursorFactory = new TaskListCursorFactory(this);
		
			mIsLicensed = LicenseUtil.hasAnyLicense(this);
/*	        if( !mIsLicensed ){
	        	mAdView = (AdView) findViewById(R.id.ad); // Must occur before doSetupWithIntent(..)
	        }
*/	        
			doSetupWithIntent(intent);

	        getListView().setTextFilterEnabled(true);
	        getListView().setOnKeyListener(this);			

			// TODO: !!! Why not just use the URI as a parameter (so no need for the if-else statements).
			if( mState == STATE_VIEW ){
				Event.onEvent(Event.VIEW_TASK_LIST, null); // Map<String,String> parameters = new HashMap<String,String>();
			}else if ( mState == STATE_ATTACH ){
				Event.onEvent(Event.ATTACH_TASK, null); // Map<String,String> parameters = new HashMap<String,String>();
			}else if ( mState == STATE_PICK ){
				Event.onEvent(Event.PICK_TASK, null); // Map<String,String> parameters = new HashMap<String,String>();
			}else if ( mState == STATE_GET_CONTENT ){
				Event.onEvent(Event.GET_TASK_CONTENT, null); // Map<String,String> parameters = new HashMap<String,String>();
			}else if ( mState == STATE_CREATE_SHORTCUT ){
				Event.onEvent(Event.CREATE_TASK_SHORTCUT, null); // Map<String,String> parameters = new HashMap<String,String>();
			}
			
            EditText textBox = (EditText)findViewById(R.id.task_list_add_item_text);
            FastAdderBoxClickHandler mFastAdderBoxClickHandler = new FastAdderBoxClickHandler(textBox);

            ImageButton addButton = (ImageButton)findViewById(R.id.task_list_add_item_button);
            addButton.setVisibility(View.VISIBLE);
            addButton.setOnClickListener(mFastAdderBoxClickHandler);
            textBox.setVisibility(View.VISIBLE);
            if( null == icicle ){
                textBox.requestFocus();
            }
            textBox.setOnKeyListener(mFastAdderBoxClickHandler);

            // Check to see if a recognition activity is present

            ImageButton speakButton = (ImageButton)findViewById(R.id.task_list_speak_item_button);
            PackageManager pm = getPackageManager();
            List<ResolveInfo> activities = pm.queryIntentActivities(// Only takes 4 milliseconds.
                    new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
            if (activities.size() != 0) {
                speakButton.setVisibility(View.VISIBLE);
                speakButton.setOnClickListener(this);
            }

			// addParticipant(new SearchServicesMenuPart(this));   // TODO: !!!! Re-enable search services.

			// Register context menu listener (so we hear about context menu events) 
			getListView().setOnCreateContextMenuListener(this);
			
        }catch(HandledException h){ // Ignore.
        }catch(Exception exp){
        	Log.e(TAG, "ERR00089", exp);
        	ErrorUtil.handleExceptionFinish("ERR00089", exp, this);
        }finally{
        	try{
        		if( !isFinishing() ){
        			if( EulaPromptPart.eulaAcceptanceRequired(this)){
        				new EulaPromptPart().promptWithEula(this);
        			}else{
        				// Handle feature introduction.
        				if( FeatureIntroduction.intro_introductionNeeded(this) ){
        					FeatureIntroduction.launch(this, true);
        				}
        			}
        		}
       			TrialPeriodUtil.checkAndNotify(this); 

            }catch(HandledException h){ // Ignore.
            }catch(Exception exp){ // TODO: !!! Add FlurryAgent.closeSession(..) calls in exception handling (or maybe add finally block and check isFinishing() ) so that Session is stopped when needed.
            	Log.e(TAG, "ERR000D8", exp);
            	ErrorUtil.handleExceptionFinish("ERR000D8", exp, this);
            }
        }
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	try{
    		FilterBitsRefreshReceiver.registerReceiver(this, mFilterBitsRefreshReceiver);
    		checkFilterBitsCurrent();
        }catch(HandledException h){ // Ignore.
        }catch(Exception exp){ 
        	Log.e(TAG, "ERR000KY", exp);
        	ErrorUtil.handleExceptionFinish("ERR000KY", exp, this);
        }
    }

    @Override
    protected void onStop() {
    	super.onStop();
    	try{
    		FilterBitsRefreshReceiver.unregisterReceiver(this, mFilterBitsRefreshReceiver);
        }catch(HandledException h){ // Ignore.
        }catch(Exception exp){ 
        	Log.e(TAG, "ERR000L1", exp);
        	ErrorUtil.handleExceptionFinish("ERR000L1", exp, this);
        }
    }
    
	private void checkFilterBitsCurrent() throws TimestampException {
		FilterBitsUpdateTimestamp filterBitsUpdatedTimestamp = new FilterBitsUpdateTimestamp(this);
		filterBitsUpdatedTimestamp.ensureExists();

		Calendar mUpdateFilterBitsCachedCalendar = Calendar.getInstance();
		mUpdateFilterBitsCachedCalendar.set(Calendar.HOUR_OF_DAY, 0);
		mUpdateFilterBitsCachedCalendar.set(Calendar.MINUTE, 0);
		mUpdateFilterBitsCachedCalendar.set(Calendar.SECOND, 0);
		mUpdateFilterBitsCachedCalendar.set(Calendar.MILLISECOND,0);
		
		if( filterBitsUpdatedTimestamp.getTimeInMillis() < mUpdateFilterBitsCachedCalendar.getTimeInMillis() ){
			FilterUtil.applyFilterBits(this);
		}
	}

	private void updateIsArchiveActive() {
		Message msg = new Message();
		msg.what = MSG_UPDATE_IS_ARCHIVE_FLAGS;
		handler.sendMessage(msg);
	}
    
    private static final int MSG_UPDATE_IS_ARCHIVE_FLAGS = 0; 
    private static final int MSG_TOGGLE_TASK_COMPLETE    = 1; 
    private static final int MSG_START_PHONE_STATE_MONITOR = 2; 
    private Handler handler = new Handler() {
    	ContentValues cv = new ContentValues(2); // Assumes this is a single threaded app.
		public void handleMessage(Message msg) {
			//Log.v(TAG, "Next is handleMessage(..)");
	        switch (msg.what) {
	            case MSG_UPDATE_IS_ARCHIVE_FLAGS:
	            	
	            	mIsArchiveActive = FilterUtil.isArchiveFilterActive(TaskList.this);
	            	mIsArchiveEnabled = ArchiveUtil.isArchiveEnabled(TaskList.this);
	            	
	            	Cursor filterTitleCursor = getContentResolver().query(Task.Filter.CONTENT_URI, new String[]{Task.Filter.DISPLAY_NAME, Task.Filter._DISPLAY_NAME_ARRAY_INDEX}, Task.Filter._SELECTED+"=? AND ("+Task.Filter._PERMANENT+"!=? OR "+Task.Filter._PERMANENT+" IS NULL)", new String[]{String.valueOf(Task.Filter.SELECTED_TRUE), TaskProvider.ID_FILTER_BASIC}, null);
	            	if( filterTitleCursor.moveToFirst() ){
	            		if( filterTitleCursor.isNull(1)){
	            			mActivityTitleDefault = TextUtils.expandTemplate(getString(R.string.activity_taskListCOLONX), filterTitleCursor.getString(0)).toString();
	            		}else{
	            			mActivityTitleDefault = TextUtils.expandTemplate(getString(R.string.activity_taskListCOLONX), getResources().getStringArray(R.array.array_stockFilters)[filterTitleCursor.getInt(1)]).toString();
	            		}
	            	}else{
	            		mActivityTitleDefault = getString(R.string.activity_taskList);
	            	}
	            	filterTitleCursor.close();
	            	
	        		if( null != mActivityTitle ){
	        			setTitle(mActivityTitle);
	        		}else{
	        			setTitle(mActivityTitleDefault);
	        		}
	            	
	            	break;
	            case MSG_TOGGLE_TASK_COMPLETE:
	            	updateTaskCompleteFlag(TaskList.this, getContentResolver(), (Uri)msg.obj, 1==msg.arg1, cv);
	            	break;
	            case MSG_START_PHONE_STATE_MONITOR:
	    			if( MonitorPhoneStateService.areCallmindersEnabled(TaskList.this)){
	    				//Log.v(TAG, "Adding Phone State Monitor Service.");
	    				MonitorPhoneStateService.startService(TaskList.this);
	    			}
	            	break;
	        }
	            
	        super.handleMessage(msg); 
		}
    };
    
    private class FastAdderBoxClickHandler implements View.OnKeyListener, View.OnClickListener {

    	EditText mTextBox = null;
    	public FastAdderBoxClickHandler(EditText textBox){
    		mTextBox = textBox;
    		assert null != mTextBox;
    	}

        private static final int MESSAGE_SET_FOCUS_TO_FAST_ADD_EDIT_TEXT = 0;
        private Handler mHandler = new Handler(){
        	private Runnable mSetFocusToFastAddEditText = null;
        	public void handleMessage(Message message){
        		switch(message.what){
        			case MESSAGE_SET_FOCUS_TO_FAST_ADD_EDIT_TEXT: 
        				// Clear edit text box.
        				mTextBox.setText("");
        				
	    				if( null == mSetFocusToFastAddEditText ){
	    					mSetFocusToFastAddEditText = new Runnable(){
	    						public void run() {
	    							mTextBox.requestFocus();
	    						}
	    					};
	    				}
	    				postDelayed(mSetFocusToFastAddEditText, 100);// TODO: !! Hack because requestFocus didn't work directly.
        		}
            }
        };
    	
		public boolean onKey(View v, int keyCode, KeyEvent event) {
	        if (event.getAction() == KeyEvent.ACTION_DOWN) {
	            switch (keyCode) {
	                case KeyEvent.KEYCODE_DPAD_CENTER:
	                case KeyEvent.KEYCODE_ENTER:
						handleEvent();
			            return true;
	            }
	        }
			return false;
		}
		
		public void onClick(View v) {
			handleEvent();
		}

		private void handleEvent() {
			try{
				//Log.v(TAG, "mFastAdderBoxClickHandler.onClick(..) called." );
				String newTaskText = mTextBox.getText().toString().trim();
				if( newTaskText.length() == 0 ){
					mTextBox.requestFocus();	
					return;
				}
				ContentValues cv = new ContentValues(1);
				cv.put(Task.Tasks.TASK_TITLE, newTaskText);
				Uri newUri = getContentResolver().insert(Task.Tasks.CONTENT_URI, cv);
				if( null == newUri ){
					Log.e(TAG, "ERR0008Q Failed to add task.");
					ErrorUtil.handle("ERR0008Q", "Failed to add task.", this);
					return;
				}					
				// Apply filter bit.
				FilterUtil.applyFilterBits(TaskList.this, newUri);

				// Refresh cursor.
				mCursor.requery();
				
				Cursor cursor = getContentResolver().query(newUri, new String[]{Task.Tasks._FILTER_BIT}, null, null, null);
				assert null != cursor;
				if( !cursor.moveToFirst() ){
					Log.e(TAG, "ERR0008W"); // Failed to find entry that was just added
					ErrorUtil.handle("ERR0008W", "Failed to find entry that was just added.", this);
					return;
				} 
				if( Task.Tasks.FILTER_OUT.equals(cursor.getString(0)) ){
					Toast.makeText(TaskList.this, R.string.toast_taskAddedButNotVisibleWithCurrentFilterSettings, Toast.LENGTH_LONG).show();
				}else{
					Toast.makeText(TaskList.this, R.string.toast_taskAdded , Toast.LENGTH_SHORT).show(); 
				}
				cursor.close();
				
				mHandler.dispatchMessage(mHandler.obtainMessage(MESSAGE_SET_FOCUS_TO_FAST_ADD_EDIT_TEXT));

			}catch(HandledException h){ // Ignore.
			}catch(Exception exp){
				Log.e(TAG, "ERR0008A", exp);
				ErrorUtil.handleExceptionNotifyUser("ERR0008A", exp, TaskList.this);
			}
		}
    }
    
	@Override
	protected void onDestroy() {
		super.onDestroy();
		try{
			//Log.v(TAG, "onDestroy(..) was called.");
			if( null != mCursor ){
				mCursor.close();
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0008B", exp);
			ErrorUtil.handleException("ERR0008B", exp, this);
		}
	}

	private long selectedId = Constants.DEFAULT_NON_ID;
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, view, menuInfo);
    	try{
    		AdapterView.AdapterContextMenuInfo menuInfo2 = (AdapterView.AdapterContextMenuInfo)menuInfo;

    		TaskListAdapter.ViewHolder holder = (ViewHolder)menuInfo2.targetView.getTag();
    		CheckBox checkBox = (CheckBox)holder.taskComplete;
    		if( checkBox.isChecked() ){ 
    			menuItemMarkDone = menu.add(0, MARK_ID, 1, R.string.context_markIncomplete);
    		}else{
    			menuItemMarkDone = menu.add(0, MARK_ID, Menu.FIRST+1, R.string.context_markComplete);
    		}
	                    
			// Find selected item.
			AdapterView.AdapterContextMenuInfo adapterViewMenuInfo = (AdapterView.AdapterContextMenuInfo)menuInfo;
		    Uri uri = ContentUris.withAppendedId(mURI, adapterViewMenuInfo.id);
		    
		    if( !mCursor.isClosed() ){
		    	menu.setHeaderTitle(mCursor.getString(CURSOR_TASK_TITLE));
		    }

		    if (LabelUtil.isLabelsEnabled(this)) {
		    	MenuItem labelMenuItem = menu.add(Menu.NONE, LABEL_ID, Menu.FIRST+30, R.string.context_labelTask);
		    	labelMenuItem.setAlphabeticShortcut('l'); 
		    	labelMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener(){
		    		public boolean onMenuItemClick(MenuItem menuitem) {
		    			try{
		    				// Prepare event info. 
		    				Event.onEvent(Event.TASK_LIST_LABELS_CONTEXT_MENU_ITEM_CLICKED, null); 
		    				
		    				// Find selected item.
		    				AdapterView.AdapterContextMenuInfo adapterViewMenuInfo = (AdapterView.AdapterContextMenuInfo)menuitem.getMenuInfo();

		    				selectedId = adapterViewMenuInfo.id;
		    				showDialog(APPLY_LABELS_DIALOG_ID);
		    				return true;
		    				
		    			}catch(Exception exp){
		    				Log.e(TAG, "ERR000AS", exp);
		    				ErrorUtil.handleException("ERR000AS", exp, TaskList.this);
		    			}
		    			return false;
		    		}
		    	});   
		    }
		    if( mIsArchiveEnabled ){
		    	if( !mIsArchiveActive ){
			    	MenuItem archiveMenuItem = menu.add(0, ARCHIVE_ID, Menu.FIRST+8, R.string.context_archiveTask);
		    		archiveMenuItem.setAlphabeticShortcut('r');
		    	}else{
			    	MenuItem archiveMenuItem = menu.add(0, UNARCHIVE_ID, Menu.FIRST+8, R.string.context_unarchiveTask);
		    		archiveMenuItem.setAlphabeticShortcut('r');
		    	}
		    }
		    
// Strange behavior, I seem to be able to pick task to attach to, but then I'm left in a strange state where I can't open a task.
//	    	MenuItem archiveMenuItem = menu.add(0, SET_AS_ID, Menu.FIRST+50, R.string.edit_task_context_menu_attach);

		    // Edit action 
		    MenuItem viewMenuItem = menu.add(Menu.CATEGORY_ALTERNATIVE, EDIT_ID, 2, R.string.context_editTask);
		    viewMenuItem.setAlphabeticShortcut('e');

		    // ... and ends with the delete command. 
		    MenuItem deletMenuItem = menu.add(Menu.CATEGORY_ALTERNATIVE, DELETE_ID, 4, R.string.context_deleteTask);

    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR0008C", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR0008C", exp, TaskList.this);
    	}
    }
    
	public void onCheckedChanged(CompoundButton button, boolean checked) {
		try{

    		handler.sendMessage( handler.obtainMessage(MSG_TOGGLE_TASK_COMPLETE, checked?1:0, 0, ContentUris.withAppendedId(Tasks.CONTENT_URI, ((TaskListAdapter.ViewHolder)button.getTag()).taskId)) );
    		// TODO: !!! Consider telling the adapter to toggle the shading for the record directly so we don't need to wait for the notification from the cursor.

		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0008D", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0008D", exp, TaskList.this);
		}
	}

	static void updateTaskCompleteFlag(Context context, ContentResolver resolver, Uri taskUri, boolean checked, ContentValues cv) {
		if( checked ){
			cv.put(Task.Tasks.COMPLETE, Task.Tasks.COMPLETE_TRUE);
			cv.put(Task.Tasks.COMPLETION_DATE, System.currentTimeMillis());
		}else{
			cv.put(Task.Tasks.COMPLETE, Task.Tasks.COMPLETE_FALSE);
			cv.putNull(Task.Tasks.COMPLETION_DATE);
		}
		int result = resolver.update(taskUri, cv, null, null);
		// Update the filter bits.
		FilterUtil.applyFilterBits(context, taskUri);
	}

    /**
     * 
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        try{
//        MenuItem searchUiMenuItem = menu.add(Menu.NONE, SEARCH_UI_ID, Menu.NONE, R.string.menu_search_ui);
//        searchUiMenuItem.setIcon(R.drawable.search);
//        searchUiMenuItem.setAlphabeticShortcut(SearchManager.MENU_KEY);
        	
        	MenuItem filterMenuItem = menu.add(0, FILTER_ID, 1, R.string.option_filter);
        	filterMenuItem.setIcon(R.drawable.filter);
        	
   	        if( mState != STATE_GET_CONTENT){
	    		MenuItem preferencesMenuItem = menu.add(0, PREFERENCES_ID, 3, R.string.option_settings);
	    		preferencesMenuItem.setIcon(android.R.drawable.ic_menu_preferences);

	        	MenuItem helpMenuItem = menu.findItem(HELP_ID);
	        	if( null == helpMenuItem ){
	        		helpMenuItem = menu.add(0, HELP_ID, Menu.FIRST+4, R.string.option_help);
	        		helpMenuItem.setIcon(android.R.drawable.ic_menu_help);
	        	}
	        	
	        	MenuItem addonsMenuItem = menu.add(0, ADD_ONS_ID, Menu.FIRST+6, R.string.option_addOns);

	        	menu.add(0, OPTION_SUPPORT_ID, Menu.FIRST+7, R.string.option_discussAndSupport);
	        	
	        	MenuItem tutorialsMenuItem = menu.add(0, TUTORIALS_ID, Menu.FIRST+8, R.string.option_tutorials);

	        	menu.add(0, OPTION_BLOG_ID, Menu.FIRST+9, R.string.option_blog);
	        	
	        	MenuItem featureIntroMenuItem = menu.add(0, FEATURE_INTRODUCTION_ID, Menu.FIRST+10, R.string.option_featuresIntroduction);
	        	
	        	menu.add(0, OPTION_DONATE_ID, Menu.FIRST+11, R.string.option_donate);
	        	
	        	// TODO: !! Consider how to integrate this data dump into a support forum process.	
	        	//FeedbackPart.addOptionsMenuItem(this, menu, FEEDBACK_ID, Menu.FIRST+12);

	        	MenuItem aboutMenuItem = menu.add(0, ABOUT_ID, Menu.FIRST+13, R.string.about);
	        	aboutMenuItem.setIcon(android.R.drawable.ic_menu_info_details);
   	        }
        	
        }catch(HandledException h){ // Ignore.
        }catch(Exception exp){
        	Log.e(TAG, "ERR0008E", exp);
        	ErrorUtil.handleExceptionNotifyUser("ERR0008E", exp, TaskList.this);
        }
        return true;
    }
    public static final String EVENT_BEGIN_TIME = "beginTime";
    public static final String EVENT_END_TIME = "endTime";
    public static final String EVENT_ALL_DAY = "allDay";
    public static final String EVENT_TITLE = "title";
    public static final String EVENT_DESCRIPTION = "description";
    public static final String EVENT_LOCATION = "eventLocation"; 

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);
    	try{
    		
    		// Allowing STATE_ATTACH, STATE_PICK, and STATE_CREATE_SHORTCUT to create new tasks. Not sure if good idea.

            MenuItem insertMenuItem = menu.findItem(INSERT_ID);
            if( null == insertMenuItem ){
                insertMenuItem = menu.add(0, INSERT_ID, 0, R.string.option_newTask);
                insertMenuItem.setIcon(android.R.drawable.ic_menu_add);
            }

            if( mState != STATE_GET_CONTENT){

                // TODO: !!! Delete/Archive should be disabled when no tasks are completed.
                if( mIsArchiveEnabled ){
                    if( !mIsArchiveActive ){
                        menu.removeItem(DELETE_COMPLETED_ID);

                        MenuItem archiveMenuItem = menu.findItem(ARCHIVE_ID);
                        if( null == archiveMenuItem ){
                            archiveMenuItem = menu.add(0, ARCHIVE_ID, 2, R.string.option_archive);
                            archiveMenuItem.setIcon(R.drawable.ic_menu_archive);
                        }
                    }else{
                        menu.removeItem(ARCHIVE_ID);

                        MenuItem menuDeleteCompleted = menu.findItem(DELETE_COMPLETED_ID);
                        if( null == menuDeleteCompleted ){
                            menuDeleteCompleted = menu.add(Menu.NONE, DELETE_COMPLETED_ID, 2, R.string.option_delete);
                            menuDeleteCompleted.setIcon(android.R.drawable.ic_menu_delete);
                        }
                    }
                }else{
                    menu.removeItem(ARCHIVE_ID);
                    MenuItem menuDeleteCompleted = menu.findItem(DELETE_COMPLETED_ID);
                    if( null == menuDeleteCompleted ){
                        menuDeleteCompleted = menu.add(Menu.NONE, DELETE_COMPLETED_ID, 2, R.string.option_delete);
                        menuDeleteCompleted.setIcon(android.R.drawable.ic_menu_delete);
                    }
                }
            }

    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR0008F", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR0008F", exp, TaskList.this);
    	}
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	try{
            switch (item.getItemId()) {
                case INSERT_ID:
            		Event.onEvent(Event.TASK_LIST_NEW_TASK_OPTIONS_MENU_ITEM_CLICKED, null); // Map<String,String> parameters = new HashMap<String,String>();
                    insertItem();
                    return true;
                case FILTER_ID:
            		Event.onEvent(Event.TASK_LIST_FILTER_OPTIONS_MENU_ITEM_CLICKED, null); // Map<String,String> parameters = new HashMap<String,String>();
            		launchFilter();
                    return true;
                case HELP_ID:
            		Event.onEvent(Event.TASK_LIST_HELP_OPTIONS_MENU_ITEM_CLICKED, null); 	
                	launchHelp();
    				return true;
                case ABOUT_ID:
					Event.onEvent(Event.TASK_LIST_ABOUT_OPTIONS_MENU_ITEM_CLICKED, null); 	
    	        	launchAbout();
                	return true;
                case PREFERENCES_ID:
					Event.onEvent(Event.TASK_LIST_PREFERENCES_OPTIONS_MENU_ITEM_CLICKED, null); 	
			        launchPreferences();
    	            return true;
//                case FEEDBACK_ID:
//            		// Add "Send Feedback" event.
//            		Event.onEvent(Event.TASK_LIST_FEEDBACK_OPTIONS_MENU_ITEM_CLICKED, null); 
//
//            		Toast.makeText(this, R.string.toast_youAreLeavingFlingTapDone, Toast.LENGTH_LONG).show();
//            		
//            		startActivityForResult(StaticWebActivity.createLaunchBrowserIntent(Uri.parse("http://flingtap.crowdsound.com/talk"), false), DEFAULT_ID);
//            		            		
//                	return true;
                case DELETE_COMPLETED_ID:
            		Event.onEvent(Event.TASK_LIST_DELETE_OPTIONS_MENU_ITEM_CLICKED, null); 		
            		deleteCompletedTasks();
					return true;
                case ARCHIVE_ID:
            		Event.onEvent(Event.TASK_LIST_ARCHIVE_OPTIONS_MENU_ITEM_CLICKED, null); 		
            		ArchiveMenuPart.archiveCompletedTasks(this);
                	return true;
                case TUTORIALS_ID:
                	launchTutorials();
                	return true;
                case ADD_ONS_ID:
                	launchAddons();                	
                	return true;
                case FEATURE_INTRODUCTION_ID:
                	HashMap<String,String> properties = new HashMap<String,String>(1);
                	properties.put(Event.TASK_LIST_OPTIONS_MENU__CHOICE, Event.TASK_LIST_OPTIONS_MENU__CHOICE__FEATURE_INTRODUCTION);
            		Event.onEvent(Event.TASK_LIST_OPTIONS_MENU, properties); 		
            		FeatureIntroduction.launch(this, false);            		
                	return true;
                	                	
                case OPTION_BLOG_ID:
                	launchBlog();
                	return true;
                	
                case OPTION_DONATE_ID:
                	launchDonations();
                	return true;
                	
                case OPTION_SUPPORT_ID:
                	launchDiscussSupport();
                	return true;

//                case SEARCH_UI_ID:
//                	onSearchRequested();
//                	break;
                }
    		
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR0008G", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR0008G", exp, TaskList.this);
    	}

        return super.onOptionsItemSelected(item);
    }

	private void launchBlog() {
		Uri theUri = Uri.parse("http://bit.ly/fxroNb");
		HashMap<String,String> properties = new HashMap<String,String>(1);
		properties.put(Event.VIEW_WEBSITE__NAME, Event.VIEW_WEBSITE__NAME__BLOG);
		properties.put(Event.VIEW_WEBSITE__URL, theUri.toString());
		Event.onEvent(Event.VIEW_WEBSITE, properties); 		
		launchWebBrowser(theUri);
	}

	private void launchDonations() {
		Uri theUri = Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=10479829");
		HashMap<String,String> properties = new HashMap<String,String>(1);
		properties.put(Event.VIEW_WEBSITE__NAME, Event.VIEW_WEBSITE__NAME__DONATE);
		properties.put(Event.VIEW_WEBSITE__URL, theUri.toString());
		Event.onEvent(Event.VIEW_WEBSITE, properties); 		
		launchWebBrowser(theUri);
	}
	
	private void launchDiscussSupport() {
		Uri theUri = Uri.parse("http://bit.ly/g5dRKD");
		HashMap<String,String> properties = new HashMap<String,String>(1);
		properties.put(Event.VIEW_WEBSITE__NAME, Event.VIEW_WEBSITE__NAME__FORUM);
		properties.put(Event.VIEW_WEBSITE__URL, theUri.toString());
		Event.onEvent(Event.VIEW_WEBSITE, properties); 		
		launchWebBrowser(theUri);
	}

	private void launchAddons() {
		Uri theUri = Uri.parse("http://bit.ly/fwuuBj");
		HashMap<String,String> properties = new HashMap<String,String>(1);
		properties.put(Event.VIEW_WEBSITE__NAME, Event.VIEW_WEBSITE__NAME__ADDONS);
		properties.put(Event.VIEW_WEBSITE__URL, theUri.toString());
		Event.onEvent(Event.VIEW_WEBSITE, properties); 		
		launchStaticWebBrowser(theUri);
	}

	private void launchTutorials() {
		Uri theUri = Uri.parse("http://bit.ly/f3GiBN"); // Removed: http://bit.ly/g0raiV because it didn't include a slash at the end and so wasn't compatible with other free website hosting services that require a .html or simple directory / at the end.
		HashMap<String,String> properties = new HashMap<String,String>(1);
		properties.put(Event.VIEW_WEBSITE__NAME, Event.VIEW_WEBSITE__NAME__TUTORIALS);
		properties.put(Event.VIEW_WEBSITE__URL, theUri.toString());
		Event.onEvent(Event.VIEW_WEBSITE, properties); 		
		launchStaticWebBrowser(theUri);
	}
	
	private void launchPrivacy() {
		Uri theUri = Uri.parse("http://bit.ly/g1Mcxm"); 
		HashMap<String,String> properties = new HashMap<String,String>(1);
		properties.put(Event.VIEW_WEBSITE__NAME, Event.VIEW_WEBSITE__NAME__TUTORIALS);
		properties.put(Event.VIEW_WEBSITE__URL, theUri.toString());
		Event.onEvent(Event.VIEW_WEBSITE, properties); 		
		launchStaticWebBrowser(theUri);
	}
	 
    private void deleteCompletedTasks(){
    	int count = TaskUtil.deleteCompleted(this);
		if( 0 < count ){
			Toast.makeText(this, TextUtils.expandTemplate(getText(R.string.toast_deletedNTask), String.valueOf(count)), Toast.LENGTH_LONG).show();
		}else{
			Toast.makeText(this, R.string.toast_noTasksDeleted, Toast.LENGTH_LONG).show();
		}
    }
    
	private void launchPreferences() {
		Intent prefIntent = ApplicationPreferenceActivity.getLaunchIntent(this);
		startActivityForResult(prefIntent, DEFAULT_ID);
	}

	private void launchAbout() {
		Intent aboutIntent = new Intent();
		aboutIntent.setComponent(new ComponentName(getPackageName(), AboutActivity.class.getName()));
		startActivityForResult(aboutIntent, DEFAULT_ID);
	}

	private void launchHelp() {
		Intent helpIntent = null;
		if( mIsArchiveEnabled ){
			helpIntent = StaticDisplayActivity.createIntent(TaskList.this, R.layout.help_task_list_archive, R.string.help_task_List);  
		}else{
			helpIntent = StaticDisplayActivity.createIntent(TaskList.this, R.layout.help_task_list, R.string.help_task_List);
		}
		assert null != helpIntent;
		startActivityForResult(helpIntent, DEFAULT_ID);
	}

	private void launchFilter() {
		if( isCallminder ){
			new AlertDialog.Builder(this)
			.setTitle(R.string.dialog_filterNotEditableNow)
			.setMessage(R.string.dialog_taskListWarnCustomFilter)
			.setPositiveButton(R.string.button_clear, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface arg0, int arg1) {
					try{
						onNewIntent(new Intent(Intent.ACTION_MAIN));
					}catch(HandledException h){ // Ignore.
					}catch(Exception exp){
						Log.e(TAG, "ERR0008T", exp);
						ErrorUtil.handleExceptionNotifyUser("ERR0008T", exp, TaskList.this);
					}
				}
			})
			.setNegativeButton(R.string.button_continue, null)
			.show();
		}else{
			actuallyLaunchFilter();
		}
	}
    
	private void actuallyLaunchFilter() {
		if( !mIsArchiveEnabled && !LabelUtil.isLabelsEnabled(this) 
				){
			// The "Basic" filter _id is always "1" because all other filters are deleted just before it is created.
			Intent filterElementListActivityIntent = FilterElementListActivity.createViewIntent(Task.Filter.CONTENT_URI.buildUpon().appendPath(TaskProvider.ID_FILTER_BASIC).build());
			startActivityForResult(filterElementListActivityIntent, DEFAULT_ID);
		}else{
			Intent filterListActivityIntent = FilterList.createViewIntent();
			startActivityForResult(filterListActivityIntent, DEFAULT_ID);

		}
	}
	
	Uri mSelectedUri = null;
	public boolean onContextItemSelected(MenuItem item) {
    	try{
    		AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
    		Long id = menuInfo.id;

    		View selectedView = ((AdapterView.AdapterContextMenuInfo)menuInfo).targetView; 
    		switch (item.getItemId()) {
    			case DELETE_ID:
					Event.onEvent(Event.TASK_LIST_DELETE_CONTEXT_MENU_ITEM_CLICKED, null); 
					mSelectedUri = ContentUris.withAppendedId(Task.Tasks.CONTENT_URI, id);    	
					showDialog( DELETE_TASK_DIALOG_ID );
    				return true;
    			case EDIT_ID:
					Event.onEvent(Event.TASK_LIST_EDIT_CONTEXT_MENU_ITEM_CLICKED, null); 
    				editItem(id);
    				return true;
    			case VIEW_ID:
					Event.onEvent(Event.TASK_LIST_VIEW_CONTEXT_MENU_ITEM_CLICKED, null); 
    				viewItem(id);
    				return true;
    			case MARK_ID:
					Event.onEvent(Event.TASK_LIST_MARK_CONTEXT_MENU_ITEM_CLICKED, null); 
    				markUnmarkItem(selectedView);
    				return true;        
    			case SEARCH_ID:
					Event.onEvent(Event.TASK_LIST_SEARCH_SERVICES_CONTEXT_MENU_ITEM_CLICKED, null); 
    				searchServicesForItem(id);
    				return true;
                case ARCHIVE_ID:
            		Event.onEvent(Event.TASK_LIST_ARCHIVE_CONTEXT_MENU_ITEM_CLICKED, null); 		
            		ArchiveMenuPart.archiveTask(this, id);
                	return true;    				
                case UNARCHIVE_ID:
            		Event.onEvent(Event.TASK_LIST_UNARCHIVE_CONTEXT_MENU_ITEM_CLICKED, null); 		
            		ArchiveMenuPart.unarchiveTask(this, id);
                	return true;    				
                case SET_AS_ID:
					mSelectedUri = ContentUris.withAppendedId(Task.Tasks.CONTENT_URI, id);    	
        	    	
        		    Intent intent = new Intent(Intent.ACTION_ATTACH_DATA, mSelectedUri);
        		    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        			Intent chooserIntent = Intent.createChooser(intent, getText(R.string.chooser_setAs)); // TODO: !! API omission (Should take resource id directly)
        			startActivity(chooserIntent);
                	return true;
    		}
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR0008H", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR0008H", exp, TaskList.this);
    	}

        return super.onContextItemSelected(item);
    }

	private void launchStaticWebBrowser(Uri data) {
		//Log.v(TAG, "launchStaticWebBrowser(..) called");
		startActivityForResult(StaticWebActivity.createLaunchBrowserIntent(data, false), DEFAULT_ID);
	}    
	
	private void launchWebBrowser(Uri data) {
		//Log.v(TAG, "launchStaticWebBrowser(..) called");
		Intent theIntent = new Intent(Intent.ACTION_VIEW, data);
		startActivity(theIntent);
	}    
    
    public void searchServicesForItem(final long selectedCursorId) {
    	Uri searchUri = ContentUris.withAppendedId(mURI, selectedCursorId);
    	Intent intent = new Intent("SEARCH_SERVICES", searchUri);
		intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NO_HISTORY);
		startActivityForResult(intent, DEFAULT_ID);
    }

    protected void markUnmarkItem(View selectedView){
    	if( selectedView != null ){
	    	CheckBox cb = (CheckBox)selectedView.findViewById(com.flingtap.done.base.R.id.task_completed);
	    	cb.setChecked(!cb.isChecked());
    	}
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	try{
    		Event.onEvent(Event.TASK_LIST_ITEM_CLICKED, null);
    		if( mState == STATE_VIEW ){
    			viewItem(id);
    		}else if(mState == STATE_ATTACH ){
    			addAttachment(id);
    		}else if( mState == STATE_PICK || mState == STATE_GET_CONTENT || mState == STATE_CREATE_SHORTCUT ){
    			pickOrGetContent(id);
    		}
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR0008I", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR0008I", exp, TaskList.this);
    	}
    }

    /**
     * Adds an attachment to the given task.
     */
    protected void addAttachment(long taskId) {
    	Intent attachIntent = new Intent(Intent.ACTION_DEFAULT, mAttachUri);
		UriInfo uriInfo = new UriInfo(attachIntent, mAttachUri, getContentResolver(), getPackageManager(), getResources(), null);

	    // Add AttachmentPart
    	AttachmentPart attachPart = new AttachmentPart(this);
    	Uri uri = attachPart.addAttachment(taskId, attachIntent, uriInfo.getLabel(), uriInfo.getIconBitmap(), uriInfo.getIconResource(), null);
    	
    	Toast.makeText(this, R.string.toast_attachmentAdded, Toast.LENGTH_SHORT).show();
    	
    	finish();
    }    
    
    /**
     * @param id
     */
    protected void pickOrGetContent(long id) {
		Uri url = ContentUris.withAppendedId(mURI, id);
    	if( mState == STATE_CREATE_SHORTCUT ){
    		Intent shortcutIntent = new Intent(Intent.ACTION_VIEW, url);
    		shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    		
    		String name = null;
    		Cursor nameCursor = getContentResolver().query(url, new String[]{Task.Tasks.TASK_TITLE}, null, null, null);
    		if( !nameCursor.moveToFirst()){
    			Log.e(TAG, "ERR0008U Failed to find uri record. " + url);
    			ErrorUtil.handle("ERR0008U", "Failed to find uri record. " + url, this);
    			setResult(RESULT_CANCELED); // We use cancel since this will be more clear to the caller. 
    			finish();
    			throw new HandledException();
    		}else{
    			name = nameCursor.getString(0);
    		}
    		nameCursor.close();
    		
    		Intent returnIntent = new Intent();
    		returnIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
    		returnIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
    		returnIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher_tasks));
    		setResult(RESULT_OK, returnIntent);
    	}else{ // TODO: ! Explicitly check for PICK mState.
    		// The caller is waiting for us to return the user selected task.
    		setResult(RESULT_OK, new Intent().setData(url));
    	}
    	finish();
    }

	protected void viewItem(long id) {
		Uri url = ContentUris.withAppendedId(mURI, id);
    	// Launch activity to view/edit the currently selected item
    	startActivityForResult(new Intent(Intent.ACTION_EDIT, url), DEFAULT_ID);
	}

    private final void insertItem() {
        // Launch activity to insert a new item
    	Intent insertIntent = new Intent(Intent.ACTION_INSERT, Task.Tasks.CONTENT_URI);
    	insertIntent.putExtra(TaskEditorTabActivity.CURRENT_TAB_LABEL, TaskEditorTabActivity.TAB_LABEL_DETAILS);
        startActivityForResult(insertIntent, DEFAULT_ID);
    }
    
    // TODO: This runs against standard Android behavior: When returning to a task from the editor, select the edit task in the task list. 

    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		try{
			//Log.v(TAG, "resultCode == " + resultCode);
			if( SharedConstant.RESULT_ERROR == resultCode ){
				ErrorUtil.notifyUser(this);
				return;
			}
			switch(requestCode){
				case INSERT_ATTACHMENT_REQUEST_CODE:
					setResult(resultCode);
					finish();
					break;
				case VOICE_RECOGNITION_REQUEST_CODE:
			        if (resultCode == RESULT_OK) {
			            // Fill the list view with the strings the recognizer thought it could have heard
			        	EditText textBox = (EditText)findViewById(R.id.task_list_add_item_text);
			            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			            if( matches == null || matches.size() == 0 ){
			            	Toast.makeText(this, R.string.toast_nothingRecognized, Toast.LENGTH_SHORT).show();
			            }else if( matches.size() == 1 ){
		    				Editable et = textBox.getEditableText();
		    				String textMatch = matches.get(0);
		    				if( et.length() != 0 ){
		    					et.insert(et.length(), " ");
		    				}
		    				et.insert(et.length(), textMatch);
		    				textBox.setText(et);
		    				textBox.setSelection(et.length());
			            }else if( matches.size() > 1 ){
			            	if( null != mSpeechMatches ){
			            		mSpeechMatches.clear();
			            		mSpeechMatches.addAll(matches);
			            		mVoiceSearchAapter.notifyDataSetChanged();
			            	}else{
			            		mSpeechMatches = matches;
			            	}
			            	showDialog(SPEECH_RESULT_DIALOG_ID);
			            }
			        }
			        break;
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0008J", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0008J", exp, TaskList.this);
		}
	}
    private ArrayList<String> mSpeechMatches = null;
    
    ArrayAdapter mVoiceSearchAapter = null;
    LabelUtil.ApplyLabelsOnClickListener clickListener = null;
    boolean isConfigChangeWithDialog = false;
	public Dialog onCreateDialog(int dialogId){
		super.onCreateDialog(dialogId);
		try{
			switch(dialogId){
				case SPEECH_RESULT_DIALOG_ID:
					mVoiceSearchAapter = new ArrayAdapter<String>(this, R.layout.simple_list_item_1,
				            mSpeechMatches);
			   		return new AlertDialog.Builder(this)
			        .setTitle(R.string.dialog_whatDidYouMeanToSay)
			        .setAdapter(mVoiceSearchAapter, new DialogInterface.OnClickListener(){
			    		public void onClick(DialogInterface dialog, int whichButton){
			    			try{
			    				EditText textBox = (EditText)findViewById(R.id.task_list_add_item_text);
			    				Editable et = textBox.getEditableText();
			    				String textMatch = mSpeechMatches.get(whichButton);
			    				if( et.length() != 0 ){
			    					et.insert(et.length(), " ");
			    				}
			    				et.insert(et.length(), textMatch);
			    				textBox.setText(et);
			    				textBox.setSelection(et.length());
			    			}catch(HandledException h){ // Ignore.
			    			}catch(Exception exp){
			    				Log.e(TAG, "ERR000D4", exp);
			    				ErrorUtil.handleExceptionNotifyUser("ERR000D4", exp, TaskList.this);
			    			}
			    		}
			   		})
			   		.create();
				case APPLY_LABELS_DIALOG_ID:
					if( null == clickListener ){
						clickListener = LabelUtil.createApplyLabelsOnClickListener(this, selectedId);
					}
					if( isConfigChangeWithDialog ){
						// data already loaded into clickListener during onRestoreInstanceState(..) 
						isConfigChangeWithDialog = false;
					}else{
						LabelUtil.loadApplyLabelDialogData(this, clickListener, selectedId);
					}
					return LabelUtil.onCreateDialogApplyLabel(this, clickListener, selectedId, dialogId); 
				case DELETE_TASK_DIALOG_ID:
			   		return new AlertDialog.Builder(this)
				        .setTitle(R.string.dialog_confirmDelete)
				        .setIcon(android.R.drawable.ic_dialog_alert)
				        .setMessage(R.string.dialog_areYouSure)
				        .setNegativeButton(R.string.button_no,null)
				        .setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener(){
				    		public void onClick(DialogInterface dialog, int whichButton){
				    			try{
				   					TaskUtil.deleteTask(TaskList.this, mSelectedUri);
				    			}catch(HandledException h){ // Ignore.
				    			}catch(Exception exp){
				    				Log.e(TAG, "ERR0008V", exp);
				    				ErrorUtil.handleExceptionNotifyUser("ERR0008V", exp, TaskList.this);
				    			}
				    		}
				   		})
				   		.create();
			}
			
		}catch(Exception exp){
			Log.e(TAG, "ERR000AR", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000AR", exp, this);
		}
		return null;
	}
	public void onPrepareDialog(int dialogId, Dialog dialog){
		super.onPrepareDialog(dialogId, dialog);
		try{
			switch(dialogId){
				case APPLY_LABELS_DIALOG_ID:
					clickListener.setTaskId(selectedId);
					LabelUtil.onPrepareDialogApplyLabel(this, dialog, selectedId);
					// ((AlertDialog)dialog).getListView().invalidate();
					break;
			}
		}catch(Exception exp){
			Log.e(TAG, "ERR000AT", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000AT", exp, this);
		}
	}

    /**
     * 
     * Note: In order to get events handled by a view, 
     *   you probably need to subclass it and override 
     *   its KeyEvent.Callback methods. 
     * 
     * TODO: !!! Consider whether E and V shortcuts are needed and fix code if needed.  
     *   
     */
    public boolean onKeyDown(int keyCode, KeyEvent event){
    	try{
    		//Log.v(TAG, "onKeyDown(..) called. " + keyCode);		
    		switch(keyCode){
    			case KeyEvent.KEYCODE_E: // This is never called because of the key word search.
    				// TODO: Is it still possible to use shortcut keys when keyword search is enabled? Maybe when holding down "menu" key.
    				long editId = getListView().getSelectedItemId(); 
    				if( Long.MIN_VALUE == editId ){
    					//Log.d(TAG, "List Item is not selected so no operation can be performed on it.");
    					return false;
    				}
    				editItem(editId);
    				return true;
//  				I just broke this, but since it isn't used currently, I'm going to skip fixing it. :)    				
//    			case KeyEvent.KEYCODE_V:  // This is never called because of the key word search.  
//    				long id = getListView().getSelectedItemId(); 
//    				if( Long.MIN_VALUE == id ){
//    					//Log.d(TAG, "List Item is not selected so not operation can be performed on it.");
//    					return false;
//    				}
//    				viewItem(id, v.getId());
//    				return true;
	            case KeyEvent.KEYCODE_DEL: {
	                long itemId = getListView().getSelectedItemId();
	                if ( ListView.INVALID_ROW_ID != itemId) {
	                	
						Event.onEvent(Event.TASK_LIST_DELETE_KEY_CLICKED, null); 
						mSelectedUri = ContentUris.withAppendedId(Task.Tasks.CONTENT_URI, itemId);    	
						showDialog( DELETE_TASK_DIALOG_ID );
	                }
	                break;
	            }
    		}
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR0008K", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR0008K", exp, TaskList.this);
    	}

    	return super.onKeyDown(keyCode, event);
    }
    protected void editItem(long editId){
        Uri editUri = ContentUris.withAppendedId(Task.Tasks.CONTENT_URI, editId);    	
        Intent editIntent = new Intent(Intent.ACTION_EDIT, editUri);
        startActivityForResult(editIntent, DEFAULT_ID);
    }

    /**
     * Fire an intent to start the speech recognition activity.
     */
    private void startVoiceRecognitionActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }
    
	protected void onPause() {
		super.onPause();
		try{
			SessionUtil.onSessionStop(this);
			//Log.v(TAG, "onPause(..) called.");		
			selectedIndex = getSelectedItemPosition();
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0008L", exp);
			ErrorUtil.handleExceptionFinish("ERR0008L", exp, TaskList.this);
		}
	}
	protected void onRestart() {
		super.onRestart();
		try{

		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0008M", exp);
			ErrorUtil.handleExceptionFinish("ERR0008M", exp, TaskList.this);
		}

	}
	
	protected void onResume() {
		super.onResume();
		try{  
			SessionUtil.onSessionStart(this);
			
			//Log.v(TAG, "onResume(..) called.");		
        	updateIsArchiveActive(); 

			setSelection(selectedIndex);  // Works, kinda (makes item at top, but not selected)
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0008N", exp);
			ErrorUtil.handleExceptionFinish("ERR0008N", exp, TaskList.this);
		}
	}
	
    @Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		try{
			//Log.v(TAG, "onSaveInstanceState(..) called.");	
			
			outState.putLong(SELECTED_ID_KEY, selectedId);
			if( -1 != getSelectedItemPosition() ){// -1 isn't documented,, but it is correct.
				outState.putInt(SELECTED_INDEX_KEY, getSelectedItemPosition());
			}
			if( null != clickListener && Constants.DEFAULT_NON_ID != clickListener.getTaskId() ){
				LabelUtil.onSaveInstanceStateApplyLabel(outState, clickListener);
			}
			if( null != mSelectedUri ){
				outState.putParcelable(SELECTED_URI_KEY, mSelectedUri);
			}
			if( null != mSpeechMatches ){
				outState.putStringArray(SPEECH_MATCHES_KEY, mSpeechMatches.toArray(new String[]{}));
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0008O", exp);
			ErrorUtil.handleExceptionFinish("ERR0008O", exp, TaskList.this);
		}
	}

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
    	super.onRestoreInstanceState(savedInstanceState);
    	try{
    		if( savedInstanceState.containsKey(SELECTED_INDEX_KEY) ){
    			//Log.v(TAG, "selectedIndex == " + savedInstanceState.getInt(SELECTED_INDEX_KEY));
    			setSelection(savedInstanceState.getInt(SELECTED_INDEX_KEY) );	
    		}
    		selectedId = savedInstanceState.getLong(SELECTED_ID_KEY);

    		if( savedInstanceState.containsKey(LabelUtil.SAVE_ID_KEY) ){
    			clickListener = LabelUtil.createApplyLabelsOnClickListener(this, selectedId);
				LabelUtil.onRestoreInstanceStateApplyLabel(savedInstanceState, clickListener);
				isConfigChangeWithDialog = true;
    		}
			if( savedInstanceState.containsKey(SELECTED_URI_KEY) ){
				mSelectedUri = savedInstanceState.getParcelable(SELECTED_URI_KEY);
			}
			if( savedInstanceState.containsKey(SPEECH_MATCHES_KEY) ){
				String[] list = (String[])savedInstanceState.getStringArray(SPEECH_MATCHES_KEY);
				mSpeechMatches = new ArrayList<String>();
				for(int i=0; i<list.length; i++){
					mSpeechMatches.add(list[i]); 
				}
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000AX", exp);
			ErrorUtil.handleExceptionFinish("ERR000AX", exp, TaskList.this);
		}
    }

	public boolean onKey(View v, int keyCode, KeyEvent event) {
		// Log.v(TAG, "onKey(..) called.");
		if(null == filter){
			filter = ((TaskListAdapter.TaskListFilter)(mAdapter.getFilter()));
		}
		
		if( KeyEvent.KEYCODE_DEL == keyCode &&
				event.getAction() == KeyEvent.ACTION_DOWN &&
				0 == filter.mFilterTextLength){
								
			long id = getListView().getSelectedItemId();
			// Log.v(TAG, "id="+id);
			if ( ListView.INVALID_ROW_ID != id) {
				Event.onEvent(Event.TASK_LIST_DELETE_KEY_CLICKED, null); 
				mSelectedUri = ContentUris.withAppendedId(Task.Tasks.CONTENT_URI, id);    	
				showDialog( DELETE_TASK_DIALOG_ID );
				return true;
			}
		}
		return false;
	}
}
