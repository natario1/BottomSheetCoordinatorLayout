package com.otaliastudios.bottomsheetcoordinatorlayout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

/**
 * This {@code Behavior} allows {@link com.google.android.material.appbar.AppBarLayout} to accept drag events.
 * <p>
 * When {@code AppBarLayout} stands inside a {@link com.google.android.material.bottomsheet.BottomSheetBehavior},
 * all touch events are stolen by the behavior, unless they focus on the nested scrolling child
 * (which the app bar isn't). (see BottomSheetBehavior.onInterceptTouchEvent, last line).
 * <p>
 * Since no event goes to app bar, its own {@link com.google.android.material.appbar.AppBarLayout.Behavior}
 * cannot control drags on the view itself. We have to take care of this.
 * <p>
 * A simple implementation for this touch policy could be just returning false when sheet is expanded,
 * so that {@code AppBarLayout} can catch the event.
 * Unfortunately, this is not enough, because if sheet is expanded **and** app bar is expanded
 * **and** the finger goes down, we want this {@code Behavior} to actually intercept.
 */
public class BottomSheetCoordinatorBehavior extends BottomSheetInsetsBehavior<BottomSheetCoordinatorLayout> {

    private boolean fingerDown;
    private float lastY;

    public BottomSheetCoordinatorBehavior() {
    }

    public BottomSheetCoordinatorBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public static BottomSheetCoordinatorBehavior from(BottomSheetCoordinatorLayout view) {
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) view.getLayoutParams();
        return (BottomSheetCoordinatorBehavior) view.getBehavior();
    }

    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, BottomSheetCoordinatorLayout sheet, MotionEvent event) {
        if (!sheet.hasAppBar()) return super.onTouchEvent(parent, sheet, event);

        // If the touch is not on the sheet, we don't care.
        if (!parent.isPointInChildBounds(sheet, (int) event.getX(), (int) event.getY())) {
            return super.onTouchEvent(parent, sheet, event);
        }
        updateDirection(event);
        if ((sheet.getState() == BottomSheetCoordinatorBehavior.STATE_HALF_EXPANDED || sheet.getState() == BottomSheetCoordinatorBehavior.STATE_EXPANDED) && !sheet.canScrollUp() && !fingerDown) {
            // Release this. Doesn't work well because BottomSheetBehavior keeps being STATE_DRAGGING
            // even when we reached full height, as long as we keep the finger there.
            return false;
        }
        return super.onTouchEvent(parent, sheet, event);
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, BottomSheetCoordinatorLayout sheet, MotionEvent event) {
        if (!sheet.hasAppBar()) return super.onInterceptTouchEvent(parent, sheet, event);

        // If the touch is not on the sheet, we don't care.
        if (!parent.isPointInChildBounds(sheet, (int) event.getX(), (int) event.getY())) {
            return super.onInterceptTouchEvent(parent, sheet, event);
        }

        updateDirection(event);
        if (sheet.getState() == BottomSheetCoordinatorBehavior.STATE_EXPANDED || sheet.getState() == BottomSheetCoordinatorBehavior.STATE_HALF_EXPANDED) {
            // If finger is going down and
            if (!sheet.canScrollUp()) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    // Pass to both, we don't know yet what will happen.
                    super.onInterceptTouchEvent(parent, sheet, event);
                    return false;
                    // return false;
                } else if (fingerDown) {
                    // Not a DOWN and finger is going down. Intercept.
                    return super.onInterceptTouchEvent(parent, sheet, event);
                } else {
                    // Not a DOWN and finger is going up. propagate to ABL.
                    return false;
                }
            } else {
                // Expanded, but ABL is not expanded. It should catch events.
                return false;
            }
        } else {
            // Sheet is not expanded. It should catch events.
            return super.onInterceptTouchEvent(parent, sheet, event);
        }
    }

    private void updateDirection(MotionEvent event) {
        if (lastY == 0) {
            lastY = event.getY();
            fingerDown = false;
            return;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                fingerDown = false;
                lastY = event.getY();
                return;
            case MotionEvent.ACTION_MOVE:
                float newY = event.getY();
                fingerDown = newY > lastY;
                lastY = newY;
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                fingerDown = false;
                lastY = 0;
                break;
        }
    }
}
