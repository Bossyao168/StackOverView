package com.wirelesspienetwork.overview.misc;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

import com.wirelesspienetwork.overview.R;

public class OverviewConfiguration {

    /** Interpolators */
    public Interpolator fastOutSlowInInterpolator;
    public Interpolator fastOutLinearInInterpolator;
    public Interpolator linearOutSlowInInterpolator;
    public Interpolator quintOutInterpolator;

    /** Insets */
    public Rect displayRect = new Rect();

    /** Task stack */
    public int taskStackScrollDuration;
    public int taskStackMaxDim;
    public int taskStackTopPaddingPx;
    public float taskStackWidthPaddingPct;
    public float taskStackOverscrollPct;

    /** Task view animation and styles */
    public int taskViewEnterFromHomeDelay;
    public int taskViewEnterFromHomeDuration;
    public int taskViewEnterFromHomeStaggerDelay;
    public int taskViewRemoveAnimDuration;
    public int taskViewRemoveAnimTranslationXPx;
    public int taskViewTranslationZMinPx;
    public int taskViewTranslationZMaxPx;

    /** Private constructor */
    public OverviewConfiguration(Context context) {
        fastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context,
                android.R.interpolator.accelerate_decelerate);
        fastOutLinearInInterpolator = AnimationUtils.loadInterpolator(context,
                android.R.interpolator.accelerate_decelerate);
        linearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context,
                android.R.interpolator.accelerate_decelerate);
        quintOutInterpolator = AnimationUtils.loadInterpolator(context,
                android.R.interpolator.decelerate_quint);
        update(context);
    }

    /** Updates the state, given the specified context */
    void update(Context context) {
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();

        // Insets
        displayRect.set(0, 0, dm.widthPixels, dm.heightPixels);

        // Task stack
        taskStackScrollDuration =
                res.getInteger(R.integer.recents_animate_task_stack_scroll_duration);

        //获取dimen资源值
        TypedValue widthPaddingPctValue = new TypedValue();
        res.getValue(R.dimen.recents_stack_width_padding_percentage, widthPaddingPctValue, true);
        taskStackWidthPaddingPct = widthPaddingPctValue.getFloat();

        //获取dimen资源值
        TypedValue stackOverscrollPctValue = new TypedValue();
        res.getValue(R.dimen.recents_stack_overscroll_percentage, stackOverscrollPctValue, true);
        taskStackOverscrollPct = stackOverscrollPctValue.getFloat();

        taskStackMaxDim = res.getInteger(R.integer.recents_max_task_stack_view_dim);
        taskStackTopPaddingPx = res.getDimensionPixelSize(R.dimen.recents_stack_top_padding);

        // Task view animation and styles
        taskViewEnterFromHomeDelay =
                res.getInteger(R.integer.recents_animate_task_enter_from_home_delay);
        taskViewEnterFromHomeDuration =
                res.getInteger(R.integer.recents_animate_task_enter_from_home_duration);
        taskViewEnterFromHomeStaggerDelay =
                res.getInteger(R.integer.recents_animate_task_enter_from_home_stagger_delay);
        taskViewRemoveAnimDuration =
                res.getInteger(R.integer.recents_animate_task_view_remove_duration);
        taskViewRemoveAnimTranslationXPx =
                res.getDimensionPixelSize(R.dimen.recents_task_view_remove_anim_translation_x);
        taskViewTranslationZMinPx = res.getDimensionPixelSize(R.dimen.recents_task_view_z_min);
        taskViewTranslationZMaxPx = res.getDimensionPixelSize(R.dimen.recents_task_view_z_max);
    }
    /**
     * Returns the task stack bounds in the current orientation. These bounds do not account for
     * the system insets.
     */
    public void getOverviewStackBounds(int windowWidth, int windowHeight,
                                       Rect taskStackBounds) {
        taskStackBounds.set(0, 64, windowWidth, windowHeight);
    }
}
