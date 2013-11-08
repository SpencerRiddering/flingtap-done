// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.util.ArrayList;

/**
 * A list of steps taken to accomplish a goal.
 * 
 * Zero '0' is the first step.
 * 
 * TODO: Override the back button to move back to the previous step. 
 * TODO: !! Consider changing the implementation of Wizard to be an ActivityGroup.
 */
public class Wizard {

	public static final int FIRST_STEP = 0;

	/**
	 * onArrive() -->  |  --> onDepart()
	 *                step
	 * onBack()   <--  |  <-- onReturn()
	 */
	public static interface WizardStep {
		public void setWizard(Wizard wizard);
		public void onArrive(); // User arrives at   the next     step.
		public void onReturn(); // User returns back to  previous step.
		public void onDepart(); // User goes on   to the next     step.
		public void onBack();   // User goes back to the previous step. 
	};

	protected ArrayList<WizardStep> steps = new ArrayList<WizardStep>();
	protected int currentStepIndex = -1;

	public boolean hasPreviousStep(){
		return currentStepIndex > 0; // Assumes can never be so great that previous step is no longer available. 
	}
	public boolean hasNextStep(){
		return (1 + currentStepIndex) < steps.size(); // Assumes can never be so great that previous step is no longer available. 
	}

	
	public boolean addStep(WizardStep step){
		step.setWizard(this);
		return steps.add(step);
	}

	public int getCurrentStep(){
		return currentStepIndex;
	}
	
	public void moveToStep(int step){
		if( step == currentStepIndex ){
			return;
		}
		int prevCurrentStepIndex = currentStepIndex;
		currentStepIndex = step;
		if( currentStepIndex >= steps.size() || currentStepIndex < 0){
			onMoveError(currentStepIndex);
			return;
		}
		if( prevCurrentStepIndex < currentStepIndex ){
			// Need to depart previous step.
			if( prevCurrentStepIndex >= 0 ){
				steps.get(prevCurrentStepIndex).onDepart();
			}
			steps.get(currentStepIndex).onArrive();
		}else{
			steps.get(prevCurrentStepIndex).onBack();
			steps.get(currentStepIndex).onReturn();
		}
	}
	
	public void moveToFirst(){
		moveToStep(FIRST_STEP);
	}
	
	public void moveNext(){
		steps.get(currentStepIndex).onDepart();
		currentStepIndex++;
		if( currentStepIndex >= steps.size() ){
			onMoveError(currentStepIndex);
			return;
		}
		steps.get(currentStepIndex).onArrive();
	}
	
	public void movePrev(){
		steps.get(currentStepIndex).onBack();
		currentStepIndex--;
		if( currentStepIndex < 0 ){
			onMoveError(currentStepIndex);
			return;
		}
		steps.get(currentStepIndex).onReturn();
	}

	public void cancel(){
		onCancel();
	}
	
	/**
	 * 	Override if you want to handle a cancel operation.
	 * 	@Override
	 * 
	 */
	protected void onCancel(){
	}
	
	public void exit(){
		WizardStep step = steps.get(currentStepIndex);
		if( null != step ){
			step.onDepart();
		}
	}
	
	/**
	 * 	Override if you want to handle move errors.
	 * 	@Override
	 * 
	 */
	protected void onMoveError(int moveStep){
	}
	
	public Wizard() {
		super();
	}

}
