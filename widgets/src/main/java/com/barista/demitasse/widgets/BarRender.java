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
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

/**
 * <p>
 * The Bar Render provides a visualization of the magnitude of a frequency at a
 * particular frequency bucket.<br/>
 * The view is to be contained within the {@link com.barista.demitasse.widgets.AudioVisualizer} ViewGroup.
 * The frequency is visualized as a vertical bar that changes height according to
 * the magnitude of the frequency. The peak frequency can be visualized if the
 * attribute {@link #showPeak} is set to true. The default disables the peak visualization.
 * </p>
 */
public class BarRender extends View implements Render {
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
     * Defines how long the peak bar should remain at it's peak position
     * before dropping.
     */
    private static final int PEAK_BAR_ANIMATION_DELAY = 250;

    /**
     * Defines how quickly the peak bar will accelerate in the downward
     * direction. A smaller value will decrease the rate of acceleration.
     */
    private static final float PEAK_BAR_ACCELERATION = 0.025f;

    /**
     * Indicates that the direction of travel for the bar is either
     * undefined or static.
     */
    private static final int DIRECTION_NONE = 0;
    /**
     * Indicates that the direction of travel for the bar is up.
     */
    private static final int DIRECTION_UP = 1;
    /**
     * Indicates that the direction of travel for the bar is down.
     */
    private static final int DIRECTION_DOWN = 2;

    private long animationStart = 0;

    private int currentPeak = 0;

    private int currentAmplitude = 0;
    private int targetAmplitude = 0;

    private int width;
    private int height;

    private int direction;
    private int delta;

    private final int peakBarHeight;
    private final int peakBarSpace;
    private final boolean showPeak;

    private final Paint barPaint;
    private final Paint peakPaint;

    private Rect peakBar;

    public BarRender(Context context) {
        this(context, null);
    }

    public BarRender(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BarRender(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final Resources resources = context.getResources();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Render, defStyleAttr, 0);

        barPaint = new Paint();
        final int defaultBarColor = resources.getColor(R.color.analyzer_primary_color);
        barPaint.setColor(a.getColor(R.styleable.Render_primaryColor, defaultBarColor));

        peakPaint = new Paint();
        final int defaultPeakColor = resources.getColor(R.color.analyzer_secondary_color);
        peakPaint.setColor(a.getColor(R.styleable.Render_secondaryColor, defaultPeakColor));

        a.recycle();

        a = context.obtainStyledAttributes(attrs, R.styleable.Render_BarRender, defStyleAttr, 0);

        final int defaultPeakBarHeight = resources.getDimensionPixelSize(R.dimen.bar_render_peak_bar_height);
        peakBarHeight = a.getDimensionPixelSize(R.styleable.Render_BarRender_peakBarHeight, defaultPeakBarHeight);

        final int defaultPeakBarSpace = resources.getDimensionPixelSize(R.dimen.bar_render_peak_bar_space);
        peakBarSpace = a.getDimensionPixelSize(R.styleable.Render_BarRender_peakBarSpace, defaultPeakBarSpace);

        showPeak = a.getBoolean(R.styleable.Render_BarRender_showPeakBar, false);

        a.recycle();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;

        peakBar = null;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        width = getMeasuredWidth();
        height = getMeasuredHeight();
    }

    /**
     * Returns true if the bar is currently animating and false otherwise.
     * @return True if animating and false otherwise.
     */
    public boolean isAnimating() {
        return currentAmplitude != targetAmplitude;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (peakBar == null) {
            peakBar = new Rect(0, 0, width, peakBarHeight);
        }

        if (!isAnimating()) {
            return;
        }

        drawBar(canvas);

        if (showPeak) {
            drawPeak(canvas);
        }
    }

    private void drawBar(Canvas canvas) {
        if (currentAmplitude == targetAmplitude) {
            return;
        }

        switch (direction) {
            case DIRECTION_DOWN:
                currentAmplitude = Math.max(Math.min(currentAmplitude + delta, currentPeak + peakBarSpace), targetAmplitude);
                break;
            case DIRECTION_UP:
                currentAmplitude = Math.min(currentAmplitude + delta, targetAmplitude);
                break;
        }

        if (currentAmplitude > 0) {
            canvas.drawRect(0, Math.max(height - currentAmplitude, 0), width, height, barPaint);
        }
    }

    private void drawPeak(Canvas canvas) {
        final int maxPeak = currentAmplitude + peakBarSpace;
        if (currentPeak > maxPeak && SystemClock.uptimeMillis() > animationStart) {
            int delta = (int) ((SystemClock.uptimeMillis() - animationStart) * PEAK_BAR_ACCELERATION);
            currentPeak = Math.max(currentPeak - delta, maxPeak);
        } else if (currentPeak < maxPeak) {
            currentPeak = maxPeak;
        }

        canvas.save();
        canvas.translate(0, Math.max(height - currentPeak - peakBar.height(), 0));
        canvas.drawRect(peakBar, peakPaint);
        canvas.restore();
    }

    /**
     * Set the target amplitude for the bar to animate to.
     * @param amplitude The target amplitude.
     */
    @Override
    public void setAmplitude(int amplitude) {
        if (amplitude < 0) {
            amplitude = 0;
        }

        direction = getDirection(amplitude, currentAmplitude);

        if (direction == DIRECTION_NONE) {
            return;
        }

        targetAmplitude = amplitude;

        if (showPeak) {
            int nextPeak = calculatePeak(targetAmplitude, currentAmplitude);

            if (currentPeak < nextPeak) {
                currentPeak = nextPeak;
                animationStart = SystemClock.uptimeMillis() + PEAK_BAR_ANIMATION_DELAY;
            }
        }

        delta = (int) calculateBarVelocity(targetAmplitude, currentAmplitude);
    }

    private int getDirection(int targetAmplitude, int currentAmplitude) {
        if (currentAmplitude == targetAmplitude) {
            return DIRECTION_NONE;
        }

        return currentAmplitude < targetAmplitude ? DIRECTION_UP : DIRECTION_DOWN;
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

    private int calculatePeak(int targetAmplitude, int currentAmplitude) {
        return (int) (targetAmplitude - (targetAmplitude - currentAmplitude) / 2.75f);
    }


    /**
     * Returns the current amplitude as rendered by the visualizer.
     */
    @Override
    public int getAmplitude() {
        return currentAmplitude;
    }
}
