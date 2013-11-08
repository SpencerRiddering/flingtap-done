// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.flingtap.common.HandledException;
import com.flingtap.done.provider.Task;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.flingtap.done.base.R;

/**
 * 
 * @author spencer
 *
 */
public class CompletableEditor 
		extends Activity 
		implements OnClickListener, View.OnKeyListener {
	private static final String TAG = "CompletableEditor";

	private EditText mEditText = null;
	private Uri mUri = null;
	private Intent mResultIntent = null;
	private boolean isInsert = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try{
			// Log.v(TAG, "onCreate(..) called.");
			// Remove Title Bar.
			requestWindowFeature(Window.FEATURE_NO_TITLE); 
			
			Intent intent = getIntent();
			mUri = intent.getData();
			String action = intent.getAction();
			if( null == mUri || null == action || !mUri.toString().startsWith(Task.Completable.CONTENT_URI.toString())){
				setResult(RESULT_CANCELED);
	    		Log.e(TAG, "ERR000F9 Invalid Intent " + intent.toURI());
	    		ErrorUtil.handleExceptionFinish("ERR000F9", (Exception)(new Exception( intent.toURI() )).fillInStackTrace(), this);
				return;
			}
			
			if(Intent.ACTION_EDIT.equals(action)){
				
			}else if(Intent.ACTION_CREATE_SHORTCUT.equals(action)){
				ContentValues cv = new ContentValues();
				cv.put(Task.Completable.TEXT_CONTENT, "");
				mUri = getContentResolver().insert(Task.Completable.CONTENT_URI, cv);
				if( null == mUri ){
		    		Log.e(TAG, "ERR000F3 Unable to insert new Completable.");
		    		ErrorUtil.handleExceptionFinish("ERR000F3",(Exception)(new Exception(  )).fillInStackTrace(), this);
		    		return;
		    	}
				mResultIntent = new Intent();
				mResultIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(Intent.ACTION_EDIT, mUri) );
				mResultIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "" );

				Intent deleteCompletableIntent = new Intent(Intent.ACTION_DELETE, mUri); 
				ComponentName cn = new ComponentName(getPackageName(), CompletableEditor.class.getName()); 
				deleteCompletableIntent.setComponent(cn);		
				mResultIntent.putExtra(PickAttachmentPart.EXTRA_DELETE_INTENT, deleteCompletableIntent);
				
				isInsert = true;
			}else if(Intent.ACTION_DELETE.equals(action)){
				//Log.v(TAG, "action is ACTION_DELETE");
				int count = getContentResolver().delete(mUri, null, null);
				if( 1 != count ){
		    		Log.e(TAG, "ERR000FF Unable to delete Completable.");
		    		ErrorUtil.handleExceptionFinish("ERR000FF", (Exception)(new Exception( mUri.toString() )).fillInStackTrace(), this);
				}
				finish();
				return;
			}else{
				setResult(RESULT_CANCELED);
	    		Log.e(TAG, "ERR000F8 Unknown ACTION " + action);
	    		ErrorUtil.handleExceptionFinish("ERR000F8", (Exception)(new Exception( action )).fillInStackTrace(), this);
				return;
			}
			
			setContentView(R.layout.activity_dialog_stub);
			
			Cursor cursor = getContentResolver().query(
					mUri, 
					new String[]{Task.Completable._ID, Task.Completable.TEXT_CONTENT}, 
					null, 
					null, 
					null);
			if( null == cursor || !cursor.moveToFirst()){
				setResult(RESULT_CANCELED);
	    		Log.e(TAG, "ERR000F4 Unknown URI." );
	    		ErrorUtil.handleExceptionFinish("ERR000F4", (Exception)(new Exception( mUri.toString() )).fillInStackTrace(), this);
				return;
			}
			
			ViewStub stub = (ViewStub) findViewById(R.id.stub_import);
			stub.setLayoutResource(R.layout.completable_editor);
			mEditText = (EditText)stub.inflate();
			mEditText.setText(cursor.getString(1));
			
			cursor.close();
			
			mEditText.setOnKeyListener(this);
			
			TextView tv = (TextView)findViewById(R.id.dialog_title);
			tv.setText(R.string.dialog_completableEditor);
			
			Button saveButton = (Button) findViewById(R.id.positive_button);
			saveButton.setText(R.string.button_save);
			saveButton.setOnClickListener(this);
			
			Button revertButton = (Button) findViewById(R.id.negative_button);
			revertButton.setText(R.string.button_cancel);
			revertButton.setOnClickListener(this);
			
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR000F2", exp);
    		ErrorUtil.handleExceptionFinish("ERR000F2", exp, this);
    	}
	}

	public void onClick(View v) {
		try{
			switch(v.getId()){
				case R.id.positive_button:
					String text = mEditText.getText().toString().trim(); 
					if( text.length() != 0 ){
	                	handleSubmitCompletableEvent(text);
					}else if( isInsert ){
						deleteTempInsertedCompletable();
						finish();
					}
					break;
				case R.id.negative_button:
					setResult(RESULT_CANCELED);
					if( isInsert ){
						deleteTempInsertedCompletable();
					}
					finish();
					break;
					
			}
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR000F5", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR000F5", exp, this);
			setResult(RESULT_CANCELED);
    	}
	}

	private void deleteTempInsertedCompletable() {
		int count = getContentResolver().delete(mUri, null, null);
		if( 1 != count ){
			Log.e(TAG, "ERR000F7 Unable to delete URI.");
			ErrorUtil.handleExceptionNotifyUser("ERR000F7", (Exception)(new Exception( mUri.toString() )).fillInStackTrace(), this);
			setResult(RESULT_CANCELED);
		}
		mUri = null;
		setResult(RESULT_CANCELED);
	}
	
	
	// NOTE: Assumes only Completable fast adder EditText events.
	public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) { 
            switch (keyCode) {
            	case KeyEvent.KEYCODE_ENTER:
            		if( event.isAltPressed() ){
            			return false;
            		}
                case KeyEvent.KEYCODE_DPAD_CENTER:
                	handleSubmitCompletableEvent(mEditText.getText().toString().trim());
		            return true;
            }
        }
		return false;
	}

	private void handleSubmitCompletableEvent(String text) {
		try{
			if( !CompletableUtil.updateCompletableText(this, text, mUri) ){
				setResult(RESULT_CANCELED);
			}else if( isInsert ){
				// Event.
				Event.onEvent(Event.CREATE_COMPLETABLE);
				
				setResult(RESULT_OK, mResultIntent);
			}			
			finish();
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000G2", exp); 
			ErrorUtil.handleExceptionNotifyUser("ERR000G2", exp, this);
		}
	}
}
