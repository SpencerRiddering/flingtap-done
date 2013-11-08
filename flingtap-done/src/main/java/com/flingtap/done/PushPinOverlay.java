// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Overlay.Snappable;

/**
 * See http://mobiforge.com/developing/story/using-google-maps-android
 *
 * 
 */
public class PushPinOverlay extends Overlay 
//		implements Snappable 
		{
	private static final String TAG = "PushPinOverlay";
	
	private final Activity mActivity;
	private Point mScreenPoint = new Point();
	private GeoPoint mGeoPoint = null;
    private final Bitmap mPushPinBitmap;
//    private final BitmapDrawable mPushPinBitmapDrawable;
//    private final BitmapDrawable mPushPinBitmapDrawableShadow;
    private final int mAdjustX, mAdjustY;
    private final float mScreenDensity;
    
    public PushPinOverlay(Activity activity, int drawableResId, int adjustX, int adjustY, float screenDensity){
    	mActivity = activity;
    	assert null != mActivity;
    	
    	mScreenDensity = screenDensity;
    	
    	mPushPinBitmap = BitmapFactory.decodeResource(mActivity.getResources(), drawableResId);   
    	if( null == mPushPinBitmap ){
    		throw new RuntimeException("Drawable " + drawableResId + " unknown.");
    	}
//    	mPushPinBitmapDrawable = new BitmapDrawable(mPushPinBitmap);
//    	mPushPinBitmapDrawableShadow = new BitmapDrawable(mPushPinBitmap);
//    	Log.v(TAG, "mPushPinBitmap.getHeight() == " + mPushPinBitmap.getHeight() );
//    	Log.v(TAG, "mPushPinBitmap.getWidth() == " + mPushPinBitmap.getWidth() );
    	
    	mAdjustX = adjustX;
    	mAdjustY = adjustY;
    	
    	paint.setColorFilter(new PorterDuffColorFilter(2130706432, android.graphics.PorterDuff.Mode.SRC_IN));

    }
    
	public void setGeopoint(GeoPoint geopoint) {
		mGeoPoint = geopoint;
		assert null != mGeoPoint;
	}
	
	Paint paint = new Paint();
	@Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        super.draw(canvas, mapView, shadow);                   
        if( null == mGeoPoint ){
        	return;
        }

        // Translate GeoPoint into screen pixels 
    	mapView.getProjection().toPixels(mGeoPoint, mScreenPoint);
//      Log.v(TAG, "x="+mScreenPoint.x+" y="+mScreenPoint.y);
//      Log.v(TAG, "canvas.getWidth() = " + canvas.getWidth() + " canvas.getHeight() " + canvas.getHeight());
        	
//        Rect rect = new Rect();
////        rect.set(mScreenPoint.x, mScreenPoint.y, mScreenPoint.x+mPushPinBitmapDrawable.getIntrinsicWidth(), mScreenPoint.y+mPushPinBitmapDrawable.getIntrinsicHeight());
//        rect.set(0, 0, mPushPinBitmapDrawable.getIntrinsicWidth(), mPushPinBitmapDrawable.getIntrinsicHeight());
////        mPushPinBitmapDrawable.setBounds(mScreenPoint.x+mAdjustX, mScreenPoint.y+mAdjustY, mScreenPoint.x+mAdjustX+mPushPinBitmapDrawable.getIntrinsicWidth(), mScreenPoint.y+mAdjustY+mPushPinBitmapDrawable.getIntrinsicHeight());
////        mPushPinBitmapDrawableShadow.setBounds(mScreenPoint.x+mAdjustX, mScreenPoint.y+mAdjustY, mScreenPoint.x+mAdjustX+mPushPinBitmapDrawable.getIntrinsicWidth(), mScreenPoint.y+mAdjustY+mPushPinBitmapDrawable.getIntrinsicHeight());
////        mPushPinBitmapDrawable.setBounds(mScreenPoint.x, mScreenPoint.y, mScreenPoint.x+mPushPinBitmapDrawable.getIntrinsicWidth(), mScreenPoint.y+mPushPinBitmapDrawable.getIntrinsicHeight());
//          mPushPinBitmapDrawable.setBounds(rect);
////        canvas.drawBitmap(mPushPinBitmap, mScreenPoint.x+mAdjustX, mScreenPoint.y+mAdjustY, null);         
////        mPushPinBitmapDrawable
//      	  drawAt(canvas, mPushPinBitmapDrawable, mScreenPoint.x+mAdjustX, mScreenPoint.y+mAdjustY, shadow);
////        drawAt(canvas, mPushPinBitmapDrawable, 0, 0, shadow);
////        drawAt(canvas, mPushPinBitmapDrawable, mAdjustX, mAdjustY, shadow);

// GOOD CODE: Shows where teh current X,Y Position is.
//            For use when examining the position of the pin and shadow.
//	    Paint paint2 = new Paint();
//	    paint2.setStyle(Paint.Style.STROKE);
//	    paint2.setStrokeWidth(2);
//    	canvas.drawCircle(mScreenPoint.x, mScreenPoint.y, 6, paint2);
//    	canvas.drawCircle(mScreenPoint.x, mScreenPoint.y, 40, paint2);
    	
        if(shadow){
        	canvas.save();
//        	canvas.translate(mScreenPoint.x+mAdjustX+(mAdjustX*(.9)), mScreenPoint.y+mAdjustY+(mPushPinBitmap.getHeight()/2));
        	canvas.translate(mScreenPoint.x+((5)*mScreenDensity), mScreenPoint.y+((-13)*mScreenDensity)); // This is good.
            canvas.skew(Overlay.SHADOW_X_SKEW, 0.0F);// This is good.
            canvas.scale(1.0F, Overlay.SHADOW_Y_SCALE);// This is good.
//           	canvas.drawBitmap(mPushPinBitmap, mScreenPoint.x+mAdjustX, mScreenPoint.y+mAdjustY, paint);
//           	canvas.drawBitmap(mPushPinBitmap, mScreenPoint.x, mScreenPoint.y, paint);
           	canvas.drawBitmap(mPushPinBitmap, 0, 0, paint);// This is good.
           	canvas.restore();
        }else{
//        	canvas.translate(mScreenPoint.x+mAdjustX, mScreenPoint.y+mAdjustY); // This is good.
////           	canvas.drawBitmap(mPushPinBitmap, mScreenPoint.x, mScreenPoint.y, null);
//           	canvas.drawBitmap(mPushPinBitmap, 0, 0, null);// This is good.
           	canvas.drawBitmap(mPushPinBitmap, mScreenPoint.x+((-10)*mScreenDensity), mScreenPoint.y+((-30)*mScreenDensity), null);// This is good.
        }
        
    }

//	public boolean onTap(GeoPoint p, MapView mapView){
//
//		return false;
//	}
//	
//	public boolean onSnapToItem(int x, int y, android.graphics.Point snapPoint, MapView mapView) {
//		
//		mapView.getProjection().toPixels(mGeoPoint, snapPoint);
//		return false;
//	}

}
