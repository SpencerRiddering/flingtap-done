// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.flingtap.common.HandledException;
import com.flingtap.done.base.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Checkable;
import android.widget.RadioButton;
import android.widget.RelativeLayout;

/**
 * An extension to CheckableRelativeLayout that supports the {@link android.widget.Checkable} interface.
 * This is useful when used in a {@link android.widget.ListView ListView} where the it's 
 * {@link android.widget.ListView#setChoiceMode(int) setChoiceMode} has been set to
 * something other than {@link android.widget.ListView#CHOICE_MODE_NONE CHOICE_MODE_NONE}.
 * 
 * 
 * 
 * Found that Google did the same thing :)
 * TODO: Consider replacing our CheckableRelativeLayout with Google's implementation:
 *   http://android.git.kernel.org/?p=platform/packages/apps/Music.git;a=blob_plain;f=src/com/android/music/CheckableRelativeLayout.java;hb=e364234e302b3a99a9aba721696d92d61b06cc57
 *
 */
public class CheckableRelativeLayout extends RelativeLayout implements Checkable {
	
	private static final String TAG = "CheckableRelativeLayout";
	
    private RadioButton mRadioButton;
    private int mRadioButtonResourceId = 0;

    public CheckableRelativeLayout(Context context) {
        this(context, null);
    }

    public CheckableRelativeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CheckableRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        try{
        	TypedArray a = context.obtainStyledAttributes(attrs,
        			R.styleable.CheckableRelativeLayout, defStyle, 0);
        	
        	mRadioButtonResourceId = a.getResourceId(R.styleable.CheckableRelativeLayout_radioButton, 0);
        	if( 0 == mRadioButtonResourceId ){
        		throw new RuntimeException("You must supply a radioButton attribute.");
        	}
        	a.recycle();
		}catch(HandledException h){ // Ignore.
        }catch(Exception exp){
        	Log.e(TAG, "ERR00019", exp);
        	ErrorUtil.handleExceptionNotifyUser("ERR00019", exp, context);
        }
    }

    public void toggle() {
		assert null != mRadioButton;
   		mRadioButton.toggle();
    }
    
    public boolean isChecked() {
		assert null != mRadioButton;
   		return mRadioButton.isChecked();
    }

    /**
     * <p>Changes the checked state of this text view.</p>
     *
     * @param checked true to check the text, false to uncheck it
     */
    public void setChecked(boolean checked) {
    	assert null != mRadioButton;
    	mRadioButton.setChecked(checked);
    }

    protected void onFinishInflate() {
    	//Log.v(TAG, "onFinishInflate() called");
    	super.onFinishInflate();
    	try{
    		View radioButtonView = findViewById(mRadioButtonResourceId);
    		assert null != radioButtonView;
    		
    		mRadioButton = (RadioButton) radioButtonView;
    		assert null != mRadioButton;
    		setMinimumHeight(mRadioButton.getHeight());
		}catch(HandledException h){ // Ignore.
        }catch(Exception exp){
        	Log.e(TAG, "ERR0001A", exp);
        	ErrorUtil.handleExceptionNotifyUser("ERR0001A", exp, getContext());
        }

    }
    
}
