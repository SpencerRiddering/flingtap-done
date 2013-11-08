// Licensed under the Apache License, Version 2.0

package com.flingtap.done;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;

import android.graphics.Rect;
import android.graphics.Paint.Align;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.text.Layout.Alignment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewManager;
import android.view.View.MeasureSpec;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.flingtap.done.base.R;


/**
 * TODO: !!! Class not used, remove it.
 * @author spencer
 *
 * 
 */ 
public class LocationDescriptionOverlay extends Overlay {
	private static final String TAG = "LocationDescriptionOverlay";
	
    Paint paint = new Paint();
    GeoPoint mPoint = null; // TODO: I think this could be replaced with sXYCoords.
    int SPOT_WIDTH = 2;
//	int[] xyCoords = new int[2];
//	int[] sXYCoords = new int[2];
    Point sXYCoords = null; // TODO: Rename to mXYCoords
    
//    PixelCalculator calculator;
	
//	MapView mMapView;
	Context mContext = null;	
//	Drawable drawable = null;
	NinePatchDrawable drawable = null; 
    int dw, dh; 
    int tw, th; 
    TextView tv = null;
    MapView mMapView; // TODO: Remove this redundant field (already passed into draw(..)
    
//    int leftPad=10, topPad=10, rightPad=10, bottomPad=10;
    int leftPad=7, topPad=13, rightPad=7, bottomPad=13;
//    int leftPad=0, topPad=0, rightPad=0, bottomPad=0;
//    int pointerAdjustment = 10;
    int pointerAdjustment = 0;
    int pointerHeight = 31;    
    int windowPadding = 8;
    String mText;
    boolean firstTime = true;
    
	public LocationDescriptionOverlay(Context context, MapView mapView, GeoPoint point, String text) {
		super();
		mContext = context; assert context != null;
		mMapView = mapView; assert mapView != null;
		mPoint   = point;   assert point   != null;
		mText    = text;    assert text   != null;
		
//		paint.setStyle(Paint.Style.FILL_AND_STROKE);
	    

//		paint.setTypeface(Typeface.MONOSPACE);
//		paint.setTypeface(Typeface.DEFAULT);
//		paint.setTypeface(Typeface.create("Arial", Typeface.NORMAL));
//		paint.setTypeface(Typeface.create("Lucida Sans", Typeface.NORMAL));

		//		paint.setTextAlign(Align.CENTER);
//		drawable = (Drawable) mContext.getResources().getDrawable(android.R.drawable.sym_def_app_icon);  
//		drawable = (NinePatchDrawable ) mContext.getResources().getDrawable(R.drawable.attachment_list_item);
		drawable = (NinePatchDrawable ) mContext.getResources().getDrawable(R.drawable.pointer_box);  
		
//		Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicWidth(), false);
//		Canvas canvas = new Canvas();
//        w = drawable.getIntrinsicWidth();
//        h = drawable.getIntrinsicHeight(); 		
		
		tv = new TextView(context);
	    paint.setColor(Color.BLACK);
		paint.setTextSize(tv.getTextSize());
		paint.setAntiAlias(true);
//		tv.setBackground(R.drawable.pointer_box);
		tv.setText(text);
		// Controls the text position within the 
		
//		tv.setPadding(left, top, right, bottom);
//		tv.setPadding(0,0,0,0);
//		tv.setAlignment(Alignment.ALIGN_CENTER);
		tv.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
//        w = tv.getMeasuredWidth();
//        h = tv.getMeasuredHeight(); 		
//        w = drawable.getIntrinsicWidth();
//        h = drawable.getIntrinsicHeight(); 		
//        dw = drawable.getIntrinsicWidth() -  drawableLeftGap;
//        dh = drawable.getIntrinsicHeight() - drawableTopGap; 		
//        tw = tv.getMeasuredWidth();
//        th = tv.getMeasuredHeight();
//        dw = dw > tw ? dw : tw + drawableLeftGap;
		
		

        
	}
	
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		super.draw(canvas, mapView, shadow);
		if( firstTime ){
			firstTime = false;
//			Log.e(TAG, "paint.ascent()=="+paint.ascent()+", paint.descent()="+paint.descent());	
//			Log.e(TAG, "mMapView.getWidth()=="+mMapView.getWidth());	
			
//	        tw = (int)paint.measureText(mText);
			
			int charCount = paint.breakText(mText, true, mMapView.getWidth() - leftPad - rightPad - 2*windowPadding, null);
			if( charCount < mText.length()){
				mText = mText.subSequence(0, charCount - 4) + " ..."; // TODO: !!! May not be internationalized correctly. Similar code: android:ellipsize and TextView.setEllipsize(..)
			}
	        tw = (int)paint.measureText(mText);
//	        th = (int)(paint.descent() - paint.ascent()); // ascent is a negative number,, so we subtract it to add it.
	        th = (int)(-1 * paint.ascent());
	        dh = th + topPad + bottomPad + pointerHeight; 		
	        dw = tw + leftPad + rightPad;
//	        dw = drawable.getIntrinsicHeight() > dw ? drawable.getIntrinsicHeight() : dw ;
		}
		
		
		
//		Path path;
//		canvas.drawPath(path, paint);
//		Rect rect = new Rect();
//		canvas.dradrawRoundRect(rect, 4, 4, paint);
//		canvas.drawBitmap(bitmap, 4f, 4f, paint);		
		
//		Log.e(TAG, "draw(Canvas canvas, PixelCalculator calculator, boolean shadow) called.");
//		this.calculator = calculator;
		
//		calculator.getPointXY(mPoint, sXYCoords);
		sXYCoords = mapView.getProjection().toPixels(mPoint, sXYCoords);
		
		
//		canvas.drawCircle(sXYCoords[0], sXYCoords[1], SPOT_WIDTH, paint);
//		sXYCoords[1] += 4; 
		sXYCoords.y += 4; // TODO: !! 4 what? is is device independent or not? 
//		sXYCoords[0] += drawableLeftGap;
//		calculator.getPointXY(mPoint, sXYCoords);
//		Log.e(TAG, "sXYCoords[0]="+sXYCoords[0]+", sXYCoords[1] == "+sXYCoords[1]);		

		
		
//		tv.setBounds(sXYCoords[0] - w / 2, sXYCoords[1] - h ,
//				sXYCoords[0] + w / 2, sXYCoords[1]); 		
				
//		canvas.drawBitmap(bitmap, drawable, sXYCoords[0], sXYCoords[1], paint);		
		
//		drawAt(canvas, drawable, 0, 0, false);
//		drawAt(canvas, drawable, sXYCoords[0], sXYCoords[1], false);
		drawable.draw(canvas);
//		canvas.drawText("Hi everyone", sXYCoords[0] - w / 2 + left, sXYCoords[1] - h/2 , paint);
//		canvas.drawText("Hi everyone", drawableLeftGap + sXYCoords[0] - (tw / 2) + leftPad,  sXYCoords[1] - .75f * dh + topPad , paint);
//		canvas.drawText(mText, pointerAdjustment + sXYCoords[0] - (tw / 2) ,  drawableTopGap + sXYCoords[1] - dh + topPad , paint);
		// TODO: Handle text that is wider than the screen. 
//		int xText = pointerAdjustment + sXYCoords[0] - (tw / 2);
		int xText = pointerAdjustment + sXYCoords.x - (tw / 2);
//		int yText = sXYCoords[1] - dh + th + topPad;
		int yText = sXYCoords.y - dh + th + topPad;

//		int charCount = paint.breakText(mText, true, mMapView.getWidth() - leftPad - rightPad, null);
//		if( charCount < mText.length()){
//			// Controls size and location.
//			drawable.setBounds(pointerAdjustment + sXYCoords[0] - (dw / 2) - leftPad, sXYCoords[1] - dh /* - topPad */,
//					pointerAdjustment + sXYCoords[0] + dw / 2 + rightPad, sXYCoords[1] ); 		
//			
//			canvas.drawText(mText.subSequence(0, charCount - 4) + " ...", xText, yText, paint);
//		}else{
			// Controls size and location.
//			drawable.setBounds(pointerAdjustment + sXYCoords[0] - (dw / 2) - leftPad, sXYCoords[1] - dh /* - topPad */,
//					pointerAdjustment + sXYCoords[0] + dw / 2 + rightPad, sXYCoords[1] ); 		
			drawable.setBounds(pointerAdjustment + sXYCoords.x - (dw / 2) - leftPad, sXYCoords.y - dh /* - topPad */,
					pointerAdjustment + sXYCoords.x + dw / 2 + rightPad, sXYCoords.y ); 		
			
			canvas.drawText(mText, xText, yText, paint);
//		}
		if( false ){
//			canvas.drawCircle(xText, sXYCoords[1] - pointerHeight, SPOT_WIDTH, paint);
			canvas.drawCircle(xText, sXYCoords.y - pointerHeight, SPOT_WIDTH, paint);
//			canvas.drawCircle(xText, sXYCoords[1] - dh + th + topPad, SPOT_WIDTH, paint);
			canvas.drawCircle(xText, sXYCoords.y - dh + th + topPad, SPOT_WIDTH, paint);
//			canvas.drawCircle(xText + 10, sXYCoords[1] - dh, SPOT_WIDTH, paint);
			canvas.drawCircle(xText + 10, sXYCoords.y - dh, SPOT_WIDTH, paint);
		}
		mMapView.invalidate();
	}
	
	public void updatePoint(GeoPoint point) {
		mPoint = point; assert point != null;
		
//		Rect rect = new Rect(sXYCoords[0]-SPOT_WIDTH, sXYCoords[1]-SPOT_WIDTH, sXYCoords[0] + SPOT_WIDTH, sXYCoords[1] + SPOT_WIDTH);
//		map.invalidate(rect); // TODO: Shouldn't this be more effecient than map.invalidate()? TH draw method is not called though.
		mMapView.invalidate();
	}


}
