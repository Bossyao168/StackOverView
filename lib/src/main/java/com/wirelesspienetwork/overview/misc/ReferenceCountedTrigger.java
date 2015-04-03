package com.wirelesspienetwork.overview.misc;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;

import java.util.ArrayList;

/**
 * A ref counted trigger that does some logic when the count is first incremented, or last
 * decremented.  Not thread safe as it's not currently needed.
 */
public class ReferenceCountedTrigger {

    Context mContext;
    int mCount;
    ArrayList<Runnable> mFirstIncRunnables = new ArrayList<Runnable>();
    ArrayList<Runnable> mLastDecRunnables = new ArrayList<Runnable>();
    Runnable mErrorRunnable;

    // Convenience runnables
    Runnable mIncrementRunnable = new Runnable() {
        @Override
        public void run() {
            increment();
        }
    };
    Runnable mDecrementRunnable = new Runnable() {
        @Override
        public void run() {
            decrement();
        }
    };

    public ReferenceCountedTrigger(Context context, Runnable firstIncRunnable,
                                   Runnable lastDecRunnable, Runnable errorRunanable) {
        mContext = context;
        if (firstIncRunnable != null) mFirstIncRunnables.add(firstIncRunnable);
        if (lastDecRunnable != null) mLastDecRunnables.add(lastDecRunnable);
        mErrorRunnable = errorRunanable;
    }

    /** Increments the ref count */
    public void increment() {
        if (mCount == 0 && !mFirstIncRunnables.isEmpty()) {
            int numRunnables = mFirstIncRunnables.size();
            for (int i = 0; i < numRunnables; i++) {
                mFirstIncRunnables.get(i).run();
            }
        }
        mCount++;
    }

    /** Convenience method to increment this trigger as a runnable */
    public Runnable incrementAsRunnable() {
        return mIncrementRunnable;
    }

    /** Adds a runnable to the last-decrement runnables list. */
    public void addLastDecrementRunnable(Runnable r) {
        // To ensure that the last decrement always calls, we increment and decrement after setting
        // the last decrement runnable
        boolean ensureLastDecrement = (mCount == 0);
        if (ensureLastDecrement) increment();
        mLastDecRunnables.add(r);
        if (ensureLastDecrement) decrement();
    }

    /** Decrements the ref count */
    public void decrement() {
        mCount--;
        if (mCount == 0 && !mLastDecRunnables.isEmpty()) {
            int numRunnables = mLastDecRunnables.size();
            for (int i = 0; i < numRunnables; i++) {
                mLastDecRunnables.get(i).run();
            }
        } else if (mCount < 0) {
            if (mErrorRunnable != null) {
                mErrorRunnable.run();
            } else {
                new Throwable("Invalid ref count").printStackTrace();
            }
        }
    }

    /** Convenience method to decrement this trigger as a runnable. */
    public Runnable decrementAsRunnable() {
        return mDecrementRunnable;
    }
    /** Convenience method to decrement this trigger as a animator listener. */
    public Animator.AnimatorListener decrementOnAnimationEnd() {
        return new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                decrement();
            }
        };
    }

    /** Returns the current ref count */
    public int getCount() {
        return mCount;
    }
}
