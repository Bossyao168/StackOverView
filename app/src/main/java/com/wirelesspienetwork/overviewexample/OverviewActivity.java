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

package com.wirelesspienetwork.overviewexample;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;

import com.wirelesspienetwork.overview.misc.Utilities;
import com.wirelesspienetwork.overview.model.OverviewAdapter;
import com.wirelesspienetwork.overview.model.ViewHolder;
import com.wirelesspienetwork.overview.views.Overview;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Random;

/**
 * The main Recents activity that is started from AlternateRecentsComponent.
 */
public class OverviewActivity extends Activity implements Overview.RecentsViewCallbacks
{
    boolean mVisible;
    // Top level views
    Overview mRecentsView;

    /** Called with the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // For the non-primary user, ensure that the SystemSericesProxy is initialized

        // Initialize the widget host (the host id is static and does not change)

        // Set the Recents layout
        setContentView(R.layout.recents);
        mRecentsView = (Overview) findViewById(R.id.recents_view);
        mRecentsView.setCallbacks(this);
        mRecentsView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        // Register the broadcast receiver to handle messages when the screen is turned off
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(SearchManager.INTENT_GLOBAL_SEARCH_ACTIVITY_CHANGED);

        // Private API calls to make the shadows look better
        try {
            Utilities.setShadowProperty("ambientRatio", String.valueOf(1.5f));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Mark Recents as visible
        mVisible = true;

        ArrayList<Integer> models = new ArrayList<>();
        for(int i = 0; i < 10; ++i)
        {
            Random random = new Random();
            random.setSeed(i);
            int color = Color.argb(255, random.nextInt(255), random.nextInt(255), random.nextInt(255));
            models.add(color);
        }

        final OverviewAdapter stack = new OverviewAdapter<ViewHolder<View, Integer>, Integer>(models)
        {
            @Override
            public ViewHolder onCreateViewHolder(Context context, ViewGroup parent) {
                View v = View.inflate(context, R.layout.recents_dummy, null);
                return new ViewHolder<View, Integer>(v);
            }

            @Override
            public void onBindViewHolder(ViewHolder<View, Integer> viewHolder) {
                viewHolder.itemView.setBackgroundColor(viewHolder.model);
            }
        };

        mRecentsView.setTaskStack(stack);

//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                stack.notifyDataSetInserted(new Integer(1), 2);
//            }
//        },2000);


    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onTrimMemory(int level) {
    }

    @Override
    public void onAllCardsDismissed() {
    }

    @Override
    public void onCardDismissed(int position) {

    }
}
