// Licensed under the Apache License, Version 2.0

package com.flingtap.common;

import android.app.Application;
import android.content.Context;
import com.flurry.android.FlurryAgent;

import java.util.concurrent.atomic.AtomicReference;

public class SessionUtilBase {
	private static final String TAG = "SessionUtil";
	
	private static AtomicReference<Thread.UncaughtExceptionHandler> mDefaultUncaughtExceptionHandler = new AtomicReference<Thread.UncaughtExceptionHandler>();
	
	public static final void initErrorHandling(Application application, String key){
		FlurryAgent.onStartSession(application, key); 
		FlurryAgent.onEndSession(application); 
		
		if( mDefaultUncaughtExceptionHandler.get() != Thread.getDefaultUncaughtExceptionHandler() ){
//			Log.v(TAG, "null == Thread.getDefaultUncaughtExceptionHandler()");
			
			if( null == mDefaultUncaughtExceptionHandler.get() ){   // We don't have a MyUncaughtExceptionHandler instance.
				if( mDefaultUncaughtExceptionHandler.compareAndSet(null, new MyUncaughtExceptionHandler()) ){
//					Log.v(TAG, "mDefaultUncaughtExceptionHandler.compareAndSet(..) is true");
				}
			}
			
			// --- Now we absolutely have that MyUncaughtExceptionHandler instance ---
			
			Thread.setDefaultUncaughtExceptionHandler(mDefaultUncaughtExceptionHandler.get());
		}
	}
	
	public static final void onSessionStart(Context context, String key){
		FlurryAgent.onStartSession(context, key); 
	}
	public static final void onSessionStop(Context context){
		FlurryAgent.onEndSession(context); 
	}

	private static class MyUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
		private Thread.UncaughtExceptionHandler eeh = null;
		public MyUncaughtExceptionHandler(){
			eeh = Thread.getDefaultUncaughtExceptionHandler();
		}
		
		private static final String TAG = "MyUncaughtExceptionHandler";
		public void uncaughtException(Thread thread, Throwable throwable) {
//			Log.v(TAG, "uncaughtException(..) called");
			try{
				ErrorUtil.handleException("UNCAUGHT", throwable);
			}catch(Throwable t){
			}finally{
				if( null != eeh ){
					eeh.uncaughtException(thread, throwable);
				}
			}
		}                              
	}

}
