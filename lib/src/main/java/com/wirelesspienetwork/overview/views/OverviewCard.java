package com.wirelesspienetwork.overview.views;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.wirelesspienetwork.overview.R;
import com.wirelesspienetwork.overview.misc.OverviewConfiguration;

/* A task view */
public class OverviewCard extends FrameLayout {

    OverviewConfiguration mConfig;

    float mTaskProgress;
    ObjectAnimator mTaskProgressAnimator;
    LinearLayout mContentContainer;
    View mContent;

    // Optimizations
    ValueAnimator.AnimatorUpdateListener mUpdateDimListener =
            new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    setTaskProgress((Float) animation.getAnimatedValue());
                }
            };


    public OverviewCard(Context context) {
        super(context);
        init(context);
    }

    public OverviewCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public OverviewCard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(21)
    public OverviewCard(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context)
    {
        setBackgroundColor(Color.TRANSPARENT);

        mContentContainer = new LinearLayout(context);
        mContentContainer.setOrientation(LinearLayout.VERTICAL);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        mContentContainer.setLayoutParams(params);
        addView(mContentContainer);
    }

    //将子view的宽高设置入此父view
    @Override
    public void getHitRect(Rect outRect)
    {
        Rect contentRect = new Rect();
        mContent.getHitRect(contentRect);
        super.getHitRect(outRect);
        outRect.left += contentRect.left;
        outRect.top += contentRect.top;
        outRect.right = outRect.left + contentRect.width();
        outRect.bottom = outRect.top + contentRect.height();
    }

    public void setConfig(OverviewConfiguration config)
    {
        mConfig = config;
    }

    public void setContent(View content)
    {
        mContent = content;
        mContentContainer.removeAllViews();
        if (mContent != null) {
            mContentContainer.addView(mContent);
        }
        setTaskProgress(getTaskProgress());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        int widthWithoutPadding = width - getPaddingLeft() - getPaddingRight();
        int heightWithoutPadding = height - getPaddingTop() - getPaddingBottom();

        // Measure the content
        mContentContainer.measure(MeasureSpec.makeMeasureSpec(widthWithoutPadding, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(heightWithoutPadding, MeasureSpec.EXACTLY));

        setMeasuredDimension(width, height);
    }

    /** Synchronizes this view's properties with the task's transform */
    void updateViewPropertiesToCardTransform(OverviewCardTransform toTransform, int duration) {
        updateViewPropertiesToCardTransform(toTransform, duration, null);
    }

    void updateViewPropertiesToCardTransform(OverviewCardTransform toTransform, int duration,
                                             ValueAnimator.AnimatorUpdateListener updateCallback) {
        // Apply the transform
        toTransform.applyToTaskView(this, duration, mConfig.fastOutSlowInInterpolator, false,
                true, updateCallback);

        // Update the task progress
        if (mTaskProgressAnimator != null) {
            mTaskProgressAnimator.removeAllListeners();
            mTaskProgressAnimator.cancel();
        }
        if (duration <= 0) {
            setTaskProgress(toTransform.p);
        } else {
            mTaskProgressAnimator = ObjectAnimator.ofFloat(this, "taskProgress", toTransform.p);
            mTaskProgressAnimator.setDuration(duration);
            mTaskProgressAnimator.addUpdateListener(mUpdateDimListener);
            mTaskProgressAnimator.start();
        }
    }

    /** Resets this view's properties */
    void resetViewProperties() {
        OverviewCardTransform.reset(this);
    }

    /** Prepares this task view for the enter-recents animations.  This is called earlier in the
     * first layout because the actual animation into recents may take a long time. */
    void prepareEnterRecentsAnimation() {
    }

    /** Animates this task view as it enters recents */
    void startEnterRecentsAnimation(final ViewAnimation.OverviewCardEnterContext ctx) {
        final OverviewCardTransform transform = ctx.currentTaskTransform;

        // Animate the tasks up
        int frontIndex = (ctx.currentStackViewCount - ctx.currentStackViewIndex - 1);
        int duration = Math.max(0, mConfig.taskViewEnterFromHomeDuration +
                frontIndex * mConfig.taskViewEnterFromHomeStaggerDelay);

        int delay =  Math.max(0, mConfig.taskViewEnterFromHomeDelay -
                frontIndex * mConfig.taskViewEnterFromHomeStaggerDelay);

        setScaleX(transform.scale);
        setScaleY(transform.scale);
        animate()
                .translationY(transform.translationY)
                .setStartDelay(delay)
                .setInterpolator(mConfig.quintOutInterpolator)
                .setDuration(duration)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        ctx.postAnimationTrigger.decrement();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                })
                .start();
        ctx.postAnimationTrigger.increment();
    }

    /** Sets the current task progress. */
    public void setTaskProgress(float p) {
        mTaskProgress = p;;
    }

    /** Returns the current task progress. */
    public float getTaskProgress() {
        return mTaskProgress;
    }

    /** Enables/disables handling touch on this task view. */
    void setTouchEnabled(boolean enabled) {
        setOnClickListener(!enabled ? null : new OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }
}
