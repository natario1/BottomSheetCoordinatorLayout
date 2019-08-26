package com.otaliastudios.bottomsheetcoordinatorlayout;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.os.ParcelableCompat;
import androidx.core.os.ParcelableCompatCreatorCallbacks;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.customview.view.AbsSavedState;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.lang.reflect.Field;

/**
 * This:
 * - tries to fix the hideable bug
 * - ensures that any inset is passed to our bottom sheet view before it is consumed by some other.
 *
 * @param <V> bottom sheet root view, typically {@link BottomSheetCoordinatorLayout}
 */
// link to bug https://code.google.com/p/android/issues/detail?id=207191&thanks=207191&ts=1460894786
public class BottomSheetInsetsBehavior<V extends View> extends BottomSheetBehavior<V> {

    public BottomSheetInsetsBehavior() {
    }

    public BottomSheetInsetsBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @NonNull
    @Override
    public WindowInsetsCompat onApplyWindowInsets(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull WindowInsetsCompat insets) {
        // Steal the inset and dispatch to view.
        ViewCompat.dispatchApplyWindowInsets(child, insets);
        DebugExtensions.log(this, "onApplyWindowInsets " + insets);
        // Pass unconsumed insets.
        return super.onApplyWindowInsets(coordinatorLayout, child, insets);
    }

    @Override
    public Parcelable onSaveInstanceState(CoordinatorLayout parent, V child) {
        if (getState() == STATE_SETTLING) {
            try {
                Field f = BottomSheetBehavior.class.getDeclaredField("mState");
                f.setAccessible(true);
                f.setInt(BottomSheetInsetsBehavior.this, STATE_HIDDEN);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new SavedState(super.onSaveInstanceState(parent, child), isHideable());
    }

    @Override
    public void onRestoreInstanceState(CoordinatorLayout parent, V child, Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(parent, child, ss.getSuperState());
        setHideable(ss.hideable);
    }


    public static class SavedState extends AbsSavedState {

        boolean hideable;

        public SavedState(Parcel source, ClassLoader classLoader) {
            super(source, classLoader);
            hideable = source.readInt() == 1;
        }

        public SavedState(Parcelable superState, boolean hideable) {
            super(superState);
            this.hideable = hideable;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(hideable ? 1 : 0);
        }

        public static final Creator<SavedState> CREATOR = ParcelableCompat.newCreator(
                new ParcelableCompatCreatorCallbacks<SavedState>() {

                    @Override
                    public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                        return new SavedState(in, loader);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                });
    }
}
