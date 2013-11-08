// Licensed under the Apache License, Version 2.0

package com.flingtap.common;

import java.util.Map;

import com.flurry.android.FlurryAgent;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class EventBase {

	protected EventBase(){};
	
	public static final void onEvent(String name){
		FlurryAgent.onEvent(name, null); 
	}	
	public static final void onEvent(String name, Map<String,String> params){
		FlurryAgent.onEvent(name, params); 
	}	
	
	public static final String STATIC_DISPLAY = "Static Display";
	public static final String STATIC_DISPLAY__PARAM_TITLE = "title";
	public static final String STATIC_DISPLAY__PARAM_LAYOUT = "layout";
	
	public static final String PROMPT_EULA   = "Prompt User to Accept EULA";
	public static final String PROMPT_EULA__DECISION = "Decision";
	public static final String PROMPT_EULA__DECISION__ACCEPT = "Accept";
	public static final String PROMPT_EULA__DECISION__DECLINE = "Decline";
	
	// TODO: !!! Consider just not not adding a parameter instead of explicitly giving it a name like "None" or "Unknown". See how the current system works.
	public static Map<String,String> prepareIntentParams(Context context, Map<String,String> parameters, Intent intent){
		parameters.put("Scheme", null == intent.getScheme()?"Unknown":intent.getScheme());
		Uri uri = intent.getData();
		parameters.put("Authority", null==uri?"None":(null==uri.getAuthority()?"None":uri.getAuthority()));
		String type = intent.getType();
		if( null == type && null != uri ){
			type = context.getContentResolver().getType(uri);
		}
		parameters.put("MIME type", type==null?"Unknown":type);
		parameters.put("Action", intent.getAction()==null?"None":intent.getAction());
		parameters.put("Component", intent.getComponent()==null?"None":intent.getComponent().flattenToShortString());
		
		return parameters;
	}
	
	// TODO: !!! Consider just not not adding a parameter instead of explicitly giving it a name like "None" or "Unknown". See how the current system works.
	public static Map<String,String> prepareUriParams(Context context, Map<String,String> parameters, Uri uri){
		parameters.put("Authority", null==uri?"None":(null==uri.getAuthority()?"None":uri.getAuthority()));
		String type = context.getContentResolver().getType(uri);
		parameters.put("MIME type", type==null?"Unknown":type);
		return parameters;
	}	
	// TODO: !!! Consider whether this is needed. See above for details.
	public static Map<String,String> prepareMissingIntentParams(Context context, Map<String,String> parameters){
		parameters.put("Scheme",    "Intent missing");
		parameters.put("Authority", "Intent missing");
		parameters.put("MIME type", "Intent missing");
		parameters.put("Action",    "Intent missing");
		parameters.put("Component", "Intent missing");
		return parameters;
	}	
}
