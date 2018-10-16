package com.otaliastudios.bottomsheetcoordinatorlayout;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.BottomSheetBehavior;import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;

/**
 * A hacky {@link CoordinatorLayout} that can act as a bottom sheet through
 * {@link BottomSheetBehavior}.
 *
 * This works by *not* reinventing the wheel and reusing the same nested scrolling logic implemented
 * by behaviors. There is a dummy view inside the sheet that is capable of getting nested scrolling
 * callbacks, and forward them to the *outer* behavior that they normally would never reach.
 *
 * Default behavior is {@link BottomSheetCoordinatorBehavior}, Which includes some workarounds
 * for window insets and app bar layout dragging.
 */
@CoordinatorLayout.DefaultBehavior(BottomSheetCoordinatorBehavior.class)
public class BottomSheetCoordinatorLayout extends CoordinatorLayout implements
        AppBarLayout.OnOffsetChangedListener {

    private static final String TAG = BottomSheetCoordinatorLayout.class.getSimpleName();

    private BottomSheetCoordinatorBehavior bottomSheetBehavior;
    private BottomSheetBehavior.BottomSheetCallback delayedBottomSheetCallback;
    private Boolean delayedHideable;
    private Boolean delayedSkipCollapsed;
    private Integer delayedState;
    private AppBarLayout.Behavior appBarBehavior;
    private int appBarOffset = 0;
    private boolean hasAppBar = false;

    public BottomSheetCoordinatorLayout(Context context) {
        super(context); i();
    }

    public BottomSheetCoordinatorLayout(Context context, AttributeSet attrs) {
        super(context, attrs); i();
    }

    public BottomSheetCoordinatorLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); i();
    }

    private void i() {
        // Add a dummy view that will receive inner touch events.
        View dummyView = new View(getContext());
        DummyBehavior dummyBehavior = new DummyBehavior();
        // I *think* this is needed for dummyView to be identified as "topmost" and receive events
        // before any other view.
        ViewCompat.setElevation(dummyView, ViewCompat.getElevation(this));
        // Make sure it does not fit windows, or it will consume insets before the AppBarLayout.
        dummyView.setFitsSystemWindows(false);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        params.setBehavior(dummyBehavior);
        addView(dummyView, params);
    }

    /**
     * Based on Coordinator source, this seems to be the earliest point where we can be sure
     * that we have a behavior.
     * We might be tempted to use onAttachedToWindow but that only works if the behavior was
     * declared through XML.
     *
     * @param widthMeasureSpec width spec
     * @param heightMeasureSpec height spec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (bottomSheetBehavior != null) return;

        // Fetch our own behavior.
        bottomSheetBehavior = BottomSheetCoordinatorBehavior.from(BottomSheetCoordinatorLayout.this);
        if (delayedBottomSheetCallback != null) {
            bottomSheetBehavior.setBottomSheetCallback(delayedBottomSheetCallback);
            delayedBottomSheetCallback = null;
        }
        if (delayedSkipCollapsed != null) {
            bottomSheetBehavior.setSkipCollapsed(delayedSkipCollapsed);
            delayedSkipCollapsed = null;
        }
        if (delayedHideable != null) {
            bottomSheetBehavior.setHideable(delayedHideable);
            delayedHideable = null;
        }
        if (delayedState != null) { // This must be the last.
            bottomSheetBehavior.setState(delayedState);
            delayedState = null;
        }

        // Store AppBar's Behavior, and allow drag events on it.
        AppBarLayout appBarLayout = findAppBar();
        if (appBarLayout != null) {
            appBarLayout.addOnOffsetChangedListener(this);
            appBarBehavior = (AppBarLayout.Behavior) ((LayoutParams) appBarLayout.getLayoutParams()).getBehavior();
            appBarBehavior.setDragCallback(new AppBarLayout.Behavior.DragCallback() {
                @Override
                public boolean canDrag(@NonNull AppBarLayout appBarLayout) {
                    return true;
                }
            });
            hasAppBar = true;
        } else {
            hasAppBar = false;
        }
    }

    /**
     * Set a {@link android.support.design.widget.BottomSheetBehavior.BottomSheetCallback} callback
     * to our behavior, as soon as it is available.
     *
     * @param bottomSheetCallback desired callback.
     */
    public void setBottomSheetCallback(final BottomSheetBehavior.BottomSheetCallback bottomSheetCallback) {
        if (bottomSheetBehavior == null) {
            delayedBottomSheetCallback = bottomSheetCallback;
        } else {
            bottomSheetBehavior.setBottomSheetCallback(bottomSheetCallback);
        }
    }

    /**
     * Set the hideable flag to our behavior, as soon as it is available.
     *
     * @param hideable whether it will be hideable
     */
    public void setHideable(boolean hideable) {
        if (bottomSheetBehavior == null) {
            delayedHideable = hideable;
        } else {
            bottomSheetBehavior.setHideable(hideable);
        }
    }

    /**
     * Set the skipCollapsed flag to our behavior, as soon as it is available.
     *
     * @param skipCollapsed whether to skip the collapsed state
     */
    public void setSkipCollapsed(boolean skipCollapsed) {
        if (bottomSheetBehavior == null) {
            delayedSkipCollapsed = skipCollapsed;
        } else {
            bottomSheetBehavior.setSkipCollapsed(skipCollapsed);
        }
    }

    /**
     * Set the state to our behavior, as soon as it is available.
     *
     * @param state the new state
     */
    public void setState(int state) {
        if (bottomSheetBehavior == null) {
            delayedState = state;
        } else {
            bottomSheetBehavior.setState(state);
        }
    }

    /**
     * Returns our behavior, if available.
     * @return our behavior, or null if not available yet.
     */
    @Nullable
    public BottomSheetBehavior<BottomSheetCoordinatorLayout> getBehavior() {
        return bottomSheetBehavior;
    }

    /**
     * Returns an {@link AppBarLayout} if present.
     * @return the first available AppBarLayout, or null.
     */
    @Nullable
    public AppBarLayout findAppBar() {
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (v instanceof AppBarLayout) {
                return (AppBarLayout) v;
            }
        }
        return null;
    }

    /**
     * Returns the current sheet behavior state, or -1 if the behavior
     * is not available yet.
     *
     * @return the current state.
     */
    public int getState() {
        return bottomSheetBehavior != null ? bottomSheetBehavior.getState() : -1;
    }

    @Override
    public final void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        if (verticalOffset == appBarOffset) {
            // Do nothing
        } else if (bottomSheetBehavior.getState() != BottomSheetCoordinatorBehavior.STATE_EXPANDED) {
            // We are trying to set a new offset, but it shouldn't change because the sheet
            // is not expanded. Let's get back to old offset.
            appBarBehavior.setTopAndBottomOffset(appBarOffset);
        } else {
            // we are trying to set a new offset, and sheet is expanded. Keep track of it.
            appBarOffset = verticalOffset;
        }
    }

    /**
     * If we have an app bar, we can simply use the appBarOffset.
     * If we have no app bar... TODO
     */
    boolean canScrollUp() {
        if (hasAppBar) {
            return appBarOffset != 0;
        } else {
            return true;
        }
    }

    boolean hasAppBar() {
        return hasAppBar;
    }

    /**
     * This behavior is assigned to our dummy, MATCH_PARENT view inside this bottom sheet layout.
     * Through this behavior the dummy view can listen to touch/scroll events.
     * Our goal is to propagate them to the sheet behavior (the BottomSheetBehavior
     * that controls the bottom sheet position),
     *
     * It has to be done manually because nested scrolls/fling events are coordinated by coordinator
     * layouts. If the bottom sheet view does not support nested scrolling (and CoordinatorLayout doesn't)
     * any event that takes place inside the sheet is confined to sheet itself.
     * This way inner events never reach the BottomSheetBehavior.
     *
     * Another option would be to make a CoordinatorLayout class that actually implements
     * NestedScrollingChild and thus propagates events to the outer CoordinatorLayout: this, then,
     * could propagate the events to BottomSheetBehavior. I was not able to make that work.
     *
     * @param <DummyView> make sure it's not a nested-scrolling-enabled view or this will break.
     */
    private static class DummyBehavior<DummyView extends View> extends CoordinatorLayout.Behavior<DummyView> {

        public DummyBehavior() {}

        public DummyBehavior(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        private boolean shouldForwardEvent(BottomSheetCoordinatorLayout sheet, boolean fingerGoingUp) {
            int state = sheet.getState();
            switch (state) {
                case BottomSheetCoordinatorBehavior.STATE_EXPANDED:
                    // If sheet is expanded, we only want to forward if the appBar is expanded.
                    // AND the touch is going down...
                    return !sheet.canScrollUp() && !fingerGoingUp;
                case BottomSheetCoordinatorBehavior.STATE_COLLAPSED:
                case BottomSheetCoordinatorBehavior.STATE_DRAGGING:
                case BottomSheetCoordinatorBehavior.STATE_SETTLING:
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, DummyView child, View directTargetChild, View target, int nestedScrollAxes) {
            BottomSheetCoordinatorLayout sheet = (BottomSheetCoordinatorLayout) coordinatorLayout;
            if (shouldForwardEvent(sheet, false)) {
                return sheet.getBehavior().onStartNestedScroll(coordinatorLayout, sheet, directTargetChild, target, nestedScrollAxes);
            } else {
                return false;
            }
        }

        @Override
        public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, DummyView child, View target, int dx, int dy, int[] consumed) {
            // When moving the finger up, dy is > 0.
            BottomSheetCoordinatorLayout sheet = (BottomSheetCoordinatorLayout) coordinatorLayout;
            if (shouldForwardEvent(sheet, dy > 0)) {
                sheet.getBehavior().onNestedPreScroll(coordinatorLayout,
                        sheet, target, dx, dy, consumed);
            }
        }

        @Override
        public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, DummyView child, View target) {
            BottomSheetCoordinatorLayout sheet = (BottomSheetCoordinatorLayout) coordinatorLayout;
            if (shouldForwardEvent(sheet, false)) {
                sheet.getBehavior().onStopNestedScroll(coordinatorLayout, sheet, target);
            }
        }

        @Override
        public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, DummyView child, View target, float velocityX, float velocityY) {
            BottomSheetCoordinatorLayout sheet = (BottomSheetCoordinatorLayout) coordinatorLayout;
            if (shouldForwardEvent(sheet, velocityY > 0)) {
                return sheet.getBehavior().onNestedPreFling(coordinatorLayout, sheet, target, velocityX, velocityY);
            } else {
                return false;
            }
        }
    }
}
