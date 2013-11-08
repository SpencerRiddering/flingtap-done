// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.flingtap.common.HandledException;
import com.flingtap.done.base.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

/**
 * 
 * 
 * @author spencer
 *
 */
public class TextEntryDialog {
	private static final String TAG = "TextEntryDialog";
	
    public static interface OnTextSetListener{
		public abstract void onTextSet(CharSequence newText);
		public abstract void onCancel();
    }
	
    public static Dialog onPrepareDialog(final Activity activity, Dialog dialog, CharSequence updatedEditText) {
    	try{
    		//Log.v(TAG, "onPrepareDialog(..) called.");
    		
        	final EditText textEdit = (EditText)dialog.findViewById(R.id.text_entry_dialog_text);
        	textEdit.setText(updatedEditText==null?"":updatedEditText);        	
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR000AI", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR000AI", exp, activity);
    	}
    	
    	return dialog;
    }
    
    public static Dialog onCreateDialog(final Activity activity, final OnTextSetListener listener, CharSequence titleText, CharSequence cancelButtonText, final CharSequence initialEditText) {
    	try{
        	//Log.v(TAG, "onCreateDialog(..) called.");
        	
        	// TODO: Give user an option/preference to always use default value.
        	
            LayoutInflater factory = LayoutInflater.from(activity); 
            final View textEntryView = factory.inflate(R.layout.text_entry_dialog, null);
        	final EditText textEdit = (EditText)textEntryView.findViewById(R.id.text_entry_dialog_text);
        	textEdit.setText(initialEditText==null?"":initialEditText);

    		final CharSequence fCancelText;
    		if( null == cancelButtonText ){
    			fCancelText = activity.getText(R.string.button_cancel);
    		}else{
    			fCancelText = cancelButtonText;
    		}	            	
        	
            View titleView = factory.inflate(R.layout.text_entry_dialog_title, null);
            TextView titleTextView = (TextView)titleView.findViewById(R.id.text_entry_dialog_title);
        	titleTextView.setText(titleText);
        	
        	final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setIcon(R.drawable.ic_dialog_menu_generic) 
                .setCustomTitle(titleView )
                .setView(textEntryView)
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	try{
                    		listener.onTextSet( textEdit.getText().toString().trim() );
                    	}catch(HandledException h){ // Ignore.
                    	}catch(Exception exp){
                    		Log.e(TAG, "ERR000AJ", exp);
                    		ErrorUtil.handleExceptionNotifyUser("ERR000AJ", exp, activity);
                    	}

                    }
                })
                .setNegativeButton(fCancelText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	try{
							listener.onCancel();
                    	}catch(HandledException h){ // Ignore.
                    	}catch(Exception exp){
                    		Log.e(TAG, "ERR000AK", exp);
                    		ErrorUtil.handleExceptionNotifyUser("ERR000AK", exp, activity);
                    	}
                    }
                })
   				.setOnCancelListener(new DialogInterface.OnCancelListener(){
					public void onCancel(DialogInterface dialog) {
						try{
							listener.onCancel();
						}catch(HandledException h){ // Ignore.
						}catch(Exception exp){
							Log.e(TAG, "ERR000AL", exp);
							ErrorUtil.handleExceptionNotifyUser("ERR000AL", exp, activity);
						}
					}
				})
                .create();
        	
        	// Allow user to just hit the "enter" key to submit the value rather than requiring that they navigate to the "OK" button or move into touch mode and touch the button.
        	textEdit.setOnKeyListener(new View.OnKeyListener(){
				public boolean onKey(View view, int i, KeyEvent keyevent) {
					try{
						if( keyevent.getKeyCode() == KeyEvent.KEYCODE_ENTER){
							listener.onTextSet( textEdit.getText().toString().trim() );
							dialog.dismiss();
							return true;
						}
					}catch(HandledException h){ // Ignore.
					}catch(Exception exp){
						Log.e(TAG, "ERR000BS", exp);
						ErrorUtil.handleExceptionNotifyUser("ERR000BS", exp, activity);
					}
					
					return false;
				}
        	});
        	return dialog;
    		
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR000AM", exp);
    		ErrorUtil.handleException("ERR000AM", exp, activity); // Can't notify user because user already notified with "Sorry" dialog by system.  http://groups.google.com/group/android-developers/browse_thread/thread/9a6d8c9113eaed2d
    	}
    	
        return null;
    }

}
