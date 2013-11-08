/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.flingtap.done;

import com.flingtap.common.HandledException;
import com.flingtap.done.base.R;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author spencer
 *
 */
public class EditNumberPreference extends DialogPreference {
	private static final String TAG = "EditNumberPreference";
	
    protected String mText;
    protected int mDialogLayoutResId;
    protected int mDefaultValue = 0;
    protected Context mContext = null;

	
    public EditNumberPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        assert null != mContext;
    }

    public EditNumberPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0); // com.android.internal.R.attr.editTextPreferenceStyle
    }

    public EditNumberPreference(Context context) {
        this(context, null);
    }

    /**
     * Saves the text to the {@link SharedPreferences}.
     * 
     * @param text The text to save
     */
    public void setText(String text) {
        final boolean wasBlocking = shouldDisableDependents();

		if( null == text || text.trim().length() == 0 ){
			Toast.makeText(mContext, R.string.toast_noValueSpecifiedNothingChanged, Toast.LENGTH_SHORT).show();
			return;
		}
        mText = text;
        persistInt(Integer.parseInt(text));
        
        final boolean isBlocking = shouldDisableDependents(); 
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
    }
    
    /**
     * Gets the text from the {@link SharedPreferences}.
     * 
     * @return The current preference value.
     */
    public String getText() {
        return mText;
    }

    /**
     * Binds views in the content View of the dialog to data.
     * 
     * @param view The content View of the dialog, if it is custom.
     */
    protected void onBindDialogView(View view) {
    	try{
    		super.onBindDialogView(view);
    		
    		mEditText = (EditText)view.findViewById(R.id.number_chooser_text_edit);
    		assert null != mEditText;
    		final EditText editText = mEditText;
    		
    		editText.setText(getText());
    		
    		Button addButton = (Button)view.findViewById(R.id.number_chooser_button_add);
    		assert null != addButton;
    		addButton.setOnClickListener(new View.OnClickListener(){
    			public void onClick(View arg0) {
    				incrementValue();
    			}
    		});

    		Button subButton = (Button)view.findViewById(R.id.number_chooser_button_substract);
    		assert null != subButton;
    		subButton.setOnClickListener(new View.OnClickListener(){
    			public void onClick(View arg0) {
    				decrementValue();
    			}
    		});

    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR0002M", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR0002M", exp, mContext);
    	}

    }    
	private void incrementValue() {
		mText = mEditText.getText().toString();
		if( null == mText || mText.trim().length() == 0 ){
			return;
		}
		mValue = (Integer.parseInt(mText))+1;
		mValue = mValue > 999 ? 999 : mValue;
		mText = String.valueOf(mValue);
		mEditText.setText(mText);
	}
    private void decrementValue() {
    	mText = mEditText.getText().toString();
		if( null == mText || mText.trim().length() == 0){
			return;
		}
    	mValue = (Integer.parseInt(mText))-1;
    	mValue = mValue < 0 ? 0 : mValue;
		mText = String.valueOf(mValue);
		mEditText.setText(mText);
    }
    protected EditText mEditText = null;
    protected int mValue = 30;

    @Override
    protected void onDialogClosed(boolean positiveResult) {
    	try{
    		super.onDialogClosed(positiveResult);
    		
    		if (positiveResult) {
    			String value = mEditText.getText().toString();
    			if (callChangeListener(value)) {
    				setText(value);
    				mOrigNumber = value;
    			}else{
    				setText(mOrigNumber);
    			}
    		}else{
				setText(mOrigNumber);
    		}
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR0002N", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR0002N", exp, mContext);
    	}
    }

    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    String mOrigNumber = "0";
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
    	mOrigNumber = restoreValue ? String.valueOf(getPersistedInt(Integer.parseInt(mText==null?"0":mText))) : (String)defaultValue; 
    	setText(mOrigNumber);
    }
    
    public boolean shouldDisableDependents() {
        return TextUtils.isEmpty(mText) || super.shouldDisableDependents();
    }

    /**
     * Returns the {@link EditText} widget that will be shown in the dialog.
     * 
     * @return The {@link EditText} widget that will be shown in the dialog.
     */
    public EditText getEditText() {
        return mEditText;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
    	try{
    		final Parcelable superState = super.onSaveInstanceState();
    		if (isPersistent()) {
    			// No need to save instance state since it's persistent
    			return superState;
    		}
    		
    		final SavedState myState = new SavedState(superState);
    		myState.number = Integer.parseInt(getText());
    		myState.origNumber = Integer.parseInt(mOrigNumber);
    		return myState;
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR0002O", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR0002O", exp, mContext);
    	}
    	return null;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
    	try{
    		if (state == null || !state.getClass().equals(SavedState.class)) {
    			// Didn't save state for us in onSaveInstanceState
    			super.onRestoreInstanceState(state);
    			return;
    		}
    		
    		SavedState myState = (SavedState) state;
    		super.onRestoreInstanceState(myState.getSuperState());
    		setText(String.valueOf(myState.number));
    		mOrigNumber = String.valueOf(myState.origNumber);
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR0002P", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR0002P", exp, mContext);
    	}
    }
    
    private static class SavedState extends BaseSavedState {
        Integer number;
        Integer origNumber;
        
        public SavedState(Parcel source) {
            super(source);
            number = source.readInt();
            origNumber = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(number);
            dest.writeInt(origNumber);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
