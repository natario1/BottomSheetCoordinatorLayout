package com.otaliastudios.bottomsheetcoordinatorlayout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

/**
 * A hacky {@link CoordinatorLayout} that can act as a bottom sheet through
 * {@link BottomSheetBehavior}.
 * <p>
 * This works by *not* reinventing the wheel and reusing the same nested scrolling logic implemented
 * by behaviors. There is a dummy view inside the sheet that is capable of getting nested scrolling
 * callbacks, and forward them to the *outer* behavior that they normally would never reach.
 * <p>
 * Default behavior is {@link BottomSheetCoordinatorBehavior}, Which includes some workarounds
 * for window insets and app bar layout dragging.
 */
public class BottomSheetCoordinatorLayout extends CoordinatorLayout implements AppBarLayout.OnOffsetChangedListener, CoordinatorLayout.AttachedBehavior {

    private BottomSheetCoordinatorBehavior bottomSheetBehavior;

    private BottomSheetBehavior.BottomSheetCallback appBarBottomSheetCallback;

    private BottomSheetBehavior.BottomSheetCallback delayedBottomSheetCallback;

    private BottomSheetBehavior.BottomSheetCallback bottomSheetCallback = new BottomSheetBehavior.BottomSheetCallback() {

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {

            if (appBarBottomSheetCallback != null)
                appBarBottomSheetCallback.onStateChanged(bottomSheet, newState);

            if (delayedBottomSheetCallback != null)
                delayedBottomSheetCallback.onStateChanged(bottomSheet, newState);
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            if (appBarBottomSheetCallback != null)
                appBarBottomSheetCallback.onSlide(bottomSheet, slideOffset);

            if (delayedBottomSheetCallback != null)
                delayedBottomSheetCallback.onSlide(bottomSheet, slideOffset);
        }
    };

    private Boolean delayedHideable;
    private Boolean delayedSkipCollapsed;
    private Integer delayedState;
    private Integer delayedPeekHeight;
    private Boolean delayedFitToContents;
    private Integer delayedExpandedOffset;
    private AppBarLayout.Behavior appBarBehavior;
    private int appBarOffset = 0;
    private boolean hasAppBar = false;

    public BottomSheetCoordinatorLayout(Context context) {
        super(context);
        init();
    }

    public BottomSheetCoordinatorLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BottomSheetCoordinatorLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Add a dummy view that will receive inner touch events.
        View dummyView = new View(getContext());
        DummyBehavior dummyBehavior = new DummyBehavior();
        // I *think* this is needed for dummyView to be identified as "topmost" and receive events
        // before any other view.
        ViewCompat.setElevation(dummyView, ViewCompat.getElevation(this));
        // Make sure it does not fit windows, or it will consume insets before the AppBarLayout.
        dummyView.setFitsSystemWindows(false);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.setBehavior(dummyBehavior);
        addView(dummyView, params);
    }

    /**
     * Based on Coordinator source, this seems to be the earliest point where we can be sure
     * that we have a behavior.
     * We might be tempted to use onAttachedToWindow but that only works if the behavior was
     * declared through XML.
     *
     * @param widthMeasureSpec  width spec
     * @param heightMeasureSpec height spec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (bottomSheetBehavior != null) return;

        // Fetch our own behavior.
        bottomSheetBehavior = (BottomSheetCoordinatorBehavior) ((LayoutParams) getLayoutParams()).getBehavior();

        bottomSheetBehavior.setBottomSheetCallback(bottomSheetCallback);

        if (delayedSkipCollapsed != null) {
            bottomSheetBehavior.setSkipCollapsed(delayedSkipCollapsed);
            delayedSkipCollapsed = null;
        }
        if (delayedHideable != null) {
            bottomSheetBehavior.setHideable(delayedHideable);
            delayedHideable = null;
        }
        if (delayedPeekHeight != null) {
            bottomSheetBehavior.setPeekHeight(delayedPeekHeight);
            delayedPeekHeight = null;
        }
        if (delayedFitToContents != null) {
            bottomSheetBehavior.setFitToContents(delayedFitToContents);
            delayedFitToContents = null;
        }
        if (delayedExpandedOffset != null) {
            bottomSheetBehavior.setExpandedOffset(delayedExpandedOffset);
            delayedExpandedOffset = null;
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


            // todo fix me D:
            //  somehow during expanding from half to expanded state, the appbar has the wrong parallax offset,
            //  we hammer the expanded state atm, but it's not particular good for performance
            appBarBottomSheetCallback = new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {

                    switch (newState) {
                        case BottomSheetCoordinatorBehavior.STATE_EXPANDED:
                        case BottomSheetCoordinatorBehavior.STATE_HALF_EXPANDED:
                            appBarLayout.setExpanded(true, true);
                            break;
                        case BottomSheetCoordinatorBehavior.STATE_COLLAPSED:
                        case BottomSheetCoordinatorBehavior.STATE_DRAGGING:
                        case BottomSheetCoordinatorBehavior.STATE_SETTLING:
                            appBarLayout.setExpanded(true, false);
                            break;
                        case BottomSheetBehavior.STATE_HIDDEN:
                            break;
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    DebugExtensions.log(this, "onSlide slideOffset=" + slideOffset);

                }
            };

            setBottomSheetCallback(delayedBottomSheetCallback);

        } else {
            hasAppBar = false;
        }
    }

    /**
     * Set a {@link com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback} callback
     * to our behavior, as soon as it is available.
     *
     * @param bottomSheetCallback desired callback.
     */
    public void setBottomSheetCallback(final BottomSheetBehavior.BottomSheetCallback bottomSheetCallback) {
        if (bottomSheetBehavior == null) {
            delayedBottomSheetCallback = bottomSheetCallback;
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
     * Set's peek height.
     *
     * @param peekHeight Peek Height
     */
    public void setPeekHeight(int peekHeight) {
        if (delayedPeekHeight == null) {
            delayedPeekHeight = peekHeight;
        } else {
            bottomSheetBehavior.setState(peekHeight);
        }
    }

    /**
     * Sets whether the height of the expanded sheet is determined by the height of its contents,
     * or if it is expanded in two stages (half the height of the parent container,
     * full height of parent container). Default value is true
     *
     * @param fitToContents – whether or not to fit the expanded sheet to its contents.
     */
    public void setFitContents(boolean fitToContents) {
        if (delayedFitToContents == null) {
            delayedFitToContents = fitToContents;
        } else {
            bottomSheetBehavior.setFitToContents(fitToContents);
        }
    }

    /**
     * Determines the top offset of the BottomSheet in the STATE_EXPANDED state when fitsToContent is false. The default value is 0, which results in the sheet matching the parent's top.
     *
     * @param offset – an integer value greater than equal to 0, representing the STATE_EXPANDED offset. Value must not exceed the offset in the half expanded state.
     */
    public void setExpandedOffset(int offset) {
        if (delayedExpandedOffset == null) {
            delayedExpandedOffset = offset;
        } else {
            bottomSheetBehavior.setExpandedOffset(offset);
        }
    }

    /**
     * Returns our behavior, if available.
     *
     * @return our behavior, or null if not available yet.
     */
    @Nullable
    public BottomSheetBehavior<BottomSheetCoordinatorLayout> getBehavior() {
        return (BottomSheetCoordinatorBehavior) ((LayoutParams) getLayoutParams()).getBehavior();
    }

    /**
     * Returns an {@link AppBarLayout} if present.
     *
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

        DebugExtensions.log(this, "onOffsetChanged=" + verticalOffset);

        if (verticalOffset == appBarOffset) {
            // Do nothing

            DebugExtensions.log(this, "onOffsetChanged=" + verticalOffset + " -> do nothing");

        } else if (bottomSheetBehavior.getState() != BottomSheetCoordinatorBehavior.STATE_EXPANDED) {
            // We are trying to set a new offset, but it shouldn't change because the sheet
            // is not expanded. Let's get back to old offset.
            appBarBehavior.setTopAndBottomOffset(appBarOffset);

            DebugExtensions.log(this, "onOffsetChanged=" + verticalOffset + " -> appBarOffset=" + appBarOffset);
        } else {
            // we are trying to set a new offset, and sheet is expanded. Keep track of it.
            appBarOffset = verticalOffset;
            DebugExtensions.log(this, "onOffsetChanged=" + verticalOffset + " = appBarOffset=" + appBarOffset);
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
     * <p>
     * It has to be done manually because nested scrolls/fling events are coordinated by coordinator
     * layouts. If the bottom sheet view does not support nested scrolling (and CoordinatorLayout doesn't)
     * any event that takes place inside the sheet is confined to sheet itself.
     * This way inner events never reach the BottomSheetBehavior.
     * <p>
     * Another option would be to make a CoordinatorLayout class that actually implements
     * NestedScrollingChild and thus propagates events to the outer CoordinatorLayout: this, then,
     * could propagate the events to BottomSheetBehavior. I was not able to make that work.
     *
     * @param <DummyView> make sure it's not a nested-scrolling-enabled view or this will break.
     */
    private static class DummyBehavior<DummyView extends View> extends CoordinatorLayout.Behavior<DummyView> {

        public DummyBehavior() {
        }

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
                case BottomSheetCoordinatorBehavior.STATE_HALF_EXPANDED:
                case BottomSheetCoordinatorBehavior.STATE_COLLAPSED:
                case BottomSheetCoordinatorBehavior.STATE_DRAGGING:
                case BottomSheetCoordinatorBehavior.STATE_SETTLING:
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull DummyView child, @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
            BottomSheetCoordinatorLayout sheet = (BottomSheetCoordinatorLayout) coordinatorLayout;
            if (shouldForwardEvent(sheet, false)) {
                return sheet.getBehavior().onStartNestedScroll(coordinatorLayout, sheet, directTargetChild, target, axes, type);
            } else {
                return false; // super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, axes, type);
            }
        }

        @Override
        public void onNestedPreScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull DummyView child, @NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
            // super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type);

            // When moving the finger up, dy is > 0.
            BottomSheetCoordinatorLayout sheet = (BottomSheetCoordinatorLayout) coordinatorLayout;
            if (shouldForwardEvent(sheet, dy > 0)) {
                sheet.getBehavior().onNestedPreScroll(coordinatorLayout, sheet, target, dx, dy, consumed, type);
            }
        }

        @Override
        public void onStopNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull DummyView child, @NonNull View target, int type) {
            // super.onStopNestedScroll(coordinatorLayout, child, target, type);

            BottomSheetCoordinatorLayout sheet = (BottomSheetCoordinatorLayout) coordinatorLayout;
            if (shouldForwardEvent(sheet, false)) {
                sheet.getBehavior().onStopNestedScroll(coordinatorLayout, sheet, target, type);
            }
        }

        @Override
        public boolean onNestedPreFling(@NonNull CoordinatorLayout coordinatorLayout, @NonNull DummyView child, @NonNull View target, float velocityX, float velocityY) {
            BottomSheetCoordinatorLayout sheet = (BottomSheetCoordinatorLayout) coordinatorLayout;
            if (shouldForwardEvent(sheet, velocityY > 0)) {
                return sheet.getBehavior().onNestedPreFling(coordinatorLayout, sheet, target, velocityX, velocityY);
            } else {
                return false; // super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY);
            }
        }
    }
}
