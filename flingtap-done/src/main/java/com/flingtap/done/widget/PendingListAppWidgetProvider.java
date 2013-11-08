// Licensed under the Apache License, Version 2.0

package com.flingtap.done.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.flingtap.common.HandledException;
import com.flingtap.done.ErrorUtil;
import com.flingtap.done.StaticConfig;
import com.flingtap.done.TaskList;
import com.flingtap.done.base.R;
import com.flingtap.done.provider.Task;
import com.flingtap.done.provider.Task.Tasks;

/**
 *
 * <p>See also the following files:
 * <ul>
 *   <li>widget.PendingListAppWidgetConfigure.java</li>
 *   <li>res/layout/pending_appwidget_configure.xml</li>
 *   <li>res/layout/pending_appwidget_provider.xml</li>
 *   <li>res/xml/pending_appwidget_provider.xml</li>
 * </ul>
 * 
 * Widgets in 1.5 (1.6?) are a little quirky. See:
 *     http://groups.google.com/group/android-developers/browse_thread/thread/365d1ed3aac30916
 */
public class PendingListAppWidgetProvider extends AppWidgetProvider {
	
    // log tag
    private static final String TAG = "PendingListAppWidgetProvider";
    
    private final static String[] QUERY_DISPLAY_TASKS_PROJECTION = new String[]{Tasks.TASK_TITLE};
    private final static String   QUERY_DISPLAY_TASKS_SELECTION = Tasks._FILTER_BIT+"=? AND " + Tasks.COMPLETE+"=?";
    private final static String[] QUERY_DISPLAY_TASKS_SELECTION_ARGS = new String[]{Tasks.FILTER_IN, Tasks.COMPLETE_FALSE};
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    	PendingListAppWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetIds);
    }
    
    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds){
    	
//          Log.d(TAG, "onUpdate");
        // For each widget that needs an update, get the text that we should display:
        //   - Create a RemoteViews object for it
        //   - Set the text in the RemoteViews object
        //   - Tell the AppWidgetManager to show that views object for the widget.
        // TODO: Test with massive number of tasks, if problem then figure out how to apply "limit" to query. Maybe include a URI request param like "limit=10".
        Cursor cursor = context.getContentResolver().query(Tasks.CONTENT_URI, 
        		QUERY_DISPLAY_TASKS_PROJECTION, 
        		QUERY_DISPLAY_TASKS_SELECTION, 
        		QUERY_DISPLAY_TASKS_SELECTION_ARGS, 
        		null);
        assert null != cursor;
        
        // TODO: !! Cache the existing widget content and the compare it here with the new data. 
        //   If the same then you can avoid "updating" the widget.
        //   Better still, do the comparison in the content provider before sending the "update" broadcast.
        
        try{
        	final int N = appWidgetIds.length;
        	for (int i=0; i<N; i++) {
        		int appWidgetId = appWidgetIds[i];
        		
    			// Actual update.
    			Cursor widgetCursor = context.getContentResolver().query(Task.AppWidgetsDeleted.CONTENT_URI,
    					null,
    					Task.AppWidgetsDeleted._APP_WIDGET_ID+"=?", 
    					new String[]{String.valueOf(appWidgetId)}, 
    					null);
    			assert null != widgetCursor;
    			try{
    				if( 0 < widgetCursor.getCount() ){
    					// Phantom widget.
    					// Just ignore.
    					//Log.v(TAG, "Skipped phantom widget with Id " + appWidgetId);
    					continue;
    				}
    			}finally{
    				if( null != widgetCursor ){
    					widgetCursor.close();
    				}
    			}
        		
        		// **********************
        		// Get config for widget.
        		// **********************
        		int theme = Task.AppWidgets.THEME_PENDING_LIGHT;
        		int textSize = Task.AppWidgets.TEXT_SIZE_SMALL;
    			Cursor checkWidgetCursor = context.getContentResolver().query(Task.AppWidgets.CONTENT_URI,
    					new String[]{Task.AppWidgets._THEME, Task.AppWidgets._TEXT_SIZE},
						Task.AppWidgets._APP_WIDGET_ID+"=?", 
						new String[]{String.valueOf(appWidgetId)}, 
						null);
				assert null != checkWidgetCursor;
				try{
					if( checkWidgetCursor.moveToFirst() ){
						theme = checkWidgetCursor.getInt(0);
						textSize = checkWidgetCursor.getInt(1);
						//Log.v(TAG, "Found config theme=" + theme + " textSize=" + textSize);
					}
				}finally{
					if( null != checkWidgetCursor ){
						checkWidgetCursor.close();
					}
				}

        		// Get the layout for the App Widget and attach an on-click listener to the button
    			AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(appWidgetId);
    			boolean is3x3 = info.initialLayout == R.layout.pending_appwidget_provider_3x3_light;
    			
    			int actualTheme = is3x3 ? R.layout.pending_appwidget_provider_3x3_light : R.layout.pending_appwidget_provider_2x2_light;
    			switch(theme){
    				case Task.AppWidgets.THEME_PENDING_DARK:
    					if( is3x3 ){
    						actualTheme = R.layout.pending_appwidget_provider_3x3_dark;
    					}else{
    						actualTheme = R.layout.pending_appwidget_provider_2x2_dark;
    					}
    					break;
    				default: // Fallthrough
    				case Task.AppWidgets.THEME_PENDING_LIGHT:
    					if( is3x3 ){
    						actualTheme = R.layout.pending_appwidget_provider_3x3_light;
    					}else{
    						actualTheme = R.layout.pending_appwidget_provider_2x2_light;
    					}
    					break;
    			}
    			
        		RemoteViews views = new RemoteViews(context.getPackageName(), actualTheme);
        		if( cursor.moveToFirst() ){
        			views.setTextViewText(R.id.task0, cursor.getString(0));
        			views.setViewVisibility(R.id.task0, View.VISIBLE);
    				views.setViewVisibility(R.id.separator1, View.VISIBLE);
        			if( cursor.moveToNext() ){
        				views.setTextViewText(R.id.task1, cursor.getString(0));
        				views.setViewVisibility(R.id.task1, View.VISIBLE);
        				views.setViewVisibility(R.id.separator2, View.VISIBLE);
        				if( cursor.moveToNext() ){
        					views.setTextViewText(R.id.task2, cursor.getString(0));
            				views.setViewVisibility(R.id.task2, View.VISIBLE);
            				views.setViewVisibility(R.id.separator3, View.VISIBLE);
            				if( cursor.moveToNext() ){
            					views.setTextViewText(R.id.task3, cursor.getString(0));
                				views.setViewVisibility(R.id.task3, View.VISIBLE);
                				views.setViewVisibility(R.id.separator4, View.VISIBLE);
                				if( cursor.moveToNext() ){
                					views.setTextViewText(R.id.task4, cursor.getString(0));
                    				views.setViewVisibility(R.id.task4, View.VISIBLE);
                    				views.setViewVisibility(R.id.separator5, View.VISIBLE);
                    				if( cursor.moveToNext() ){
                    					views.setTextViewText(R.id.task5, cursor.getString(0));
                        				views.setViewVisibility(R.id.task5, View.VISIBLE);
                    					views.setViewVisibility(R.id.separator6, View.VISIBLE);
                    					if( cursor.moveToNext() ){
                    						views.setTextViewText(R.id.task6, cursor.getString(0));
                    						views.setViewVisibility(R.id.task6, View.VISIBLE);
                    						views.setViewVisibility(R.id.separator7, View.VISIBLE);
                    						if( cursor.moveToNext() ){
                    							views.setTextViewText(R.id.task7, cursor.getString(0));
                    							views.setViewVisibility(R.id.task7, View.VISIBLE);
                    							if( is3x3 ){
                        							views.setViewVisibility(R.id.separator8, View.VISIBLE);
                        							if( cursor.moveToNext() ){
                        								views.setTextViewText(R.id.task8, cursor.getString(0));
                        								views.setViewVisibility(R.id.task8, View.VISIBLE);
                        								views.setViewVisibility(R.id.separator9, View.VISIBLE);
                        								if( cursor.moveToNext() ){
                        									views.setTextViewText(R.id.task9, cursor.getString(0));
                        									views.setViewVisibility(R.id.task9, View.VISIBLE);
                        									views.setViewVisibility(R.id.separator10, View.VISIBLE);
                        									if( cursor.moveToNext() ){
                        										views.setTextViewText(R.id.task10, cursor.getString(0));
                        										views.setViewVisibility(R.id.task10, View.VISIBLE);
                        										views.setViewVisibility(R.id.separator11, View.VISIBLE);
                        										if( cursor.moveToNext() ){
                        											views.setTextViewText(R.id.task11, cursor.getString(0));
                        											views.setViewVisibility(R.id.task11, View.VISIBLE);
	                        										views.setViewVisibility(R.id.separator12, View.VISIBLE);
	                        										if( cursor.moveToNext() ){
	                        											views.setTextViewText(R.id.task12, cursor.getString(0));
	                        											views.setViewVisibility(R.id.task12, View.VISIBLE);
		                        										views.setViewVisibility(R.id.separator13, View.VISIBLE);
		                        										if( cursor.moveToNext() ){
		                        											views.setTextViewText(R.id.task13, cursor.getString(0));
		                        											views.setViewVisibility(R.id.task13, View.VISIBLE);
		                        										}else{
		                        					    					views.setViewVisibility(R.id.task13, View.GONE);
		                        										}
	                        										}else{
	                        					    					views.setViewVisibility(R.id.task12, View.GONE);
	                        					        				views.setViewVisibility(R.id.separator13, View.GONE);
	                        					    					views.setViewVisibility(R.id.task13, View.GONE);
	                        										}
                        										}else{
                        											views.setViewVisibility(R.id.task11, View.GONE);
                        					        				views.setViewVisibility(R.id.separator12, View.GONE);
                        					    					views.setViewVisibility(R.id.task12, View.GONE);
                        					        				views.setViewVisibility(R.id.separator13, View.GONE);
                        					    					views.setViewVisibility(R.id.task13, View.GONE);
                        										}
                        									}else{
                        										views.setViewVisibility(R.id.task10, View.VISIBLE);
                        										views.setViewVisibility(R.id.separator11, View.GONE);
                        										views.setViewVisibility(R.id.task11, View.GONE);
                        				        				views.setViewVisibility(R.id.separator12, View.GONE);
                        				    					views.setViewVisibility(R.id.task12, View.GONE);
                        				        				views.setViewVisibility(R.id.separator13, View.GONE);
                        				    					views.setViewVisibility(R.id.task13, View.GONE);
                        									}
                        								}else{
                        									views.setViewVisibility(R.id.task9, View.GONE);
                        									views.setViewVisibility(R.id.separator10, View.GONE);
                        									views.setViewVisibility(R.id.task10, View.GONE);
                        									views.setViewVisibility(R.id.separator11, View.GONE);
                        									views.setViewVisibility(R.id.task11, View.GONE);
                        			        				views.setViewVisibility(R.id.separator12, View.GONE);
                        			    					views.setViewVisibility(R.id.task12, View.GONE);
                        			        				views.setViewVisibility(R.id.separator13, View.GONE);
                        			    					views.setViewVisibility(R.id.task13, View.GONE);
                        								}
                        							}else{
                        								views.setViewVisibility(R.id.task8, View.GONE);
                        								views.setViewVisibility(R.id.separator9, View.GONE);
                        								views.setViewVisibility(R.id.task9, View.GONE);
                        								views.setViewVisibility(R.id.separator10, View.GONE);
                        								views.setViewVisibility(R.id.task10, View.GONE);
                        								views.setViewVisibility(R.id.separator11, View.GONE);
                        								views.setViewVisibility(R.id.task11, View.GONE);
                        		        				views.setViewVisibility(R.id.separator12, View.GONE);
                        		    					views.setViewVisibility(R.id.task12, View.GONE);
                        		        				views.setViewVisibility(R.id.separator13, View.GONE);
                        		    					views.setViewVisibility(R.id.task13, View.GONE);
                        							}
                    							}
                    						}else{
                    							views.setViewVisibility(R.id.task7, View.GONE);
                                				if( is3x3 ){
                        							views.setViewVisibility(R.id.separator8, View.GONE);
                        							views.setViewVisibility(R.id.task8, View.GONE);
                        							views.setViewVisibility(R.id.separator9, View.GONE);
                        							views.setViewVisibility(R.id.task9, View.GONE);
                        							views.setViewVisibility(R.id.separator10, View.GONE);
                        							views.setViewVisibility(R.id.task10, View.GONE);
                        							views.setViewVisibility(R.id.separator11, View.GONE);
                        							views.setViewVisibility(R.id.task11, View.GONE);
                        	        				views.setViewVisibility(R.id.separator12, View.GONE);
                        	    					views.setViewVisibility(R.id.task12, View.GONE);
                        	        				views.setViewVisibility(R.id.separator13, View.GONE);
                        	    					views.setViewVisibility(R.id.task13, View.GONE);
                                				}
                    						}
                    					}else{
                    						views.setViewVisibility(R.id.task6, View.GONE);
                    						views.setViewVisibility(R.id.separator7, View.GONE);
                    						views.setViewVisibility(R.id.task7, View.GONE);
                            				if( is3x3 ){
                        						views.setViewVisibility(R.id.separator8, View.GONE);
                        						views.setViewVisibility(R.id.task8, View.GONE);
                        						views.setViewVisibility(R.id.separator9, View.GONE);
                        						views.setViewVisibility(R.id.task9, View.GONE);
                        						views.setViewVisibility(R.id.separator10, View.GONE);
                        						views.setViewVisibility(R.id.task10, View.GONE);
                        						views.setViewVisibility(R.id.separator11, View.GONE);
                        						views.setViewVisibility(R.id.task11, View.GONE);
                    	        				views.setViewVisibility(R.id.separator12, View.GONE);
                    	    					views.setViewVisibility(R.id.task12, View.GONE);
                    	        				views.setViewVisibility(R.id.separator13, View.GONE);
                    	    					views.setViewVisibility(R.id.task13, View.GONE);
                            				}
                    					}
                    				}else{
                    					views.setViewVisibility(R.id.task5, View.GONE);
                    					views.setViewVisibility(R.id.separator6, View.GONE);
                    					views.setViewVisibility(R.id.task6, View.GONE);
                    					views.setViewVisibility(R.id.separator7, View.GONE);
                    					views.setViewVisibility(R.id.task7, View.GONE);
                        				if( is3x3 ){
                        					views.setViewVisibility(R.id.separator8, View.GONE);
                        					views.setViewVisibility(R.id.task8, View.GONE);
                        					views.setViewVisibility(R.id.separator9, View.GONE);
                        					views.setViewVisibility(R.id.task9, View.GONE);
                        					views.setViewVisibility(R.id.separator10, View.GONE);
                            				views.setViewVisibility(R.id.task10, View.GONE);
                            				views.setViewVisibility(R.id.separator11, View.GONE);
                        					views.setViewVisibility(R.id.task11, View.GONE);
                	        				views.setViewVisibility(R.id.separator12, View.GONE);
                	    					views.setViewVisibility(R.id.task12, View.GONE);
                	        				views.setViewVisibility(R.id.separator13, View.GONE);
                	    					views.setViewVisibility(R.id.task13, View.GONE);
                        				}
                    				}
                				}else{
                					views.setViewVisibility(R.id.task4, View.GONE);
                    				views.setViewVisibility(R.id.separator5, View.GONE);
                        			views.setViewVisibility(R.id.task5, View.GONE);        		
                					views.setViewVisibility(R.id.separator6, View.GONE);
                					views.setViewVisibility(R.id.task6, View.GONE);
                					views.setViewVisibility(R.id.separator7, View.GONE);
                					views.setViewVisibility(R.id.task7, View.GONE);
                    				if( is3x3 ){
                    					views.setViewVisibility(R.id.separator8, View.GONE);
                    					views.setViewVisibility(R.id.task8, View.GONE);
                    					views.setViewVisibility(R.id.separator9, View.GONE);
                    					views.setViewVisibility(R.id.task9, View.GONE);
                    					views.setViewVisibility(R.id.separator10, View.GONE);
                        				views.setViewVisibility(R.id.task10, View.GONE);
                        				views.setViewVisibility(R.id.separator11, View.GONE);
                    					views.setViewVisibility(R.id.task11, View.GONE);
            	        				views.setViewVisibility(R.id.separator12, View.GONE);
            	    					views.setViewVisibility(R.id.task12, View.GONE);
            	        				views.setViewVisibility(R.id.separator13, View.GONE);
            	    					views.setViewVisibility(R.id.task13, View.GONE);
                    				}                            			
                				}
            				}else{
            					views.setViewVisibility(R.id.task3, View.GONE);
                				views.setViewVisibility(R.id.separator4, View.GONE);
                    			views.setViewVisibility(R.id.task4, View.GONE);        		
                				views.setViewVisibility(R.id.separator5, View.GONE);
                    			views.setViewVisibility(R.id.task5, View.GONE);        		
                    			views.setViewVisibility(R.id.separator6, View.GONE);
                    			views.setViewVisibility(R.id.task6, View.GONE);
                    			views.setViewVisibility(R.id.separator7, View.GONE);
                    			views.setViewVisibility(R.id.task7, View.GONE);
                				if( is3x3 ){
                					views.setViewVisibility(R.id.separator8, View.GONE);
                					views.setViewVisibility(R.id.task8, View.GONE);
                					views.setViewVisibility(R.id.separator9, View.GONE);
                					views.setViewVisibility(R.id.task9, View.GONE);
                					views.setViewVisibility(R.id.separator10, View.GONE);
                    				views.setViewVisibility(R.id.task10, View.GONE);
                    				views.setViewVisibility(R.id.separator11, View.GONE);
                					views.setViewVisibility(R.id.task11, View.GONE);
        	        				views.setViewVisibility(R.id.separator12, View.GONE);
        	    					views.setViewVisibility(R.id.task12, View.GONE);
        	        				views.setViewVisibility(R.id.separator13, View.GONE);
        	    					views.setViewVisibility(R.id.task13, View.GONE);
                				}
            				}
        				}else{
        					views.setViewVisibility(R.id.task2, View.GONE);
            				views.setViewVisibility(R.id.separator3, View.GONE);
                			views.setViewVisibility(R.id.task3, View.GONE);        		
            				views.setViewVisibility(R.id.separator4, View.GONE);
                			views.setViewVisibility(R.id.task4, View.GONE);        		
            				views.setViewVisibility(R.id.separator5, View.GONE);
                			views.setViewVisibility(R.id.task5, View.GONE);        		
                			views.setViewVisibility(R.id.separator6, View.GONE);
                			views.setViewVisibility(R.id.task6, View.GONE);
                			views.setViewVisibility(R.id.separator7, View.GONE);
                			views.setViewVisibility(R.id.task7, View.GONE);
            				if( is3x3 ){
            					views.setViewVisibility(R.id.separator8, View.GONE);
            					views.setViewVisibility(R.id.task8, View.GONE);
            					views.setViewVisibility(R.id.separator9, View.GONE);
            					views.setViewVisibility(R.id.task9, View.GONE);
            					views.setViewVisibility(R.id.separator10, View.GONE);
                				views.setViewVisibility(R.id.task10, View.GONE);
                				views.setViewVisibility(R.id.separator11, View.GONE);
            					views.setViewVisibility(R.id.task11, View.GONE);
    	        				views.setViewVisibility(R.id.separator12, View.GONE);
    	    					views.setViewVisibility(R.id.task12, View.GONE);
    	        				views.setViewVisibility(R.id.separator13, View.GONE);
    	    					views.setViewVisibility(R.id.task13, View.GONE);
            				}
        				}
        			}else{
        				views.setViewVisibility(R.id.task1, View.GONE);
        				views.setViewVisibility(R.id.separator2, View.GONE);
        				views.setViewVisibility(R.id.task2, View.GONE);
        				views.setViewVisibility(R.id.separator3, View.GONE);
            			views.setViewVisibility(R.id.task3, View.GONE);        		
        				views.setViewVisibility(R.id.separator4, View.GONE);
            			views.setViewVisibility(R.id.task4, View.GONE);        		
        				views.setViewVisibility(R.id.separator5, View.GONE);
            			views.setViewVisibility(R.id.task5, View.GONE);        		
            			views.setViewVisibility(R.id.separator6, View.GONE);
            			views.setViewVisibility(R.id.task6, View.GONE);
            			views.setViewVisibility(R.id.separator7, View.GONE);
            			views.setViewVisibility(R.id.task7, View.GONE);
        				if( is3x3 ){
        					views.setViewVisibility(R.id.separator8, View.GONE);
        					views.setViewVisibility(R.id.task8, View.GONE);
        					views.setViewVisibility(R.id.separator9, View.GONE);
        					views.setViewVisibility(R.id.task9, View.GONE);
        					views.setViewVisibility(R.id.separator10, View.GONE);
            				views.setViewVisibility(R.id.task10, View.GONE);
            				views.setViewVisibility(R.id.separator11, View.GONE);
        					views.setViewVisibility(R.id.task11, View.GONE);
	        				views.setViewVisibility(R.id.separator12, View.GONE);
	    					views.setViewVisibility(R.id.task12, View.GONE);
	        				views.setViewVisibility(R.id.separator13, View.GONE);
	    					views.setViewVisibility(R.id.task13, View.GONE);
        				}
        			}
        		}else{
        			views.setViewVisibility(R.id.task0, View.INVISIBLE);
    				views.setViewVisibility(R.id.separator1, View.GONE);
        			views.setTextViewText(R.id.task1, "No tasks");
    				views.setViewVisibility(R.id.task1, View.VISIBLE);
    				views.setViewVisibility(R.id.separator2, View.GONE);
        			views.setViewVisibility(R.id.task2, View.GONE);        		
    				views.setViewVisibility(R.id.separator3, View.GONE);
        			views.setViewVisibility(R.id.task3, View.GONE);        		
    				views.setViewVisibility(R.id.separator4, View.GONE);
        			views.setViewVisibility(R.id.task4, View.GONE);        		
    				views.setViewVisibility(R.id.separator5, View.GONE);
        			views.setViewVisibility(R.id.task5, View.GONE);        		
        			views.setViewVisibility(R.id.separator6, View.GONE);
        			views.setViewVisibility(R.id.task6, View.GONE);
        			views.setViewVisibility(R.id.separator7, View.GONE);
        			views.setViewVisibility(R.id.task7, View.GONE);
    				if( is3x3 ){
    					views.setViewVisibility(R.id.separator8, View.GONE);
    					views.setViewVisibility(R.id.task8, View.GONE);
    					views.setViewVisibility(R.id.separator9, View.GONE);
    					views.setViewVisibility(R.id.task9, View.GONE);
    					views.setViewVisibility(R.id.separator10, View.GONE);
        				views.setViewVisibility(R.id.task10, View.GONE);
        				views.setViewVisibility(R.id.separator11, View.GONE);
    					views.setViewVisibility(R.id.task11, View.GONE);
        				views.setViewVisibility(R.id.separator12, View.GONE);
    					views.setViewVisibility(R.id.task12, View.GONE);
        				views.setViewVisibility(R.id.separator13, View.GONE);
    					views.setViewVisibility(R.id.task13, View.GONE);
    				}
        		}
        		
        		// Create an Intent to launch TaskList activity.
        		Intent intent = new Intent(context, TaskList.class);
        		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        		views.setOnClickPendingIntent(R.id.frame, pendingIntent);

        		// TODO: Can I just do this the first time? and avoid it on subsequent times? 
				float actualTextSize = 14;
				switch(textSize){
					default:
					case Task.AppWidgets.TEXT_SIZE_SMALL:
						actualTextSize = 14;
						break;
					case Task.AppWidgets.TEXT_SIZE_MEDIUM:
						actualTextSize = 18;
						break;
					case Task.AppWidgets.TEXT_SIZE_LARGE:
						actualTextSize = 22;
						break;
					case Task.AppWidgets.TEXT_SIZE_JUMBO:
						actualTextSize = 26;
						break;
				}
            	views.setFloat(R.id.task0, "setTextSize", actualTextSize);
            	views.setFloat(R.id.task1, "setTextSize", actualTextSize);
            	views.setFloat(R.id.task2, "setTextSize", actualTextSize);
            	views.setFloat(R.id.task3, "setTextSize", actualTextSize);
            	views.setFloat(R.id.task4, "setTextSize", actualTextSize);
            	views.setFloat(R.id.task5, "setTextSize", actualTextSize);
            	views.setFloat(R.id.task6, "setTextSize", actualTextSize);
            	views.setFloat(R.id.task7, "setTextSize", actualTextSize);
				if( is3x3 ){
					views.setFloat(R.id.task8, "setTextSize", actualTextSize);
                	views.setFloat(R.id.task9, "setTextSize", actualTextSize);
                	views.setFloat(R.id.task10, "setTextSize", actualTextSize);
                	views.setFloat(R.id.task11, "setTextSize", actualTextSize);
                	views.setFloat(R.id.task12, "setTextSize", actualTextSize);
                	views.setFloat(R.id.task13, "setTextSize", actualTextSize);
				}

        		// Tell the AppWidgetManager to perform an update on the current App Widget
        		appWidgetManager.updateAppWidget(appWidgetId, views);    
        	}
        }finally{
        	if( null != cursor ){
        		cursor.close();
        	}
        }
    }
    
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
    	for(int appWidgetId: appWidgetIds){
    		try{
    			// Delete from app widget table.
    			try{
    				context.getContentResolver().delete(Task.AppWidgets.CONTENT_URI, 
	    					Task.AppWidgets._APP_WIDGET_ID + "=?",
	    					new String[]{String.valueOf(appWidgetId)});
    			}finally{
    				// Add to deleted widget table.
    				Log.v(TAG, "Adding widget with Id " + appWidgetId + " to deleted widget table.");
    				// Not bound when this method called, so this is first setup.
    				ContentValues values = new ContentValues(1);
    				values.put(Task.AppWidgetsDeleted._APP_WIDGET_ID, appWidgetId);
    				context.getContentResolver().insert(Task.AppWidgetsDeleted.CONTENT_URI, values);
    			}

    		}catch(HandledException h){ // Ignore.
    		}catch(Exception e){
    			Log.e(TAG, "ERR000JX", e);
    			ErrorUtil.handleException("ERR000JX", e, context);
    		}
    	}
    	
    }
    
    private static final ComponentName COMPONENT_NAME_2x2 = new ComponentName(StaticConfig.PACKAGE_NAME, PendingListAppWidgetProvider2x2.class.getName());
    private static final ComponentName COMPONENT_NAME_3x3 = new ComponentName(StaticConfig.PACKAGE_NAME, PendingListAppWidgetProvider3x3.class.getName());

    /**
     * Workaround for Android v1.5 not handling ACTION_APPWIDGET_DELETED correctly.
     * See: http://groups.google.com/group/android-developers/msg/e405ca19df2170e2
     */
    public void onReceive(Context context, Intent intent) {
    	try{
    		final String action = intent.getAction(); 
    		final Bundle extras = intent.getExtras();
    		if (null != extras && AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
				final int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
				if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
					this.onDeleted(context, new int[] { appWidgetId });
				}
			} else if (null == extras && ("com.flingtap.done.intent.action.FILTER_BITS_CHANGED".equals(intent.getAction()) || "com.flingtap.done.intent.action.TASK_TITLE_CHANGED".equals(intent.getAction()))) {

				int[] widgetIds2 = AppWidgetManager.getInstance(context).getAppWidgetIds(COMPONENT_NAME_2x2);
				int[] widgetIds3 = AppWidgetManager.getInstance(context).getAppWidgetIds(COMPONENT_NAME_3x3);
				int[] widgetIdsCall = new int[widgetIds2.length + widgetIds3.length];
				System.arraycopy(widgetIds2, 0, widgetIdsCall, 0, widgetIds2.length);
				System.arraycopy(widgetIds3, 0, widgetIdsCall, widgetIds2.length, widgetIds3.length);

				if (0 != widgetIdsCall.length) {
					onUpdate(context, AppWidgetManager.getInstance(context), widgetIdsCall);
				}
			} else {
				super.onReceive(context, intent);
			} 
		}catch(HandledException h){ // Ignore.
		}catch(Exception e){
			Log.e(TAG, "ERR000JY", e);
			ErrorUtil.handleException("ERR000JY", e, context);
		}
    }
}


