package com.solomonj.ipynbviewer;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.Window;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Utilities {

    public void setupEdgeToEdgeWithConditionalPadding(Activity activity, final View targetView, final boolean applyLeftPadding, final boolean applyTopPadding,
                                                      final boolean applyRightPadding, final boolean applyBottomPadding){
        final int initialPaddingLeft = targetView.getPaddingLeft();
        final int initialPaddingTop = targetView.getPaddingTop();
        final int initialPaddingRight = targetView.getPaddingRight();
        final int initialPaddingBottom = targetView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(targetView, (v, windowInsets) -> {
            Insets insets = Insets.max(
                    windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()),
                    windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
            );

            int finalPaddingLeft = initialPaddingLeft + (applyLeftPadding ? insets.left : 0);
            int finalPaddingTop = initialPaddingTop + (applyTopPadding ? insets.top : 0);
            int finalPaddingRight = initialPaddingRight + (applyRightPadding ? insets.right : 0);
            int finalPaddingBottom = initialPaddingBottom + (applyBottomPadding ? insets.bottom : 0);

            // Apply the calculated padding to the view.
            v.setPadding(finalPaddingLeft, finalPaddingTop, finalPaddingRight, finalPaddingBottom);

            return windowInsets;
        });

        if (targetView.isAttachedToWindow()) {
            ViewCompat.requestApplyInsets(targetView);
        } else {
            targetView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    v.removeOnAttachStateChangeListener(this);
                    ViewCompat.requestApplyInsets(v);
                }

                @Override
                public void onViewDetachedFromWindow(View v) { }
            });
        }

        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }
    }

}
