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

import java.util.Calendar;

import com.flingtap.common.HandledException;
import com.flingtap.done.base.R;

import android.app.AlertDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
//import android.pim.DateFormat;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.TimePicker.OnTimeChangedListener;
/**
 * Crammed a date picker and time picker into the same dialog box.
 *
 * Derived from: 
 *   http://android.git.kernel.org/?p=platform/frameworks/base.git;a=blob;f=core/java/android/app/DatePickerDialog.java
 *   and
 *   http://android.git.kernel.org/?p=platform/frameworks/base.git;a=blob;f=core/java/android/app/TimePickerDialog.java
 *
 * @author spencer
 */
public class DateTimePickerDialog extends AlertDialog implements
		OnClickListener, OnTimeChangedListener, OnDateChangedListener {
	private static final String TAG = "DateTimePickerDialog";
	
	private OnDateTimeSetListener mCallBack = null;
	
	// DatePicker
    private static final String YEAR = "year";
    private static final String MONTH = "month";
    private static final String DAY = "day";
    private final DatePicker mDatePicker;
    private final java.text.DateFormat mDateTitleDateFormat;

    // TimePicker
    private static final String HOUR = "hour";
    private static final String MINUTE = "minute";
    private static final String IS_24_HOUR = "is24hour";
    private final TimePicker mTimePicker;
    private final java.text.DateFormat mTimeTitleDateFormat;

    // Merged
    private final Calendar mDateTimeCalendar;
    
    public DateTimePickerDialog(Context context, Calendar cal, OnDateTimeSetListener callBack, boolean is24HourView){
    	this(context, callBack, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), is24HourView);
    }    
    
    public DateTimePickerDialog(Context context, OnDateTimeSetListener callBack, int year,      int monthOfYear, int dayOfMonth, int hourOfDay, int minute,      boolean is24HourView){
    	// Notice that the theme is being ignored now.
    	this(context, 0, callBack, year, monthOfYear, dayOfMonth, hourOfDay, minute, is24HourView);
    }
    public DateTimePickerDialog(Context context, int theme, OnDateTimeSetListener callBack, int year,      int monthOfYear, int dayOfMonth, int hourOfDay, int minute,      boolean is24HourView){
// Was creating double frames around the dialog.    	
//    	super(context, theme);
    	super(context);
    	
    	assert callBack != null;
    	
    	// For both.
    	LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dateTimeView = inflater.inflate(R.layout.date_time_picker_dialog, null);
        setView(dateTimeView);
        mCallBack = callBack;
        setButton(context.getText(R.string.button_set), this);
        setButton2(context.getText(R.string.button_cancel), (android.content.DialogInterface.OnClickListener)null);
        
    	// For DatePicker
        mDateTitleDateFormat = java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL);
        mDatePicker = (DatePicker)dateTimeView.findViewById(R.id.date_picker);
        mDatePicker.init(year, monthOfYear, dayOfMonth, this);

        // For TimePicker
        mTimeTitleDateFormat = DateFormat.getTimeFormat(context);
        mTimePicker = (TimePicker)dateTimeView.findViewById(R.id.time_picker);
        mTimePicker.setCurrentHour(Integer.valueOf(hourOfDay));
        mTimePicker.setCurrentMinute(Integer.valueOf(minute));
        mTimePicker.setIs24HourView(Boolean.valueOf(is24HourView));
        mTimePicker.setOnTimeChangedListener(this);
    	
        // Merged: 
        mDateTimeCalendar = Calendar.getInstance();
        updateTitle(year, monthOfYear, dayOfMonth, hourOfDay, minute);
    }
	
	public void onClick(DialogInterface arg0, int arg1) {
        try{
        	mCallBack.onDateTimeSet(mDatePicker, mTimePicker, mDatePicker.getYear(), mDatePicker.getMonth(), mDatePicker.getDayOfMonth(), mTimePicker.getCurrentHour().intValue(), mTimePicker.getCurrentMinute().intValue());
        	mTimePicker.clearFocus();
        	mDatePicker.clearFocus();
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR0001I", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR0001I", exp, getContext());
    	}catch(ThreadDeath td){
    		ErrorUtil.handleError("ERR0001I", td, getContext());
    	}catch(OutOfMemoryError oome){
    		ErrorUtil.handleError("ERR0001I", oome, getContext());
    	} 
		
	}

	public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
		updateTitle(hourOfDay, minute);
	}

	public void onDateChanged(DatePicker view, int year, int monthOfYear,
			int dayOfMonth) {
		updateTitle(year, monthOfYear, dayOfMonth);
	}
	
    public Bundle onSaveInstanceState() {
        try{
        	Bundle state = super.onSaveInstanceState();
        	state.putInt(YEAR, mDatePicker.getYear());
        	state.putInt(MONTH, mDatePicker.getMonth());
        	state.putInt(DAY, mDatePicker.getDayOfMonth());
        	state.putInt(HOUR, mTimePicker.getCurrentHour().intValue());
        	state.putInt(MINUTE, mTimePicker.getCurrentMinute().intValue());
        	state.putBoolean(IS_24_HOUR, mTimePicker.is24HourView());
        	return state;
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR0001K", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR0001K", exp, getContext());
    	}
    	return null;
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        try{
        	int year = savedInstanceState.getInt(YEAR);
        	int month = savedInstanceState.getInt(MONTH);
        	int day = savedInstanceState.getInt(DAY);
        	mDatePicker.init(year, month, day, this);
        	
        	int hour = savedInstanceState.getInt(HOUR);
        	int minute = savedInstanceState.getInt(MINUTE);
        	mTimePicker.setCurrentHour(Integer.valueOf(hour));
        	mTimePicker.setCurrentMinute(Integer.valueOf(minute));
        	mTimePicker.setIs24HourView(Boolean.valueOf(savedInstanceState.getBoolean(IS_24_HOUR)));
        	mTimePicker.setOnTimeChangedListener(this);

        	updateTitle(year, month, day, hour, minute);
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR0001L", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR0001L", exp, getContext());
    	}
    }
	
    public static interface OnDateTimeSetListener {

        public abstract void onDateTimeSet(DatePicker datepicker, TimePicker timepicker, int year, int month, int day, int hour, int minute);

    }
    
    public void updateDateTime(int year, int monthOfYear, int dayOfMonth, int hour, int minute){
    	mDatePicker.updateDate(year, monthOfYear, dayOfMonth);
    	mTimePicker.setCurrentHour(hour);
    	mTimePicker.setCurrentMinute(minute);
    }
 
    private void updateTitle(int year, int month, int day, int hour, int minute) {
		mDateTimeCalendar.set(Calendar.YEAR, year);
		mDateTimeCalendar.set(Calendar.MONTH, month);
		mDateTimeCalendar.set(Calendar.DAY_OF_MONTH, day);
		mDateTimeCalendar.set(Calendar.HOUR_OF_DAY, hour);
		mDateTimeCalendar.set(Calendar.MINUTE, minute);
 		updateTitle();
     }    
    
     private void updateTitle(int year, int month, int day) {
		mDateTimeCalendar.set(Calendar.YEAR, year);
		mDateTimeCalendar.set(Calendar.MONTH, month);
		mDateTimeCalendar.set(Calendar.DAY_OF_MONTH, day);
 		updateTitle();
     }    
     private void updateTitle(int hour, int minute) {
 		mDateTimeCalendar.set(Calendar.HOUR_OF_DAY, hour);
 		mDateTimeCalendar.set(Calendar.MINUTE, minute);
 		updateTitle();
     }    
     private void updateTitle() {
  		setTitle(mDateTitleDateFormat.format(mDateTimeCalendar.getTime()) + " " + mTimeTitleDateFormat.format(mDateTimeCalendar.getTime()));
     }    
}
