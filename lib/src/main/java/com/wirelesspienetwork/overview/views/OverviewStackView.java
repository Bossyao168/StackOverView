package com.wirelesspienetwork.overview.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import com.wirelesspienetwork.overview.misc.OverviewConfiguration;
import com.wirelesspienetwork.overview.model.OverviewAdapter;
import com.wirelesspienetwork.overview.model.ViewHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/* The visual representation of a task stack view */
public class OverviewStackView extends FrameLayout implements OverviewAdapter.Callbacks, OverviewStackViewScroller.Callbacks,
        ObjectPool.ObjectPoolConsumer<ViewHolder, Integer> {

    /** The TaskView callbacks */
    interface Callbacks {
        public void onCardDismissed(int position);
        public void onAllCardsDismissed();
    }

    OverviewConfiguration mConfig;

    OverviewAdapter mStack;
    OverviewStackViewLayoutAlgorithm mLayoutAlgorithm;
    OverviewStackViewScroller mStackScroller;
    OverviewStackViewTouchHandler mTouchHandler;
    Callbacks mCb;
    ObjectPool<ViewHolder, Integer> mViewPool;
    ArrayList<OverviewCardTransform> mCurrentCardTransforms = new ArrayList<OverviewCardTransform>();
    HashMap<OverviewCard, ViewHolder> mViewHolderMap = new HashMap<>();

    Rect mOverviewStackBounds = new Rect();

    // Optimizations
    int mStackViewsAnimationDuration;
    boolean mStackViewsDirty = true;
    boolean mStackViewsClipDirty = true;
    boolean mAwaitingFirstLayout = true;
    boolean mStartEnterAnimationRequestedAfterLayout;
    boolean mStartEnterAnimationCompleted;
    ViewAnimation.OverviewCardEnterContext mStartEnterAnimationContext;
    int[] mTmpVisibleRange = new int[2];
    Rect mTmpRect = new Rect();
    OverviewCardTransform mTmpTransform = new OverviewCardTransform();
    LayoutInflater mInflater;

    ValueAnimator.AnimatorUpdateListener mRequestUpdateClippingListener =
            new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            requestUpdateStackViewsClip();
        }
    };

    public OverviewStackView(Context context, OverviewAdapter adapter, OverviewConfiguration config) {
        super(context);
        mConfig = config;
        mStack = adapter;
        mStack.setCallbacks(this);
        mViewPool = new ObjectPool<>(context, this);
        mInflater = LayoutInflater.from(context);
        mLayoutAlgorithm = new OverviewStackViewLayoutAlgorithm(mConfig);
        mStackScroller = new OverviewStackViewScroller(context, mConfig, mLayoutAlgorithm);
        mStackScroller.setCallbacks(this);
        mTouchHandler = new OverviewStackViewTouchHandler(context, this, mConfig, mStackScroller);
    }

    /** Sets the callbacks */
    void setCallbacks(Callbacks cb) {
        mCb = cb;
    }

    /** Requests that the views be synchronized with the model */
    void requestSynchronizeStackViewsWithModel() {
        requestSynchronizeStackViewsWithModel(0);
    }
    void requestSynchronizeStackViewsWithModel(int duration) {
        if (!mStackViewsDirty) {
            invalidate();
            mStackViewsDirty = true;
        }
        if (mAwaitingFirstLayout) {
            // Skip the animation if we are awaiting first layout
            mStackViewsAnimationDuration = 0;
        } else {
            mStackViewsAnimationDuration = Math.max(mStackViewsAnimationDuration, duration);
        }
    }

    /** Requests that the views clipping be updated. */
    void requestUpdateStackViewsClip() {
        if (!mStackViewsClipDirty) {
            invalidate();
            mStackViewsClipDirty = true;
        }
    }

    public OverviewCard getChildViewForIndex(int index) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            OverviewCard tv = (OverviewCard) getChildAt(i);
            ViewHolder holder = mViewHolderMap.get(tv);
            if (holder != null && holder.getPosition() == index) {
                return tv;
            }
        }
        return null;
    }

    private boolean updateStackTransforms(ArrayList<OverviewCardTransform> cardTransforms,
                                       int itemCount,
                                       float stackScroll,
                                       int[] visibleRangeOut,
                                       boolean boundTranslationsToRect) {
        // XXX: We should be intelligent about where to look for the visible stack range using the
        //      current stack scroll.
        // XXX: We should log extra cases like the ones below where we don't expect to hit very often
        // XXX: Print out approximately how many indices we have to go through to find the first visible transform

        int transformCount = cardTransforms.size();
        int frontMostVisibleIndex = -1;
        int backMostVisibleIndex = -1;

        // We can reuse the card transforms where possible to reduce object allocation
        if (transformCount < itemCount) {
            // If there are less transforms than cards, then add as many transforms as necessary
            for (int i = transformCount; i < itemCount; i++) {
                cardTransforms.add(new OverviewCardTransform());
            }
        } else if (transformCount > itemCount) {
            // If there are more transforms than cards, then just subset the transform list
            cardTransforms.subList(0, itemCount);
        }

        // Update the stack transforms
        OverviewCardTransform prevTransform = null;
        for (int i = itemCount - 1; i >= 0; i--) {

            // 这里将空的 OverviewCardTransform 丢进去
            OverviewCardTransform transform = mLayoutAlgorithm.getStackTransform(i,
                    stackScroll, cardTransforms.get(i), prevTransform);
            if (transform.visible) {
                if (frontMostVisibleIndex < 0) {
                    frontMostVisibleIndex = i;
                }
                backMostVisibleIndex = i;
            } else {
                if (backMostVisibleIndex != -1) {
                    // We've reached the end of the visible range, so going down the rest of the
                    // stack, we can just reset the transforms accordingly
                    while (i >= 0) {
                        cardTransforms.get(i).reset();
                        i--;
                    }
                    break;
                }
            }

            if (boundTranslationsToRect) {
                transform.translationY = Math.min(transform.translationY,
                        mLayoutAlgorithm.mViewRect.bottom);
            }
            prevTransform = transform;
        }
        if (visibleRangeOut != null) {
            visibleRangeOut[0] = frontMostVisibleIndex;
            visibleRangeOut[1] = backMostVisibleIndex;
        }
        return frontMostVisibleIndex != -1 && backMostVisibleIndex != -1;
    }

    /** Synchronizes the views with the model */
    boolean synchronizeStackViewsWithModel() {
        if (mStackViewsDirty) {

            float stackScroll = mStackScroller.getStackScroll();
            int[] visibleRange = mTmpVisibleRange;
            boolean isValidVisibleRange = updateStackTransforms(mCurrentCardTransforms, mStack.getNumberOfItems(),
                    stackScroll, visibleRange, false);

            ArrayList<Map.Entry<OverviewCard, ViewHolder>> entrySet = new ArrayList<>(mViewHolderMap.entrySet());

            Map<Integer, ViewHolder> reusedMap = new HashMap<>();

            for(Map.Entry<OverviewCard, ViewHolder> entry : entrySet)
            {
                int position = entry.getValue().getPosition();
                if (visibleRange[1] <= position && position <= visibleRange[0])
                {
                    ViewHolder vh = entry.getValue();
                    reusedMap.put(position, vh);
                } else {
                    mViewPool.returnObjectToPool(entry.getValue());
                }
            }

            // Pick up all the newly visible children and update all the existing children
            for (int i = visibleRange[0]; isValidVisibleRange && i >= visibleRange[1]; i--) {
                OverviewCardTransform transform = mCurrentCardTransforms.get(i);

                ViewHolder vh = reusedMap.get(i);
                if (vh == null) {
                    vh = mViewPool.pickUpObjectFromPool(i, i);

                    if (mStackViewsAnimationDuration > 0) {
                        // For items in the list, put them in start animating them from the
                        // approriate ends of the list where they are expected to appear
                        if (Float.compare(transform.p, 0f) <= 0) {
                            mLayoutAlgorithm.getStackTransform(0f, 0f, mTmpTransform, null);
                        } else {
                            mLayoutAlgorithm.getStackTransform(1f, 0f, mTmpTransform, null);
                        }
                        vh.getContainer().updateViewPropertiesToCardTransform(mTmpTransform, 0);
                    }
                }

                // Animate the card into place
                vh.getContainer().updateViewPropertiesToCardTransform(mCurrentCardTransforms.get(i),
                        mStackViewsAnimationDuration, mRequestUpdateClippingListener);
            }

            // Reset the request-synchronize params
            mStackViewsAnimationDuration = 0;
            mStackViewsDirty = false;
            mStackViewsClipDirty = true;
            return true;
        }
        return false;
    }

    /** Updates the clip for each of the task views. */
    void clipTaskViews() {
        mStackViewsClipDirty = false;
    }

    /** The stack insets to apply to the stack contents */
    public void setStackInsetRect(Rect r) {
        mOverviewStackBounds.set(r);
    }

    @Override
    public void setOnTouchListener(OnTouchListener l) {
        super.setOnTouchListener(l);
    }

    /** Updates the min and max virtual scroll bounds */
    void updateMinMaxScroll(boolean boundScrollToNewMinMax) {
        // Compute the min and max scroll values
        mLayoutAlgorithm.computeMinMaxScroll(mStack.getNumberOfItems());

        // Debug logging
        if (boundScrollToNewMinMax) {
            mStackScroller.boundScroll();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mTouchHandler.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return mTouchHandler.onTouchEvent(ev);
    }

    @Override
    public void computeScroll() {
        mStackScroller.computeScroll();
        // Synchronize the views
        synchronizeStackViewsWithModel();
        clipTaskViews();
    }

    /** Computes the stack and task rects */
    public void computeRects(int windowWidth, int windowHeight, Rect taskStackBounds) {
        // Compute the rects in the stack algorithm
        mLayoutAlgorithm.computeRects(windowWidth, windowHeight, taskStackBounds);

        // Update the scroll bounds
        updateMinMaxScroll(false);
    }

    /**
     * This is called with the full window width and height to allow stack view children to
     * perform the full screen transition down.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        //空间大部分的初始化都在这里

        // Compute our stack/task rects
        Rect taskStackBounds = new Rect(mOverviewStackBounds);
        computeRects(width, height, taskStackBounds);

        // If this is the first layout, then scroll to the front of the stack and synchronize the
        // stack views immediately to load all the views
        if (mAwaitingFirstLayout) {
            mStackScroller.setStackScrollToInitialState();
            requestSynchronizeStackViewsWithModel();
            synchronizeStackViewsWithModel();
        }

        // Measure each of the TaskViews
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            OverviewCard tv = (OverviewCard) getChildAt(i);
            if (tv.getBackground() != null) {
                tv.getBackground().getPadding(mTmpRect);
            } else {
                mTmpRect.setEmpty();
            }
            tv.measure(
                MeasureSpec.makeMeasureSpec(
                        mLayoutAlgorithm.mTaskRect.width() + mTmpRect.left + mTmpRect.right,
                        MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(
                        mLayoutAlgorithm.mTaskRect.height() + mTmpRect.top + mTmpRect.bottom, MeasureSpec.EXACTLY));
        }

        setMeasuredDimension(width, height);
    }

    /**
     * This is called with the size of the space not including the top or right insets, or the
     * search bar height in portrait (but including the search bar width in landscape, since we want
     * to draw under it.
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
       // Layout each of the children
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            OverviewCard tv = (OverviewCard) getChildAt(i);
            if (tv.getBackground() != null) {
                tv.getBackground().getPadding(mTmpRect);
            } else {
                mTmpRect.setEmpty();
            }
            tv.layout(mLayoutAlgorithm.mTaskRect.left - mTmpRect.left,
                    mLayoutAlgorithm.mTaskRect.top - mTmpRect.top,
                    mLayoutAlgorithm.mTaskRect.right + mTmpRect.right,
                    mLayoutAlgorithm.mTaskRect.bottom + mTmpRect.bottom);
        }

        if (mAwaitingFirstLayout) {
            mAwaitingFirstLayout = false;
            onFirstLayout();
        }
    }

    /** Handler for the first layout. */
    void onFirstLayout() {
        int offscreenY = mLayoutAlgorithm.mViewRect.bottom -
                (mLayoutAlgorithm.mTaskRect.top - mLayoutAlgorithm.mViewRect.top);

        for (Map.Entry<OverviewCard, ViewHolder> entry : mViewHolderMap.entrySet())
        {
            entry.getKey().prepareEnterRecentsAnimation();
        }

        // If the enter animation started already and we haven't completed a layout yet, do the
        // enter animation now
        if (mStartEnterAnimationRequestedAfterLayout) {
            startEnterRecentsAnimation(mStartEnterAnimationContext);
            mStartEnterAnimationRequestedAfterLayout = false;
            mStartEnterAnimationContext = null;
        }
    }

    /** Requests this task stacks to start it's enter-recents animation */
    public void startEnterRecentsAnimation(ViewAnimation.OverviewCardEnterContext ctx) {
        // If we are still waiting to layout, then just defer until then
        if (mAwaitingFirstLayout) {
            mStartEnterAnimationRequestedAfterLayout = true;
            mStartEnterAnimationContext = ctx;
            return;
        }

        int childCount = getChildCount();

        if (mStack.getNumberOfItems() > 0) {
            // Find the launch target task

            int launchTargetIndex = childCount == 0 ? -1 : 0;

            for(int i = 0; i < childCount; ++i)
            {
                OverviewCard card = (OverviewCard)getChildAt(i);
                ViewHolder holder = mViewHolderMap.get(card);

                ctx.currentTaskTransform = new OverviewCardTransform();
                ctx.currentStackViewIndex = i;
                ctx.currentStackViewCount = childCount;
                ctx.currentTaskRect = mLayoutAlgorithm.mTaskRect;
                ctx.currentTaskOccludesLaunchTarget = (launchTargetIndex != -1);
                ctx.updateListener = mRequestUpdateClippingListener;
                mLayoutAlgorithm.getStackTransform(i, mStackScroller.getStackScroll(), ctx.currentTaskTransform, null);
                card.startEnterRecentsAnimation(ctx);
            }

            // Add a runnable to the post animation ref counter to clear all the views
            ctx.postAnimationTrigger.addLastDecrementRunnable(new Runnable() {
                @Override
                public void run() {
                    mStartEnterAnimationCompleted = true;
                }
            });
        }
    }

    public boolean isTransformedTouchPointInView(float x, float y, View child) {
        final Rect frame = new Rect();
        child.getHitRect(frame);
        return frame.contains((int)x, (int)y);
    }

    public void onCardAdded(OverviewAdapter stack, int position) {
        requestSynchronizeStackViewsWithModel();
    }

    public void onCardRemoved(OverviewAdapter stack, int removedTask) {
        // Remove the view associated with this task, we can't rely on updateTransforms
        // to work here because the task is no longer in the list
        OverviewCard tv = getChildViewForIndex(removedTask);
        ViewHolder holder = mViewHolderMap.get(tv);

        // Notify the callback that we've removed the task and it can clean up after it
        mCb.onCardDismissed(removedTask);

        if (tv != null) {
            holder.setPosition(-1);
            mViewPool.returnObjectToPool(holder);
        }

        for (ViewHolder vh : mViewHolderMap.values())
        {
            if (vh.getPosition() > removedTask) {
                vh.setPosition(vh.getPosition() - 1);
                // No need to rebind, it's just an index change.
            }
        }

        // Get the stack scroll of the task to anchor to (since we are removing something, the front
        // most task will be our anchor task)
        int anchorPosition = -1;
        float prevAnchorTaskScroll = 0;
        boolean pullStackForward = stack.getNumberOfItems() > 0;
        if (pullStackForward) {
            anchorPosition = stack.getNumberOfItems() - 1;
            prevAnchorTaskScroll = mLayoutAlgorithm.getStackScrollForTask(anchorPosition);
        }

        // Update the min/max scroll and animate other task views into their new positions
        updateMinMaxScroll(true);

        // Offset the stack by as much as the anchor task would otherwise move back
        if (pullStackForward) {
            float anchorTaskScroll = mLayoutAlgorithm.getStackScrollForTask(anchorPosition);
            mStackScroller.setStackScroll(mStackScroller.getStackScroll() + (anchorTaskScroll
                    - prevAnchorTaskScroll));
            mStackScroller.boundScroll();
        }

        // Animate all the tasks into place
        requestSynchronizeStackViewsWithModel(200);

        // If there are no remaining tasks, then either unfilter the current stack, or just close
        // the activity if there are no filtered stacks
        if (mStack.getNumberOfItems() == 0) {
            mCb.onAllCardsDismissed();
        }
    }

    public void onCardDismissed(OverviewCard tv) {

        ViewHolder vh = mViewHolderMap.get(tv);
        int taskIndex = vh.getPosition();
        mStack.notifyDataSetRemoved(taskIndex);
    }

    @Override
    public ViewHolder createObject(Context context) {
        return mStack.createViewHolder(context, mConfig);
    }

    @Override
    public void prepareObjectToEnterPool(ViewHolder vh) {

        mViewHolderMap.remove(vh.getContainer());
        // Detach the view from the hierarchy
        detachViewFromParent(vh.getContainer());

        // Reset the view properties
        vh.getContainer().resetViewProperties();
    }

    @Override
    public void prepareObjectToLeavePool(ViewHolder vh, Integer position, boolean isNewView) {
        // Rebind the task and request that this task's data be filled into the TaskView

        mViewHolderMap.put(vh.getContainer(), vh);
        vh.setPosition(position);
        mStack.bindViewHolder(vh, position);
        OverviewCard container = vh.getContainer();

        // Find the index where this task should be placed in the stack
        int insertIndex = -1;
        int taskIndex = position;
        if (taskIndex != -1) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                OverviewCard insertTV = (OverviewCard)getChildAt(i);
                ViewHolder holder = mViewHolderMap.get(insertTV);
                if (taskIndex < holder.getPosition()) {
                    insertIndex = i;
                    break;
                }
            }
        }

        // Add/attach the view to the hierarchy
        if (isNewView) {
            addView(container, insertIndex);

            // Set the callbacks and listeners for this new view
            container.setTouchEnabled(true);
        } else {
            attachViewToParent(container, insertIndex, container.getLayoutParams());
        }
    }

    @Override
    public boolean hasPreferredData(ViewHolder vh, Integer preferredData) {
        return (vh.getPosition() == preferredData);
    }

    /**** TaskStackViewScroller.TaskStackViewScrollerCallbacks ****/
    @Override
    public void onScrollChanged(float p) {
        requestSynchronizeStackViewsWithModel();
        if (Build.VERSION.SDK_INT >= 16) {
            postInvalidateOnAnimation();
        }
        else
        {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    invalidate();
                }
            });
        }
    }
}
