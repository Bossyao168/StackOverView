package com.wirelesspienetwork.overview.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.widget.OverScroller;

import com.wirelesspienetwork.overview.misc.OverviewConfiguration;

/* The scrolling logic for a TaskStackView */
public class OverviewStackViewScroller {
    public interface Callbacks {
        public void onScrollChanged(float p);
    }

    OverviewConfiguration mConfig;
    OverviewStackViewLayoutAlgorithm mLayoutAlgorithm;
    Callbacks mCb;

    float mStackScrollP;

    OverScroller mScroller;
    ObjectAnimator mScrollAnimator;

    public OverviewStackViewScroller(Context context, OverviewConfiguration config, OverviewStackViewLayoutAlgorithm layoutAlgorithm) {
        mConfig = config;
        mScroller = new OverScroller(context);
        mLayoutAlgorithm = layoutAlgorithm;
        setStackScroll(getStackScroll());
    }

    /** Sets the callbacks */
    void setCallbacks(Callbacks cb) {
        mCb = cb;
    }

    /** Gets the current stack scroll */
    public float getStackScroll() {
        return mStackScrollP;
    }

    /** Sets the current stack scroll */
    public void setStackScroll(float s) {
        mStackScrollP = s;
        if (mCb != null) {
            mCb.onScrollChanged(mStackScrollP);
        }
    }

    /** Sets the current stack scroll without calling the callback. */
    void setStackScrollRaw(float s) {
        mStackScrollP = s;
    }

    /** Sets the current stack scroll to the initial state when you first enter recents */
    public void setStackScrollToInitialState() {
        setStackScroll(getBoundedStackScroll(mLayoutAlgorithm.mInitialScrollP));
    }

    /** Bounds the current scroll if necessary */
    public boolean boundScroll() {
        float curScroll = getStackScroll();
        float newScroll = getBoundedStackScroll(curScroll);
        if (Float.compare(newScroll, curScroll) != 0) {
            setStackScroll(newScroll);
            return true;
        }
        return false;
    }
    /** Bounds the current scroll if necessary, but does not synchronize the stack view with the model. */
    public boolean boundScrollRaw() {
        float curScroll = getStackScroll();
        float newScroll = getBoundedStackScroll(curScroll);
        if (Float.compare(newScroll, curScroll) != 0) {
            setStackScrollRaw(newScroll);
            return true;
        }
        return false;
    }

    /** Returns the bounded stack scroll */
    float getBoundedStackScroll(float scroll) {
        return Math.max(mLayoutAlgorithm.mMinScrollP, Math.min(mLayoutAlgorithm.mMaxScrollP, scroll));
    }

    /** Returns the amount that the aboslute value of how much the scroll is out of bounds. */
    float getScrollAmountOutOfBounds(float scroll) {
        if (scroll < mLayoutAlgorithm.mMinScrollP) {
            return Math.abs(scroll - mLayoutAlgorithm.mMinScrollP);
        } else if (scroll > mLayoutAlgorithm.mMaxScrollP) {
            return Math.abs(scroll - mLayoutAlgorithm.mMaxScrollP);
        }
        return 0f;
    }

    /** Returns whether the specified scroll is out of bounds */
    boolean isScrollOutOfBounds() {
        return Float.compare(getScrollAmountOutOfBounds(mStackScrollP), 0f) != 0;
    }

    /** Animates the stack scroll into bounds */
    ObjectAnimator animateBoundScroll() {
        float curScroll = getStackScroll();
        float newScroll = getBoundedStackScroll(curScroll);
        if (Float.compare(newScroll, curScroll) != 0) {
            // Start a new scroll animation
            animateScroll(curScroll, newScroll, null);
        }
        return mScrollAnimator;
    }

    /** Animates the stack scroll */
    void animateScroll(float curScroll, float newScroll, final Runnable postRunnable) {
        // Abort any current animations
        stopScroller();
        stopBoundScrollAnimation();

        mScrollAnimator = ObjectAnimator.ofFloat(this, "stackScroll", curScroll, newScroll);
        mScrollAnimator.setDuration(mConfig.taskStackScrollDuration);
        mScrollAnimator.setInterpolator(mConfig.linearOutSlowInInterpolator);
        mScrollAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setStackScroll((Float) animation.getAnimatedValue());
            }
        });
        mScrollAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (postRunnable != null) {
                    postRunnable.run();
                }
                mScrollAnimator.removeAllListeners();
            }
        });
        mScrollAnimator.start();
    }

    /** Aborts any current stack scrolls */
    void stopBoundScrollAnimation() {
        if (mScrollAnimator != null) {
            mScrollAnimator.removeAllListeners();
            mScrollAnimator.cancel();
        }
    }

    /**** OverScroller ****/

    int progressToScrollRange(float p) {
        return (int) (p * mLayoutAlgorithm.mStackVisibleRect.height());
    }

    float scrollRangeToProgress(int s) {
        return (float) s / mLayoutAlgorithm.mStackVisibleRect.height();
    }

    /** Called from the view draw, computes the next scroll. */
    boolean computeScroll() {
        if (mScroller.computeScrollOffset()) {
            float scroll = scrollRangeToProgress(mScroller.getCurrY());
            setStackScrollRaw(scroll);
            if (mCb != null) {
                mCb.onScrollChanged(scroll);
            }
            return true;
        }
        return false;
    }

    /** Returns whether the overscroller is scrolling. */
    boolean isScrolling() {
        return !mScroller.isFinished();
    }

    /** Stops the scroller and any current fling. */
    void stopScroller() {
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
    }
}