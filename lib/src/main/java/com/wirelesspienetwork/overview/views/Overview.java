/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.wirelesspienetwork.overview.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.wirelesspienetwork.overview.misc.OverviewConfiguration;
import com.wirelesspienetwork.overview.misc.ReferenceCountedTrigger;
import com.wirelesspienetwork.overview.model.OverviewAdapter;

public class Overview extends FrameLayout implements OverviewStackView.Callbacks {

    public interface RecentsViewCallbacks {
        public void onCardDismissed(int position);
        public void onAllCardsDismissed();
    }

    OverviewStackView mStackView;
    OverviewConfiguration mConfig;
    OverviewAdapter mAdapter;
    RecentsViewCallbacks mCallbacks;

    public Overview(Context context) {
        super(context);
        init(context);
    }

    public Overview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public Overview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(21)
    public Overview(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context)
    {
        mConfig = new OverviewConfiguration(context);
    }

    /** Sets the callbacks */
    public void setCallbacks(RecentsViewCallbacks cb) {
        mCallbacks = cb;
    }

    /** Set/get the bsp root node */
    public void setTaskStack(OverviewAdapter adapter) {

        if (mStackView != null) {
            removeView(mStackView);
        }

        mAdapter = adapter;
        mStackView = new OverviewStackView(getContext(), adapter, mConfig);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mStackView.setLayoutParams(params);

        mStackView.setCallbacks(this);
        mStackView.setAlpha(0);
        mStackView.animate().alpha(1.f).setStartDelay(2000).setDuration(3500).start();

        //所以说 OverviewStackView 才是重点
        addView(mStackView);
    }

    /**
     * This is called with the full size of the window since we are handling our own insets.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (mStackView != null) {
            Rect stackBounds = new Rect();
            mConfig.getOverviewStackBounds(width, height, stackBounds);
            mStackView.setStackInsetRect(stackBounds);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void onCardDismissed(int position) {
        if (mCallbacks != null) {
            mCallbacks.onCardDismissed(position);
        }
    }

    @Override
    public void onAllCardsDismissed() {
        if (mCallbacks != null) {
            mCallbacks.onAllCardsDismissed();
        }
    }
}
