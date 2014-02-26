// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;
import android.util.Log;
import android.view.View;

import com.flingtap.common.HandledException;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

/**
 * 
 * @author spencer
 *
 */
public class ProximityOverlay extends Overlay {

    public static interface OnProximityExceedingScreenListener{
		public abstract void proximityExceededScreen();
    }

    public static interface OnProximityAlertDistanceSetListener{
        public abstract void distanceSet(int distanceValue, int distanceUnit);
    }

	private static final String TAG = "ProximityOverlay";
	
    Paint paint = new Paint();
    GeoPoint mPoint = null; // TODO: Rename to mPoint to be consistent with other Overlay classes. 
    public void setPoint(GeoPoint point) {
		mPoint = point;
		assert null != mPoint;
		mMapView.invalidate(); 
	}

    
	float pixelRadius = 30;
    int mMeterRadius = 10;
    MapView mMapView;
    MapController mController;
	OnProximityExceedingScreenListener largeListener;
    Point sXYCoords = null; // TODO: Rename to mXYCoords
    private float mScreenDensity = 0;
    
	public void setColor(int color){
	    paint.setColor(color);
	}
	
	public void updateColor(){
	    if( mMapView.isSatellite() ){
	    	paint.setColor(Color.WHITE);;
	    }else{
	    	paint.setColor(Color.BLACK);;
	    }
	}

	public ProximityOverlay(GeoPoint point, int initialMeterRadius, 
			OnProximityExceedingScreenListener largeListener, MapView mapView,
			float screenDensity) {
		super();
		assert point != null;
		assert largeListener != null;
		assert mapView != null;
		
		this.largeListener = largeListener;
		this.mMapView = mapView;
		
		mScreenDensity = screenDensity;
		
	    paint.setStyle(Paint.Style.STROKE);

	    updateColor();
	    paint.setStrokeWidth(2);
		this.mPoint = point;
		mMeterRadius = initialMeterRadius;
		setMeterRadius(initialMeterRadius);
	}

	private boolean mActOnIt = true;
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		super.draw(canvas, mapView, shadow);
		try{
			// Log.e(TAG, "draw(Canvas canvas, MapView mapView, boolean shadow) called.");
			
			float pixelsPerMeter = (float) calcPixelsPerMeter(
					(double) mPoint.getLatitudeE6(), 
					mapView.getZoomLevel(),
					mMeterRadius,
					mScreenDensity); 
			
			setPixelRadius( pixelsPerMeter );
			
			sXYCoords = mapView.getProjection().toPixels(mPoint, sXYCoords);
			
			canvas.drawCircle(sXYCoords.x, sXYCoords.y, pixelRadius, paint);
			displayPoints(mapView, canvas);
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			if( mActOnIt ){
				Log.e(TAG, "ERR00053", exp);
			}
			mActOnIt = ErrorUtil.handle("ERR00053", "", this, mActOnIt);
		}
	}

	private void setPixelRadius(float radius){
        //		Log.v(Tags.TAG_RADIUS, "setPixelRadius("+radius+") called.");
		pixelRadius = radius;
	}
	public void setMeterRadius(int meterRadius){
        //		Log.v(Tags.TAG_RADIUS, "setMeterRadius("+meterRadius+") called.");

		mMeterRadius = meterRadius;		
		
		setPixelRadius( (float) calcPixelsPerMeter(
				(double) mPoint.getLatitudeE6(), 
				mMapView.getZoomLevel(),
				meterRadius,
				mScreenDensity));
		mMapView.invalidate(); // TODO: Restrict to a more narrow region.
	}
	protected void calculatePixelRadius(){
		
		setPixelRadius((float) calcPixelsPerMeter((double) mPoint.getLatitudeE6(), mMapView.getZoomLevel(),
				mMeterRadius, mScreenDensity));
	}

	public void displayPoints(MapView m_MapView, Canvas canvas){
        //		Log.e(TAG, "displayPoints(..) called.");
        //		Log.e(TAG, "pixelRadius == "+pixelRadius);

		// calculate the lat/long span of the screen
		int latSpan = m_MapView.getLatitudeSpan();
		int longSpan = m_MapView.getLongitudeSpan();
		GeoPoint mapCenter = m_MapView.getMapCenter();
		int halfLatSpan = (latSpan/2);
		int halfLongSpan = (longSpan/2);
		int latitudeE6TopEdge = mapCenter.getLatitudeE6()  + halfLatSpan;
		int longitudeE6ToTop = mapCenter.getLongitudeE6() + halfLongSpan;
		
		int latitudeE6BottomEdge = mapCenter.getLatitudeE6()  - halfLatSpan;
		int longitudeE6BottomTop = mapCenter.getLongitudeE6() - halfLongSpan;
		
		GeoPoint upperRightCornerPoint = new GeoPoint(latitudeE6TopEdge, longitudeE6ToTop);
		GeoPoint lowerLeftCornerPoint = new GeoPoint(latitudeE6BottomEdge, longitudeE6BottomTop);
		
		Paint spot = new Paint();
		spot.setStyle(Paint.Style.FILL);

		Point xyCoords = mMapView.getProjection().toPixels(upperRightCornerPoint, null);
		
		int mapTopEdge = xyCoords.y;
		int mapRightEdge = xyCoords.x;
		
		xyCoords = mMapView.getProjection().toPixels(lowerLeftCornerPoint, xyCoords);

		int mapBottomEdge = xyCoords.y;
		int mapLeftEdge = xyCoords.x;
		
		xyCoords = mMapView.getProjection().toPixels(mPoint, xyCoords);
	}

	public MapView getMapView() {
		return mMapView;
	}

	public MapController getController() {
		return mController;
	}

	public void setController(MapController controller) {
		mController = controller;
	}

	// ***************************************
	// From ProximityAlertEditor
	// ***************************************
	public static final double EARTH_RADIUS = 6378200d;
	public static final double CIRCUMFERENCE_MULTIPLIER = 2d * Math.PI
			* EARTH_RADIUS;

	public static double calcCircumference(double latitude) {
		return CIRCUMFERENCE_MULTIPLIER * Math.abs(Math.cos(Math.toRadians(latitude)));
	}

	public static final double[] WIDTH_ZOOM_TABLE = new double[] { 0, // not used.
			     256d, // 1
			     512d, // 2
			    1024d, // 3
			    2048d, // 4
			    4096d, // 5
			    8192d, // 6
			   16384d, // 7
			   32768d, // 8
			   65536d, // 9
			  131072d, // 10
			  262144d, // 11
			  524288d, // 12
			 1048576d, // 13
			 2097152d, // 14
			 4194304d, // 15
			 8388608d, // 16
			16777216d, // 17
			// Added 18 to	 21 now.
			33554432d, // 18
			67108864d, // 19
		   134217728d, // 20
		   268435456d  // 21
	};

	public static double calcMetersPerPixel(double latitude, int zoomLevel,
			double pixels) {
		assert zoomLevel > 0;
		assert zoomLevel <= 17;
		return (pixels * calcCircumference(latitude))
				/ WIDTH_ZOOM_TABLE[zoomLevel];
	}

	/**
	 * TODO: Change latitudeE6 to regular degrees so we don't need to divide multiple times.
	 * @param latitudeE6
	 * @param zoomLevel
	 * @param meters
	 * @return
	 */
	public static double calcPixelsPerMeter(double latitudeE6, int zoomLevel,
			double meters, float screenDensity) {
		
		assert zoomLevel >= 1;
		assert zoomLevel <= 21;
		
        //		Log.v(Tags.TAG_RADIUS, "zoomLevel == " + zoomLevel + ", WIDTH_ZOOM_TABLE[zoomLevel] == " + WIDTH_ZOOM_TABLE[zoomLevel] );
		
		double metersXzoomTable = meters * WIDTH_ZOOM_TABLE[zoomLevel]; 
        //		Log.v(Tags.TAG_RADIUS, metersXzoomTable + " == " + meters + " * " + WIDTH_ZOOM_TABLE[zoomLevel] + ")");
		
		double calcCircumference = calcCircumference(latitudeE6/1E6);
        //		Log.v(Tags.TAG_RADIUS, calcCircumference + " calcCircumference("+ latitudeE6/1E6 + ")");
		
		double pixels = metersXzoomTable / calcCircumference;
		
        //		Log.v(Tags.TAG_RADIUS, pixels + " == " + metersXzoomTable + " / " + calcCircumference );
		
        // Log.v(Tags.TAG_RADIUS, pixels +" == calcPixelsPerMeter(" + latitudeE6/1E6 + "," + zoomLevel + "," + meters + ") called.");
		
		// Adjust pixels for screen density.
		pixels *= screenDensity;
		
		return pixels; 
	}
	

	public void distanceSet(View view, int distanceValue, int distanceUnit) {
		//Log.v(TAG, "distanceSet(..) called.");
		mMeterRadius = distanceValue;
		setPixelRadius((float) calcPixelsPerMeter((double) mPoint
				.getLatitudeE6(), mMapView.getZoomLevel(),
				mMeterRadius,
				mScreenDensity));
		mMapView.invalidate(); // TODO: Restrict the invalidate to a smaller region.
	}

}
