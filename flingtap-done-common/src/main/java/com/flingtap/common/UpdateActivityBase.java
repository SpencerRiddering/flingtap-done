// Licensed under the Apache License, Version 2.0

package com.flingtap.common;

import android.R.style;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tomgibara.android.veecheck.VeecheckActivity;
import com.tomgibara.android.veecheck.VeecheckState;
import com.tomgibara.android.veecheck.util.PrefState;

public abstract class UpdateActivityBase extends VeecheckActivity {
	private static final String TAG = "UpdateActivityBase";

	protected abstract String getSessionApiKey();
	protected abstract int getIconResId(); // ic_launcher_tasks
	protected abstract int getTitleTextResId(); // activity_flingtapDone
	protected abstract int getConfirmTextResId(); // wouldYouLikeToUpdateThisApplicationToTheLatestVersion
	protected abstract int getYesButtonResId(); // yes
	protected abstract int getNoButtonResId(); // no
	protected abstract int getConfirmStopResId(); // dontNotifyMeOfThisUpdateAgain
	
	public static final String EXTRA_UPDATE_DEADLINE = "com.flingtap.common.intent.extra.UPDATE_DEADLINE"; // long, 

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SessionUtilBase.onSessionStart(this, getSessionApiKey());

		try{
//			setContentView(R.layout.update);
//			setContentView(createContent());

			//		<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
			//		    android:layout_width="fill_parent"
			//		    android:layout_height="fill_parent"
			//		    android:orientation="vertical"
			//			>
					LinearLayout outerLayout = new LinearLayout(this);
					outerLayout.setOrientation(LinearLayout.VERTICAL);
					LinearLayout.LayoutParams outerParams = new LinearLayout.LayoutParams( LayoutParams.FILL_PARENT,  LayoutParams.FILL_PARENT);
					outerLayout.setLayoutParams(outerParams);
					
			//			<LinearLayout
			//				android:layout_width="fill_parent"
			//				android:layout_height="wrap_content"
			//				android:layout_marginTop="10px"
			//				android:gravity="center_vertical"
			//				>
					LinearLayout headerLayout = new LinearLayout(this);
					LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams( LayoutParams.FILL_PARENT,  LayoutParams.WRAP_CONTENT);
					headerParams.gravity = Gravity.CENTER_VERTICAL;
					headerParams.topMargin = 10;
					headerLayout.setLayoutParams(headerParams);
					
					outerLayout.addView(headerLayout);
					
			//				<ImageView
			//					android:layout_width="wrap_content"
			//					android:layout_height="wrap_content"
			//					android:src="@drawable/ic_launcher_tasks"
			//				/>
					ImageView icon = new ImageView(this);
					icon.setImageResource(getIconResId());
					LayoutParams iconParams = new LayoutParams(LayoutParams.WRAP_CONTENT,  LayoutParams.WRAP_CONTENT);
					icon.setLayoutParams(iconParams);
					
					headerLayout.addView(icon);
					
			//				<TextView
			//					android:layout_width="fill_parent"
			//					android:layout_height="wrap_content"
			//					android:text="@string/activity_flingtapDone"
			//					style="@android:style/TextAppearance.Large"
			//				/>
					TextView titleText = new TextView(this);
					titleText.setText(getTitleTextResId());
					titleText.setTextAppearance(this, style.TextAppearance_Large);
					LayoutParams titleTextParams = new LayoutParams(LayoutParams.FILL_PARENT,  LayoutParams.WRAP_CONTENT);
					titleText.setLayoutParams(titleTextParams);
					
					headerLayout.addView(titleText);
					
			//			</LinearLayout>
					
					
			//			<TextView
			//				android:layout_height="0px"
			//				android:layout_width="fill_parent"
			//				android:layout_margin="10px"
			//				android:layout_weight="1"
			//				android:text="@string/wouldYouLikeToUpdateThisApplicationToTheLatestVersion"
			//				style="@android:style/TextAppearance.Medium"
			//			/>
					TextView confirmText = new TextView(this);
					confirmText.setText(getConfirmTextResId());
					confirmText.setTextAppearance(this, style.TextAppearance_Medium);
					LinearLayout.LayoutParams confirmTextParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, 0);
					confirmTextParams.weight=1;
					confirmTextParams.topMargin 	= 10;
					confirmTextParams.rightMargin 	= 10;
					confirmTextParams.bottomMargin 	= 10;
					confirmTextParams.leftMargin 	= 10;
					confirmText.setLayoutParams(confirmTextParams);
					
					outerLayout.addView(confirmText);
					
			//			<LinearLayout
			//				android:layout_width="fill_parent"
			//				android:layout_height="wrap_content"
			//				>
					LinearLayout choiceLayout = new LinearLayout(this);
					LinearLayout.LayoutParams choiceParams = new LinearLayout.LayoutParams( LayoutParams.FILL_PARENT,  LayoutParams.WRAP_CONTENT);
					choiceLayout.setLayoutParams(choiceParams);			
						
					outerLayout.addView(choiceLayout);
					
			//				<Button
			//					android:id="@+id/yes"
			//					android:layout_width="0px"
			//					android:layout_height="wrap_content"
			//					android:layout_weight="1"
			//					android:text="@android:string/yes"
			//				/>
					yesButton = new Button(this);
					yesButton.setText(getYesButtonResId()); 
					LinearLayout.LayoutParams yesButtonParams = new LinearLayout.LayoutParams( 0,  LayoutParams.WRAP_CONTENT);
					yesButtonParams.weight = 1;
					yesButton.setLayoutParams(yesButtonParams);
					
					choiceLayout.addView(yesButton);				
							
			//				<Button
			//					android:id="@+id/no"
			//					android:layout_width="0px"
			//					android:layout_height="wrap_content"
			//					android:layout_weight="1"
			//					android:text="@android:string/no"
			//				/>
					noButton = new Button(this);
					noButton.setText(getNoButtonResId()); 
					LinearLayout.LayoutParams noButtonParams = new LinearLayout.LayoutParams( 0,  LayoutParams.WRAP_CONTENT);
					noButtonParams.weight = 1;
					noButton.setLayoutParams(noButtonParams);
					
					choiceLayout.addView(noButton);				
					
			//			</LinearLayout>
					
			//			<CheckBox
			//				android:id="@+id/stop"
			//				android:layout_width="fill_parent"
			//				android:layout_height="wrap_content"
			//				android:text="@string/dontNotifyMeOfThisUpdateAgain"
			//			/>
					confirmStopCheckBox = new CheckBox(this);
					confirmStopCheckBox.setText(getConfirmStopResId());  
					confirmStopCheckBox.setTextAppearance(this, style.TextAppearance_Large);
					LayoutParams confirmStopCheckBoxParams = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
					confirmStopCheckBox.setLayoutParams(confirmStopCheckBoxParams);
					
					outerLayout.addView(confirmStopCheckBox);					
					
				setContentView(outerLayout);
			
			// Prevent user from ignoring required updates. 
	        Intent updateIntent = getUpdateIntent();
	        if(updateIntent != null && updateIntent.hasExtra(EXTRA_UPDATE_DEADLINE)){
	        	confirmStopCheckBox.setChecked(false);
	        	confirmStopCheckBox.setVisibility(View.GONE);
	        }
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000IF", exp);
			ErrorUtil.handleExceptionFinish("ERR000IF", exp, this);
		}

		
	}
	
	
	protected CheckBox confirmStopCheckBox = null;
	protected Button yesButton = null;
	protected Button noButton = null;
	
	@Override
	protected VeecheckState createState() {
		return new PrefState(this);
	}
	
	@Override
	protected View getNoButton() {
		return noButton;
	}
	
	@Override
	protected View getYesButton() {
		return yesButton;
	}

	@Override
	protected Checkable getStopCheckBox() {
		return confirmStopCheckBox;
	}
	@Override
	protected void onResume() {
		super.onResume();
		SessionUtilBase.onSessionStart(this, getSessionApiKey());
	}

	@Override
	protected void onPause() {
		super.onPause();
		SessionUtilBase.onSessionStop(this);
	}
	

}
