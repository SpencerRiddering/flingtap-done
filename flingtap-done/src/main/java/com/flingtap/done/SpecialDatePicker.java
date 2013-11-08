// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;

/**
 * Changes the behavior of the DatePicker.
 * 
 * Changes:
 *  - Sets all descendant views (recursively) to: 
 *      android:focusable="false"
 *      android:focusableInTouchMode="false"
 *      android:clickable="false"
 *
 */
public class SpecialDatePicker extends DatePicker {
	private static final String TAG = "SpecialDatePicker";
	
	public SpecialDatePicker(Context context) {
		super(context);
	}

	public SpecialDatePicker(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SpecialDatePicker(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

    protected void onFinishInflate() {
    	ViewGroup parent = (ViewGroup)getChildAt(0);
    	recurse(parent);
    }

    protected void recurse(ViewGroup parent){
		int childCount = parent.getChildCount();
		for(int i=0; i< childCount; i++){
			View child = parent.getChildAt(i);
			child.setFocusable(false);
			child.setFocusableInTouchMode(false);
			child.setClickable(false);
			if( child instanceof ViewGroup ){
				recurse((ViewGroup)child);
			}
		}
    }
}
