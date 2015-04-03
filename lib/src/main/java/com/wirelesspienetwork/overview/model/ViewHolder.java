package com.wirelesspienetwork.overview.model;

import android.view.View;

import com.wirelesspienetwork.overview.views.OverviewCard;

public class ViewHolder<V extends View, Model extends Object>
{
    public final V itemView;
    public Model model;

    private OverviewCard mContainer;

    private int mCurrentPosition = -1;
    private int mLastPosition = -1;

    public ViewHolder(V view)
    {
        this.itemView = view;
    }

    public void setPosition(int position) {
        mLastPosition = mCurrentPosition;
        mCurrentPosition = position;
    }

    public int getPosition() {
        return mCurrentPosition;
    }

    public int getLastPosition() {
        return mLastPosition;
    }

    public OverviewCard getContainer()
    {
        return mContainer;
    }

    protected void setContainer(OverviewCard container) {
        if (mContainer != null) {
            mContainer.setContent(null);
        }
        mContainer = container;
        if (mContainer != null && itemView != null) {
            mContainer.setContent(itemView);
        }
    }
}
