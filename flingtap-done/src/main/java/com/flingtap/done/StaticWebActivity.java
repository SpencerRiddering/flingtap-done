/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flingtap.done;

import com.flingtap.common.HandledException;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
//import android.view.Display;
import android.view.KeyEvent;
import android.view.Window;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.flingtap.done.base.R;
/**
 * Most of original source code is from: http://android.git.kernel.org/?p=platform/packages/apps/HTMLViewer.git;a=blob_plain;f=src/com/android/htmlviewer/HTMLViewerActivity.java
 * @author spencer
 *
 * 
 *
 */
public class StaticWebActivity extends Activity {
	private static final String TAG = "StaticWebActivity";
	
	private WebView mWebView;
	
	public static final String EXTRA_IPHONE_AGENT_MODE = "com.flingtap.done.intent.extra.IPHONE_AGENT_MODE";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try{
			SessionUtil.onSessionStart(this);
			//Log.v(TAG, "onCreate(..) called");
			
			requestWindowFeature(Window.FEATURE_PROGRESS);
			
			Intent intent = getIntent();
			if( null == intent.getData() ){
				Log.e(TAG, "ERR000GG");
				ErrorUtil.handleExceptionFinish("ERR000GG", (Exception)(new Exception( )).fillInStackTrace(), this);
				setResult(RESULT_CANCELED);
				finish();
				return;
			}
			
			setContentView(R.layout.static_web_view);
			
			mWebView = (WebView) findViewById(R.id.web_view);
			
			// Setup callback support for title and progress bar
			mWebView.setWebChromeClient( new WebChrome() );
			mWebView.setWebViewClient(new MyWebViewClient());
			
			WebSettings settings = mWebView.getSettings();
			settings.setBuiltInZoomControls(true);
			settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
			settings.setJavaScriptEnabled(true);

			if(intent.getBooleanExtra(EXTRA_IPHONE_AGENT_MODE, false)){
				settings.setUserAgentString("Mozilla/5.0 (iPhone; U; CPU like Mac OS X; en)"); // Mozilla/5.0 (iPhone; U; CPU like Mac OS X; en) AppleWebKit/420+ (KHTML, like Gecko) Version/3.0 Mobile/1A542a Safari/419.3
				
			}
			
			// Restore a webview if we are meant to restore
			
			if (savedInstanceState != null) {
				mWebView.restoreState(savedInstanceState);
	        } else {
	            // Check the intent for the content to view
	            if (intent.getData() != null) {
	                Uri uri = intent.getData();
	                if (!"file".equals(uri.getScheme())) {
	                    mWebView.loadUrl(intent.getData().toString());
	                }else{
	                	setResult(RESULT_CANCELED);
	                	finish();
	                	return;
	                }
	            }
	        }

		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000GW", exp);
			ErrorUtil.handleExceptionNotifyUserFinish("ERR000GW", exp, this);										
			return;
		}  		
	}

    @Override
    protected void onResume() {
        super.onResume();
		SessionUtil.onSessionStart(this);
    }

    @Override
    protected void onPause() {
        super.onResume();
		SessionUtil.onSessionStop(this);
    }
    
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mWebView.saveState(outState);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		mWebView.stopLoading();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mWebView.destroy();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
	    	mWebView.goBack();
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	private class MyWebViewClient extends WebViewClient {
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	    	try{
	    		Uri uri = Uri.parse(url);
	    		String host = uri.getHost();
	    		String scheme = uri.getScheme();
	    		
	    		// Is it an HTTP or HTTPS scheme?
	    		if( null != scheme && scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https") ){
	    			
	    			if( null != host ){
		    			if( host.endsWith("youtube.com")){
	    					Intent theIntent = new Intent(Intent.ACTION_VIEW);
	    					theIntent.addFlags( Intent.FLAG_ACTIVITY_NO_HISTORY );
	    					theIntent.setData(uri);
	    					theIntent.setComponent(new ComponentName("com.google.android.youtube", "com.google.android.youtube.YouTubePlayer")); // TODO: !!!!! Check that youtube app is installed and redirect otherwise.
	    					startActivity(theIntent);
	    					return true;
		    			}
		    			
		    			if( host.equalsIgnoreCase("bit.ly")){
	    					Intent theIntent = new Intent(Intent.ACTION_VIEW);
	    					theIntent.addFlags( Intent.FLAG_ACTIVITY_NO_HISTORY );
	    					theIntent.setData(uri);
	    					startActivity(theIntent);
	    					return true;
		    			}
	    			}	    			
	    		}
	    		
	    		
	    		if(	(	// TODO: !!!! Check if MarketPlace is installed before adding the option menu item.
	    				null != scheme  && scheme.equalsIgnoreCase("http") 
	    				&& 
	    				null != host && host.equalsIgnoreCase("market.android.com")
	    			)
	    				|| 
	    			(
		    				null != scheme && scheme.equalsIgnoreCase("market") 
	    			)
	    		){
	    			
	    			Intent theIntent = new Intent(Intent.ACTION_VIEW);
	    			theIntent.addFlags( Intent.FLAG_ACTIVITY_NO_HISTORY );
	    			theIntent.setData(uri);
	    			startActivity(theIntent);
	    			return true;
	    		}
	    		
			}catch(HandledException h){ // Ignore.
			}catch(Exception exp){
				Log.e(TAG, "ERR000GX", exp);
				ErrorUtil.handleExceptionNotifyUserFinish("ERR000GX", exp, StaticWebActivity.this);										
				return true;
			}  		    	
	    	
	        view.loadUrl(url);
	        return true;
	    }
	}
	
	private class WebChrome extends WebChromeClient {
        
        @Override
        public void onReceivedTitle(WebView view, String title) {
        	int start = title.indexOf('(');
        	int end = title.indexOf(')');
        	if( -1 != start && -1 != end && start < end){
        		title = title.substring(0, start);
        		StaticWebActivity.this.setTitle(title);
        	}
        }
        
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            getWindow().setFeatureInt(Window.FEATURE_PROGRESS, newProgress*100);
        }
    }

	public static Intent createLaunchBrowserIntent(Uri data, boolean iphoneMode){
		Intent staticWebIntent = new Intent();
		ComponentName component = new ComponentName(StaticConfig.PACKAGE_NAME, StaticWebActivity.class.getName());
		staticWebIntent.setComponent(component);
		staticWebIntent.setData(data);
		staticWebIntent.putExtra(EXTRA_IPHONE_AGENT_MODE, iphoneMode);
		return staticWebIntent;
	}
	
}
