package com.zugobite.skaflik.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Fades a loading skeleton in and out while data is on its way.
 *
 * <p>The pulse is what separates a skeleton from a broken screen: static grey
 * bars read as content that failed to load, whereas a slow fade reads as work
 * in progress.</p>
 *
 * <p>Written against a plain {@link ObjectAnimator} rather than pulling in a
 * shimmer library, because one alpha animation is the whole requirement and a
 * dependency would be the larger cost.</p>
 */
final class SkeletonPulse {

    /** How faint the skeleton goes at the bottom of the pulse. */
    private static final float DIM_ALPHA = 0.35f;

    /** One half-cycle. Slow enough to read as breathing, not flashing. */
    private static final long HALF_CYCLE_MILLIS = 700L;

    private final View skeleton;

    @Nullable
    private ObjectAnimator animator;

    SkeletonPulse(@NonNull View skeleton) {
        this.skeleton = skeleton;
    }

    /** Shows the skeleton and starts the pulse. Safe to call when running. */
    void show() {
        skeleton.setVisibility(View.VISIBLE);

        if (animator != null) {
            return;
        }
        animator = ObjectAnimator.ofFloat(skeleton, View.ALPHA, 1f, DIM_ALPHA);
        animator.setDuration(HALF_CYCLE_MILLIS);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.start();
    }

    /**
     * Hides the skeleton and stops the pulse.
     *
     * <p>The alpha is put back to 1 afterwards: the animator leaves it wherever
     * the fade had reached, which would show through if the skeleton is ever
     * shown again.</p>
     */
    void hide() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        skeleton.setAlpha(1f);
        skeleton.setVisibility(View.GONE);
    }

    /** True while the skeleton is on screen. */
    boolean isShowing() {
        return skeleton.getVisibility() == View.VISIBLE;
    }
}
