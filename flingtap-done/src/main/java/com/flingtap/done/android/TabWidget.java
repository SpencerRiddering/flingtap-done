package com.flingtap.done.android;
/*
 * Copyright (C) 2006 The Android Open Source Project
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

import com.flingtap.done.base.R;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnFocusChangeListener;
import android.widget.LinearLayout;



/**
 * 
 * Added the following images to satisfy this class. 
 *     tab_bottom_left.xml, tab_bottom_right.xml
 *     tab_press_bar_left.png, tab_selected_bar_left.png, tab_focus_bar_left.png
 *     tab_press_bar_right.png, tab_selected_bar_right.png, tab_focus_bar_right.png
 *     
 * 
 * Displays a list of tab labels representing each page in the parent's tab
 * collection. The container object for this widget is
 * {@link android.widget.TabHost TabHost}. When the user selects a tab, this
 * object sends a message to the parent container, TabHost, to tell it to switch
 * the displayed page. You typically won't use many methods directly on this
 * object. The container TabHost is used to add labels, add the callback
 * handler, and manage callbacks. You might call this object to iterate the list
 * of tabs, or to tweak the layout of the tab list, but most methods should be
 * called on the containing TabHost object.
 */
public class TabWidget extends LinearLayout implements OnFocusChangeListener { // android.widget.TabWidget


    private OnTabSelectionChanged mSelectionChangedListener;
    private int mSelectedTab = 0;
    private Drawable mBottomLeftStrip;
    private Drawable mBottomRightStrip;
    private boolean mStripMoved;
    protected Context mContext;

    public TabWidget(Context context) {
        this(context, null);
        mContext = context;
    }

    public TabWidget(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.tabWidgetStyle);
        mContext = context;
    }

    public TabWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        mContext = context;
        initTabWidget();

        TypedArray a =
            context.obtainStyledAttributes(attrs, R.styleable.TabWidget,
                    defStyle, 0);

        a.recycle();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mStripMoved = true;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private void initTabWidget() {
//        setOrientation(LinearLayout.HORIZONTAL); // This is handled in the xml layout now (since it is different for different orientations.
//    	setOrientation(LinearLayout.VERTICAL);
		mBottomLeftStrip = mContext.getResources().getDrawable(
				R.drawable.tab_bottom_left);
		mBottomRightStrip = mContext.getResources().getDrawable(
				R.drawable.tab_bottom_right);

        // Deal with focus, as we don't want the focus to go by default
        // to a tab other than the current tab
        setFocusable(true);
        setOnFocusChangeListener(this);
    }

    @Override
    public void childDrawableStateChanged(View child) {
        if (child == getChildAt(mSelectedTab)) {
            // To make sure that the bottom strip is redrawn
            invalidate();
        }
        super.childDrawableStateChanged(child);
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        View selectedChild = getChildAt(mSelectedTab);
        
        mBottomLeftStrip.setState(selectedChild.getDrawableState());
        mBottomRightStrip.setState(selectedChild.getDrawableState());
        
        if (mStripMoved) {
            Rect selBounds = new Rect(); // Bounds of the selected tab indicator
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ){
            	selBounds.top = selectedChild.getTop();
            	selBounds.bottom = selectedChild.getBottom();
            	final int myWidth = getWidth();
            	mBottomLeftStrip.setBounds(
            			myWidth - mBottomLeftStrip.getIntrinsicWidth(),
            			Math.min(0, selBounds.top 
            					- mBottomLeftStrip.getIntrinsicHeight()),
    					getWidth(),
    					selBounds.top);
            	mBottomRightStrip.setBounds(
            			myWidth - mBottomRightStrip.getIntrinsicWidth(),
            			selBounds.bottom,
            			myWidth,
            			Math.max(getHeight(), 
            					selBounds.bottom + mBottomRightStrip.getIntrinsicHeight()));
            }else{
	            selBounds.left = selectedChild.getLeft();
	            selBounds.right = selectedChild.getRight();
	            final int myHeight = getHeight();
	            mBottomLeftStrip.setBounds(
	                    Math.min(0, selBounds.left 
	                                 - mBottomLeftStrip.getIntrinsicWidth()),
	                    myHeight - mBottomLeftStrip.getIntrinsicHeight(),
	                    selBounds.left,
	                    myHeight);
	            mBottomRightStrip.setBounds(
	                    selBounds.right,
	                    myHeight - mBottomRightStrip.getIntrinsicHeight(),
	                    Math.max(getWidth(), 
	                            selBounds.right + mBottomRightStrip.getIntrinsicWidth()),
	                    myHeight);
            }
            
            mStripMoved = false;
        }

        mBottomLeftStrip.draw(canvas);
        mBottomRightStrip.draw(canvas);
    }

    /**
     * Sets the current tab.
     * This method is used to bring a tab to the front of the Widget,
     * and is used to post to the rest of the UI that a different tab
     * has been brought to the foreground.
     *
     * Note, this is separate from the traditional "focus" that is
     * employed from the view logic.
     *
     * For instance, if we have a list in a tabbed view, a user may be
     * navigating up and down the list, moving the UI focus (orange
     * highlighting) through the list items.  The cursor movement does
     * not effect the "selected" tab though, because what is being
     * scrolled through is all on the same tab.  The selected tab only
     * changes when we navigate between tabs (moving from the list view
     * to the next tabbed view, in this example).
     *
     * To move both the focus AND the selected tab at once, please use
     * {@link #setCurrentTab}. Normally, the view logic takes care of
     * adjusting the focus, so unless you're circumventing the UI,
     * you'll probably just focus your interest here.
     *
     *  @param index The tab that you want to indicate as the selected
     *  tab (tab brought to the front of the widget)
     *
     *  @see #focusCurrentTab
     */
    public void setCurrentTab(int index) {
        if (index < 0 || index >= getChildCount()) {
            return;
        }

        getChildAt(mSelectedTab).setSelected(false);
        mSelectedTab = index;
        getChildAt(mSelectedTab).setSelected(true);
        mStripMoved = true;
    }

    /**
     * Sets the current tab and focuses the UI on it.
     * This method makes sure that the focused tab matches the selected
     * tab, normally at {@link #setCurrentTab}.  Normally this would not
     * be an issue if we go through the UI, since the UI is responsible
     * for calling TabWidget.onFocusChanged(), but in the case where we
     * are selecting the tab programmatically, we'll need to make sure
     * focus keeps up.
     *
     *  @param index The tab that you want focused (highlighted in orange)
     *  and selected (tab brought to the front of the widget)
     *
     *  @see #setCurrentTab
     */
    public void focusCurrentTab(int index) {
        final int oldTab = mSelectedTab;

        // set the tab
        setCurrentTab(index);

        // change the focus if applicable.
        if (oldTab != index) {
            getChildAt(index).requestFocus();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        int count = getChildCount();

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            child.setEnabled(enabled);
        }
    }

    @Override
    public void addView(View child) {
        if (child.getLayoutParams() == null) {
            final LinearLayout.LayoutParams lp;
        	if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ){
	            lp = new LayoutParams(
	            		ViewGroup.LayoutParams.WRAP_CONTENT,
	                    0, 1);
        	}else{
	            lp = new LayoutParams(
	                    0,
	                    ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f); // Note: Original code is now FILL_PARENT.
        	}
            lp.setMargins(0, 0, 0, 0);
            child.setLayoutParams(lp);
        }

        // Ensure you can navigate to the tab with the keyboard, and you can touch it
        child.setFocusable(true);
        child.setClickable(true);

        super.addView(child);

        // TODO: detect this via geometry with a tabwidget listener rather
        // than potentially interfere with the view's listener
        child.setOnClickListener(new TabClickListener(getChildCount() - 1));
        child.setOnFocusChangeListener(this);
    }

    /**
     * Provides a way for {@link TabHost} to be notified that the user clicked on a tab indicator.
     */
    public void setTabSelectionListener(OnTabSelectionChanged listener) {
        mSelectionChangedListener = listener;
    }

    public void onFocusChange(View v, boolean hasFocus) {
        if (v == this && hasFocus) {
            getChildAt(mSelectedTab).requestFocus();
            return;
        }

        if (hasFocus) {
            int i = 0;
            int numTabs = getChildCount();
            while (i < numTabs) {
                if (getChildAt(i) == v) {
                    setCurrentTab(i);
                    mSelectionChangedListener.onTabSelectionChanged(i, false);
                    break;
                }
                i++;
            }
        }
    }

    // registered with each tab indicator so we can notify tab host
    private class TabClickListener implements OnClickListener {

        private final int mTabIndex;

        private TabClickListener(int tabIndex) {
            mTabIndex = tabIndex;
        }

        public void onClick(View v) {
            mSelectionChangedListener.onTabSelectionChanged(mTabIndex, true);
        }
    }

    /**
     * Let {@link TabHost} know that the user clicked on a tab indicator.
     */
    static interface OnTabSelectionChanged {
        /**
         * Informs the TabHost which tab was selected. It also indicates
         * if the tab was clicked/pressed or just focused into.
         *
         * @param tabIndex index of the tab that was selected
         * @param clicked whether the selection changed due to a touch/click
         * or due to focus entering the tab through navigation. Pass true
         * if it was due to a press/click and false otherwise.
         */
        void onTabSelectionChanged(int tabIndex, boolean clicked);
    }

}

