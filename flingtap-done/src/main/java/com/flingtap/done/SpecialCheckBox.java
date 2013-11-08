// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.CheckBox;

/**
 * Changes the behavior of the DatePicker.
 */
public class SpecialCheckBox extends CheckBox {
    public static final String TAG = "SpecialCheckBox";

    public SpecialCheckBox(Context context) {
        super(context);
    }

    public SpecialCheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SpecialCheckBox(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public boolean onTouchEvent(MotionEvent event) {
//	        Log.v(TAG, "onTouchEvent(..) called. " + event.toString());		
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
//	        	Log.v(TAG, "event.getAction() == MotionEvent.ACTION_DOWN is true.");		
            toggle();
        }
        return true;
    }
}
