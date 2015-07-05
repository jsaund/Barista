/*
* Copyright (C) 2015 [Jag Saund]
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.barista.demitasse.widgets;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * <p>
 * The Rounded Bar Render provides a visualization of the magnitude of a frequency at a
 * particular frequency bucket.<br/>
 * The view is to be contained within the {@link com.barista.demitasse.widgets.AudioVisualizer} ViewGroup.
 * The frequency is visualized as a vertical bar that changes height according to
 * the magnitude of the frequency.
 * </p>
 */
public class RoundedBarRender extends View implements Render {
    /**
     * The animation tick rate. This provides us with 60 fps.
     */
    private static final int ANIMATION_DURATION = 20;

    /**
     * Defines the duration of travel for the bar to reach it's target
     * position. The timer is started when the height is set.
     */
    private static final int DURATION = 100;

    /**
     * Indicates that the bar is not animating.
     */
    private static final int ANIMATION_MODE_NONE = 0;
    /**
     * Indicates that the bar should grow or expand.
     */
    private static final int ANIMATION_MODE_GROW = 1;
    /**
     * Indicates that the bar should shrink.
     */
    private static final int ANIMATION_MODE_SHRINK = 2;

    private int currentAmplitude = 0;
    private int targetAmplitude = 0;

    private int width;
    private int height;

    private int animationMode;
    private int velocity;

    private final Paint linePaint;

    public RoundedBarRender(Context context) {
        this(context, null);
    }

    public RoundedBarRender(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundedBarRender(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final Resources resources = context.getResources();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Render, defStyleAttr, 0);

        linePaint = new Paint();
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        final int defaultColor = resources.getColor(R.color.analyzer_primary_color);
        linePaint.setColor(a.getColor(R.styleable.Render_primaryColor, defaultColor));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;

        linePaint.setStrokeWidth(width);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        width = getMeasuredWidth();
        height = getMeasuredHeight();

        linePaint.setStrokeWidth(width);
    }

    public boolean isAnimating() {
        return currentAmplitude != targetAmplitude;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isAnimating()) {
            return;
        }

        drawBar(canvas);
    }

    private void drawBar(Canvas canvas) {
        if (currentAmplitude == targetAmplitude) {
            return;
        }

        switch (animationMode) {
            case ANIMATION_MODE_SHRINK:
                currentAmplitude = Math.max(currentAmplitude + velocity, targetAmplitude);
                break;
            case ANIMATION_MODE_GROW:
                currentAmplitude = Math.min(currentAmplitude + velocity, targetAmplitude);
                break;
        }

        if (currentAmplitude > 0) {
            final float startX = width / 2.0f;
            final float startY = (height - currentAmplitude) / 2.0f;
            final float endX = startX;
            final float endY = (height + currentAmplitude) / 2.0f;

            canvas.drawLine(startX, startY, endX, endY, linePaint);
        }
    }

    @Override
    public void setAmplitude(int amplitude) {
        if (amplitude < width / 2) {
            amplitude = width / 2;
        }

        animationMode = getAnimationMode(amplitude, currentAmplitude);

        if (animationMode == ANIMATION_MODE_NONE) {
            return;
        }

        targetAmplitude = amplitude;

        velocity = (int) calculateBarVelocity(targetAmplitude, currentAmplitude);

        invalidate();
    }

    private int getAnimationMode(int targetAmplitude, int currentAmplitude) {
        if (currentAmplitude == targetAmplitude) {
            return ANIMATION_MODE_NONE;
        }

        return currentAmplitude < targetAmplitude ? ANIMATION_MODE_GROW : ANIMATION_MODE_SHRINK;
    }

    private float calculateBarVelocity(int targetAmplitude, int currentAmplitude) {
        float velocity = (((float) (targetAmplitude - currentAmplitude)) / DURATION) * ANIMATION_DURATION;

        if (velocity < 0 && velocity > -1.0) {
            velocity = -1.0f;
        } else if (velocity > 0 && velocity < 1.0) {
            velocity = 1.0f;
        }

        return velocity;
    }

    @Override
    public int getAmplitude() {
        return currentAmplitude;
    }
}
