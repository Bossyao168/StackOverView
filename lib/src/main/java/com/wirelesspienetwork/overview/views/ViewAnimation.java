package com.wirelesspienetwork.overview.views;

import android.animation.ValueAnimator;
import android.graphics.Rect;
import com.wirelesspienetwork.overview.misc.ReferenceCountedTrigger;

public class ViewAnimation {

    public static class OverviewCardEnterContext {
        // A trigger to run some logic when all the animations complete.  This works around the fact
        // that it is difficult to coordinate ViewPropertyAnimators
        ReferenceCountedTrigger postAnimationTrigger;
        // An update listener to notify as the enter animation progresses (used for the home transition)
        ValueAnimator.AnimatorUpdateListener updateListener;

        // These following properties are updated for each task view we start the enter animation on

        // Whether or not the current task occludes the launch target
        boolean currentTaskOccludesLaunchTarget;
        // The task rect for the current stack
        Rect currentTaskRect;
        // The transform of the current task view
        OverviewCardTransform currentTaskTransform;
        // The view index of the current task view
        int currentStackViewIndex;
        // The total number of task views
        int currentStackViewCount;

        public OverviewCardEnterContext(ReferenceCountedTrigger t) {
            postAnimationTrigger = t;
        }
    }

    /* The animation context for a task view animation out of Recents */
    public static class OverviewCardExitContext {
        // A trigger to run some logic when all the animations complete.  This works around the fact
        // that it is difficult to coordinate ViewPropertyAnimators
        ReferenceCountedTrigger postAnimationTrigger;

        // The translationY to apply to a TaskView to move it off the bottom of the task stack
        int offscreenTranslationY;

        public OverviewCardExitContext(ReferenceCountedTrigger t) {
            postAnimationTrigger = t;
        }
    }

}
