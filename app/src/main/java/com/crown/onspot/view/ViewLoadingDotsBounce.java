package com.crown.onspot.view;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.ItemTouchHelper.Callback;

public class ViewLoadingDotsBounce extends LinearLayout {
    private static final int DURATION = 500;
    private static final int OBJECT_SIZE = 3;
    private static final int POST_DIV = 6;
    boolean onLayoutReach = false;
    private ObjectAnimator[] animator;
    private GradientDrawable circle = new GradientDrawable();
    private Context context;
    private ImageView[] img;

    public ViewLoadingDotsBounce(Context context2) {
        super(context2);
    }

    public ViewLoadingDotsBounce(Context context2, AttributeSet attributeSet) {
        super(context2, attributeSet);
        this.context = context2;
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(17);
        setLayoutParams(new LayoutParams(-1, -1));
        initView();
    }

    public ViewLoadingDotsBounce(Context context2, AttributeSet attributeSet, int i) {
        super(context2, attributeSet, i);
    }

    private void initView() {
        Drawable background = getBackground();
        int color = background instanceof ColorDrawable ? ((ColorDrawable) background).getColor() : -7829368;
        setBackgroundColor(0);
        removeAllViews();
        this.img = new ImageView[3];
        this.circle.setShape(GradientDrawable.OVAL);
        this.circle.setColor(color);
        this.circle.setSize(Callback.DEFAULT_DRAG_ANIMATION_DURATION, Callback.DEFAULT_DRAG_ANIMATION_DURATION);
        LayoutParams layoutParams = new LayoutParams(0, -1);
        layoutParams.weight = 1.0f;
        LinearLayout[] linearLayoutArr = new LinearLayout[3];
        for (int i = 0; i < 3; i++) {
            linearLayoutArr[i] = new LinearLayout(this.context);
            linearLayoutArr[i].setGravity(17);
            linearLayoutArr[i].setLayoutParams(layoutParams);
            this.img[i] = new ImageView(this.context);
            this.img[i].setBackgroundDrawable(this.circle);
            linearLayoutArr[i].addView(this.img[i]);
            addView(linearLayoutArr[i]);
        }
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (!this.onLayoutReach) {
            this.onLayoutReach = true;
            LayoutParams layoutParams = new LayoutParams(getWidth() / 5, getWidth() / 5);
            for (int i5 = 0; i5 < 3; i5++) {
                this.img[i5].setLayoutParams(layoutParams);
            }
            animateView();
        }
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.animator == null) return;
        for (int i = 0; i < 3; i++) {
            if (this.animator[i].isRunning()) {
                this.animator[i].removeAllListeners();
                this.animator[i].end();
                this.animator[i].cancel();
            }
        }
    }

    private void animateView() {
        this.animator = new ObjectAnimator[3];
        for (int i = 0; i < 3; i++) {
            this.img[i].setTranslationY((float) (getHeight() / 6));
            ImageView imageView = this.img[i];
            PropertyValuesHolder ofFloat = PropertyValuesHolder.ofFloat(ImageView.TRANSLATION_Y, (float) ((-getHeight()) / 6));
            ImageView imageView2 = this.img[i];
            PropertyValuesHolder ofFloat2 = PropertyValuesHolder.ofFloat(ImageView.TRANSLATION_X, 0.0f);
            this.animator[i] = ObjectAnimator.ofPropertyValuesHolder(this.img[i], ofFloat2, ofFloat);
            this.animator[i].setRepeatCount(-1);
            this.animator[i].setRepeatMode(ValueAnimator.REVERSE);
            this.animator[i].setDuration(500);
            this.animator[i].setStartDelay(i * 166);
            this.animator[i].start();
        }
    }
}
