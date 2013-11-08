// Licensed under the Apache License, Version 2.0

package com.flingtap.done.android;

import com.flingtap.done.base.R.style;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.TextView;

public class LandscapeTabTextView extends TextView {
	private static final String TAG = "LandscapeTabTextView";
	
	private TextPaint mPaint = new TextPaint();
	private Path mPath = new Path();
	private float textWidth = 0;
	
	public LandscapeTabTextView(Context context) {
		super(context);
	}

	public LandscapeTabTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public LandscapeTabTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	protected void onDraw(Canvas canvas){
		mPaint.set(getPaint());
		
		int color = getTextColors().getColorForState(getDrawableState(), 0);
		mPaint.setColor(color);
		mPaint.drawableState = getDrawableState();
		mPaint.setAntiAlias(true); 

		textWidth = mPaint.measureText(getText().toString());

		mPath.moveTo(getWidth()-10, getMeasuredHeight());
		mPath.lineTo(getWidth()-10, 0);

		canvas.drawTextOnPath(getText().toString(), mPath, (getMeasuredHeight()-textWidth)/2, 0, mPaint);
	}
}
