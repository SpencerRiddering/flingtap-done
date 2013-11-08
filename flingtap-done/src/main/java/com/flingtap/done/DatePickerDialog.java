// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.flingtap.common.HandledException;
import com.flingtap.done.base.R;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.widget.DatePicker;

/**
 * 
 *  
 * @author spencer
 *
 */
public class DatePickerDialog extends Dialog {
	private static final String TAG = "DatePickerDialog";
    private DatePicker mDatePicker;
//    private android.widget.DatePicker.OnDateSetListener mCallBack;
    private android.widget.DatePicker.OnDateChangedListener mCallBack;

    public DatePickerDialog(final Context context, android.widget.DatePicker.OnDateChangedListener callBack, int year, int monthOfYear, 
    		int dayOfMonth, int weekStartDay) {
    	super(context);
        mCallBack = callBack;
        requestWindowFeature(1);
        setContentView(R.layout.date_picker_dialog);
        mDatePicker = (DatePicker)findViewById(R.id.date_picker);
        android.widget.DatePicker.OnDateChangedListener wrappedListener = new android.widget.DatePicker.OnDateChangedListener() {

			public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
				try{
					mCallBack.onDateChanged(view, year, monthOfYear, dayOfMonth);
					dismiss();
		    	}catch(HandledException h){ // Ignore.
		    	}catch(Exception exp){
		    		Log.e(TAG, "ERR0001J", exp);
		    		ErrorUtil.handleExceptionNotifyUser("ERR0001J", exp, context);
				}
			}           
        };
        mDatePicker.init(year, monthOfYear, dayOfMonth, wrappedListener);
    	
    }

}
