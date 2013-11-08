// Licensed under the Apache License, Version 2.0

package com.flingtap.done.addon.quicklaunch;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.*;
import android.preference.Preference.OnPreferenceChangeListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.flingtap.common.ErrorUtil;
import com.flingtap.common.HandledException;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * 
 * @author spencer
 *
 */
public class ApplicationPreferenceActivity 
		extends PreferenceActivity 
		implements OnPreferenceChangeListener, OnShowDialogListener, DialogInterface.OnClickListener {
	private static final String TAG = "ApplicationPreferenceActivity";
	
    public static final int HELP_ID = Menu.FIRST + 10;

    private CheckBoxPreference quickLaunchEnabledPref = null;
    private MyListPreference shortcutPref = null;
    private SharedPreferences settings = null;
    
    private class MyListPreference extends ListPreference{
    	public MyListPreference(Context context, OnShowDialogListener showDialogListener) {
			super(context);
			mShowDialogListener = showDialogListener; 
		}
    	private OnShowDialogListener mShowDialogListener = null;
    	
		public void showDialog(Bundle state) {
    		mShowDialogListener.onShowDialog(this);
    		super.showDialog(state);
    	}
		
    };

	public void onShowDialog(ListPreference listPreference) {
		try{
			String value = listPreference.getValue();
			if( null != value && !Bookmarks.exists(getContentResolver(), value.charAt(0))){
				listPreference.setValue(null);
				settings.edit().remove(ApplicationPreference.QUICK_LAUNCH_SHORTCUT).commit();
				mPrevShortcut = null;
				
				isEnableQuickLaunchDialog = true;
				quickLaunchEnabledPref.setChecked(false);	
			}		
			
			updateShortcutSummary();
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000HR", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000HR", exp, this);
		}  
	}

    protected void updateShortcutSummary(){
    	if( null != shortcutPref.getValue() ){// 
    		shortcutPref.setSummary(TextUtils.expandTemplate(getText(R.string.preferences_quick_launch_shortcut_summary_key), shortcutPref.getValue()));
    	}else{
    		shortcutPref.setSummary(R.string.preferences_quick_launch_shortcut_summary_not_set);
    	}
    }
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	try{
	        //Log.v(TAG, "onCreate(..) called.");
    		SessionUtil.onSessionStart(this);

    		try{
    			getPackageManager().getApplicationInfo("com.flingtap.done.base", 0);
    			int match = getPackageManager().checkSignatures(getPackageName(), "com.flingtap.done.base");
    			if( match != PackageManager.SIGNATURE_MATCH ){
    				showDialog(DIALOG_FLINGTAP_DONE_NOT_INSTALLED_ID);
    				return;
    			}
    		}catch(NameNotFoundException nnfe){
				showDialog(DIALOG_FLINGTAP_DONE_NOT_INSTALLED_ID);
				return;
    		}
			
	        // Explicitly set the preferences file name.
	        PreferenceManager prefMan = getPreferenceManager(); 
	        prefMan.setSharedPreferencesName(ApplicationPreference.NAME);
	        
	        // Load the preferences from an XML resource
	        addPreferencesFromResource(R.xml.application_preferences);
	        
	        settings = getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE);
			
	        mPrevShortcut = settings.getString(ApplicationPreference.QUICK_LAUNCH_SHORTCUT, null);
	        
			Event.onEvent(Event.VIEW_APPLICATION_PREFERENCES); // Map<String, String> parameters = new HashMap<String, String>();
			
	        quickLaunchEnabledPref = (CheckBoxPreference) findPreference(ApplicationPreference.ENABLE_QUICK_LAUNCH);
	        quickLaunchEnabledPref.setOnPreferenceChangeListener(this);
	        
	        shortcutPref = new MyListPreference(this, this);
	        ((PreferenceGroup)(getPreferenceScreen().findPreference("the_cagetory"))).addPreference(shortcutPref);
	        shortcutPref.setKey("quick_launch_shortcut");
	        shortcutPref.setDependency("enable_quick_launch");
	        shortcutPref.setTitle(R.string.preferences_quick_launch_shortcut_title);
	        shortcutPref.setSummary(R.string.preferences_quick_launch_shortcut_summary_not_set);
	        shortcutPref.setEntries(R.array.preferences_quick_launch_shortcut_entries);
	        shortcutPref.setEntryValues(R.array.preferences_quick_launch_shortcut_entry_values);
	        shortcutPref.setDialogTitle(R.string.preferences_quick_launch_shortcut_dialog_title);
	        shortcutPref.setOnPreferenceChangeListener(this);
	        shortcutPref.setValue(settings.getString(ApplicationPreference.QUICK_LAUNCH_SHORTCUT, null));
	        
	        updateShortcutSummary();
		        
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000HS", exp);
			ErrorUtil.handleExceptionFinish("ERR000HS", exp, this);
        }finally{
        	try{
        		if( !isFinishing() ){
        			if( EulaPrompt.eulaAcceptanceRequired(this)){
        				new EulaPrompt().promptWithEula(this);
        			}
        		}
            }catch(HandledException h){ // Ignore.
            }catch(Exception exp){ // TODO: !!! Add FlurryAgent.closeSession(..) calls in exception handling (or maybe add finally block and check isFinishing() ) so that Session is stopped when needed.
            	Log.e(TAG, "ERR000IA", exp);
            	ErrorUtil.handleExceptionFinish("ERR000IA", exp, this);
            }
        }
    }
	
	@Override
	protected void onResume() {
		super.onResume();
		SessionUtil.onSessionStart(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		SessionUtil.onSessionStop(this);
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		try{
			MenuItem helpMenuItem = menu.findItem(HELP_ID);
			if( null == helpMenuItem ){
				helpMenuItem = menu.add(0, HELP_ID, 5, R.string.help_menu);
				helpMenuItem.setAlphabeticShortcut('h');
				helpMenuItem.setIcon(android.R.drawable.ic_menu_help);
				Intent helpIntent = StaticDisplayActivity.createIntent(this, R.layout.help_quick_launch, R.string.help_quick_launch_title);
				assert null != helpIntent;
				helpMenuItem.setIntent(helpIntent);
			}
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000HT", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000HT", exp, ApplicationPreferenceActivity.this);										
		}

		
		return true;
	}
	
	private String lookupName(){
		String name = Bookmarks.lookupName(this, mNewShortcut);
		if( null == name ){
			name = "?";
		}
		return name;
	}

	private static final int DIALOG_DISABLE_QUICK_LAUNCH_ID 			= 50;
	private static final int DIALOG_SHORTCUT_ALREADY_IN_USE_ID 			= 51;
	private static final int DIALOG_FLINGTAP_DONE_NOT_INSTALLED_ID 		= 52;
	private static final int DIALOG_BOOKMARKS_NOT_SUPPORTED_INSTALLED_ID =53;
	boolean isEnableQuickLaunchDialog = false;

	public void onPrepareDialog(int dialogId, Dialog dialog){
		try{
			switch(dialogId){
				case DIALOG_SHORTCUT_ALREADY_IN_USE_ID:
					String name = lookupName();
					((AlertDialog)dialog).setMessage(TextUtils.expandTemplate(getText(R.string.preferences_replace_shortcut_dialog_message), mNewShortcut.toString(), name));
					break;
			}
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000HU", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000HU", exp, ApplicationPreferenceActivity.this);										
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = super.onCreateDialog(id);
		try{
			if(null != dialog){
				return dialog;
			}
			switch(id){
				case DIALOG_DISABLE_QUICK_LAUNCH_ID:
					dialog = new AlertDialog.Builder(ApplicationPreferenceActivity.this)
					.setTitle(R.string.preferences_quick_launch_disable_dialog_title)
					.setMessage(R.string.preferences_quick_launch_disable_dialog_message)
					.setNegativeButton(R.string.preferences_quick_launch_disable_dialog_button_cancel, new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface arg0, int arg1) {
							try{
								Toast.makeText(ApplicationPreferenceActivity.this, R.string.preferences_quick_launch_disable_negative_toast, Toast.LENGTH_SHORT).show();
							}catch(HandledException h){ // Ignore.
							}catch(Exception exp){
								Log.e(TAG, "ERR000HW", exp);
								ErrorUtil.handleExceptionNotifyUser("ERR000HW", exp, ApplicationPreferenceActivity.this);										
							}  		
						}
					})
					.setPositiveButton(R.string.preferences_quick_launch_disable_dialog_button_ok, new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface arg0, int arg1) {
							try{
								actuallyDisableQuickLaunch();
								Toast.makeText(ApplicationPreferenceActivity.this, R.string.preferences_quick_launch_disable_positive_toast, Toast.LENGTH_SHORT).show();

							}catch(HandledException h){ // Ignore.
							}catch(Exception exp){
								Log.e(TAG, "ERR000HX", exp);
								ErrorUtil.handleExceptionNotifyUser("ERR000HX", exp, ApplicationPreferenceActivity.this);										
							}   		
						}


					})
					.setOnCancelListener(new DialogInterface.OnCancelListener(){
						public void onCancel(DialogInterface arg0) {
							try{
								Toast.makeText(ApplicationPreferenceActivity.this, R.string.preferences_quick_launch_disable_negative_toast, Toast.LENGTH_SHORT).show();
							}catch(HandledException h){ // Ignore.
							}catch(Exception exp){
								Log.e(TAG, "ERR000HY", exp);
								ErrorUtil.handleExceptionNotifyUser("ERR000HY", exp, ApplicationPreferenceActivity.this);										
							}  		
						}
					})
					.create();

					break;
					
				case DIALOG_SHORTCUT_ALREADY_IN_USE_ID:
					dialog = new AlertDialog.Builder(ApplicationPreferenceActivity.this)
					.setTitle(R.string.preferences_replace_shortcut_dialog_title)
					.setMessage(TextUtils.expandTemplate(getText(R.string.preferences_replace_shortcut_dialog_message), mNewShortcut==null?"?":mNewShortcut.toString(), lookupName()))
					.setNegativeButton(R.string.preferences_replace_shortcut_dialog_button_cancel, new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface arg0, int arg1) {
							try{
								Toast.makeText(ApplicationPreferenceActivity.this, R.string.preferences_replace_shortcut_negative_toast, Toast.LENGTH_SHORT).show();
								if( isEnableQuickLaunchDialog ){
									shortcutPref.setValue(null);
									settings.edit().remove(ApplicationPreference.QUICK_LAUNCH_SHORTCUT).commit();
									
									shortcutPref.showDialog(null);
								}
								removeDialog(DIALOG_SHORTCUT_ALREADY_IN_USE_ID); // Required because after dismissing this dialog, then doing a config change the activity attempts to (erroneousely) restore this dialog.

							}catch(HandledException h){ // Ignore.
							}catch(Exception exp){
								Log.e(TAG, "ERR000HZ", exp);
								ErrorUtil.handleExceptionNotifyUser("ERR000HZ", exp, ApplicationPreferenceActivity.this);										
							}  		
						}
					})
					.setPositiveButton(R.string.preferences_replace_shortcut_dialog_button_ok, new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface arg0, int arg1) {
							try{
								assert null != mNewShortcut;

                                replaceShortcut(mNewShortcut);
								mNewShortcut = null;

								isEnableQuickLaunchDialog =  false;

								Toast.makeText(ApplicationPreferenceActivity.this, R.string.preferences_replace_shortcut_positive_toast, Toast.LENGTH_SHORT).show();
								
							}catch(HandledException h){ // Ignore.
							}catch(Exception exp){
								Log.e(TAG, "ERR000I0", exp);
								ErrorUtil.handleExceptionNotifyUser("ERR000I0", exp, ApplicationPreferenceActivity.this);										
							}   		
						}
					})
					.setOnCancelListener(new DialogInterface.OnCancelListener(){
						public void onCancel(DialogInterface arg0) {
							try{
								Toast.makeText(ApplicationPreferenceActivity.this, R.string.preferences_replace_shortcut_negative_toast, Toast.LENGTH_SHORT).show();
								if( isEnableQuickLaunchDialog ){
									shortcutPref.setValue(null);
									settings.edit().remove(ApplicationPreference.QUICK_LAUNCH_SHORTCUT).commit();

									shortcutPref.showDialog(null);
								}
								removeDialog(DIALOG_SHORTCUT_ALREADY_IN_USE_ID); // Required because after dismissing this dialog, then doing a config change the activity attempts to (erroneousely) restore this dialog.
								// TODO: !!!! Report this issue to the Android db.
								
							}catch(HandledException h){ // Ignore.
							}catch(Exception exp){
								Log.e(TAG, "ERR000I1", exp);
								ErrorUtil.handleExceptionNotifyUser("ERR000I1", exp, ApplicationPreferenceActivity.this);										
							}  		
						}
					})
					.create();
					break;
				case DIALOG_FLINGTAP_DONE_NOT_INSTALLED_ID:
					dialog = new AlertDialog.Builder(ApplicationPreferenceActivity.this)
					.setTitle(R.string.preferences_flingtapdone_not_installed_dialog_title)
					.setMessage(R.string.preferences_flingtapdone_not_installed_dialog_message)
					.setPositiveButton(R.string.preferences_flingtapdone_not_installed_dialog_button_ok, new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface arg0, int arg1) {
							try{
								setResult(RESULT_CANCELED);
								finish();
							}catch(HandledException h){ // Ignore.
							}catch(Exception exp){
								Log.e(TAG, "ERR000ID", exp);
								ErrorUtil.handleExceptionNotifyUser("ERR000ID", exp, ApplicationPreferenceActivity.this);										
							}   		
						}
					})
					.setOnCancelListener(new DialogInterface.OnCancelListener(){
						public void onCancel(DialogInterface arg0) {
							try{
								setResult(RESULT_CANCELED);
								finish();
							}catch(HandledException h){ // Ignore.
							}catch(Exception exp){
								Log.e(TAG, "ERR000IE", exp);
								ErrorUtil.handleExceptionNotifyUser("ERR000IE", exp, ApplicationPreferenceActivity.this);										
							}  		
						}
					})
					.create();
					break;
				case DIALOG_BOOKMARKS_NOT_SUPPORTED_INSTALLED_ID:
					dialog = new AlertDialog.Builder(ApplicationPreferenceActivity.this)
					.setTitle(R.string.preferences_bookmarks_not_supported_dialog_title)
					.setMessage(R.string.preferences_bookmarks_not_supported_dialog_message)
					.setPositiveButton(R.string.preferences_flingtapdone_not_installed_dialog_button_ok, new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface arg0, int arg1) {
							try{
								dismissDialog(DIALOG_BOOKMARKS_NOT_SUPPORTED_INSTALLED_ID);
								//arg0.cancel();
							}catch(HandledException h){ // Ignore.
							}catch(Exception exp){
								Log.e(TAG, "ERR000ID", exp);
								ErrorUtil.handleExceptionNotifyUser("ERR000ID", exp, ApplicationPreferenceActivity.this);										
							}   		
						}
					})
					.create();
					break;
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000HV", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000HV", exp, ApplicationPreferenceActivity.this);										
		}
		return dialog;
	}

	private String mPrevShortcut = null;
	private Character mNewShortcut = null;
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		try{
			if( preference == quickLaunchEnabledPref ){
				return changeQuickLaunchEnabledPreference((Boolean)newValue);
			}else if( preference == shortcutPref ){
				

				// Is the new selection the same as the current selection?
				if( null != shortcutPref.getValue() && (((String)newValue).charAt(0) == (shortcutPref.getValue()).charAt(0) )){
					return true;
				}
				
				// Now add the new shortcut.
				if( addShortcut(((String)newValue).charAt(0)) ){
					mNewShortcut = null;
					
					isEnableQuickLaunchDialog =  false;
					((CheckBoxPreference)quickLaunchEnabledPref).setChecked(true);
					return true;
				}
				return false;
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000I2", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000I2", exp, ApplicationPreferenceActivity.this);										
		}  		
		return false;
	}

	public boolean changeQuickLaunchEnabledPreference(boolean newValue) {
		// If attempting to disable labels ...
		if( !newValue ){
			showDialog(DIALOG_DISABLE_QUICK_LAUNCH_ID);
			return false;
		}else{
			return enableQuickLaunch(this);
		}
	}

	public boolean enableQuickLaunch(PreferenceActivity context) {
		if( !Bookmarks.providerExists(getContentResolver()) ){
			showDialog(DIALOG_BOOKMARKS_NOT_SUPPORTED_INSTALLED_ID);
			return false;
		}
		
		shortcutPref.setValue(null);
		settings.edit().remove(ApplicationPreference.QUICK_LAUNCH_SHORTCUT).commit();
		mPrevShortcut = null;
		
		isEnableQuickLaunchDialog = true;
		boolean result = addShortcut(ApplicationPreference.QUICK_LAUNCH_SHORTCUT_DEFAULT.charAt(0)); 

		if( result ){
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(Event.TOGGLE_QUICK_LAUNCH_ENABLED__ENABLED, Event.TOGGLE_QUICK_LAUNCH_ENABLED__ENABLED__TRUE);
			Event.onEvent(Event.TOGGLE_QUICK_LAUNCH_ENABLED, parameters); 		
		}
		return result;
	}
	
	private void actuallyDisableQuickLaunch() {
		Map<String, String> parameters = new HashMap<String, String>();	
		parameters.put(Event.TOGGLE_QUICK_LAUNCH_ENABLED__ENABLED, Event.TOGGLE_QUICK_LAUNCH_ENABLED__ENABLED__FALSE);
		Event.onEvent(Event.TOGGLE_QUICK_LAUNCH_ENABLED, parameters); 		
		
		String currentShortcut = settings.getString(ApplicationPreference.QUICK_LAUNCH_SHORTCUT, null);
		if( null != currentShortcut && currentShortcut.length() == 1 ){
			Bookmarks.remove(this.getContentResolver(), currentShortcut.charAt(0));
		}
			
		mPrevShortcut = null;
		mNewShortcut = null;
		shortcutPref.setValue(null);
		settings.edit().remove(ApplicationPreference.QUICK_LAUNCH_SHORTCUT).commit();
		
		quickLaunchEnabledPref.setChecked(false);		
		
		updateShortcutSummary();
	}
	
	private boolean addShortcut(char key) {
		mNewShortcut = key;
		if( Bookmarks.exists(this.getContentResolver(), key)){
			showDialog(DIALOG_SHORTCUT_ALREADY_IN_USE_ID);
			return false;
		}
		
		if( actuallyAddShortcut(key) ){
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(Event.SET_SHORTCUT__REPLACE, Event.SET_SHORTCUT__REPLACE__FALSE);
			parameters.put(Event.SET_SHORTCUT__KEY, String.valueOf(key));
			Event.onEvent(Event.SET_SHORTCUT, parameters); 		
			
			if( null != mPrevShortcut ){
				Bookmarks.remove(getContentResolver(), mPrevShortcut.charAt(0));
			}
			mPrevShortcut = settings.getString(ApplicationPreference.QUICK_LAUNCH_SHORTCUT, null);

			return true;
		}
		
		return false;
	}

	private boolean actuallyAddShortcut(char key) {
		Intent quickLaunchIntent = new Intent(Intent.ACTION_MAIN);
		quickLaunchIntent.setComponent(new ComponentName("com.flingtap.done.base", "com.flingtap.done.TaskList"));
		
		// Update the bookmark for a shortcut
		Uri uri = Bookmarks.add(
				this.getContentResolver(), 
				quickLaunchIntent, 
				getString(R.string.quick_launch_name), 
				null, // TODO: Make this configurable? 
				key,   
				0);
		
		if( null != uri ){

			shortcutPref.setValue(String.valueOf(key));

			updateShortcutSummary();

			return true;
		}
		
		return false;
	}
	
	private boolean replaceShortcut(char key){
		
		if( actuallyAddShortcut(key) ){
			
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(Event.SET_SHORTCUT__REPLACE, Event.SET_SHORTCUT__REPLACE__TRUE);
			parameters.put(Event.SET_SHORTCUT__KEY, String.valueOf(key));
			Event.onEvent(Event.SET_SHORTCUT, parameters); 		
			
			((CheckBoxPreference)quickLaunchEnabledPref).setChecked(true);
			
			if( null != mPrevShortcut ){
				Bookmarks.remove(getContentResolver(), mPrevShortcut.charAt(0));
			}
			mPrevShortcut = settings.getString(ApplicationPreference.QUICK_LAUNCH_SHORTCUT, null);

			return true;
		}
		return false;

	}

	protected static final String I_S_NEW_SHORTCUT_KEY = "ApplicationPreferenceActivity.I_S_NEW_SHORTCUT_KEY";
	protected static final String I_S_PREV_SHORTCUT_KEY = "ApplicationPreferenceActivity.I_S_PREV_SHORTCUT_KEY";

	
    @Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		try{
			//Log.v(TAG, "onSaveInstanceState(..) called.");	
			
			outState.putString(I_S_NEW_SHORTCUT_KEY, mNewShortcut==null?null:mNewShortcut.toString());
			outState.putString(I_S_PREV_SHORTCUT_KEY, mPrevShortcut);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000I3", exp);
			ErrorUtil.handleExceptionFinish("ERR000I3", exp, this);
		}
	}

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
    	super.onRestoreInstanceState(savedInstanceState);
    	try{
    		String newValue = savedInstanceState.getString(  I_S_NEW_SHORTCUT_KEY);
    		if( null != newValue ){
    			mNewShortcut = newValue.charAt(0);
    		}
    		mPrevShortcut = savedInstanceState.getString(I_S_PREV_SHORTCUT_KEY);
    		
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000I4", exp);
			ErrorUtil.handleExceptionFinish("ERR000I4", exp, this);
		}
    }

	public void onClick(DialogInterface dialog, int which) {
		// TODO Auto-generated method stub
	}
}

interface OnShowDialogListener {
	public void onShowDialog(ListPreference listPreference);
}
