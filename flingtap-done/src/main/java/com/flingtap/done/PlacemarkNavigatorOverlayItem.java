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

/**
 * TODO: !!! There has got to be a cleaner way to do this.
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

				final Paint paint = new Paint();
			    paint.setColor(Color.BLACK);
				paint.setTextSize(tvTitle.getTextSize());
				paint.setAntiAlias(true);

				String snippet = mSnippet;

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
				
				final String title = getTitle()==null?"  ":getTitle(); // NOTE: String with spaces ensures that the title is measured correctly.
				int width = marker.getIntrinsicWidth();
				int height = marker.getIntrinsicHeight();
				
				float titleWidth = paint.measureText(title);
				float snippetWidth = paint.measureText(snippetText);
				int fullWidth = (Math.max(width, Math.round((Math.max(snippetWidth, titleWidth)))));
				final int halfWidth = fullWidth/2;
				
				marker.setBounds(-halfWidth - 10, 1 - height, halfWidth  + 10, 1);

				Drawable[] drawableArray = new Drawable[2];	    	
		    	
				drawableArray[0] = marker; 
				drawableArray[1] = new Drawable(){

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
						canvas.drawText(title, -halfWidth, 0 - 52, paint);
						canvas.drawText(snippetText, -halfWidth, 0 - 36, paint);
					}
				};								
				
				LayerDrawable ld = new LayerDrawable(drawableArray);
				return ld;
			}else{
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
