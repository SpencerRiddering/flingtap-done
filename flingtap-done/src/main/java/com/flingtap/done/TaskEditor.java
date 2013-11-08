// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.*;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseArray;
import android.view.*;
import android.widget.*;
import com.flingtap.common.ErrorUtil;
import com.flingtap.common.HandledException;
import com.flingtap.done.base.R;
import com.flingtap.done.provider.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * A generic activity for editing a task in a database. This can be used either
 * to view and edit a task (Intent.ACTION_EDIT) or create a new task (Intent.ACTION_INSERT).
 * 
 * TODO: Managed and unmanaged cursors should have different name prefixes.
 */
public class TaskEditor 
		extends CoordinatedActivity 
		implements View.OnClickListener, View.OnFocusChangeListener, View.OnKeyListener {

    public static final String ACTION_TASK_TITLE_CHANGED = "com.flingtap.done.intent.action.TASK_TITLE_CHANGED"; // Broadcast
	
	private static final String TAG = "TaskEditor";
	protected SharedPreferences settings = null;

	private static final int DATE_TYPE_DATE_INDEX = 0;
	private static final int DATE_TYPE_NONE_INDEX = 1;

	private static final int ALARM_TYPE_ALARM_INDEX = 0;
	private static final int ALARM_TYPE_NONE_INDEX = 1;
	
	protected Cursor mCursor;

    // TODO: !!! There seems to be some confusion between menu id and request id here.
    private static final int DEFAULT_ID= Menu.FIRST     + 50; // Used when we only care about error conditions.
//	private static final int EDIT_ID = Menu.FIRST 		+ 51;
	private static final int HELP_ID = Menu.FIRST 		+ 52;
	private static final int SAVE_ID = Menu.FIRST 		+ 53;
	private static final int DISCARD_ID = Menu.FIRST 	+ 54;
	private static final int DELETE_ID  = Menu.FIRST    + 55;
	private static final int ARCHIVE_ID = Menu.FIRST    + 56;
	private static final int COMPLETE_ID= Menu.FIRST   + 57;
	private static final int REVERT_ID  = Menu.FIRST 	+ 58;
	
	// This is our state data that is stored when freezing.
	private static final String ORIG_PRIORITY_INDEX = "ORIG_PRIORITY_INDEX";
	private static final String NEW_PRIORITY_INDEX = "NEW_PRIORITY_INDEX";
	private static final String ORIG_DUE_DATE_CONTENT = "ORIG_DUE_DATE_CONTENT";
	private static final String NEW_DUE_DATE_CONTENT = "NEW_DUE_DATE_CONTENT";
	private static final String ORIG_ALARM_CONTENT = "ORIG_ALARM_CONTENT";
	private static final String NEW_ALARM_CONTENT = "NEW_ALARM_CONTENT";
	private static final String SAVE_FOCUS = "SAVE_FOCUS";
	private static final String SAVE_REVERT_DATA = "SAVE_REVERT_DATA";
	

	public static final int PICK_DATE_OR_NONE_REQUEST 	= 11;
	public static final int PICK_ALARM_OR_NONE_REQUEST 	= 12;
	public static final int PICK_BUCKET_REQUEST 		= 13;

	// The different distinct states the activity can be run in.
	private static final int STATE_EDIT = 0;
//	private static final int STATE_VIEW = 1;
	private static final int STATE_INSERT = 2;  
	
	// The different distinct states the activity can result in.
	private static final int RESULT_STATE_SAVE = 0;
	private static final int RESULT_STATE_CANCEL = 1;

	/**
	 * The label columns for auto-complete.
	 */
	public static final String[] LABEL_PROJECTION = new String[] { Task.LabelsColumns._ID, // 0
			Task.LabelsColumns.DISPLAY_NAME, // 1
	};
	public static final int CURSOR__ID = 0;
	public static final int CURSOR_DISPLAY_NAME = 1;

	private int mState;
	private Uri mUri; 
	
	private EditText mTextDesc;
	private EditText mTextTitle;
	private Button pickDueDateButton = null;
	private Button pickPriorityButton = null;
	private Button pickAlarmButton = null;
	private Button mPickBucketButton = null;

	private ImageView alarmWarnImage = null;
	private ImageView dueDateWarnImage = null;
	
	protected Uri contactsURI = android.provider.Contacts.People.CONTENT_URI;

	private View focusedView = null;

	public static final String NOTIFY_ACTION = "com.flingtap.done.TaskEditor.NOTIFY_ACTION"; 
	
	private int[] STATE_SET_WARNING_ENABLED = new int[]{android.R.attr.state_enabled };
	private int[] STATE_SET_WARNING_DISABLED = new int[]{ };
	
	private java.text.DateFormat mTimeDateFormat = null;
	private java.text.DateFormat mDateDateFormat = null;
	
	private boolean mIsArchiveEnabled = false;
    private boolean mIsTaskArchived = false;
    private boolean mLabelsEnabled = false;
    
	private Cursor createCursor() {
		// Get the task!
		final Cursor cursor = managedQuery(mUri, TaskProjection.PROJECTION, null, null, null);
		assert null != cursor;
		if( !cursor.moveToFirst() ){
			Log.e(TAG, "ERR000HJ");
			ErrorUtil.handleExceptionNotifyUserFinish("ERR000HJ", (Exception)(new Exception(  )).fillInStackTrace(), TaskEditor.this);
			throw new HandledException();
		}
		cursor.registerContentObserver(new ContentObserver(new Handler()) {
			@Override
			public void onChange(boolean selfChange) {
				super.onChange(selfChange);
				try {
					if (null != cursor && !cursor.isClosed()) {
						boolean result = cursor.requery();
						assert result;
						cursor.moveToFirst(); // This could be false,, I
												// think when deleting the
												// task when empty and
												// cancel/back-button is
												// called.
					} else {
						//Log.w(TAG, "onChange(..) called when managed cursor is closed");
					}
				} catch (HandledException h) { // Ignore.
				} catch (Exception exp) {
					Log.e(TAG, "ERR00073", exp);
					ErrorUtil.handleExceptionFinish("ERR00073", exp, TaskEditor.this);
				}
			}
		});
		return cursor;
	}

	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		try {
			//Log.v(TAG, "onCreate(..) called");
        	this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

			// Allow user to use menu shortcuts without opening menu.
			setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL); // TODO: This mode
															// doesn't seem to
															// invoke
															// onSearchRequested
															// () correctly.
															// Report it as a
															// bug.

			// Restore preferences
			settings = getSharedPreferences(ApplicationPreference.NAME, MODE_PRIVATE);

			final Intent intent = getIntent();

			// Do some setup based on the action being performed.
			final String action = intent.getAction();
			if (action.equals(Intent.ACTION_EDIT) || action.equals(NOTIFY_ACTION) || action.equals(Intent.ACTION_VIEW)) { // ACTION_VIEW is no longer supported, so just edit the task instead. 
				mState = STATE_EDIT;
				Event.onEvent(Event.VIEW_TASK_EDITOR, null); // Map<String,String> parameters = new HashMap<String,String>();
				mUri = intent.getData();
			} else if (action.equals(Intent.ACTION_INSERT) ) {
				// *******************************************************
				// ACTION_INSERT is just like ACTION_EDIT except that the 
				//   menus are slightly different.
				// *******************************************************
				
				mState = STATE_INSERT;
				Event.onEvent(Event.VIEW_TASK_INSERT, null); // Map<String,String> parameters = new HashMap<String,String>();
				mUri = intent.getData();
			} else {
				// Whoops, unknown action! Bail.
				Log.e(TAG, "Unknown action, exiting");
				ErrorUtil.handleExceptionFinish("ERR00071", (Exception)(new Exception( intent.toURI() + " " + action )).fillInStackTrace(), this);
				return;
			}

			assert null != mUri;
			
			mTimeDateFormat = DateFormat.getTimeFormat(this);
			mDateDateFormat = DateFormat.getMediumDateFormat(this);			

			// Set the layout for this activity.
			setContentView(R.layout.task_details_editor);
			
			mCursor = createCursor();

			// *************************************************************
			// Setup common view elements
			// *************************************************************
			mLabelsEnabled = LabelUtil.isLabelsEnabled(this);
			
			// Setup label list
			mLabelListLayout = (LinearLayout) findViewById(R.id.label_list);
			assert null != mLabelListLayout;
			
			alarmWarnImage = (ImageView) findViewById(R.id.pickAlarmInPastWarning);
			assert null != alarmWarnImage;
			dueDateWarnImage = (ImageView) findViewById(R.id.pickDueDateInPastWarning);
			assert null != dueDateWarnImage;

			// *************************************************************
			// Setup editor.
			// *************************************************************
			View editorView = findViewById(R.id.task_editor);

			// *************************************
			// Button bar.
			// TODO: !!!! Use ViewStub with MergeView for button bar.
			// *************************************
			mIsArchiveEnabled = ArchiveUtil.isArchiveEnabled(this);
			mIsTaskArchived = ArchiveUtil.isTaskArchived(this, mUri.getLastPathSegment());
			if(mIsTaskArchived){
				editorView.findViewById(R.id.button_bar).setVisibility(View.GONE); // Hide the button bar if the task is archived.
			}else{
				// Revert button
				Button revertButton = (Button)editorView.findViewById(R.id.revert_button);
				if( mState == STATE_INSERT ){
					revertButton.setText(R.string.button_discard);
				}
				revertButton.setOnClickListener(this);
				
				// Save button
				((Button)editorView.findViewById(R.id.save_button)).setOnClickListener(this);
				
				// Complete button
				Button completeButton = ((Button)editorView.findViewById(R.id.complete_button));
				if( mState != STATE_INSERT ){
					if( mCursor.getInt(TaskProjection.COMPLETE_INDEX) == Task.Tasks.COMPLETE_TRUE_INT){
						completeButton.setText(R.string.button_incomplete); 
					}
					completeButton.setOnClickListener(this);
				}else{
					completeButton.setVisibility(View.GONE);
				}
			}
			
			pickPriorityButton = (Button) editorView.findViewById(R.id.priority);
			pickPriorityButton.setOnClickListener(this);

			pickDueDateButton = (Button) editorView.findViewById(R.id.pickDate);
			pickDueDateButton.setOnClickListener(this);
			

			mTextDesc = (EditText) editorView.findViewById(R.id.edit_description);
			mTextDesc.setOnFocusChangeListener(this);
			
			mTextTitle = (EditText) editorView.findViewById(R.id.edit_name);
			mTextTitle.setOnFocusChangeListener(this);
			
			pickAlarmButton = (Button) editorView.findViewById(R.id.pickAlarm);
			pickAlarmButton.setOnClickListener(this);

			// **********************************
			// Setup auto-complete label adder
			// **********************************
			if (mLabelsEnabled) {
				mLabelAutoCompleteTextBox = (AutoCompleteTextView) editorView.findViewById(R.id.task_labels_add_text_box);
				assert null != mLabelAutoCompleteTextBox;
				View labelAddButton = editorView.findViewById(R.id.task_label_add_button);
				assert null != labelAddButton;

				View focusController = findViewById(R.id.focus_controller);
				focusController.setOnFocusChangeListener(this);

				editorView.findViewById(R.id.task_labels_row).setVisibility(View.VISIBLE);

				Cursor labelListCursor = getContentResolver().query(Task.Labels.CONTENT_URI, LABEL_PROJECTION,
						Task.LabelsColumns._USER_APPLIED + "=?", new String[] { Task.LabelsColumns.USER_APPLIED_TRUE },
						Task.LabelsColumns.DISPLAY_NAME);
				assert null != labelListCursor;
				startManagingCursor(labelListCursor);

				PatchedResourceCursorAdapter labelAdapter = new PatchedResourceCursorAdapter(this,
						android.R.layout.simple_dropdown_item_1line, labelListCursor, 1) {
					boolean actOnIt = true;

					@Override
					public void bindView(View view, Context context, Cursor cursor) {
						try {
							//Log.v(TAG + ".PatchedResourceCursorAdapter", "bindView(..) called.");
							((TextView) view).setText(cursor.getString(mConvertToStringColumn));
						} catch (HandledException h) { // Ignore.
						} catch (Exception exp) {
							if (actOnIt) {
								Log.e(TAG, "ERR00077", exp);
								ErrorUtil.handleExceptionNotifyUser("ERR00077", exp, TaskEditor.this);
								actOnIt = false;
							}
						}
					}
				};
				FilterQueryProvider filterQueryProvider = new FilterQueryProvider() {

					boolean actOnIt = true;

					public Cursor runQuery(CharSequence filterText) {
						try {
							//Log.v(TAG + ".FilterQueryProvider", "runQuery(..) called.");
							StringBuffer where = null;
							where = new StringBuffer();

							// String where;
							where.append(Task.LabelsColumns._USER_APPLIED + "=?");
							if (filterText != null && filterText.length() != 0) {
								if (!("*".equals(filterText.toString().trim()))) { // TODO: !!! Consider whether * needs to be internationalized.
									where.append("AND (" + Task.LabelsColumns.DISPLAY_NAME + " LIKE "
											+ DatabaseUtils.sqlEscapeString(filterText.toString() + "%") + " OR "
											+ Task.LabelsColumns.DISPLAY_NAME + " LIKE "
											+ DatabaseUtils.sqlEscapeString("% " + filterText + "%") + ")");
								}
							}
							Cursor returnCursor = managedQuery(Task.Labels.CONTENT_URI, LABEL_PROJECTION,
									where == null ? null : where.toString(),
									new String[] { Task.LabelsColumns.USER_APPLIED_TRUE },
									Task.LabelsColumns.DISPLAY_NAME);
							assert null != returnCursor;

							return returnCursor;

						} catch (HandledException h) { // Ignore.
						} catch (Exception exp) {
							if (actOnIt) {
								Log.e(TAG, "ERR00078", exp);
								ErrorUtil.handleExceptionNotifyUser("ERR00078", exp, TaskEditor.this);
								actOnIt = false;
							}
						}
						// Return an empty cursor when an error occurs.
						MatrixCursor emptyCursor = new MatrixCursor(LABEL_PROJECTION);
						return emptyCursor;
					}

				};
				labelAdapter.setFilterQueryProvider(filterQueryProvider);
				mLabelAutoCompleteTextBox.setAdapter(labelAdapter);

				mLabelAutoCompleteTextBox.setOnKeyListener(this);

				labelAddButton.setOnClickListener(this);
			}

		} catch (HandledException h) { // Ignore.
		} catch (Exception exp) {
			Log.e(TAG, "ERR0006P", exp);
			ErrorUtil.handleExceptionFinish("ERR0006P", exp, this); // TODO: !!! Errors that reach here don't seem to cause the activity to finish().
		}
	}

	public boolean onKey(View v, int keyCode, KeyEvent event) {
		try {
			switch(v.getId()){
				case R.id.task_labels_add_text_box:
					// Log.v(TAG, "autoCompleteTextView.isPopupShowing()=="+autoCompleteTextView.isPopupShowing());
					// Log.v(TAG, "autoCompleteTextView.isSelected()=="+autoCompleteTextView.isSelected());
					
					// TODO: !! Strange situation where pressing enter key (not center D-Pad) when a completion item is _not_ selected results in the selected doesn't add the label, and focus moves to to whatever is below it. 
					
					if (((KeyEvent.KEYCODE_ENTER == keyCode) || (KeyEvent.KEYCODE_DPAD_CENTER == keyCode))
							&& event.getAction() == KeyEvent.ACTION_DOWN) {

						mIsAddLabelEnterClick = true;
						
						if( mLabelAutoCompleteTextBox.getListSelection() == ListView.INVALID_POSITION ){
							
							// Prepare event info. 
							Event.onEvent(Event.EDIT_TASK_ADD_LABEL_TEXT_BOX, null); 
							
							handler.sendMessageDelayed(Message.obtain(handler, MSG_APPLY_LABEL, mLabelAutoCompleteTextBox.getText().toString()), 50);
							return true;
						}
					}
					break;
			}
		} catch (HandledException h) { // Ignore.
		} catch (Exception exp) {
			Log.e(TAG, "ERR000DP", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000DP", exp, TaskEditor.this);
		}

		return false;
	}
	
	public void onFocusChange(View v, boolean hasFocus) {
		// Log.v(TAG, "onFocusChange(.., "+hasFocus+") called.");
		switch(v.getId()){
			case R.id.edit_name:
				if( !hasFocus ){
					persistTitle();
				}
				break;
			case R.id.edit_description:
				if( !hasFocus ){
					persistDescription();
				}
				break;
			case R.id.focus_controller:
				//Log.v(TAG, "onFocusChange("+hasFocus+") called.");
				if( hasFocus ){
					if( mIsAddLabelEnterClick){
						mIsAddLabelEnterClick = false;
						mLabelAutoCompleteTextBox.requestFocus();
					}else{
						View nextFocus = v.focusSearch(View.FOCUS_RIGHT);
						if( null != nextFocus ){
							nextFocus.requestFocus();
						}
					}
				}
				break;
			default:
				Log.e(TAG, "ERR000DL");
				ErrorUtil.handle("ERR000DL", "onFocusChange() called erroneously.", this);
		}
	}

	private void updateDateButtonEnabled(boolean enabled) {
		pickDueDateButton.setEnabled(enabled);
		pickDueDateButton.setFocusable(enabled);
		pickDueDateButton.invalidate();
	}

	private void updateAlarmButtonEnabled(boolean enabled) {
		pickAlarmButton.setEnabled(enabled);
		pickAlarmButton.setFocusable(enabled);
		pickAlarmButton.invalidate();
	}

	protected void onStop() {
		super.onStop();
		try {
			if( !isFinishing() ){
				//Log.v(TAG, "onStop(..) called.");
				focusedView = findViewById(R.id.task_editor).findFocus();
			}
		} catch (HandledException h) { // Ignore.
		} catch (Exception exp) {
			Log.e(TAG, "ERR0006Q", exp);
			ErrorUtil.handleExceptionFinish("ERR0006Q", exp, this);
		}
	}

	protected void onStart() {
		super.onStart();
		try {
			//Log.v(TAG, "onStart(..) called.");
			if( mLabelsEnabled ) {
				rebuildLabelListView();
			}
			//Log.v(TAG, "onStart(..) finished.");
		} catch (HandledException h) { // Ignore.
		} catch (Exception exp) {
			Log.e(TAG, "ERR0006R", exp);
			ErrorUtil.handleExceptionFinish("ERR0006R", exp, this);
		}
	}

	private static final int DIALOG_EDIT_ALARM 											= 6;
	private static final int DIALOG_EDIT_DUE_DATE										= 7;
	private static final int DIALOG_EDIT_PRIORITY										= 8;
	private static final int DIALOG_DELETE_TASK_ID										= 9;
	
	@Override
	protected Dialog onCreateDialog(int id) {
		//Log.v(TAG, "onCreateDialog("+id+") called.");
		Dialog dialog = super.onCreateDialog(id);
		if( null == dialog ){
			try {
				switch (id) {
					case DIALOG_EDIT_PRIORITY:
						dialog = editPriority();
						break;
					case DIALOG_EDIT_ALARM:
						dialog = editAlarm();
						break;
					case DIALOG_EDIT_DUE_DATE:
						dialog = editDueDate();
						break;
					case DIALOG_DELETE_TASK_ID:
						
						dialog = new AlertDialog.Builder(this)
				        .setTitle(R.string.dialog_confirmDelete)
   				        .setIcon(android.R.drawable.ic_dialog_alert)
				        .setMessage(R.string.dialog_areYouSure)
				        .setNegativeButton(R.string.button_no, null)
				        .setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener(){
				    		public void onClick(DialogInterface dialog, int whichButton){
				    			try{
			    					int count = TaskUtil.deleteTask(TaskEditor.this, mUri);
			    					if(1 != count){
					    				Log.e(TAG, "ERR000CA");
			    	    				ErrorUtil.handle("ERR000CA", "Delete of URI " + mUri + " removed " + count + " record(s).", TaskEditor.this);
			    					}
			    					if( null != mCursor ){
			    						mCursor.close();
			    						mCursor = null;
			    					}
				    			}catch(HandledException h){ // Ignore.
				    			}catch(Exception exp){
				    				Log.e(TAG, "ERR000CB", exp);
				    				ErrorUtil.handleExceptionNotifyUser("ERR000CB", exp, TaskEditor.this);
				    			}finally{
				    				finish();
				    			}
				    		}
				   		})
				   		.create();

						break;
                    default:

                        Log.e(TAG, "ERR000BG Unkonwn dialog id " + id);
                        ErrorUtil.handle("ERR000BG", "Unkonwn dialog id " + id, this);
                        return null;
				}
	
			} catch (HandledException h) { // Ignore.
			} catch (Exception exp) {
				Log.e(TAG, "ERR0006S", exp);
				ErrorUtil.handleExceptionNotifyUser("ERR0006S", exp, this);
				return null;
			}
		}
		if( null == dialog ){ // Don't want to add it to mManagedDialogs. 
			Log.e(TAG, "ERR000BF");
			ErrorUtil.handleException("ERR000BF", (Exception)(new Exception(String.valueOf(id))).fillInStackTrace(), this);
			return null;
		}

        if (mManagedDialogs == null) {
            mManagedDialogs = new SparseArray<Dialog>();
        }				
        mManagedDialogs.put(id, dialog); // This may replace a dialog (with the same id) which was restored after a config change. Maybe you want to dismiss it first, just in case.
        
		return dialog;
	}

	/**
	 * Prepare for the due date editor and then display the editor.
	 */
	private Dialog editDueDate() {
		Calendar dueDateCal = Calendar.getInstance();
		// if( !mCursor.isNull(TaskProjection.DUE_DATE_INDEX) ){
		if (mCursor.getLong(TaskProjection.DUE_DATE_INDEX) != Task.Tasks.DUE_DATE_NOT_SET_LONG) {
			dueDateCal.setTimeInMillis(mCursor.getLong(TaskProjection.DUE_DATE_INDEX));
		} else {
			dueDateCal.roll(Calendar.DAY_OF_YEAR, true); 
		}
		createDueDateClickListener(dueDateCal);
		
		final DatePickerDialog dpd = new DatePickerDialog(this, 
				mEditDueDateSetListener, 
				dueDateCal.get(Calendar.YEAR),
				dueDateCal.get(Calendar.MONTH), 
				dueDateCal.get(Calendar.DATE));
		dpd.setOnCancelListener(mEditDueDateSetListener);
		dpd.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.button_cancel), mEditDueDateSetListener);
		dpd.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.button_remove), mEditDueDateSetListener);
		return dpd;
	}

	/**
	 * Prepare for the alarm editor.
	 * 
	 */
	private Dialog editAlarm() {

		Calendar alarmCal = Calendar.getInstance();

		if (!mCursor.isNull(TaskProjection.ALARM_TIME_INDEX)) { // Edit existing
																// alarm
			alarmCal.setTimeInMillis(mCursor.getLong(TaskProjection.ALARM_TIME_INDEX));
		} else { // Create new alarm

			if (mCursor.getLong(TaskProjection.DUE_DATE_INDEX) != Task.Tasks.DUE_DATE_NOT_SET_LONG) { // There is a due date.
				// Set to due date
				alarmCal.setTimeInMillis(mCursor.getLong(TaskProjection.DUE_DATE_INDEX));
			} else { // No due date exists
				// Set to tomorrow.
				alarmCal.roll(Calendar.DAY_OF_YEAR, true);
			}
			// TODO: ! Add a preferences setting for the default alarm time.
			// Default alarm time is 10:00 AM.
			alarmCal.set(Calendar.HOUR, 10);
			alarmCal.set(Calendar.MINUTE, 0);
			alarmCal.set(Calendar.SECOND, 0);
			alarmCal.set(Calendar.AM_PM, Calendar.AM);
		}
		return showAlarmEditorDialog(alarmCal);
	}

	/**
	 * Actually present the alarm editor.
	 * 
	 * @param alarmCal
	 */
	private Dialog showAlarmEditorDialog(Calendar alarmCal) {
		createAlarmClickListener(alarmCal);
		
		DateTimePickerDialog dateTimePicker = new DateTimePickerDialog(this, alarmCal, mAlarmDateTimeSetListener, DateFormat.is24HourFormat(this)); 
		dateTimePicker.setOnCancelListener(mAlarmDateTimeSetListener);
		dateTimePicker.setButton(DialogInterface.BUTTON_NEGATIVE, getText(R.string.button_cancel), mAlarmDateTimeSetListener);
		dateTimePicker.setButton(DialogInterface.BUTTON_NEUTRAL, getText(R.string.button_remove), mAlarmDateTimeSetListener);

		return dateTimePicker;
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		try {
			// Log.v(TAG, "onActivityResult(..) called");
			if (SharedConstant.RESULT_ERROR == resultCode) {
				ErrorUtil.notifyUser(this);
				return;
			}
			if( RESULT_CANCELED == resultCode ){
				return;
			}
			switch(requestCode){
                // Do nothing.
			}
		} catch (HandledException h) { // Ignore.
		} catch (Exception exp) {
			Log.e(TAG, "ERR0006T", exp);
			ErrorUtil.handleExceptionFinish("ERR0006T", exp, this);
		}

	}

	private class PickPriorityListener implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
		private int mOrigSelectedIndex = -1;
		private int mNewSelectedIndex  = -1;		
		
		public int getOrigSelectedIndex() {
			return mOrigSelectedIndex;
		}
		public void setOrigSelectedIndex(int origSelectedIndex) {
			mOrigSelectedIndex = origSelectedIndex;
		}
		
		public int getNewSelectedIndex() {
			return mNewSelectedIndex;
		}
		public void setNewSelectedIndex(int newSelectedIndex) {
			mNewSelectedIndex = newSelectedIndex;
		}
		
		public PickPriorityListener(int selectedIndex){
			mOrigSelectedIndex = selectedIndex;
			mNewSelectedIndex = mOrigSelectedIndex;
		}

		public void onClick(final DialogInterface dialog, final int which) {
			try {
				if( DialogInterface.BUTTON_NEGATIVE == which ){
					handleCancel(dialog);
				}else if( DialogInterface.BUTTON_POSITIVE == which ){
					if( mNewSelectedIndex == mOrigSelectedIndex ){
						return;
					}
					
					// Prepare event info.
					Event.onEvent(Event.EDIT_TASK_EDIT_PRIORITY, null); 
					
					updateDisplayPriority(pickPriorityButton, mNewSelectedIndex); // Notice the fancy coding to map the presentation index to the priority value.

					// Update database.
					ContentValues cv = new ContentValues();
			        cv.put(Task.Tasks.PRIORITY, mNewSelectedIndex); 
					cv.put(Task.Tasks.MODIFIED_DATE, System.currentTimeMillis()); // Bump last modified date.
			
					if (1 != getContentResolver().update(mUri, cv, null, null)) {
						Log.e(TAG, "ERR000BH Failed to update task priority. ");
						ErrorUtil.handleExceptionNotifyUser("ERR000BH",  (Exception)(new Exception( mUri.toString() )).fillInStackTrace(), TaskEditor.this); 
						return;
					}					
					
					mOrigSelectedIndex = mNewSelectedIndex;
					
					// Update the filter bits.
					FilterUtil.applyFilterBits(TaskEditor.this, mUri);
					
				}else if( 0 <= which ){
					mNewSelectedIndex = 3-which;
				}
			} catch (HandledException h) { // Ignore.
			} catch (Exception exp) {
				Log.e(TAG, "ERR0007J", exp);
				ErrorUtil.handleExceptionNotifyUser("ERR0007J", exp, TaskEditor.this);
			}
		}
		
		private void handleCancel(DialogInterface dialog) {
			mNewSelectedIndex = mOrigSelectedIndex;
			ListView listView = ((AlertDialog)dialog).getListView();
			
			// listView.setSelection(mNewSelectedIndex); // NOTE: setSelection(..) does not "select" a radio group item. It's a different type of "selection". 
			if( !listView.isItemChecked(mOrigSelectedIndex) ){
				listView.setItemChecked(mOrigSelectedIndex, true);
			}
		}
		public void onCancel(DialogInterface dialog) {
			handleCancel(dialog);
		}
	}

	PickPriorityListener mPriorityDialogListener = null;

	private Dialog editPriority() {
		// Log.v(TAG, "editPriority() called.");

		final BaseAdapter priorityAdapter = new LeanAdapter<Object>(this, R.layout.priority_options_dialog_list_item) {

			public int getCount() {
				return 4;
			}

			public Object getItem(int position) {
				return 3 - position; // Just a trick to get the right value.
			}

			boolean actOnIt = true;

			public void bindView(View view, Context context, Object data, int position) {
				try {
					switch (position) {
						case 0:
							((TextView) view.findViewById(R.id.priority_options_list_item_dual_line_text_1))
									.setText(context.getResources().getStringArray(R.array.array_priority)[3]);

							((ImageView) view.findViewById(R.id.priority_options_list_item_icon))
									.setImageResource(R.drawable.large_priority_high);
							view.setTag(3);
							break;
						case 1:
							((TextView) view.findViewById(R.id.priority_options_list_item_dual_line_text_1))
									.setText(context.getResources().getStringArray(R.array.array_priority)[2]);

							((ImageView) view.findViewById(R.id.priority_options_list_item_icon))
									.setImageResource(R.drawable.large_priority_middium); // TODO: this image is misspelled.
							view.setTag(2);
							break;
						case 2:
							((TextView) view.findViewById(R.id.priority_options_list_item_dual_line_text_1))
									.setText(context.getResources().getStringArray(R.array.array_priority)[1]);
							
							((ImageView) view.findViewById(R.id.priority_options_list_item_icon))
									.setImageResource(R.drawable.large_priority_low);
							view.setTag(1);
							break;
						case 3:
							((TextView) view.findViewById(R.id.priority_options_list_item_dual_line_text_1))
									.setText(context.getResources().getStringArray(R.array.array_priority)[0]);
							
							((ImageView) view.findViewById(R.id.priority_options_list_item_icon))
									.setImageResource(R.drawable.large_priority_none);
							view.setTag(0);
							break;
					}
				} catch (HandledException h) { // Ignore.
				} catch (Exception exp) {
					if (actOnIt) {
						Log.e(TAG, "ERR0007I", exp);
						ErrorUtil.handleExceptionNotifyUser("ERR0007I", exp, TaskEditor.this);
						actOnIt = false;
					}
				}
			}

			@Override
			public long getItemId(int position) {
				return position;
			}
		};

		int selectedIndex = mCursor.getInt(TaskProjection.PRIORITY_INDEX);
		
		createPriorityClickListener(selectedIndex);
		
		AlertDialog priorityPickerDialog = new AlertDialog.Builder(this)
				.setTitle(R.string.dialog_selectAPriority)
				.setSingleChoiceItems(priorityAdapter, 3 - selectedIndex, mPriorityDialogListener)
				.setPositiveButton(R.string.button_ok, mPriorityDialogListener)
				.setNegativeButton(R.string.button_cancel, mPriorityDialogListener)
				.setOnCancelListener(mPriorityDialogListener)
				.create();
		return priorityPickerDialog;
	}
	
	
	private PickPriorityListener createPriorityClickListener(int selectedIndex) {
		if( null == mPriorityDialogListener ){ // This is very important because it prevents the intialization by the createDialog(..) from overriding the restored state.
			mPriorityDialogListener = new PickPriorityListener(selectedIndex);
		}
		return mPriorityDialogListener;
	}

	
	// private void updateDisplayPriority(int priority) {
	private void updateDisplayPriority(TextView priorityView, int priority) {

		if( priority < 0 || priority > 3 ){
			Log.e(TAG, "ERR000IV Unkown priority value " + priority);
			ErrorUtil.handle("ERR000IV", "Unkown priority value " + priority, this);
			return;
		}
		priorityView.setText(getResources().getStringArray(R.array.array_priority)[priority]);
		
		priorityView.setTag(priority);
	}

	private boolean applyDueDateAndAlarmSanityChecks(Calendar dueDate, Calendar alarmTime) {
		// alarm must always be before the due date.

		if (dueDate != null && alarmTime != null && dueDate.after(alarmTime)) {
			// Toast.makeText(mContext, "Due date may not be after the alarm.",
			// Toast.LENGTH_LONG).show();
			Toast.makeText(TaskEditor.this, R.string.toast_dueDateMayNotBeAfterTheAlarm, Toast.LENGTH_LONG).show();
			return true;
		}
		return false;
	}

	private EditDueDateListener createDueDateClickListener(Calendar dueDateCal) {
		if( null == mEditDueDateSetListener ){ // This is very important because it prevents the intialization by the createDialog(..) from overriding the restored state.
			mEditDueDateSetListener = new EditDueDateListener(dueDateCal);
		}
		return mEditDueDateSetListener;
	}

	private EditDueDateListener mEditDueDateSetListener = null;
	
	/**
	 * Handles changes to the due date.
	 */
	private class EditDueDateListener implements DialogInterface.OnClickListener, DatePickerDialog.OnDateSetListener, DialogInterface.OnCancelListener {

		private Calendar mOrigDueDate = null;
		public long getOrigDueDate() {
			return mOrigDueDate.getTimeInMillis();
		}
		public void setOrigDueDate(long origDueDateTime) {
			mOrigDueDate.setTimeInMillis(origDueDateTime);
		}

		private Calendar mNewDueDate  = null;
		public long getNewDueDate() {
			return mNewDueDate.getTimeInMillis();
		}
		public void setNewDueDate(long newDueDateTime) {
			mNewDueDate.setTimeInMillis(newDueDateTime);
		}

		
		public EditDueDateListener(Calendar dueDateCal){
			mOrigDueDate = Calendar.getInstance();
			mOrigDueDate.setTimeInMillis(dueDateCal.getTimeInMillis());
			mNewDueDate = Calendar.getInstance();
			mNewDueDate.setTimeInMillis(mOrigDueDate.getTimeInMillis());
		}
		
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			try {

				// Prepare event info.
				Event.onEvent(Event.EDIT_TASK_EDIT_DUE_DATE, null); 
				
				mNewDueDate.set(year, monthOfYear, dayOfMonth);
				
				// Update the database
				ContentValues cv = new ContentValues();
		        cv.put(Task.Tasks.DUE_DATE, mNewDueDate.getTimeInMillis());
				cv.put(Task.Tasks.MODIFIED_DATE, System.currentTimeMillis()); // Bump last modified date.
				if (1 != getContentResolver().update(mUri, cv, null, null)) {
					Log.e(TAG, "ERR000BL Failed to update task due date. ");
					ErrorUtil.handleExceptionNotifyUser("ERR000BL",  (Exception)(new Exception( mUri.toString() )).fillInStackTrace(), TaskEditor.this); 
					return;
				}
				
				updateDisplayDueDate(pickDueDateButton, mNewDueDate);

				checkForWarningConditions(mOrigDueDate.getTimeInMillis() == Task.Tasks.DUE_DATE_NOT_SET_LONG);

				mOrigDueDate.setTimeInMillis(mNewDueDate.getTimeInMillis());

				// Automatically move focus to the alarm button.
				if (pickDueDateButton.hasFocus()) {
					pickAlarmButton.requestFocus();
				}
			} catch (HandledException h) { // Ignore.
			} catch (Exception exp) {
				Log.e(TAG, "ERR0007K", exp);
				ErrorUtil.handleExceptionNotifyUser("ERR0007K", exp, TaskEditor.this);
			}
		}
		private void checkForWarningConditions(boolean dueDateWasNotSet) {
			// Get a calendar instance for the alarm date-time.
			Calendar alarmCal = Calendar.getInstance();
			alarmCal.setTimeInMillis(mCursor.getLong(TaskProjection.ALARM_TIME_INDEX));
			
			// Check if alarm is after due date.
			if (alarmCal.after(mNewDueDate)) { // Alarm is after due date.
				// Warn user that alarm is after the due date.
				Toast.makeText(TaskEditor.this, R.string.toast_dueDateIsBeforeAlarm, Toast.LENGTH_LONG).show();
				alarmWarnImage.setVisibility(View.VISIBLE);
			} else { // Alarm is before due date.
				// Check if the new due date is in the past.
				Calendar cal = Calendar.getInstance();
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND,0);
				if( mNewDueDate.before(cal)){ // Due date is before today 
					// Warn the user that the new due date is in the past.
					Toast.makeText(TaskEditor.this, R.string.toast_dueDateIsInThePast, Toast.LENGTH_LONG).show(); 

				} else { // New due date is in the future.
					if (alarmCal.getTimeInMillis() != 0) { // Is the alarm set? 
						if( alarmCal.getTimeInMillis() > System.currentTimeMillis() ){ // Is alarm in future?
							alarmWarnImage.setVisibility(View.GONE);
						}
						if( !dueDateWasNotSet ){
							// Prompt user to update the alarm.
							Toast.makeText(TaskEditor.this, R.string.toast_alarmHasNotChanged, Toast.LENGTH_LONG).show();
						}
					}
					dueDateWarnImage.setVisibility(View.GONE);
				}
			}
		}

		public void onCancel(DialogInterface dialog) {
			try{
				//Log.v(TAG, "onCancel(..) called.");
				handleCancel(dialog);
			} catch (HandledException h) { // Ignore.
			} catch (Exception exp) {
				Log.e(TAG, "ERR000BI", exp);
				ErrorUtil.handleExceptionNotifyUser("ERR000BI", exp, TaskEditor.this);
			}
		}
		
		private void handleCancel(DialogInterface dialog) { 
			mNewDueDate.setTimeInMillis(mOrigDueDate.getTimeInMillis());
			((DatePickerDialog)dialog).updateDate(mOrigDueDate.get(Calendar.YEAR), mOrigDueDate.get(Calendar.MONTH), mOrigDueDate.get(Calendar.DAY_OF_MONTH));
		}

		public void onClick(DialogInterface dialog, int which) {
			try{
				if( DialogInterface.BUTTON_NEGATIVE == which ){
					// Log.v(TAG, "onClick(..) called.");
					handleCancel(dialog);
				}else if( DialogInterface.BUTTON_NEUTRAL == which ){
					
					mNewDueDate.setTimeInMillis( Task.Tasks.DUE_DATE_NOT_SET_LONG ); 
					updateDisplayDueDateToNotSet();

					checkForWarningConditions(true);
					
					// Update the database
					ContentValues cv = new ContentValues();
			        cv.put(Task.Tasks.DUE_DATE, Task.Tasks.DUE_DATE_NOT_SET_LONG);
					cv.put(Task.Tasks.MODIFIED_DATE, System.currentTimeMillis()); // Bump last modified date.
					if (1 != getContentResolver().update(mUri, cv, null, null)) {
						Log.e(TAG, "ERR000BZ"); // Failed to update task due date
						ErrorUtil.handleExceptionNotifyUser("ERR000BZ",  (Exception)(new Exception( mUri.toString() )).fillInStackTrace(), TaskEditor.this); 
						return;
					}					
		
					
					mOrigDueDate.setTimeInMillis(mNewDueDate.getTimeInMillis());
					
					// Update the filter bits.
					FilterUtil.applyFilterBits(TaskEditor.this, mUri);
				}
			} catch (HandledException h) { // Ignore.
			} catch (Exception exp) {
				Log.e(TAG, "ERR000BK", exp);
				ErrorUtil.handleExceptionNotifyUser("ERR000BK", exp, TaskEditor.this);
			}
			
		}
	};

	private AlarmEditDialogListener createAlarmClickListener(Calendar theAlarmCal) {
		if( null == mAlarmDateTimeSetListener ){ // This is very important because it prevents the intialization by the createDialog(..) from overriding the restored state.
			mAlarmDateTimeSetListener = new AlarmEditDialogListener(theAlarmCal);
		}
		return mAlarmDateTimeSetListener;
	}

	
	AlarmEditDialogListener mAlarmDateTimeSetListener = null;
	private class AlarmEditDialogListener implements DateTimePickerDialog.OnDateTimeSetListener, DialogInterface.OnClickListener, DialogInterface.OnCancelListener {

		private Calendar mOrigAlarmDate = null;
		public long getOrigAlarmDate() {
			return mOrigAlarmDate.getTimeInMillis();
		}
		public void setOrigAlarmDate(long origAlarmDateTime) {
			mOrigAlarmDate.setTimeInMillis(origAlarmDateTime);
		}

		private Calendar mNewAlarmDate  = null;
		public long getNewAlarmDate() {
			return mNewAlarmDate.getTimeInMillis();
		}
		public void setNewAlarmDate(long newAlarmDateTime) {
			mNewAlarmDate.setTimeInMillis(newAlarmDateTime);
		}

		public AlarmEditDialogListener(Calendar theAlarmDateCal){
			mOrigAlarmDate = Calendar.getInstance();
			mOrigAlarmDate.setTimeInMillis(theAlarmDateCal.getTimeInMillis());
			mNewAlarmDate = Calendar.getInstance();
			mNewAlarmDate.setTimeInMillis(mOrigAlarmDate.getTimeInMillis());
		}
		
		
		public void onDateTimeSet(DatePicker datepicker, TimePicker timepicker, int year, int month, int day, int hour, int minute) {
			try {
				
				// Prepare event info.
				Event.onEvent(Event.EDIT_TASK_EDIT_ALARM, null); 

				// Get a calendar instance for the alarm date-time.
				updateAlarmCalendar(year, month, day, hour, minute, 0, mNewAlarmDate);

				// Get a calendar instance for the due date.
				Calendar dueDateCal = null; // TODO: Add localization

				// Set the calendar to the task's due date
				if (Long.MAX_VALUE != mCursor.getLong(TaskProjection.DUE_DATE_INDEX)) {
					dueDateCal = Calendar.getInstance(); // TODO: Add
															// localization
					dueDateCal.setTimeInMillis(mCursor.getLong(TaskProjection.DUE_DATE_INDEX));

					// Set the calendar to the last moment of the day.
					dueDateCal.set(Calendar.HOUR_OF_DAY, 23); // last hour of day
					dueDateCal.set(Calendar.MINUTE, 59);
					dueDateCal.set(Calendar.SECOND, 59);

				}

				// Update the database
				ContentValues cv = new ContentValues();
		        cv.put(Task.Tasks.ALARM_TIME, mNewAlarmDate.getTimeInMillis());
				cv.put(Task.Tasks.MODIFIED_DATE, System.currentTimeMillis()); // Bump last modified date.
				if (1 != getContentResolver().update(mUri, cv, null, null)) {
					Log.e(TAG, "ERR000BM Failed to update task alarm.");
					ErrorUtil.handleExceptionNotifyUser("ERR000BM",  (Exception)(new Exception( mUri.toString() )).fillInStackTrace(), TaskEditor.this); 
					return;
				}
								
				mOrigAlarmDate.setTimeInMillis(mNewAlarmDate.getTimeInMillis());

				updateAlarmButtonText(mNewAlarmDate);
				if (pickAlarmButton.hasFocus()) {
					View v = pickAlarmButton.focusSearch(View.FOCUS_DOWN);
					v.requestFocus();
				}

				checkAndWarnForInvalidAlarmAndRegisterAlarm(mNewAlarmDate, dueDateCal);
				
			} catch (HandledException h) { // Ignore.
			} catch (Exception exp) {
				Log.e(TAG, "ERR0007L", exp);
				ErrorUtil.handleExceptionNotifyUser("ERR0007L", exp, TaskEditor.this);
			}
		}
		
		private void checkAndWarnForInvalidAlarmAndRegisterAlarm(Calendar alarmCal, Calendar dueDateCal) {
			// Check if alarm is in the past.
			if (alarmCal.getTimeInMillis() < System.currentTimeMillis() ) { // new alarm is in the past.
				// Warn the user that the new due date is in the past.
				Toast.makeText(TaskEditor.this, R.string.toast_alarmIsInThePast, Toast.LENGTH_LONG).show();

				// Show icon warning user that alarm is in the past.
				alarmWarnImage.setVisibility(View.VISIBLE);
				return;
			} else { // new alarm is in the future

			}
			// Check if alarm is after due date.
			if (null != dueDateCal && alarmCal.after(dueDateCal)) { // Alarm is
																	// after due
																	// date.
				// Warn user that alarm is after the due date.
				Toast.makeText(TaskEditor.this, R.string.toast_alarmIsAfterDueDate, Toast.LENGTH_LONG).show();
				// Show icon warning user that alarm is after due date.
				alarmWarnImage.setVisibility(View.VISIBLE);
				return;
			}
			// Hide icon warning user that alarm is invalid.
			alarmWarnImage.setVisibility(View.GONE);
		}
		
		public void onCancel(DialogInterface dialog) {
			try{
				// Log.v(TAG, "onCancel(..) called.");
				handleCancel(dialog);
			} catch (HandledException h) { // Ignore.
			} catch (Exception exp) {
				Log.e(TAG, "ERR000BN", exp);
				ErrorUtil.handleExceptionNotifyUser("ERR000BN", exp, TaskEditor.this);
			}
		}
		
		private void handleCancel(DialogInterface dialog) { 
			mNewAlarmDate.setTimeInMillis(mOrigAlarmDate.getTimeInMillis());
			((DateTimePickerDialog)dialog).updateDateTime(mOrigAlarmDate.get(Calendar.YEAR), mOrigAlarmDate.get(Calendar.MONTH), mOrigAlarmDate.get(Calendar.DAY_OF_MONTH), mOrigAlarmDate.get(Calendar.HOUR_OF_DAY), mOrigAlarmDate.get(Calendar.MINUTE));
		}

		public void onClick(DialogInterface dialog, int which) {
			try{
				if( DialogInterface.BUTTON_NEGATIVE == which ){
					// Log.v(TAG, "onClick(..) called.");
					handleCancel(dialog);
				}else if( DialogInterface.BUTTON_NEUTRAL == which ){ // "Remove" button.

					// Update the database
					ContentValues cv = new ContentValues();
			        cv.put(Task.Tasks.ALARM_TIME, (Long)Task.Tasks.ALARM_TIME_NOT_SET);
					cv.put(Task.Tasks.MODIFIED_DATE, System.currentTimeMillis()); // Bump last modified date.
					if (1 != getContentResolver().update(mUri, cv, null, null)) {
						Log.e(TAG, "ERR000BZ"); // Failed to update task alarm
						ErrorUtil.handleExceptionNotifyUser("ERR000BZ", (Exception)(new Exception( mUri.toString() )).fillInStackTrace(), TaskEditor.this); 
						return;
					}
					
					mNewAlarmDate.setTimeInMillis(Calendar.getInstance().getTimeInMillis());
					mOrigAlarmDate.setTimeInMillis(mNewAlarmDate.getTimeInMillis());
					
					updateDisplayAlarmToNotSet();
					
					// Update the filter bits.
					FilterUtil.applyFilterBits(TaskEditor.this, mUri);
				}
			} catch (HandledException h) { // Ignore.
			} catch (Exception exp) {
				Log.e(TAG, "ERR000BJ", exp);
				ErrorUtil.handleExceptionNotifyUser("ERR000BK", exp, TaskEditor.this);
			}
		}
	};

	private Calendar createAlarmCalendar(int year, int month, int day, int hour, int minute, int second) {
		// Get an calendar instance for the alarm date-time.
		Calendar alarmCal = Calendar.getInstance(); // TODO: Add localization
		updateAlarmCalendar(year, month, day, hour, minute, second, alarmCal);
		return alarmCal;
	}
	private void updateAlarmCalendar(int year, int month, int day, int hour, int minute, int second, Calendar alarmCal) {

		alarmCal.set(Calendar.YEAR, year);
		alarmCal.set(Calendar.MONTH, month);
		alarmCal.set(Calendar.DAY_OF_MONTH, day);
		alarmCal.set(Calendar.HOUR_OF_DAY, hour);
		alarmCal.set(Calendar.MINUTE, minute);
		alarmCal.set(Calendar.SECOND, second);
		alarmCal.set(Calendar.MILLISECOND, 0);
	}

	private void updateDisplayDueDate(TextView textView, Calendar cal) {
		textView.setText(mDateDateFormat.format(cal.getTime()));
		textView.setTag(cal.getTimeInMillis()); // TODO: !!! Is this tag thing necessary? 
	}
	
	// TODO: May not be local appropriate.
	private void updateAlarmButtonText(Calendar alarmCal) {
		assert alarmCal != null;
		assert pickAlarmButton != null;
		pickAlarmButton.setText(mDateDateFormat.format(alarmCal.getTime()) + " " + mTimeDateFormat.format(alarmCal.getTime()));
		pickAlarmButton.setTag(alarmCal.getTimeInMillis());
	}

	private void updateDisplayDueDateToNotSet() {
		pickDueDateButton.setText(getText(R.string.notSet));
		pickDueDateButton.setTag(Task.Tasks.DUE_DATE_NOT_SET_LONG);
		// Hide icon warning user that alarm is in the past.
		dueDateWarnImage.setVisibility(View.GONE);
	}

	private void updateDisplayAlarmToNotSet() {
		pickAlarmButton.setText(getText(R.string.notSet)); 
		pickAlarmButton.setTag(Task.Tasks.ALARM_TIME_NOT_SET);
		// Hide icon warning user that alarm is in the past.
		alarmWarnImage.setVisibility(View.GONE);
	}

	private RevertData mRevertData = null;
	
	@Override
	protected void onResume() {
		super.onResume();
		try {
			//Log.v(TAG, "onResume(..) called.");
			if (mCursor == null) {
				Log.e(TAG, "ERR0007N Cursor is null. " + mUri);
				ErrorUtil.handleExceptionNotifyUserFinish("ERR0007N", (Exception)(new Exception( mUri.toString() )).fillInStackTrace() , this);
				finish();
				return;
			}

			// If we didn't have any trouble retrieving the data, it is now
			// time to get at the stuff.

			// Make sure we are at the one and only row in the cursor.
			if (!mCursor.moveToFirst()) {
				// How did we resume an non-existent task?
				Log.e(TAG, "ERR0007M Attempted to resume editing a non-existent task.");
				ErrorUtil.handleException("ERR0007M", (Exception)(new Exception( mUri.toString() + " state==" + mState + " isHiberating==" + isHiberating +(mCursor.isClosed()?" mCursor is closed. ":""))).fillInStackTrace(), this);
				
				try{
					mCursor.close();
					mCursor = null;
				}catch(Exception e){
					Log.e(TAG, "ERR000L3 Failed to close old cursor.");
					ErrorUtil.handleExceptionNotifyUserFinish("ERR000L3", (Exception)(new Exception( mUri.toString() )).fillInStackTrace() , this);
					finish();
					return;
				}
				
				mCursor = createCursor();
				
				if( mCursor == null ){
					ErrorUtil.handle("ERR000L4", "Failed to create new cursor.", this); // If this is happening then many the problem is just a bug in the managed cursor code. (comment is from time in past when we used the requery() call on Cursor)
					ErrorUtil.notifyUser(this, null);
					setResult(Activity.RESULT_CANCELED);
					finish();
					return;
				}
				
			}

			// Do only once.
			if(null == mRevertData){
				mRevertData = new RevertData();
				mRevertData.title 		= mCursor.getString(TaskProjection.TASK_TITLE_INDEX);			
				mRevertData.description = mCursor.getString(TaskProjection.TASK_DESC_INDEX);			
				mRevertData.priority 	= mCursor.getString(TaskProjection.PRIORITY_INDEX);			
				mRevertData.dueDate 	= mCursor.getString(TaskProjection.DUE_DATE_INDEX);			
				mRevertData.alarm 		= mCursor.getString(TaskProjection.ALARM_TIME_INDEX);
				if( mLabelsEnabled ){
					mRevertData.mTaskLabelEntries = new ArrayList<TaskLabelEntry>();
					for(TaskLabelEntry entry: mTaskLabelEntries){
						mRevertData.mTaskLabelEntries.add(entry);
					}
				}
			}

			// This is a little tricky: we may be resumed after previously being
			// paused/stopped. We want to put the new text in the text view,
			// but leave the user where they were (retain the cursor position
			// etc). This version of setText does that for us.

				String taskDesc = mCursor.getString(TaskProjection.TASK_DESC_INDEX);
				mTextDesc.setTextKeepState(taskDesc);

				String taskTitle = mCursor.getString(TaskProjection.TASK_TITLE_INDEX);
				mTextTitle.setTextKeepState(taskTitle);

				// Get a calendar instance for the alarm date-time.
				Calendar alarmCal = null;
				Calendar dueDateCal = null;

				// TODO: Add bundle persistent code.

				// *********************
				// Add due date text
				// *********************

				// if( !mCursor.isNull(TaskProjection.DUE_DATE_INDEX)){
				if (mCursor.getLong(TaskProjection.DUE_DATE_INDEX) != Task.Tasks.DUE_DATE_NOT_SET_LONG) {
					dueDateCal = Calendar.getInstance();	
					dueDateCal.setTimeInMillis(mCursor.getLong(TaskProjection.DUE_DATE_INDEX));
					updateDisplayDueDate(pickDueDateButton, dueDateCal);
				} else {
					updateDisplayDueDateToNotSet();
				}

				// *********************
				// Add alarm text
				// *********************
				if (!mCursor.isNull(TaskProjection.ALARM_TIME_INDEX)) {
					alarmCal = Calendar.getInstance();
					alarmCal.setTimeInMillis(mCursor.getLong(TaskProjection.ALARM_TIME_INDEX));
					updateAlarmButtonText(alarmCal);
				} else {
					updateDisplayAlarmToNotSet();
				}

				checkAndWarnOnInvalidAlarmAndDueDate(alarmCal, dueDateCal);

				int priority = mCursor.getInt(TaskProjection.PRIORITY_INDEX);
				updateDisplayPriority(pickPriorityButton, priority);

		} catch (HandledException h) { // Ignore.
		} catch (Exception exp) {
			Log.e(TAG, "ERR0006U", exp);
			ErrorUtil.handleExceptionFinish("ERR0006U", exp, this);
		}
	}

	private void checkAndWarnOnInvalidAlarmAndDueDate(Calendar alarmCal, Calendar dueDateCal) {
		// ************************************************
		// Display/Hide due date and alarm warning flags.
		// ************************************************
		alarmWarnImage.setVisibility(View.GONE);
		dueDateWarnImage.setVisibility(View.GONE);
		
		// Check if the new due date is in the past.
		if (null != dueDateCal) {
			if (dueDateCal.getTimeInMillis() < System.currentTimeMillis()) { // New due date is in the past.
				// Warn the user that the new due date is in the past.
				dueDateWarnImage.setVisibility(View.VISIBLE);
			}
		}

		// Check if alarm is in the past.
		if (null != alarmCal) {
			if (alarmCal.getTimeInMillis() < System.currentTimeMillis()) { // new alarm is in the past.
				// Warn the user that the new due date is in the past.
				alarmWarnImage.setVisibility(View.VISIBLE);
			}
		}

		// Check if alarm is after due date.
		if (null != dueDateCal && null != alarmCal) {
			if (alarmCal.after(dueDateCal)) { // Alarm is after due date.
				// Warn user that alarm is after the due date.
				alarmWarnImage.setVisibility(View.VISIBLE);
			}
		}
	}

	boolean isHiberating = false;
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		try {
			isHiberating = true;
			
			saveManagedDialogs(outState);
			//Log.v(TAG, "onSaveInstanceState(..) called.");

			// Save away the original text, so we still have it if the activity
			// needs to be killed while paused.
			if( null != mPriorityDialogListener ){
				outState.putInt(ORIG_PRIORITY_INDEX, mPriorityDialogListener.getOrigSelectedIndex());
				outState.putInt(NEW_PRIORITY_INDEX, mPriorityDialogListener.getNewSelectedIndex());
			}
			if( null != mEditDueDateSetListener ){
				outState.putLong(ORIG_DUE_DATE_CONTENT, mEditDueDateSetListener.getOrigDueDate());
				outState.putLong(NEW_DUE_DATE_CONTENT, mEditDueDateSetListener.getNewDueDate());
			}
			if( null != mAlarmDateTimeSetListener ){
				outState.putLong(ORIG_ALARM_CONTENT, mAlarmDateTimeSetListener.getOrigAlarmDate());
				outState.putLong(NEW_ALARM_CONTENT, mAlarmDateTimeSetListener.getNewAlarmDate());
			}

			if( null != mRevertData ){
				outState.putParcelable(SAVE_REVERT_DATA, mRevertData);
			}
			
			View theFocusedView = findViewById(R.id.task_editor).findFocus();
			if( null != theFocusedView ){
				outState.putInt(SAVE_FOCUS, theFocusedView.getId());
			}
		} catch (HandledException h) { // Ignore.
		} catch (Exception exp) {
			Log.e(TAG, "ERR0006V", exp);
			ErrorUtil.handleExceptionFinish("ERR0006V", exp, this);
		}
	}

	/**
	 * 
	 */
	public void onRestoreInstanceState(Bundle inState) {
		super.onRestoreInstanceState(inState);
		try {
			restoreManagedDialogs(inState);
			
			// Restore Priority Dialog
			if( inState.containsKey(NEW_PRIORITY_INDEX) ){
				mPriorityDialogListener = createPriorityClickListener(inState.getInt(ORIG_PRIORITY_INDEX));
				mPriorityDialogListener.setNewSelectedIndex(inState.getInt(NEW_PRIORITY_INDEX));				
			}
			// Restore Due Date Dialog
			if( inState.containsKey(NEW_DUE_DATE_CONTENT) ){
				Calendar origStateCalendar = Calendar.getInstance();
				origStateCalendar.setTimeInMillis(inState.getLong(ORIG_DUE_DATE_CONTENT));
				mEditDueDateSetListener = createDueDateClickListener(origStateCalendar);
				mEditDueDateSetListener.setNewDueDate(inState.getLong(NEW_DUE_DATE_CONTENT));				
			}
			// Restore Alarm Dialog
			if( inState.containsKey(NEW_ALARM_CONTENT) ){
				Calendar origStateCalendar = Calendar.getInstance();
				origStateCalendar.setTimeInMillis(inState.getLong(ORIG_ALARM_CONTENT));
				mAlarmDateTimeSetListener = createAlarmClickListener(origStateCalendar);
				mAlarmDateTimeSetListener.setNewAlarmDate(inState.getLong(NEW_ALARM_CONTENT));				
			}
			
			if( inState.containsKey(SAVE_REVERT_DATA)){
				mRevertData = inState.getParcelable(SAVE_REVERT_DATA);
			}
			
			// Now that everything is restored, build the view
			rebuildLabelListView();
			
			if( inState.containsKey(SAVE_FOCUS) ){
				handler.sendMessage(Message.obtain(handler, MSG_RESTORE_FOCUS, inState.getInt(SAVE_FOCUS), 0));
			}
		} catch (HandledException h) { // Ignore.
		} catch (Exception exp) {
			Log.e(TAG, "ERR00072", exp);
			ErrorUtil.handleExceptionFinish("ERR00072", exp, this);
		}

	}

	protected void onRestart() {
		super.onRestart();
		try {
			//Log.v(TAG, "onRestart(..) called.");

			if (null != focusedView) {
				focusedView.requestFocus();
			}
			mCursor.moveToFirst(); // Managed query will cause requery() to be
									// called but this Activity expects that the
									// cursor already is at the first position.
		} catch (HandledException h) { // Ignore.
		} catch (Exception exp) {
			Log.e(TAG, "ERR0006W", exp);
			ErrorUtil.handleExceptionFinish("ERR0006W", exp, this);
		}
	}

	// TODO: !!! Search for all instances of setResult(RESULT_CANCELED) and finish() to make sure they don't mean to use RESULT_ERROR.
	// TODO: !!! Even after a call to finish(), activities still climb down through the life cycle methods,, so life cycle methods (onPause(), onStop(), onDestroy(), etc) need to check isFinishing().
	protected void onPause() {
		super.onPause();
		try {
			// Log.v(TAG, "onPause(..) called.");
			
	        // The user is going somewhere else, so make sure their current
	        // changes are safely saved away i  n the provider.  We don't need
	        // to do this if only editing.
			// Android uses "edit in place",, so even unconfirmed changes should be committed.
	        if (mCursor != null) { // mCursor is null when the user is discarding a task.
	        	
	        	if( STATE_INSERT == mState  && !isHiberating ){
	    			//Log.v(TAG, "STATE_INSERT == mState  && !isHiberating");
	        		String descText = mTextDesc.getText().toString().trim();
	        		int length = descText.length();
	        		
	        		String titleText = mTextTitle.getText().toString().trim();
	        		length += titleText.length();

	        		// If this activity is finished, and there is no text, then we
	        		// do something a little special: simply delete the task entry.
	        		// Note that we only do this for inserting... it 
	        		// would be reasonable to do it for both inserting and editing.
	        		Cursor mAttachCursor = getContentResolver().query(Task.TaskAttachments.CONTENT_URI,
	        				new String[] {}, Task.TaskAttachments.TASK_ID + "=?",
	        				new String[] { mUri.getLastPathSegment() }, null);
	        		if (isFinishing() && (length == 0)
	        				&& mAttachCursor.getCount() == 0
	        				&& (mCursor.getLong(TaskProjection.DUE_DATE_INDEX) == Task.Tasks.DUE_DATE_NOT_SET_LONG)
	        				&& mCursor.isNull(TaskProjection.ALARM_TIME_INDEX)) {
	        			getParent().setResult(RESULT_CANCELED);
	        			actuallyDeleteDbRecord();
	        			
	        			Toast.makeText(this, R.string.toast_taskDiscarded, Toast.LENGTH_SHORT).show();
	        		}else{
	        			// TODO: !! Consider checking if the text has actually been changed and only then persist and update the filter bits.
						if( mTextTitle.isFocused() ){
							//Log.v(TAG, "mTextTitle is focused");
							persistTitle();						
						} else if( mTextDesc.isFocused() ){
							//Log.v(TAG, "mTextDesc is focused");
							persistDescription();						
						}
	        		}
	        		mAttachCursor.close();
				} else { 
					//Log.v(TAG, "Persist data");
					if( mTextTitle.isFocused() ){
						//Log.v(TAG, "mTextTitle is focused");
						persistTitle();						
					} else if( mTextDesc.isFocused() ){
						//Log.v(TAG, "mTextDesc is focused");
						persistDescription();						
					}

					if( isFinishing() ){
						Toast.makeText(this, R.string.toast_taskSaved, Toast.LENGTH_SHORT).show();
					}
				}
	        }
		} catch (HandledException h) { // Ignore.
		} catch (Exception exp) {
			Log.e(TAG, "ERR0006X", exp);
			ErrorUtil.handleExceptionFinish("ERR0006X", exp, this);
		}
	}

	private void persistDescription() {
		ContentValues cv = new ContentValues();
		cv.put(Task.Tasks.TASK_DESC, mTextDesc.getText().toString());
		cv.put(Task.Tasks.MODIFIED_DATE, System.currentTimeMillis()); // Bump last modified date.
		int count = getContentResolver().update(mUri, cv, null, null); // TODO: !!! Occassionally, this fails to update, not sure why. It has a valid looking URI. 
		if (1 != count ) {
			Log.e(TAG, "ERR000CL"); // Failed to update task
			ErrorUtil.handleExceptionFinish("ERR000CL", (Exception)(new Exception( count + " " + mUri.toString() )).fillInStackTrace(), this); // TODO: !!! An error that arrives here does not trigger a dialog after returning to TaskList.
			return;
		}
	}

	
	// TODO: !!!! Found a strange situation where two tasks ended up with the same title. I had just enabled(or disabled) both archive and labels (I forget which) but I think that the problem stemmed from updating the title in onPause(..) because although the tasks had identical titles, their attachments were different so I think that one task was updated with the others title. Now, that begs the question of how this can occur. I don't have time to investigate this now. The problem was observed in 0.9.5.1.  
	private void persistTitle() {
		ContentValues cv = new ContentValues();
		cv.put(Task.Tasks.TASK_TITLE, mTextTitle.getText().toString());
		cv.put(Task.Tasks.MODIFIED_DATE, System.currentTimeMillis()); // Bump last modified date.
		
		if (1 != getContentResolver().update(mUri, cv, null, null)) {
			Log.e(TAG, "ERR000CK Failed to update task. " + mUri);
			ErrorUtil.handleExceptionFinish("ERR000CK", (Exception)(new Exception( mUri.toString() )).fillInStackTrace(), TaskEditor.this); // TODO: !!! An error that arrives here does not trigger a dialog after returning to TaskList.
			return;
		}

		sendBroadcast(new Intent(ACTION_TASK_TITLE_CHANGED));
	}

	private void actuallyDeleteDbRecord() {
    	if( -1 == ContentUris.parseId(mUri) ){
			Log.e(TAG, "ERR000H4");
			ErrorUtil.handleExceptionNotifyUser("ERR000H4", (Exception)(new Exception( mUri.toString() )).fillInStackTrace(), this);
			return;
    	}
		getContentResolver().delete(mUri, null, null);
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		try {

			if( null == mCursor ){
				finish();
				return false;
			}

			MenuItem helpMenuItem = menu.add(0, HELP_ID, 100, R.string.option_help);
			helpMenuItem.setIcon(android.R.drawable.ic_menu_help);

			MenuItem doneMenuItem = menu.findItem(SAVE_ID);
			
			if (mState == STATE_INSERT || mState == STATE_EDIT) {
				if( null == doneMenuItem ){
					doneMenuItem = menu.add(0, SAVE_ID, 10, R.string.option_save);
					doneMenuItem.setIcon(android.R.drawable.ic_menu_save);
				}			
			}
			
			if (mState == STATE_EDIT ) { // || mState == STATE_VIEW
				MenuItem revertMenuItem = menu.findItem(REVERT_ID);
				if( null == revertMenuItem ){
					revertMenuItem = menu.add(0, REVERT_ID, 15, R.string.option_revert);
					revertMenuItem.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
				}
			}else if( mState == STATE_INSERT ){
				MenuItem discardMenuItem = menu.findItem(DISCARD_ID);
				if( null == discardMenuItem ){
					discardMenuItem = menu.add(0, DISCARD_ID, 15, R.string.option_discard);
					discardMenuItem.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
				}
			}

			MenuItem deleteTaskMenuItem = menu.findItem(DELETE_ID);
			if( null == deleteTaskMenuItem ){
				deleteTaskMenuItem = menu.add(0, DELETE_ID, 20, R.string.option_delete);
				deleteTaskMenuItem.setIcon(android.R.drawable.ic_menu_delete);
			}
			
			MenuItem archiveMenuItem = menu.findItem(ARCHIVE_ID);
			if( mIsArchiveEnabled ){ // Archive _is_ enabled.
				if( null == archiveMenuItem ){
					archiveMenuItem = menu.add(0, ARCHIVE_ID, 17, R.string.option_archive);
					archiveMenuItem.setIcon(R.drawable.ic_menu_archive);
				}
				if( mIsTaskArchived ){
					archiveMenuItem.setTitle(R.string.option_unarchive);
				}else{ // Task is not in archive.						
					archiveMenuItem.setTitle(R.string.option_archive); 
				}
				if( mState == STATE_INSERT ){
					archiveMenuItem.setEnabled(false);
				}
			}
			
			
			MenuItem completeMenuItem = menu.findItem(COMPLETE_ID);
			if( null == completeMenuItem ){
				completeMenuItem = menu.add(0, COMPLETE_ID, 5, R.string.option_complete);
				completeMenuItem.setIcon(R.drawable.ic_menu_mark);
			}
			if( mCursor.getInt(TaskProjection.COMPLETE_INDEX) == Task.Tasks.COMPLETE_FALSE_INT){
				completeMenuItem.setTitle(R.string.option_complete);
			}else{
				completeMenuItem.setTitle(R.string.option_incomplete);
			}
			
			if( mState == STATE_INSERT ){
				deleteTaskMenuItem.setEnabled(false); 
				completeMenuItem.setEnabled(false); 
			}		
			
			
			
			// Append to menu items for any other activities that can do stuff with it
			// as well. This does a query on the system for any activities that
			// implement the ALTERNATIVE_ACTION for our data, adding a menu item
			// for each one that is found.
			Intent intent = new Intent(null, getIntent().getData());
			intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
			menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, Menu.NONE, new ComponentName(this, TaskEditor.class),
					null, intent, 0, null);

		} catch (HandledException h) { // Ignore.
		} catch (Exception exp) {
			Log.e(TAG, "ERR0006Y", exp);
			ErrorUtil.handleExceptionFinish("ERR0006Y", exp, this);
		}
		return true;
	}

	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		try{
			// Log.v(TAG, "onPrepareOptionsMenu(..) called");
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0009Z", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0009Z", exp, this);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		try {
			// Handle all of the possible menu actions.
			switch (item.getItemId()) {
				case DELETE_ID:
		            showDialog(DIALOG_DELETE_TASK_ID);
					break;
				case REVERT_ID:
					revertChanges();
					finish();
					break;
				case ARCHIVE_ID:
					if( mIsTaskArchived ){
	            		Event.onEvent(Event.TASK_EDIT_ARCHIVE_OPTIONS_MENU_ITEM_CLICKED, null); 		
	            		ArchiveMenuPart.unarchiveTask(this, Long.parseLong(mUri.getLastPathSegment()));
					}else{
						// TODO: !!! Consider whether to return to the task list immediately when a task is archived this way (or at least indicate in the task editor that the current task is archived).
						Event.onEvent(Event.TASK_EDIT_ARCHIVE_OPTIONS_MENU_ITEM_CLICKED, null); 		
						ArchiveMenuPart.archiveTask(this, Long.parseLong(mUri.getLastPathSegment()));
					}
					finish();
					break;
				case SAVE_ID:
					finish();
					break;
				case DISCARD_ID:
					discardTask();
					break;
				case HELP_ID:
					final Intent helpIntent;
					if (ArchiveUtil.isArchiveEnabled(TaskEditor.this) ||
							LabelUtil.isLabelsEnabled(TaskEditor.this)) {
						helpIntent = StaticDisplayActivity.createIntent(TaskEditor.this,
								R.layout.help_task_editor_all, R.string.help_task_editor);
					}else{
						helpIntent = StaticDisplayActivity.createIntent(TaskEditor.this,
								R.layout.help_task_editor, R.string.help_task_editor);
					}
					assert null != helpIntent;
					
					// Prepare event info.
					Event.onEvent(Event.VIEW_TASK_EDITOR_HELP, null); // Map < String , String > parameters = new HashMap < String , String > ();

					startActivityForResult(helpIntent, DEFAULT_ID);

					break;
				case COMPLETE_ID:
					if( mCursor.getInt(TaskProjection.COMPLETE_INDEX) == Task.Tasks.COMPLETE_FALSE_INT){
						TaskList.updateTaskCompleteFlag(this, getContentResolver(), mUri, true, new ContentValues(2));
						finish();
					}else{
						TaskList.updateTaskCompleteFlag(this, getContentResolver(), mUri, false, new ContentValues(2));
					}
					break;
			}
		} catch (HandledException h) { // Ignore.
		} catch (Exception exp) {
			Log.e(TAG, "ERR0006Z", exp);
			ErrorUtil.handleExceptionFinish("ERR0006Z", exp, this);
		}

		return super.onOptionsItemSelected(item);
	}

	private void revertChanges() {
        if (null != mRevertData) {
			if(null != mCursor){
				mCursor.close();
				mCursor = null; // Setting mCursor to null will prevent onPause from overwriting the reverted data.
			}
			ContentValues cv = new ContentValues();
			if( mRevertData.toValues(cv) ){
				if( 1 != getContentResolver().update(mUri, cv, null, null) ){
					Log.e(TAG, "ERR000DK"); // Failed to revert changes for URI
					ErrorUtil.handleExceptionNotifyUser("ERR000DK",  (Exception)(new Exception( mUri.toString() )).fillInStackTrace(), this); 
				}
				
				// ***********************************************************************
				// The code below really should belong in a transaction. 
				// TODO: !!! Move this code into the content provider.
				// ***********************************************************************
				
				if( mLabelsEnabled ){
					// Clear out whatever labels are applied.
					// TODO: !! Assumes that archive label is the only non-user-applied label. True for now,, but ...
					getContentResolver().delete(Task.LabeledContent.CONTENT_URI, 
							Task.LabeledContent._LABEL_ID + " != ? AND " + Task.LabeledContent._CONTENT_URI + "=?", 
							new String[] { TaskProvider.ID_LABEL_ARCHIVED, mUri.toString() });
	
					// Apply previously applied labels.
					long[] labelIds = new long[mRevertData.mTaskLabelEntries.size()]; 
					for(int i=0; i < mRevertData.mTaskLabelEntries.size(); i++){
						labelIds[i] = mRevertData.mTaskLabelEntries.get(i).id;
					}
					LabelUtil.applyLabels(this, labelIds, mUri);
				}
								
			}else{
				// Error already logged and reported in toValues(..)
				//   All that is left is to notify the user.
				ErrorUtil.notifyUser(this);
			}
		}
	}

	private boolean mIsAddLabelEnterClick = false;
	public void onClick(View v) {
		try {
			switch(v.getId()){
				case R.id.revert_button:
					if( mState == STATE_INSERT ){
						discardTask();
					}else{
						try{
							revertChanges();
						}finally{
							finish();
						}
					}
					break;
				case R.id.save_button:
					finish();
					break;
				case R.id.priority:
					showDialog(DIALOG_EDIT_PRIORITY);
					break;
				case R.id.pickDate:
					showDialog(DIALOG_EDIT_DUE_DATE);
					break;
				case R.id.pickAlarm:
					showDialog(DIALOG_EDIT_ALARM);
					break;
				case R.id.complete_button:
					if( null != mCursor ){
						TaskList.updateTaskCompleteFlag(this, getContentResolver(), mUri, mCursor.getInt(TaskProjection.COMPLETE_INDEX) != Task.Tasks.COMPLETE_TRUE_INT, new ContentValues(2));
					}
					finish();
					break;
				case R.id.task_label_add_button:
					mIsAddLabelEnterClick = true;
			    	// Prepare event info. 
			    	Event.onEvent(Event.EDIT_TASK_ADD_LABEL_BUTTON, null); 

			    	handler.sendMessage(Message.obtain(handler, MSG_APPLY_LABEL, mLabelAutoCompleteTextBox.getText().toString()));
					break;
					
				case R.id.delete:
					TaskLabelEntry entry = (TaskLabelEntry) v.getTag();
					assert null != entry;
					
					// Prepare event info. 
					Event.onEvent(Event.EDIT_TASK_DELETE_LABEL_BUTTON, null); 
					
					mLabelListLayout.removeView(entry.view);
					
			    	handler.sendMessage(Message.obtain(handler, MSG_REMOVE_LABEL, entry));

					break;
			}
		} catch (HandledException h) { // Ignore.
		} catch (Exception exp) {
			Log.e(TAG, "ERR000D5", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000D5", exp, this);
		}
	}

    /**
     * Take care of canceling work on a task.  Deletes the task if we
     * had created it.
     */
    private final void discardTask() {
    	try{
    		if (mCursor != null) {
    			// We inserted an empty note, make sure to delete it
    			deleteTaskNoPrompt();
    			Toast.makeText(this, R.string.toast_taskDiscarded, Toast.LENGTH_SHORT).show();
    		}
    	}finally{
    		setResult(RESULT_CANCELED);
    		finish();
    	}
    }
	
    /**
     * Take care of deleting a note.  Simply deletes the entry.
     */
    private final void deleteTaskNoPrompt() {
        if (mCursor != null) {
        	mCursor.close();
        	mCursor = null;
			int count = TaskUtil.deleteTaskNoToast(TaskEditor.this, mUri); 
			if(1 != count){
				Log.e(TAG, "ERR000CA Delete of URI " + mUri + " removed " + count + " record(s).");
				ErrorUtil.handle("ERR000CA", "Delete of URI " + mUri + " removed " + count + " record(s).", TaskEditor.this);
			}
        }
    }

	/**
	 * TaskLabelEntry Data structure for an individual label applied to a task.
	 */
	private static final class RevertData implements Parcelable {

		// These are stuffed into the parcel
		public String title;
		public String description;
		public String priority;
		public String dueDate;
		public String alarm;
		public List<TaskLabelEntry> mTaskLabelEntries = null;

		private RevertData() {
			// only used by CREATOR
		}

		public RevertData(
				String title,
				String description,
				String priority,
				String dueDate,
				String alarm,
				List<TaskLabelEntry> taskLabelEntries
				) {
			this.title = title;
			this.description = description;
			this.priority = priority;
			this.dueDate = dueDate;
			this.alarm = alarm;
			this.mTaskLabelEntries = taskLabelEntries;
		}

		public int describeContents() {
			return 0;
		}

		public void writeToParcel(Parcel parcel, int flags) {
			// Make sure to read data from the input field, if anything is entered

			// Write in our own fields.
			parcel.writeString(title);
			parcel.writeString(description);
			parcel.writeString(priority);
			parcel.writeString(dueDate);
			parcel.writeString(alarm);
			parcel.writeTypedList(mTaskLabelEntries);
		}

		public static final Parcelable.Creator<RevertData> CREATOR = new Parcelable.Creator<RevertData>() {
			public RevertData createFromParcel(Parcel in) {
				try {
					RevertData entry = new RevertData();
					entry.mTaskLabelEntries = new ArrayList<TaskLabelEntry>();

					// Read out our own fields
					entry.title = in.readString();
					entry.description = in.readString();
					entry.priority = in.readString();
					entry.dueDate = in.readString();
					entry.alarm = in.readString();
					in.readTypedList(entry.mTaskLabelEntries, TaskLabelEntry.CREATOR);

					// Read out the fields from Entry
					return entry;
				} catch (HandledException h) { // Ignore.
				} catch (Exception exp) {
					Log.e(TAG, "ERR000DJ", exp);
					ErrorUtil.handleException("ERR000DJ", exp);
				}
				return null;
			}

			public RevertData[] newArray(int size) {
				return new RevertData[size];
			}
		};

		/**
		 * Dumps the entry into a HashMap suitable for passing to the database.
		 * 
		 * @param values
		 *            the HashMap to fill in.
		 * @return true if the value should be saved, false otherwise
		 */
		public boolean toValues(ContentValues values) {
			try{
				// Save the data
				values.put(Task.Tasks.TASK_TITLE, title);
				values.put(Task.Tasks.TASK_DESC, description);
				values.put(Task.Tasks.PRIORITY, priority);
				values.put(Task.Tasks.DUE_DATE, dueDate);
				values.put(Task.Tasks.ALARM_TIME, alarm);
				return true;

			} catch (HandledException h) { // Ignore.
			} catch (Exception exp) {
				Log.e(TAG, "ERR0007W", exp);
				ErrorUtil.handleException("ERR0007W", exp);
			}
			return false;
		}
	}

	//**************************************************************************
	// TODO: Refactor this code into a separate label participant.
	//**************************************************************************

	protected LinearLayout mLabelListLayout;
	protected List<TaskLabelEntry> mTaskLabelEntries = Collections.synchronizedList(new ArrayList<TaskLabelEntry>());
	
	
	AutoCompleteTextView mLabelAutoCompleteTextBox = null;
    private static final int MSG_APPLY_LABEL   = 0; 
    private static final int MSG_RESTORE_FOCUS = 1; 
    private static final int MSG_REMOVE_LABEL   = 4;
    
    // TODO: !!!! When sending these messages, the URI and any other relevent data should be included in the message so that we know for sure that the right records are updated. (I'm concerned that the state of the Activity may have changed between the time the message was sent and the time the message was processed.)
    private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			try{
				//Log.v(TAG, "Next is handleMessage(..)");
		        switch (msg.what) {
		            case MSG_APPLY_LABEL:
		            	applyLabel(msg);

		            	break;
		            case MSG_RESTORE_FOCUS:
						final View theFocusedView = findViewById(msg.arg1);
						if( null != theFocusedView ){
							theFocusedView.requestFocus();
						}
		            	break;
		            case MSG_REMOVE_LABEL:
		            	removeLabel(msg);
		            	break;
		        }
			} catch (HandledException h) { // Ignore.
			} catch (Exception exp) {
				Log.e(TAG, "ERR000DO", exp);
				ErrorUtil.handleExceptionNotifyUser("ERR000DO", exp, TaskEditor.this);
			}
	        super.handleMessage(msg); 
		}

		private void removeLabel(Message msg) {
			TaskLabelEntry entry = (TaskLabelEntry)msg.obj;
			
			mTaskLabelEntries.remove(entry);
			
			LabelUtil.removeLabel(TaskEditor.this, entry.id); // Remove the label here.
			
			// Update the filter bits.
			FilterUtil.applyFilterBits(TaskEditor.this, mUri); 
		}
		
		private void applyLabel(Message msg) {
			mLabelAutoCompleteTextBox.setText("");

			String labelText = (String)msg.obj;
			
			assert null != labelText;
			labelText = labelText.trim();
			assert null != labelText;

			// Ignore empty strings.
			if (labelText.length() == 0) {
				return;
			}
			
			assert null != labelText;
			
			assert null != getText(R.string.unlabeled);
			if ("*".equals(labelText) || getText(R.string.unlabeled).equals(labelText)) {
				// Toast user.
				Toast.makeText(TaskEditor.this, TextUtils.expandTemplate(getText(R.string.toast_labelNameXNotAllowed), labelText), Toast.LENGTH_SHORT).show();
				mLabelAutoCompleteTextBox.setText("");
				return;
			}

			// Check if label exists or is a new label.
			Cursor labelCursor = getContentResolver().query(Task.Labels.CONTENT_URI,
					new String[] { Task.LabelsColumns._ID },
					Task.LabelsColumns._USER_APPLIED + "=? AND " + Task.LabelsColumns.DISPLAY_NAME + "=?",
					new String[] { Task.LabelsColumns.USER_APPLIED_TRUE, labelText }, Task.LabelsColumns._ID);
			assert null != labelCursor;

			Uri labeledContentUri = null;
			if (labelCursor.getCount() > 0) {
				// Label already exists.

				// Position cursor.
				if (!labelCursor.moveToFirst()) {
					assert false;
				}

				// Apply the label.
				try {
					labeledContentUri = LabelUtil.applyLabel(TaskEditor.this, labelCursor.getLong(0), mUri);
					// Toast user.
					Toast.makeText(TaskEditor.this, R.string.toast_labelApplied, Toast.LENGTH_SHORT).show();
				} catch (SQLException sce) {
				    // TODO: Check for "error 19" to ensure it's the unquie constaint.
					// TODO: !! Better solution is to just check for the record before you add it.

					// Toast user.
					Toast.makeText(TaskEditor.this, R.string.toast_duplicateLabel, Toast.LENGTH_SHORT).show();
					return;
				}

			} else {
				// Label does not exist.

				// Create the label.
				Uri uri = LabelUtil.createUserAppliedLabel(TaskEditor.this, labelText);

				labeledContentUri = LabelUtil.applyLabel(TaskEditor.this, Long.valueOf(uri.getLastPathSegment()), mUri);
				// Toast user.
				Toast.makeText(TaskEditor.this, R.string.toast_labelCreatedAndApplied, Toast.LENGTH_SHORT).show();

			}
			
			labelCursor.close();

			// Add the new entry to the displayed list.
			// rebuildLabelListView();
			TaskLabelEntry entry = makeTaskLabelEntry(TaskEditor.this, Long.parseLong(labeledContentUri.getLastPathSegment()), 0, labelText);
			if (!mTaskLabelEntries.add(entry)) {
				assert false;
			}
			View view = buildViewForEntry(entry);
			final LinearLayout layout = mLabelListLayout;
			layout.addView(view, 0);

			// Update the filter bits.
			FilterUtil.applyFilterBits(TaskEditor.this, mUri);
		}
    };

	/**
	 * Removes all existing views, builds new ones for all the entries, and adds
	 * them.
	 */
	private void buildViews() {
		// Remove existing views
		final LinearLayout layout = mLabelListLayout;
		layout.removeAllViews();

		// Build views for the current section
		for (TaskLabelEntry entry : mTaskLabelEntries) {
			View view = buildViewForEntry(entry);
			layout.addView(view);
		}
	}

	/**
	 * Builds a view to display an TaskLabelEntry.
	 * 
	 * @param entry
	 *            the entry to display
	 * @return a view that will display the given entry
	 */
	/* package */ View buildViewForEntry(final TaskLabelEntry entry) {

		// Build a new view
		final ViewGroup parent = mLabelListLayout;
		entry.view = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(
				R.layout.task_editor_label_entry, parent, false);

		// Set the entry as the tag so we can find it again later given just the view
		entry.view.setTag(entry); 

		// Bind data
		TextView data = (TextView) entry.view.findViewById(R.id.label_text);
		assert null != data;
		data.setText(entry.data);

		// Hook up the delete button
		View delete = entry.view.findViewById(R.id.delete);
		assert null != delete;

        delete.setOnClickListener(this);
        delete.setTag(entry);

		return entry.view;
	}

	protected void rebuildLabelListView() {
		buildEntries();
		buildViews();
	}

	/**
	 * TODO: Could be made a bit more efficient by not clearing everything and
	 * just selectively adding/removing entries.
	 */
	private void buildEntries() {
		assert null != mUri;

		// Clear out entries.
		mTaskLabelEntries.clear();

		Cursor labeledContentCursor = getContentResolver().query(
                Task.LabeledContent.CONTENT_URI,
                new String[]{Task.LabeledContent._ID, Task.LabeledContent._LABEL_ID, Task.LabeledContent._CONTENT_URI},
                Task.LabeledContent._LABEL_ID + " != ? AND " + Task.LabeledContent._CONTENT_URI + "=?",
                new String[]{TaskProvider.ID_LABEL_ARCHIVED, mUri.toString()},
                Task.LabeledContentColumns._ID); // TODO: !!! Sort by label text. (need to join)

		assert null != labeledContentCursor;
		if (labeledContentCursor.getCount() == 0) {
			labeledContentCursor.close();
			return;
		}

		TaskLabelEntry entry;
		Cursor labelCuror = null;
		while (labeledContentCursor.moveToNext()) {
			entry = makeTaskLabelEntry(this, labeledContentCursor.getLong(0), labeledContentCursor.getLong(1), null);
			
			if (!mTaskLabelEntries.add(entry)) {
				assert false;
			}
		}
		labeledContentCursor.close();
	}

	private static TaskLabelEntry makeTaskLabelEntry(Context context, long labeldContentId, long labelId, String labelText){
		TaskLabelEntry entry = new TaskLabelEntry();

		entry.id = labeldContentId;
		if( null == labelText ){
			Cursor labelCuror = context.getContentResolver().query(
					ContentUris.withAppendedId(Task.Labels.CONTENT_URI, labelId),
					new String[] { Task.LabelsColumns.DISPLAY_NAME }, null, null, Task.LabeledContentColumns._ID);
			assert null != labelCuror;
			if (!labelCuror.moveToFirst()) {
				Log.w(TAG, "ERR000DQ No label record found.");
				ErrorUtil.handle("ERR000DQ", "No label record found for labeled content record.", context);
				return null;
			}
			entry.data = labelCuror.getString(0);
			
			labelCuror.close();
		}else{
			entry.data = labelText;
		}

		return entry;
	}
	
	/**
	 * TaskLabelEntry Data structure for an individual label applied to a task.
	 */
	private static final class TaskLabelEntry implements Parcelable {
		// These aren't stuffed into the parcel
		public View view;

		// These are stuffed into the parcel
		public String data;
		public long id = 0;

		private TaskLabelEntry() {
			// only used by CREATOR
		}

		public TaskLabelEntry(String data, long id) {
			this.data = data;
			this.id = id;
		}

		public int describeContents() {
			return 0;
		}

		public void writeToParcel(Parcel parcel, int flags) {
			// Make sure to read data from the input field, if anything is
			// entered

			// Write in our own fields.
			parcel.writeString(data);
			// parcel.writeParcelable(uri, 0);
			parcel.writeLong(id);
		}

		public static final Parcelable.Creator<TaskLabelEntry> CREATOR = new Parcelable.Creator<TaskLabelEntry>() {
			public TaskLabelEntry createFromParcel(Parcel in) {
				try {
					TaskLabelEntry entry = new TaskLabelEntry();

					// Read out our own fields
					entry.data = in.readString();
					// entry.uri = in.readParcelable(null);
					entry.id = in.readLong();

					// Read out the fields from Entry
					return entry;
				} catch (HandledException h) { // Ignore.
				} catch (Exception exp) {
					Log.e(TAG, "ERR0007V", exp);
					ErrorUtil.handleException("ERR0007V", exp);
				}
				return null;
			}

			public TaskLabelEntry[] newArray(int size) {
				return new TaskLabelEntry[size];
			}
		};

		/**
		 * Dumps the entry into a HashMap suitable for passing to the database.
		 * 
		 * @param values
		 *            the HashMap to fill in.
		 * @return true if the value should be saved, false otherwise
		 */
		public boolean toValues(ContentValues values) {
			try{
				// Save the data
				if (view != null) {
					TaskLabelEntry entry = (TaskLabelEntry) view.getTag();
					values.put(Task.LabeledContentColumns._LABEL_ID, entry.id);
					return true;
				}
			} catch (HandledException h) { // Ignore.
			} catch (Exception exp) {
				Log.e(TAG, "ERR0007W", exp);
				ErrorUtil.handleException("ERR0007W", exp);
			}
			return false;
		}

		// /**
		// * Create a new TaskLabelEntry with the given data.
		// */
		// public static final TaskLabelEntry newEntry(Activity activity,
		// String data, Uri uri, long id) {
		// TaskLabelEntry entry = new TaskLabelEntry(activity, data, uri, id);
		// return entry;
		// }

	}

    protected void onDestroy() {
    	super.onDestroy();
    	destroyManagedDialogs();
    }    


    // ******************************************************************
    // TabActivity children do not store/restore managed dialogs. (Bug)
    //
    //   The code below is my workaround for storing/restoring 
    //   managed dialogs. The source code comes from private methods
    //   in Activity.java.
    // 
    // -- Begin: AAAAAAB --- 
    // ******************************************************************
    
    private static final String SAVED_DIALOG_IDS_KEY    = "TaskEditor:savedDialogIds";
    private static final String SAVED_DIALOGS_TAG       = "TaskEditor:savedDialogs";
    private static final String SAVED_DIALOG_KEY_PREFIX = "TaskEditor:dialog_";
	private SparseArray<Dialog> mManagedDialogs;
	
    /**
     * Save the state of any managed dialogs.
     *
     * @param outState place to store the saved state.
     */
    protected void saveManagedDialogs(Bundle outState) {
        if (mManagedDialogs == null) {
            return;
        }

        final int numDialogs = mManagedDialogs.size();
        if (numDialogs == 0) {
            return;
        }

        Bundle dialogState = new Bundle();

        int[] ids = new int[mManagedDialogs.size()];

        // save each dialog's bundle, gather the ids
        for (int i = 0; i < numDialogs; i++) {
            final int key = mManagedDialogs.keyAt(i);
            ids[i] = key;
            final Dialog dialog = mManagedDialogs.valueAt(i);
            dialogState.putBundle(savedDialogKeyFor(key), dialog.onSaveInstanceState());
        }

        dialogState.putIntArray(SAVED_DIALOG_IDS_KEY, ids);
        outState.putBundle(SAVED_DIALOGS_TAG, dialogState);
    }
    private String savedDialogKeyFor(int key) {
        return SAVED_DIALOG_KEY_PREFIX + key;
    }
    /**
     * Restore the state of any saved managed dialogs.
     *
     * @param savedInstanceState The bundle to restore from.
     */
    protected void restoreManagedDialogs(Bundle savedInstanceState) {
        final Bundle b = savedInstanceState.getBundle(SAVED_DIALOGS_TAG);
        if (b == null) {
            return;
        }

        final int[] ids = b.getIntArray(SAVED_DIALOG_IDS_KEY);
        final int numDialogs = ids.length;
        mManagedDialogs = new SparseArray<Dialog>(numDialogs);
        for (int i = 0; i < numDialogs; i++) {
            final Integer dialogId = ids[i];
            Bundle dialogState = b.getBundle(savedDialogKeyFor(dialogId));
            if (dialogState != null) {
                final Dialog dialog = onCreateDialog(dialogId);
                if( null != dialog ){ // Not sure why, but sometimes the dialog is == null.
                	dialog.onRestoreInstanceState(dialogState);
                	mManagedDialogs.put(dialogId, dialog);
                }
            }
        }
    }

    protected void destroyManagedDialogs(){
    	// dismiss any dialogs we are managing.
    	if (mManagedDialogs != null) {
    		
    		final int numDialogs = mManagedDialogs.size();
    		for (int i = 0; i < numDialogs; i++) {
    			final Dialog dialog = mManagedDialogs.valueAt(i);
    			if (dialog.isShowing()) {
    				dialog.dismiss();
    			}
    		}
    	}
    }
    
    // **************************************************************
    // -- END: AAAAAAB --- 
    // **************************************************************

	
}


