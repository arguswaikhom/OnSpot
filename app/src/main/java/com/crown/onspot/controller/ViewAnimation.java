package com.crown.onspot.controller;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Transformation;

public class ViewAnimation {

    public static void expand(View view, final AnimListener animListener) {
        Animation expandAction = expandAction(view);
        expandAction.setAnimationListener(new AnimationListener() {
            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                animListener.onFinish();
            }
        });
        view.startAnimation(expandAction);
    }

    public static void expand(View view) {
        view.startAnimation(expandAction(view));
    }

    private static Animation expandAction(final View view) {
        view.measure(-1, -2);
        final int measuredHeight = view.getMeasuredHeight();
        view.getLayoutParams().height = 0;
        view.setVisibility(View.VISIBLE);
        Animation r1 = new Animation() {
            public boolean willChangeBounds() {
                return true;
            }

            /* access modifiers changed from: protected */
            public void applyTransformation(float f, Transformation transformation) {
                view.getLayoutParams().height = f == 1.0f ? -2 : (int) (((float) measuredHeight) * f);
                view.requestLayout();
            }
        };
        r1.setDuration((int) (((float) measuredHeight) / view.getContext().getResources().getDisplayMetrics().density));
        view.startAnimation(r1);
        return r1;
    }

    public static void collapse(final View view) {
        final int measuredHeight = view.getMeasuredHeight();
        Animation r1 = new Animation() {
            public boolean willChangeBounds() {
                return true;
            }

            /* access modifiers changed from: protected */
            public void applyTransformation(float f, Transformation transformation) {
                if (f == 1.0f) {
                    view.setVisibility(View.GONE);
                    return;
                }
                LayoutParams layoutParams = view.getLayoutParams();
                int i = measuredHeight;
                layoutParams.height = i - ((int) (((float) i) * f));
                view.requestLayout();
            }
        };
        r1.setDuration((int) (((float) measuredHeight) / view.getContext().getResources().getDisplayMetrics().density));
        view.startAnimation(r1);
    }

    public static void flyInDown(View view, final AnimListener animListener) {
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0.0f);
        view.setTranslationY(0.0f);
        view.setTranslationY((float) (-view.getHeight()));
        view.animate().setDuration(200).translationY(0.0f).setListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                // AnimListener animListener = animListener;
                if (animListener != null) {
                    animListener.onFinish();
                }
                super.onAnimationEnd(animator);
            }
        }).alpha(1.0f).start();
    }

    public static void flyOutDown(View view, final AnimListener animListener) {
        view.setVisibility(View.GONE);
        view.setAlpha(1.0f);
        view.setTranslationY(0.0f);
        view.animate().setDuration(200).translationY((float) view.getHeight()).setListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                // AnimListener animListener = animListener;
                if (animListener != null) {
                    animListener.onFinish();
                }
                super.onAnimationEnd(animator);
            }
        }).alpha(0.0f).start();
    }

    public static void fadeIn(View view) {
        fadeIn(view, null);
    }

    public static void fadeIn(final View view, final AnimListener animListener) {
        view.setVisibility(View.GONE);
        view.setAlpha(0.0f);
        view.animate().setDuration(200).setListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                view.setVisibility(View.VISIBLE);
                //AnimListener animListener = animListener;
                if (animListener != null) {
                    animListener.onFinish();
                }
                super.onAnimationEnd(animator);
            }
        }).alpha(1.0f);
    }

    public static void fadeOut(View view) {
        fadeOut(view, null);
    }

    public static void fadeOut(View view, final AnimListener animListener) {
        view.setAlpha(1.0f);
        view.animate().setDuration(500).setListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                //AnimListener animListener = animListener;
                if (animListener != null) {
                    animListener.onFinish();
                }
                super.onAnimationEnd(animator);
            }
        }).alpha(0.0f);
    }

    public static void showIn(View view) {
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0.0f);
        view.setTranslationY((float) view.getHeight());
        view.animate().setDuration(200).translationY(0.0f).setListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);
            }
        }).alpha(1.0f).start();
    }

    public static void initShowOut(View view) {
        view.setVisibility(View.GONE);
        view.setTranslationY((float) view.getHeight());
        view.setAlpha(0.0f);
    }

    public static void showOut(final View view) {
        view.setVisibility(View.VISIBLE);
        view.setAlpha(1.0f);
        view.setTranslationY(0.0f);
        view.animate().setDuration(200).translationY((float) view.getHeight()).setListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                view.setVisibility(View.GONE);
                super.onAnimationEnd(animator);
            }
        }).alpha(0.0f).start();
    }

    public static boolean rotateFab(View view, boolean z) {
        view.animate().setDuration(200).setListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);
            }
        }).rotation(z ? 135.0f : 0.0f);
        return z;
    }

    public static void fadeOutIn(View view) {
        view.setAlpha(0.0f);
        AnimatorSet animatorSet = new AnimatorSet();
        String str = "alpha";
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(view, str, 0.0f, 0.5f, 1.0f);
        ObjectAnimator.ofFloat(view, str, 0.0f).start();
        ofFloat.setDuration(500);
        animatorSet.play(ofFloat);
        animatorSet.start();
    }

    public static void showScale(View view) {
        showScale(view, null);
    }

    public static void showScale(View view, final AnimListener animListener) {
        view.animate().scaleY(1.0f).scaleX(1.0f).setDuration(200).setListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                //AnimListener animListener = animListener;
                if (animListener != null) {
                    animListener.onFinish();
                }
                super.onAnimationEnd(animator);
            }
        }).start();
    }

    public static void hideScale(View view) {
        fadeOut(view, null);
    }

    public static void hideScale(View view, final AnimListener animListener) {
        view.animate().scaleY(0.0f).scaleX(0.0f).setDuration(200).setListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                //AnimListener animListener = animListener;
                if (animListener != null) {
                    animListener.onFinish();
                }
                super.onAnimationEnd(animator);
            }
        }).start();
    }

    public static void hideFab(View view) {
        view.animate().translationY((float) (view.getHeight() * 2)).setDuration(300).start();
    }

    public static void showFab(View view) {
        view.animate().translationY(0.0f).setDuration(300).start();
    }

    public interface AnimListener {
        void onFinish();
    }
}
