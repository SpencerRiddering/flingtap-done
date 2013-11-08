// Licensed under the Apache License, Version 2.0

package com.flingtap.common;

import java.util.LinkedList;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.flingtap.common.HandledException;
import com.flurry.android.FlurryAgent;

public abstract class ErrorUtil {

	private static final String PACKAGE_COM_FLINGTAP = "com.flingtap.";

	protected ErrorUtil(){}
	
	public static final String extractRootCauseMessage(Throwable e) {
		while(null != e.getCause()){
			e = e.getCause();
		}
		return e.getMessage();
	}
	
	public static final Throwable extractRootCauseException(Throwable e) {
		while(null != e.getCause()){
			e = e.getCause();
		}
		return e;
	}
	
	
	public static void handleError(String errorId, final Error error, final Context context) {
		FlurryAgent.onError(errorId, ErrorUtil.extractRootCauseMessage(error), error.getClass().getName());
		throw error;
	}

	public static void handleExceptionNotifyUser(String errorId, final Exception exp, final Context context) {
		FlurryAgent.onError(errorId, ErrorUtil.getProcessedStackTrace(exp, PACKAGE_COM_FLINGTAP), exp.getClass().getName()); 
		ErrorUtil.notifyUser(context, exp);
	}
	
	public static void handleExceptionNotifyUserFinish(String errorId, final Exception exp, final Activity activity) {
		FlurryAgent.onError(errorId, ErrorUtil.getProcessedStackTrace(exp, PACKAGE_COM_FLINGTAP), exp.getClass().getName());
		ErrorUtil.notifyUser(activity, exp);
		activity.setResult(Activity.RESULT_CANCELED);
		activity.finish();
	}
	
	public static long handleExceptionNotifyUser(String errorId, final Exception exp, final Context context, long lastTimeUserNotified, int durration) {
		if( lastTimeUserNotified + durration < System.currentTimeMillis() ){
			FlurryAgent.onError(errorId, ErrorUtil.getProcessedStackTrace(exp, PACKAGE_COM_FLINGTAP), exp.getClass().getName());
			lastTimeUserNotified = System.currentTimeMillis();
			ErrorUtil.notifyUser(context, exp);
		}
		return lastTimeUserNotified;
	}

	public static void handleExceptionNotifyUserAndThrow(String errorId, final Exception exp, final Context context) {
		FlurryAgent.onError(errorId, ErrorUtil.getProcessedStackTrace(exp, PACKAGE_COM_FLINGTAP), exp.getClass().getName());
		ErrorUtil.notifyUser(context, exp);
		throw new HandledException(errorId);
	}
	
	// TODO: !!! Reconsider whether this method should be used (rather than handleExceptionNotifyUserFinish which seems to work well).
	public static void handleExceptionFinish(String errorId, final Exception exp, final Activity activity) {
		ErrorUtil.handleException(errorId, exp, activity);
		activity.setResult(SharedConstant.RESULT_ERROR);
		activity.finish();
	}
	public static void handleException(String errorId, final Throwable exp, final Context context) {
		FlurryAgent.onError(errorId, ErrorUtil.getProcessedStackTrace(exp, PACKAGE_COM_FLINGTAP), exp.getClass().getName());
	}
	
	public static void handleException(String errorId, final Throwable exp) {
		FlurryAgent.onError(errorId, ErrorUtil.getProcessedStackTrace(exp, PACKAGE_COM_FLINGTAP), exp.getClass().getName());
	}

	public static void handleExceptionAndThrow(String errorId, final Exception exp) {
		handleExceptionAndThrow(errorId, exp, null);
	}
	
	public static void handleExceptionAndThrow(String errorId, final Exception exp, final Context context) {
		FlurryAgent.onError(errorId, ErrorUtil.getProcessedStackTrace(exp, PACKAGE_COM_FLINGTAP), exp.getClass().getName());
		throw new HandledException(errorId);
	}

	// TODO: !!! All errors should include an exception so we get a stack trace.
	public static void handle(String errorId, String message, Object theObject) {
	    // TODO: !! Create (and fill) a new exception in this situation so that you know how you go here. Example: Exception exp = (Exception)(new Exception("Unable to rename label.").fillInStackTrace());
		FlurryAgent.onError(errorId, null==message?"":message, null==theObject?"":theObject.getClass().getName());
	}

	// TODO: !!! All errors should include an exception so we get a stack trace.
	public static boolean handle(String errorId, String message, Object theObject, boolean actOnIt) {
		if( actOnIt ){
			FlurryAgent.onError(errorId, null==message?"":message, null==theObject?"":theObject.getClass().getName());
		}
		return false;
	}

	public static void notifyUser(final Context context) {
		notifyUser(context, null);
	}
	
	public static Integer NOTIFY_USER_TOAST_MESSAGE_STRING_ID = null; // This MUST be initialized in the 

	public static void notifyUser(final Context context, Throwable throwable) {
		
		// If the root cause is OutOfMemoryError then we don't want to launch a Toast since this will likely trigger the memory problem yet again and show user the Sorry dialog of death.
        if(null != throwable) { 
        	while(null != throwable.getCause()){
        		throwable = throwable.getCause();
        	}
        	if( throwable instanceof OutOfMemoryError ){
        		return;
        	}
        }
        
		if( null == NOTIFY_USER_TOAST_MESSAGE_STRING_ID ){
			Toast.makeText(context, "Sorry, an error occurred", Toast.LENGTH_LONG).show();
		}else{
			Toast.makeText(context, NOTIFY_USER_TOAST_MESSAGE_STRING_ID, Toast.LENGTH_LONG).show();
		}
	}

	public static String getProcessedStackTrace(Throwable throwable, String packageName){
        if(null == throwable) {
            return "";
        }
        
		LinkedList<Throwable> throwables = new LinkedList<Throwable>();
		do{
			throwables.addFirst(throwable);
			throwable = throwable.getCause();
		}while(null != throwable);
		
		StringBuffer sb = new StringBuffer();
		appendLeanRootStackTrace(throwables.get(0), sb, true, packageName);
		
		for(int i=1; i < throwables.size(); i++){
			appendLeanRootStackTrace(throwables.get(i), sb, false, packageName);
			sb.append("\n-\n");
		}
		return sb.toString();
	}
		
	
	/**
	 * @param rootExp
	 * @return
	 */
	public static void appendLeanRootStackTrace(Throwable rootExp, StringBuffer sb, boolean showMessage, String packageName){
        if(null == rootExp) {
            return;
        }
		StackTraceElement[] elements = rootExp.getStackTrace();
		if( null == sb ){
			sb = new StringBuffer();
		}
		if( showMessage && null != rootExp.getMessage() ){
			sb.append(rootExp.getMessage()+" ");
		}
		boolean printDots = true;
		for(int i=0; i<elements.length; i++){
			if(elements[i].toString().startsWith(packageName)){
				sb.append(elements[i].toString().replaceFirst(packageName + '.', "")+" "); // "\tat " + 
				printDots = true;
			}else{
				if( printDots ){
					sb.append(". "); 
					printDots = false;
				}
			}
		}
		return;
	}
}

