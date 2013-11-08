// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.Log;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;
import com.flingtap.common.HandledException;
import com.flingtap.done.base.R;

// TODO: !!! There has got to be a cleaner way to do this.
/**
 * 
 */
public class PlacemarkNavigatorOverlayItem extends OverlayItem  {
	public static final String TAG = "PlacemarkNavigatorOverlayItem";
	
	private Context mContext = null;
	private String mSnippet = null;
	private long lastTimeUserNotified = 0;
	
	public PlacemarkNavigatorOverlayItem(GeoPoint point, String title, String snippet, Context context) {
		super(point, title, snippet);
		
		assert null != context;
		mContext = context;
		
		assert null != snippet;
		mSnippet = snippet;
	}

	@Override
	public Drawable getMarker(int stateBitset) {
		try{
			//Log.v(TAG, "getMarker("+stateBitset+") called.");

			
			if( ((stateBitset & OverlayItem.ITEM_STATE_PRESSED_MASK)  == OverlayItem.ITEM_STATE_PRESSED_MASK) ){								
				final Drawable markerPressed = mContext.getResources().getDrawable(R.drawable.pins_red);
                MyItemizedOverlay.boundCenterBottom(markerPressed);
		    	return markerPressed;
			}else if( stateBitset > 0 || iAmFocused ){ // ((stateBitset & OverlayItem.ITEM_STATE_SELECTED_MASK) == OverlayItem.ITEM_STATE_SELECTED_MASK) || ((stateBitset & OverlayItem.ITEM_STATE_FOCUSED_MASK)  == OverlayItem.ITEM_STATE_FOCUSED_MASK)
				// TODO: ! Android does not report focus correctly all the time here. When a mark is focused, and then looses focus, and then is focused again (with no other drawable gaining focus inbetween) then stateBitset == 0 which is incorrect. Instead stateBitset should have the following bit OverlayItem.ITEM_STATE_FOCUSED_MASK 
				//         This problem is fixed by adding a OnFocusChangeListener to SearchServiceLocationsOverlays which catches the focus changes correctly and then sets iAmFocused appropriately.
				final Drawable marker = mContext.getResources().getDrawable(R.drawable.pointer_box);
                MyItemizedOverlay.boundCenterBottom(marker);
		    	
				final TextView tvTitle = new TextView(mContext); // TODO: Is there a better way to keep this manually drawn text in sync with the users presentation preferences?
//					final TextView tvSnippet = new TextView(mContext); // TODO: Is there a better way to keep this manually drawn text in sync with the users presentation preferences?
				
				final Paint paint = new Paint();
			    paint.setColor(Color.BLACK);
				paint.setTextSize(tvTitle.getTextSize());
				paint.setAntiAlias(true);
//					paint.setStyle(Paint.Style.FILL);
//					Log.v(TAG, "canvas.getWidth()=="+canvas.getWidth() + " canvas.getHeight()="+canvas.getHeight());
//					canvas.drawText("TEST", 0 - marker.getIntrinsicWidth()/2 + 2, 0 - 2 * (marker.getIntrinsicHeight()/3), paint);
//					canvas.drawText("TEST", 0, 0, paint);
//					String snippet = getSnippet();
				
				String snippet = mSnippet;
//				String snippet = getSnippet(); // TODO: ! verify that this works
															
				int splitIndex = snippet.indexOf('\n');
				String findSnippetText = null;
				if( -1 != splitIndex ){
					try{
						findSnippetText = snippet.substring(0, splitIndex);
					}catch(IndexOutOfBoundsException ioobe){
						findSnippetText = snippet;
					}
				}else{
					findSnippetText = snippet;
				}
				final String snippetText = findSnippetText;
				
//					// Find snippet width
//					tvSnippet.setText(snippetText);
//					tvSnippet.requestLayout();
//					tvSnippet.measure(View.MeasureSpec.AT_MOST, View.MeasureSpec.AT_MOST);											
//					int snippetWidth = tvSnippet.getMeasuredWidth(); 
//					
				final String title = getTitle()==null?"  ":getTitle(); // NOTE: String with spaces ensures that the title is measured correctly.
//					// Find snippet width
//					tvTitle.setText(title);
//					tvTitle.requestLayout();
//					tvTitle.measure(View.MeasureSpec.AT_MOST, View.MeasureSpec.AT_MOST);											
//					int titleWidth = tvTitle.getMeasuredWidth(); 
//					
//					int width = marker.getIntrinsicWidth();
//					int height = marker.getIntrinsicHeight();
				int width = marker.getIntrinsicWidth();
				int height = marker.getIntrinsicHeight();
				
				
				//											
//					int fullWidth = (Math.max(width, (Math.max(snippetWidth, titleWidth))));
//					int halfWidth = fullWidth/2;
				float titleWidth = paint.measureText(title);
				float snippetWidth = paint.measureText(snippetText);
				int fullWidth = (Math.max(width, Math.round((Math.max(snippetWidth, titleWidth)))));
				final int halfWidth = fullWidth/2;
				
//			        marker.setBounds(-halfWidth - 10, 1 - height, halfWidth  + 10, 1);
				marker.setBounds(-halfWidth - 10, 1 - height, halfWidth  + 10, 1);

				Drawable[] drawableArray = new Drawable[2];	    	
		    	
				drawableArray[0] = marker; 
				drawableArray[1] = new Drawable(){
//					drawableArray[0] = new Drawable(){
					
					@Override
					public void setColorFilter(ColorFilter colorfilter) {
					}
				
					@Override
					public void setAlpha(int i) {
					}
				
					@Override
					public int getOpacity() {
						return 0;
					}

					@Override
					public void draw(Canvas canvas) {
						


//							canvas.drawText(title, 0 - 52, 0 - 52, paint);
//							canvas.drawText(snippetText, 0 - 52, 0 - 36, paint);
						
						canvas.drawText(title, -halfWidth, 0 - 52, paint);
						canvas.drawText(snippetText, -halfWidth, 0 - 36, paint);
						
						// snippet.substring(splitIndex+1)

						
						// This doesn't display anything. I don't know why.
//							titleTv.setText(snippet.substring(0, splitIndex));
//							snippetTv.setText(snippet.substring(splitIndex+1));
//							searchServiceMarker.draw(canvas);
						
					}
				};								
				
				LayerDrawable ld = new LayerDrawable(drawableArray);
//					return super.getMarker(stateBitset);
				return ld;

	//TODO: why doesn't this code work? 									
//					String snippet = getSnippet();
//					int splitIndex = snippet.indexOf('\n');
//					titleTv.setText(snippet.substring(0, splitIndex));
//					snippetTv.setText(snippet.substring(splitIndex+1));
//					searchServiceMarker.buildDrawingCache();
//					Bitmap bitmap = searchServiceMarker.getDrawingCache(); // bitmap is always null! why?
//					searchServiceMarker.destroyDrawingCache();
//					BitmapDrawable bDrawable = new BitmapDrawable(bitmap);
//					return bDrawable;
				
			}else{
//					return plainMarker;
				return null;
			}
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0004Q", exp);
			lastTimeUserNotified = ErrorUtil.handleExceptionNotifyUser("ERR0004Q", exp, mContext, lastTimeUserNotified, 1000 * 20);
		}
		return null;
			
	}
	public boolean iAmFocused = false;
	
}


abstract class MyItemizedOverlay extends ItemizedOverlay<OverlayItem>{
    protected MyItemizedOverlay(Drawable drawable) {
        super(drawable);
    }

    public static Drawable boundCenterBottom(Drawable balloon){
            return ItemizedOverlay.boundCenterBottom(balloon);
    }

    public static Drawable boundCenter(Drawable balloon){
            return ItemizedOverlay.boundCenter(balloon);
    }
}
