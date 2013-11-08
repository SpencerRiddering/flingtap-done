// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.flingtap.common.HandledException;
import com.flingtap.done.Wizard.WizardStep;
import com.flingtap.done.provider.Task;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;
import android.widget.ViewSwitcher;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation;

import com.flingtap.done.base.R;

/**
 * 
 * @author spencer
 *
 */
public class FeatureIntroduction extends Activity implements View.OnClickListener {
	private static final String TAG = "FeatureIntroduction";
	
	private static final int REQUEST_TOGGLE = 0;
	
	public static final String EXTRA_SHOW_ENABLE_OPTIONS = "com.flingtap.done.intent.extra.SHOW_ENABLE_OPTIONS"; // boolean. 
	
	private static final String OUT_STATE_WIZARD_STEP = "WIZARD_STEP";
	
	private Wizard wizard = null;
	private boolean mShowEnableOptions = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try{
			makeFeatureIntroductionNotNeeded(this);
			
			// Remove Title Bar.
			requestWindowFeature(Window.FEATURE_NO_TITLE); 
			
			// Remove Status Bar
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
			
			Intent intent = getIntent();
			mShowEnableOptions = intent.getBooleanExtra(EXTRA_SHOW_ENABLE_OPTIONS, false);
			
			// Set content view.
			// TODO: !!! This layout does not squish correctly when more text is specified than can fit in the available space. The problem is the image view is cut off instead of retaining it's original size. It's like the image view mis-calculates the size of the image.
			setContentView(R.layout.feature_introduction);
			
			final ViewAnimator va = (ViewAnimator) findViewById(R.id.view_animator);
			
			wizard = new Wizard(){
				protected void onCancel() {
					//Log.v(TAG, "wizard.cancel() called.");
					showDialog(DIALOG_EXIT_CONFIRM_ID);
				}
				protected void onMoveError(int moveStep) {
					Log.e(TAG, "ERR000G8 A Wizard move error occured. moveStep == " + moveStep);
					ErrorUtil.handleExceptionFinish("ERR000G8", (Exception)(new Exception( String.valueOf(moveStep) )).fillInStackTrace(), FeatureIntroduction.this);
					return;
				}
			};
			SharedPreferences settings = getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE);
			
			int index = -1;
			
			// Introduction
			final int firstIndex = ++index;// Need to increment the counter even though the index is hardcoded just below.
			wizard.addStep(new AbstractWizardStep(){
				public void onArrive() {
					init();
				}
				
				public void onReturn() {
					va.setInAnimation(FeatureIntroduction.this, R.anim.slide_right_in);
					va.setOutAnimation(FeatureIntroduction.this, R.anim.slide_right_out);
					va.setDisplayedChild(firstIndex);
					init();
				}
				
				@Override
				public void onDepart() {
					((Button)findViewById(R.id.prev_button)).setVisibility(View.VISIBLE);
				}
				
				public void init(){ 
					Button prevButton = ((Button)findViewById(R.id.prev_button)); 
					prevButton.setVisibility(View.GONE);
					prevButton.invalidate();
				}
			});   	
			
			wizard.addStep(new FeatureStep(null, va, null, false, null, ++index));   	
			wizard.addStep(new FeatureStep(null, va, null, false, null, ++index));   	
			wizard.addStep(new FeatureStep(null, va, null, false, null, ++index));   	
			wizard.addStep(new FeatureStep(null, va, null, false, null, ++index));   	
			wizard.addStep(new FeatureStep(null, va, null, false, null, ++index));   	
			wizard.addStep(new FeatureStep(null, va, null, false, null, ++index));
			
			// Conclusion
			final int lastIndex = ++index; // Need to increment the counter even though the index is hardcoded just below.
			wizard.addStep(new AbstractWizardStep(){
				public void onArrive() {
					((Button)findViewById(R.id.next_button)).setVisibility(View.GONE);
					((Button)findViewById(R.id.close_button)).setVisibility(View.VISIBLE);
					((Button)findViewById(R.id.cancel_button)).setVisibility(View.GONE);
					
					va.setInAnimation(FeatureIntroduction.this, R.anim.slide_left_in);
					va.setOutAnimation(FeatureIntroduction.this, R.anim.slide_left_out);
					va.setDisplayedChild(lastIndex);
				}
				
				@Override
				public void onBack() {
					((Button)findViewById(R.id.next_button)).setVisibility(View.VISIBLE);
					((Button)findViewById(R.id.close_button)).setVisibility(View.GONE);
					((Button)findViewById(R.id.cancel_button)).setVisibility(View.VISIBLE);
					
				}
				
			});   	
			
			// Last Step.
			wizard.addStep(new AbstractWizardStep(){
				public void onArrive() {
					finish();
				}
			});
			
			((Button)findViewById(R.id.next_button)).setOnClickListener(this);
			((Button)findViewById(R.id.prev_button)).setOnClickListener(this);
			((Button)findViewById(R.id.cancel_button)).setOnClickListener(this);
			((Button)findViewById(R.id.close_button)).setOnClickListener(this);
			
			if( null != savedInstanceState ){
				// Restore state.
				wizard.moveToStep(savedInstanceState.getInt(OUT_STATE_WIZARD_STEP, Wizard.FIRST_STEP)); // Start the wizard process.
			}else{
				wizard.moveToFirst();
			}
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000GC", exp);
			ErrorUtil.handleExceptionNotifyUserFinish("ERR000GC", exp, this);
		}
	}

	private class FeatureStep implements WizardStep, CompoundButton.OnCheckedChangeListener{

		private ViewAnimator mViewAnimator = null;
		private String mPreferenceName = null;
		private SharedPreferences mSettings = null;
		private boolean mPreferenceDefault = false;
		private String mTogglePreferenceAction = null;
		private int mIndex = 0;
		
		public FeatureStep(SharedPreferences settings, ViewAnimator viewAnimator, String preferenceName, boolean preferenceDefault, String togglePreferenceAction, int index){
			mViewAnimator = viewAnimator;
			mIndex = index;
			
			if( null != settings ){
				mSettings = settings;
				mPreferenceName = preferenceName;
				mPreferenceDefault = preferenceDefault;
				mTogglePreferenceAction = togglePreferenceAction;
			}
			
		}
		
		public void onArrive() {
	    	mViewAnimator.setInAnimation(FeatureIntroduction.this, R.anim.slide_left_in);
	    	mViewAnimator.setOutAnimation(FeatureIntroduction.this, R.anim.slide_left_out);
			mViewAnimator.setDisplayedChild(mIndex);
			initView();
		}

		private void initView() {
			View preferenceLayout = mViewAnimator.getCurrentView().findViewById(R.id.preference_layout);
			if( null != mSettings ){
				if( mShowEnableOptions ){
					preferenceLayout.setVisibility(View.VISIBLE);
	 				updateCheckBox();
				}else{
					preferenceLayout.setVisibility(View.GONE);
				}
			} else {
				if( null != preferenceLayout ){
					preferenceLayout.setVisibility(View.GONE);
				}
			}
		}

		private void cleanView() {
			if( null != mSettings ){
			}
		}

		public void onBack() {
			cleanView();
		}

		public void onDepart() {
			cleanView();
		}

		public void onReturn() {
	    	mViewAnimator.setInAnimation(FeatureIntroduction.this, R.anim.slide_right_in);
	    	mViewAnimator.setOutAnimation(FeatureIntroduction.this, R.anim.slide_right_out);
			mViewAnimator.setDisplayedChild(mIndex);
			initView();
		}

		public void setWizard(Wizard wizard) {
		}
		
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			Intent togglePreferenceIntent = new Intent(mTogglePreferenceAction);
			togglePreferenceIntent.putExtra(ApplicationPreferenceActivity.EXTRA_TOGGLE_VALUE, isChecked);
			startActivityForResult(togglePreferenceIntent, REQUEST_TOGGLE);
		}
		
		public void updateCheckBox() {
			CheckBox checkBox = (CheckBox) mViewAnimator.getCurrentView().findViewById(R.id.preference_checkbox);
			checkBox.setOnCheckedChangeListener(null);
			checkBox.setChecked(mSettings.getBoolean(mPreferenceName, mPreferenceDefault));
			checkBox.setOnCheckedChangeListener(this);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		try{
			switch(requestCode){
				case REQUEST_TOGGLE:
					if( RESULT_OK == resultCode ){
						((FeatureStep)wizard.steps.get(wizard.getCurrentStep())).updateCheckBox();
					}
					break;
			}
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000GD", exp);
			ErrorUtil.handleExceptionNotifyUserFinish("ERR000GD", exp, this);
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		try{
			outState.putInt(OUT_STATE_WIZARD_STEP, wizard.getCurrentStep());
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000GA", exp);
			ErrorUtil.handleExceptionNotifyUserFinish("ERR000GA", exp, this);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		try{
			if( isFinishing() ){
				wizard.exit();
			}
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000GE", exp);
			ErrorUtil.handleExceptionNotifyUserFinish("ERR000GE", exp, this);
		}
	}

	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		try{
			if( keyCode == KeyEvent.KEYCODE_BACK ) { // TODO: !! Move this code into Wizard.checkAndHandleBackButton(..)
				if( wizard.getCurrentStep() > Wizard.FIRST_STEP ){
					wizard.movePrev();
					return true;
				}else if( wizard.getCurrentStep() == Wizard.FIRST_STEP ){
					showDialog(DIALOG_EXIT_CONFIRM_ID);
					return true;
				}
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000GB", exp);
			ErrorUtil.handleExceptionNotifyUserFinish("ERR000GB", exp, this);
		}		
		return super.onKeyDown(keyCode, event);
	}

	public void onClick(View v) {
		try{
			switch(v.getId()){
				case R.id.next_button:
				case R.id.close_button:
					wizard.moveNext();
					break;
				case R.id.prev_button:
					wizard.movePrev();
					break;
				case R.id.cancel_button:
					wizard.cancel();
					break;
			}
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000GF", exp);
			ErrorUtil.handleExceptionNotifyUserFinish("ERR000GF", exp, this);
		}
	}
	
	
	/**
	 * Checks to see if the user should be prompted to accept a EULA.
	 * 
	 * @param context
	 * @return
	 */
	public static final boolean intro_introductionNeeded(Context context){
		assert null != context;

        // Retrieve shared preferences object.
		SharedPreferences settings = context.getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE); 
		assert null != settings;
		
		boolean needed = settings.getBoolean(ApplicationPreference.FEATURE_INTRODUCTION_NEEDED, ApplicationPreference.FEATURE_INTRODUCTION_NEEDED_DEFAULT);
		return needed;
	}

	/**
	 * Checks to see if the user should be prompted to accept a EULA.
	 * 
	 * @param context
	 * @return
	 */
	public static final void makeFeatureIntroductionNotNeeded(Context context){
		assert null != context;

        // Retrieve shared preferences object.
		SharedPreferences settings = context.getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE); 
		assert null != settings;
		
        SharedPreferences.Editor ed = settings.edit();
        ed.putBoolean(ApplicationPreference.FEATURE_INTRODUCTION_NEEDED, false);
        ed.commit();
	}

	private static final int DIALOG_EXIT_CONFIRM_ID = 51;

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = super.onCreateDialog(id);
		try{
			if(null != dialog){
				return dialog;
			}
			switch(id){
				case DIALOG_EXIT_CONFIRM_ID:
					dialog = new AlertDialog.Builder(this)
					.setTitle(R.string.dialog_areYouSure)
					.setMessage(R.string.leaveFeatureIntroduction)
					.setNegativeButton(R.string.button_no, null)
					.setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface arg0, int arg1) {
							setResult(RESULT_CANCELED);
							finish();
						}
					})
					.create();

					break;

			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000HK", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000HK", exp, this);										
		}
		return dialog;
	}

	public static void launch(Activity activity, boolean showPref){
		Intent intent = new Intent();
		ComponentName cn = new ComponentName(activity.getPackageName(), FeatureIntroduction.class.getName());
		intent.setComponent(cn);
		intent.putExtra(EXTRA_SHOW_ENABLE_OPTIONS, showPref);
		activity.startActivity(intent);
	}
}
