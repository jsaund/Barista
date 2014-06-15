/*
* Copyright (C) 2014 [Jag Saund]
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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import java.text.NumberFormat;
import java.util.Locale;


/**
 * <p>
 * The Radial Progress Indicator provides a visual indicator of progress for an operation. The
 * progress is displayed as a radial arc from [0 .. 360].<br/>
 * The component operates in 3 modes:
 * <ol>
 * <li>Timer: The application can provide a timeout duration in seconds. The radial indicator
 * will countdown from the duration to 0. A callback is triggered upon reaching 0.</li>
 * <li>Percentage: The application can update the amount of progress and the indicator will
 * advance the position radially along the track of the indicator. The overall progress is
 * displayed as a percentage from [0 .. 100] in the center of the radial indicator.</li>
 * <li>Fixed: Operates similar to Percentage with the exception that the progress value is
 * rendered as the current value. For example: if the max progress is set to 3000 and the
 * current progress is 500 then 500 will be displayed in the center (as opposed to the
 * percentage).</li>
 * </ol>
 * </p>
 */
public class RadialProgressIndicator extends View {
    private static final String TAG = RadialProgressIndicator.class.getSimpleName();
    private static final boolean DEBUG = false;

    /**
     * The radial indicator will animate the progress of the indicator from [0 .. 360] as the timer
     * duration decreases. If the application has registered a {@link com.barista.demitasse.widgets.RadialProgressIndicator.TimerListener}, then
     * upon reaching the timeout value, {@link com.barista.demitasse.widgets.RadialProgressIndicator.TimerListener#onTimeout()} will be called.<br/>
     * If the timeout value has not been set ({@link #setTimeout(int)}, then the default timeout
     * value of {@value #DEFAULT_TIMEOUT} will be used.<br/>
     * The application must call {@link #startTimer()} to start the timer and can optionally call
     * {@link #stopTimer()} to stop the timer.<br/>
     * To reset the timer the application must call {@link #resetTimer()}.
     */
    public static final int INDICATOR_STYLE_TIMER = 0;

    /**
     * The radial indicator will animate the progress of the indicator from the current progress
     * position to the new progress position. As the progress indicator animates to the new value,
     * the value of the indicator is updated as a percentage from [0 .. 100].<br/>
     * The default max progress of the indicator is {@value #DEFAULT_MAX_PROGRESS}. The application
     * can change this value by calling {@link #setMaxProgress(int)}.<br/>
     * To update the progress, the application must call {@link #setProgress(int)}.
     */
    public static final int INDICATOR_STYLE_PERCENTAGE = 1;

    /**
     * The radial indicator will animate the progress of the indicator from the current progress
     * position to the new progress position. As the progress indicator animates to the new value,
     * the value of the indicator is updated as the actual value from [0 .. MAX PROGRESS].<br/>
     * The default max progress of the indicator is {@value #DEFAULT_MAX_PROGRESS}. The application
     * can change this value by calling {@link #setMaxProgress(int)}.<br/>
     * To update the progress, the application must call {@link #setProgress(int)}.
     */
    public static final int INDICATOR_STYLE_FIXED = 2;

    /**
     * The radial indicator has an optional secondary indicator. The secondary indicator can be
     * painted in one of three locations. This location paints the secondary indicator on the inside
     * track of the primary indicator.
     */
    public static final int SECONDARY_INDICATOR_STYLE_INSIDE = 0;

    /**
     * The radial indicator has an optional secondary indicator. The secondary indicator can be
     * painted in one of three locations. This location paints the secondary indicator on the same
     * track as the primary indicator.
     */
    public static final int SECONDARY_INDICATOR_STYLE_OVERLAY = 1;

    /**
     * The radial indicator has an optional secondary indicator. The secondary indicator can be
     * painted in one of three locations. This location paints the secondary indicator on the outside
     * track of the primary indicator.
     */
    public static final int SECONDARY_INDICATOR_STYLE_OUTSIDE = 2;

    /**
     * If the radial indicator is considered as a circle positioned on a unit cartesian coordinate
     * system, then 0 degrees is considered as the point located at (0, 1). The arc is swept CW from
     * 0 degrees to 360 degrees.
     */
    private static final int ANGLE_START = -90;

    private static final int DEFAULT_INDICATOR_STYLE = INDICATOR_STYLE_TIMER;
    private static final int DEFAULT_SECONDARY_INDICATOR_STYLE = SECONDARY_INDICATOR_STYLE_OUTSIDE;
    private static final int DEFAULT_MAX_PROGRESS = 100;
    private static final int DEFAULT_TIMEOUT = 30 * 1000;
    private static final int DEFAULT_ANIMATION_TICK_DEGREES = 1;

    /**
     * The progress indicator style can be one of:<br/>
     * {@link #INDICATOR_STYLE_TIMER}<br/>
     * {@link #INDICATOR_STYLE_PERCENTAGE}<br/>
     * {@link #INDICATOR_STYLE_FIXED}<br/>
     */
    private int indicatorStyle = DEFAULT_INDICATOR_STYLE;

    /**
     * The secondary progress indicator style can be one of:<br/>
     * {@link #SECONDARY_INDICATOR_STYLE_INSIDE}<br/>
     * {@link #SECONDARY_INDICATOR_STYLE_OVERLAY}<br/>
     * {@link #SECONDARY_INDICATOR_STYLE_OUTSIDE}
     */
    private int secondaryIndicatorStyle = DEFAULT_SECONDARY_INDICATOR_STYLE;

    /**
     * Indicates the current progress. This value ranges between [0 .. {@link #maxProgress}.
     */
    private int progress = 0;

    /**
     * Indicates the current secondary progress. This value is optional and the application may
     * never use this. This value ranges between [0 .. 100].
     */
    private int secondaryProgress = 0;

    /**
     * Indicates the maximum progress range. Once {@link #progress} reaches this value, the
     * indicator will display a complete circle.
     */
    private int maxProgress = DEFAULT_MAX_PROGRESS;

    /**
     * The timeout value to start from and countdown to 0. This value is in <b>milliseconds</b>.
     */
    private int timeout = DEFAULT_TIMEOUT;

    /**
     * If {@link #smoothAnimation} is enabled then the current sweep angle indicates the angle to
     * be drawn for this frame while animating from the current angle to the target angle defined
     * by {@link #sweepAngle}.
     */
    private float currentSweepAngle = 0.0f;

    /**
     * If {@link #smoothAnimation} is enabled then the current secondary sweep angle indicates the
     * angle to be drawn for this frame while animating from the current angle to the target angle
     * defined by {@link #secondarySweepAngle}.
     */
    private float currentSecondarySweepAngle = 0.0f;

    /**
     * Represents the arc to be drawn from 0 to this value. This value ranges from [0 .. 360].<br/>
     * This sweep angle is for the current progress.
     */
    private float sweepAngle = 0.0f;

    /**
     * Represents the arc to be drawn from 0 to this value. This value ranges from [0 .. 360].<br/>
     * This sweep angle is for the secondary progress.
     */
    private float secondarySweepAngle = 0.0f;

    /**
     * Indicates if the timer has been started.
     * True if the timer has been started and False otherwise.
     */
    private volatile boolean timerStarted = false;

    /**
     * Indicates if the secondary progress has started.
     * True if the secondary progress has been started and False otherwise.
     */
    private volatile boolean secondaryProgressStarted = false;

    private Paint indicatorPaint;
    private int indicatorColor;

    private Paint secondaryIndicatorPaint;
    private int secondaryIndicatorColor;

    /**
     * The overall track paint. This is the track paint that will draw the entire circle when the
     * progress is 0.
     */
    private Paint trackPaint;
    private int trackColor;

    /**
     * The overall track paint of the secondary track. This will be painted either inside or outside
     * the primary track.
     */
    private Paint secondaryTrackPaint;
    private int secondaryTrackColor;

    /**
     * Used to paint the textual representation of the progress as dictated by the {@link #indicatorStyle}.
     */
    private Paint textPaint;
    private int textColor;
    private int textSize;

    /**
     * By default the progress text is always visible in the indicator.
     */
    private boolean showProgressText;

    /**
     * Represents the stroke thickness of the primary progress track and indicator.
     */
    private int thickness;

    /**
     * Represents the stroke thickness of the secondary progress track and indicator.
     */
    private int secondaryThickness;

    /**
     * Bounds of the primary radial progress indicator as represented by the primary progress and
     * track.
     */
    private RectF bounds;

    /**
     * Bounds of the secondary radial progress indicator. This may be greater than, equal, or less
     * than {@link #bounds}. This is dependent upon where the secondary progress is drawn. It can
     * be painted outside of the primary track edge, in the primary track, or on the inside track
     * edge.
     */
    private RectF secondaryProgressBounds;
//    private int[] colors = new int[] { Color.argb(0x10, 0xFF, 0xFF, 0xFF), Color.WHITE, Color.WHITE };

    private float centerX;
    private float centerY;

    /**
     * Used to manage the animation primaryIndicatorTick.
     */
    private long animationStartTime = 0;

    /**
     * Optional drawable. This is displayed when the application calls {@link #showFailure()}. The
     * failure icon is painted where the indicator text is normally painted -- inside the track
     * bounds.<br/>
     */
    private Drawable failureDrawable;

    /**
     * Optional drawable. This is displayed when the application calls {@link #showSuccess()}. The
     * success icon is painted where the indicator text is normally painted -- inside the track
     * bounds.
     */
    private Drawable successDrawable;

    private boolean showFailure = false;
    private boolean showSuccess = false;

    /**
     * Using smooth animation causes incremental progress updates to be animated from the current
     * angle to the target angle at a rate of 1 degree per frame where the frame rate is 60fps.
     *
     * The rate can be changed by setting {@link #animationTickDegrees}
     */
    private boolean smoothAnimation = false;

    /**
     * The handler used to execute the {@link #primaryIndicatorTick} and/or {@link #secondaryIndicatorTick}.
     */
    private final Handler animationHandler = new Handler();

    /**
     * Increasing the animationTickDegrees will make the maximum speed
     * of the smoothAnimation faster
     */
    private int animationTickDegrees = DEFAULT_ANIMATION_TICK_DEGREES;

    /**
     * Each animation tick is dispatched to perform the operation as defined by this runnable.
     * The animation tick updates the sweep angle at 60fps until the target angle is reached.
     */
    private final Runnable primaryIndicatorTick = new Runnable() {
        @Override
        public void run() {
            // Redraw the view
            invalidate();

            // If the target angle has not been reached then schedule this tick to be dispatched
            // at 60fps.
            if (isIndicatorStyleTimer() && sweepAngle < 360.0f) {
                animationHandler.postDelayed(this, 20);
            } else if (currentSweepAngle < sweepAngle) {
                currentSweepAngle += animationTickDegrees;
                currentSweepAngle = Math.min(currentSweepAngle, sweepAngle);
                animationHandler.postDelayed(this, 20);
            }
        }
    };

    /**
     * Performs the update of the secondary sweep angle until the target secondary sweep angle has
     * been reached. The animation tick updates at a rate of 60fps.
     */
    private final Runnable secondaryIndicatorTick = new Runnable() {
        @Override
        public void run() {
            // Redraw the view
            invalidate();

            // If the target angle has not been reached then schedule this tick to be dispatched
            // at 60fps.
            if (currentSecondarySweepAngle < secondarySweepAngle) {
                currentSecondarySweepAngle += animationTickDegrees;
                currentSecondarySweepAngle = Math.min(currentSecondarySweepAngle, secondarySweepAngle);
                animationHandler.postDelayed(this, 20);
            }
        }
    };

    private TimerListener timerListener;

    /**
     * An optional interface which an application can register to receive a notification when the
     * RadialProgressIndicator has timed out. This will not be triggered if the
     * RadialProgressIndicator style is not set to {@link #INDICATOR_STYLE_TIMER}.
     */
    public interface TimerListener {

        /**
         * The timer has completed it's countdown.
         */
        public void onTimeout();
    }

    public RadialProgressIndicator(Context context) {
        this(context, null);
    }

    public RadialProgressIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RadialProgressIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final Resources resources = context.getResources();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RadialProgressIndicator, defStyle, 0);
        this.maxProgress = a.getInt(R.styleable.RadialProgressIndicator_max, DEFAULT_MAX_PROGRESS);
        this.timeout = a.getInt(R.styleable.RadialProgressIndicator_timeout, DEFAULT_TIMEOUT);
        this.progress = a.getInt(R.styleable.RadialProgressIndicator_progress, 0);

        this.indicatorColor = a.getColor(R.styleable.RadialProgressIndicator_indicatorColor, Color.WHITE);
        this.secondaryIndicatorColor = a.getColor(R.styleable.RadialProgressIndicator_secondaryIndicatorColor, Color.DKGRAY);
        this.trackColor = a.getColor(R.styleable.RadialProgressIndicator_trackColor, resources.getColor(R.color.radial_indicator_track));
        this.secondaryTrackColor = a.getColor(R.styleable.RadialProgressIndicator_secondaryTrackColor, Color.TRANSPARENT);

        final int defaultThickness = resources.getDimensionPixelSize(R.dimen.radial_indicator_primary_track_thickness);
        final int secondaryDefaultThickness = resources.getDimensionPixelSize(R.dimen.radial_indicator_secondary_track_thickness);
        this.thickness = a.getDimensionPixelSize(R.styleable.RadialProgressIndicator_thickness, defaultThickness);
        this.secondaryThickness = a.getDimensionPixelSize(R.styleable.RadialProgressIndicator_secondaryThickness, secondaryDefaultThickness);

        final int defaultTextSize = resources.getDimensionPixelSize(R.dimen.radial_indicator_text_size);
        this.textSize = a.getDimensionPixelSize(R.styleable.RadialProgressIndicator_textSize, defaultTextSize);
        this.textColor = a.getColor(R.styleable.RadialProgressIndicator_textColor, Color.WHITE);
        this.showProgressText = a.getBoolean(R.styleable.RadialProgressIndicator_showProgressText, true);

        this.indicatorStyle = a.getInteger(R.styleable.RadialProgressIndicator_indicatorStyle, DEFAULT_INDICATOR_STYLE);
        if (!isIndicatorStyleSupported(this.indicatorStyle)) {
            throw new IllegalArgumentException("An unsupported indicator style: " + this.indicatorStyle + " was provided.");
        }

        this.secondaryIndicatorStyle = a.getInteger(R.styleable.RadialProgressIndicator_secondaryIndicatorStyle, DEFAULT_SECONDARY_INDICATOR_STYLE);
        if (!isSecondaryIndicatorStyleSupported(this.secondaryIndicatorStyle)) {
            throw new IllegalArgumentException("An unsupported secondary indicator style: " + this.secondaryIndicatorStyle + " was provided.");
        }

        this.failureDrawable = a.getDrawable(R.styleable.RadialProgressIndicator_failureDrawable);
        if (failureDrawable != null) {
            updateDrawableBounds(failureDrawable);
        }

        this.successDrawable = a.getDrawable(R.styleable.RadialProgressIndicator_successDrawable);
        if (successDrawable != null) {
            updateDrawableBounds(successDrawable);
        }

        this.smoothAnimation = a.getBoolean(R.styleable.RadialProgressIndicator_smoothAnimation, false);
        this.animationTickDegrees = a.getInteger(R.styleable.RadialProgressIndicator_animationTickDegrees, DEFAULT_ANIMATION_TICK_DEGREES);

        a.recycle();

        this.indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        indicatorPaint.setStyle(Paint.Style.STROKE);
        indicatorPaint.setStrokeWidth(this.thickness);
        indicatorPaint.setStrokeCap(Paint.Cap.BUTT);
        indicatorPaint.setDither(true);
        indicatorPaint.setFilterBitmap(false);
        indicatorPaint.setColor(this.indicatorColor);

        this.secondaryIndicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        secondaryIndicatorPaint.setStyle(Paint.Style.STROKE);
        secondaryIndicatorPaint.setStrokeWidth(this.secondaryThickness);
        secondaryIndicatorPaint.setStrokeCap(Paint.Cap.BUTT);
        secondaryIndicatorPaint.setDither(true);
        secondaryIndicatorPaint.setFilterBitmap(false);
        secondaryIndicatorPaint.setColor(this.secondaryIndicatorColor);

        this.trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(this.thickness);
        trackPaint.setStrokeCap(Paint.Cap.BUTT);
        trackPaint.setDither(true);
        trackPaint.setFilterBitmap(false);
        trackPaint.setColor(this.trackColor);

        this.textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(textColor);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(textSize);
    }

    /**
     * Returns true if the current indicator style is {@link #INDICATOR_STYLE_PERCENTAGE}.
     *
     * @return True if the current indicator style is {@link #INDICATOR_STYLE_PERCENTAGE} and False
     * otherwise.
     * @see #setIndicatorStylePercentage()
     * @see #setIndicatorStyle(int)
     */
    public boolean isIndicatorStylePercentage() {
        return getIndicatorStyle() == INDICATOR_STYLE_PERCENTAGE;
    }

    /**
     * Sets the indicator style to use percentage.<br/>
     * The radial indicator will animate the progress of the indicator from the current progress
     * position to the new progress position. As the progress indicator animates to the new value,
     * the value of the indicator is updated as a percentage from [0 .. 100].<br/>
     *
     * @see #isIndicatorStylePercentage()
     * @see #setIndicatorStyle(int)
     * @see #setMaxProgress(int)
     * @see #setProgress(int)
     */
    public void setIndicatorStylePercentage() {
        setIndicatorStyle(INDICATOR_STYLE_PERCENTAGE);
    }

    /**
     * Returns true if the current indicator style is {@link #INDICATOR_STYLE_TIMER}.
     *
     * @return True if the current indicator style is {@link #INDICATOR_STYLE_TIMER} and False
     * otherwise.
     * @see #setIndicatorStyleTimer()
     * @see #setIndicatorStyle(int)
     */
    public boolean isIndicatorStyleTimer() {
        return getIndicatorStyle() == INDICATOR_STYLE_TIMER;
    }

    /**
     * Sets the indicator style to use a timer.<br/>
     * The radial indicator will animate the progress of the indicator from [0 .. 360] as the timer
     * duration decreases. If the application has registered a {@link com.barista.demitasse.widgets.RadialProgressIndicator.TimerListener}, then
     * upon reaching the timeout value, {@link com.barista.demitasse.widgets.RadialProgressIndicator.TimerListener#onTimeout()} will be called.<br/>
     *
     * @see #isIndicatorStyleTimer()
     * @see #setIndicatorStyle(int)
     * @see #setTimeout(int)
     * @see #startTimer()
     * @see #stopTimer()
     * @see #resetTimer()
     */
    public void setIndicatorStyleTimer() {
        setIndicatorStyle(INDICATOR_STYLE_TIMER);
    }

    /**
     * Returns true if the current indicator style is {@link #INDICATOR_STYLE_FIXED}.
     *
     * @return True if the current indicator style is {@link #INDICATOR_STYLE_FIXED} and False
     * otherwise.
     * @see #setIndicatorStyleFixed()
     * @see #setIndicatorStyle(int)
     */
    public boolean isIndicatorStyleFixed() {
        return getIndicatorStyle() == INDICATOR_STYLE_FIXED;
    }

    /**
     * Sets the indicator style to use a fixed value.<br/>
     * The radial indicator will animate the progress of the indicator from the current progress
     * position to the new progress position. As the progress indicator animates to the new value,
     * the value of the indicator is updated as the actual value from [0 .. MAX PROGRESS].<br/>
     *
     * @see #isIndicatorStyleFixed()
     * @see #setIndicatorStyle(int)
     * @see #setMaxProgress(int)
     * @see #setProgress(int)
     */
    public void setIndicatorStyleFixed() {
        setIndicatorStyle(INDICATOR_STYLE_FIXED);
    }

    /**
     * Sets the indicator style to one of the supported indicator styles:<br/>
     * {@link #INDICATOR_STYLE_PERCENTAGE}<br/>
     * {@link #INDICATOR_STYLE_FIXED}<br/>
     * {@link #INDICATOR_STYLE_TIMER}<br/>
     *
     * @param indicatorStyle Sets the indicator style to one of the provided styles. If the style
     *                       is not one of the supported styles then an {@link java.lang.IllegalArgumentException} is thrown.
     * @see #getIndicatorStyle()
     */
    public void setIndicatorStyle(int indicatorStyle) {
        if (!isIndicatorStyleSupported(indicatorStyle)) {
            throw new IllegalArgumentException("An unsupported indicator style: " + this.indicatorStyle + " was provided.");
        }
        this.indicatorStyle = indicatorStyle;
    }

    /**
     * Retrieve the current indicator style.
     * Supported indicator styles:<br/>
     * {@link #INDICATOR_STYLE_PERCENTAGE}<br/>
     * {@link #INDICATOR_STYLE_FIXED}<br/>
     * {@link #INDICATOR_STYLE_TIMER}<br/>
     *
     * @return
     * @see #setIndicatorStyle(int)
     */
    public int getIndicatorStyle() {
        return this.indicatorStyle;
    }

    /**
     * Returns true if the current secondary indicator style is {@link #SECONDARY_INDICATOR_STYLE_INSIDE}.
     *
     * @return True if the current secondary indicator style is {@link #SECONDARY_INDICATOR_STYLE_INSIDE} and False otherwise.
     * @see #setSecondaryIndicatorStyleInside()
     * @see #setSecondaryIndicatorStyle(int)
     */
    public boolean isSecondaryIndicatorStyleInside() {
        return this.secondaryIndicatorStyle == SECONDARY_INDICATOR_STYLE_INSIDE;
    }

    /**
     * Sets the secondary indicator style to paint the indicator on the inside.
     *
     * @see #isSecondaryIndicatorStyleInside()
     * @see #setSecondaryIndicatorStyle(int)
     */
    public void setSecondaryIndicatorStyleInside() {
        if (!isSecondaryIndicatorStyleInside()) {
            this.secondaryIndicatorStyle = SECONDARY_INDICATOR_STYLE_INSIDE;
        }
    }

    /**
     * Returns true if the current secondary indicator style is {@link #SECONDARY_INDICATOR_STYLE_OVERLAY}.
     *
     * @return True if the current secondary indicator style is {@link #SECONDARY_INDICATOR_STYLE_OVERLAY} and False otherwise.
     * @see #setSecondaryIndicatorStyleOverlay()
     * @see #setSecondaryIndicatorStyle(int)
     */
    public boolean isSecondaryIndicatorStyleOverlay() {
        return this.secondaryIndicatorStyle == SECONDARY_INDICATOR_STYLE_OVERLAY;
    }

    /**
     * Sets the secondary indicator style to paint the indicator on the same track as the primary
     * indicator.
     *
     * @see #isSecondaryIndicatorStyleOverlay()
     * @see #setSecondaryIndicatorStyle(int)
     */
    public void setSecondaryIndicatorStyleOverlay() {
        if (!isSecondaryIndicatorStyleOverlay()) {
            this.secondaryIndicatorStyle = SECONDARY_INDICATOR_STYLE_OVERLAY;
        }
    }

    /**
     * Returns true if the current secondary indicator style is {@link #SECONDARY_INDICATOR_STYLE_OUTSIDE}.
     *
     * @return True if the current secondary indicator style is {@link #SECONDARY_INDICATOR_STYLE_OUTSIDE} and False otherwise.
     * @see #setSecondaryIndicatorStyleOutside()
     * @see #setSecondaryIndicatorStyle(int)
     */
    public boolean isSecondaryIndicatorStyleOutside() {
        return this.secondaryIndicatorStyle == SECONDARY_INDICATOR_STYLE_OUTSIDE;
    }

    /**
     * Sets the secondary indicator style to paint the indicator on the outside track.
     *
     * @see #isSecondaryIndicatorStyleOutside()
     * @see #setSecondaryIndicatorStyle(int)
     */
    public void setSecondaryIndicatorStyleOutside() {
        if (!isSecondaryIndicatorStyleOutside()) {
            this.secondaryIndicatorStyle = SECONDARY_INDICATOR_STYLE_OUTSIDE;
        }
    }

    /**
     * Sets the secondary indicator style to one of the supported indicator styles:<br/>
     * {@link #SECONDARY_INDICATOR_STYLE_INSIDE}<br/>
     * {@link #SECONDARY_INDICATOR_STYLE_OVERLAY}<br/>
     * {@link #SECONDARY_INDICATOR_STYLE_OUTSIDE}<br/>
     *
     * @param indicatorStyle Sets the secondary indicator style to one of the provided styles. If the style
     *                       is not one of the supported styles then an {@link java.lang.IllegalArgumentException} is thrown.
     * @see #getSecondaryIndicatorStyle()
     */
    public void setSecondaryIndicatorStyle(int indicatorStyle) {
        if (this.secondaryIndicatorStyle != indicatorStyle) {
            this.secondaryIndicatorStyle = indicatorStyle;
        }
    }

    /**
     * Retrieve the current secondary indicator style.
     * Supported indicator styles:<br/>
     * {@link #SECONDARY_INDICATOR_STYLE_INSIDE}<br/>
     * {@link #SECONDARY_INDICATOR_STYLE_OVERLAY}<br/>
     * {@link #SECONDARY_INDICATOR_STYLE_OUTSIDE}<br/>
     *
     * @return
     * @see #setSecondaryIndicatorStyle(int)
     */
    public int getSecondaryIndicatorStyle() {
        return this.secondaryIndicatorStyle;
    }

    /**
     * Set the color of the progress indicator.
     *
     * @param color
     */
    public void setIndicatorColor(int color) {
        this.indicatorColor = color;
        this.indicatorPaint.setColor(this.indicatorColor);
    }

    /**
     * Set the color of the secondary progress indicator.
     *
     * @param color
     */
    public void setSecondaryIndicatorColor(int color) {
        this.secondaryIndicatorColor = color;
        this.secondaryIndicatorPaint.setColor(this.secondaryIndicatorColor);
    }

    /**
     * Set the color of the track color. The track is always present and represents the background
     * track for the primary and/or secondary indicator to sit on top of.
     *
     * @param color
     */
    public void setTrackColor(int color) {
        this.trackColor = color;
        this.trackPaint.setColor(this.trackColor);
    }

    /**
     * Set whether the radial indicator should smoothly animate or jump immediately to latest progress
     * @param smoothAnimation
     */
    public void setSmoothAnimation(boolean smoothAnimation) {
        this.smoothAnimation = smoothAnimation;
    }

    /**
     * Set speed of smoothAnimation if enabled
     * @param animationTickDegrees Number of degrees maximum to be animated per frame (at 60fps)
     */
    public void setAnimationTickDegrees(int animationTickDegrees) {
        this.animationTickDegrees = animationTickDegrees;
    }

    /**
     * Set the upper limit of the radial indicator's primary progress track to be between
     * [0 .. maxProgress].
     *
     * @param maxProgress The upper limit of the primary indicator's track range.
     * @see #getMaxProgress()
     */
    public synchronized void setMaxProgress(int maxProgress) {
        this.maxProgress = maxProgress;
    }

    /**
     * Return the upper limit of the primary indicator's track range.
     *
     * @return The upper limit.
     * @see #setMaxProgress(int)
     */
    public synchronized int getMaxProgress() {
        return this.maxProgress;
    }

    /**
     * Sets the current progress to the provided value. This does nothing if the current indicator
     * style is {@link #INDICATOR_STYLE_TIMER}.<br/>
     * The current progress is ensured to be between [0 .. {@link #getMaxProgress()}].
     *
     * @param progress The new progress (between [0 .. {@link #getMaxProgress()}]).
     * @see #getProgress()
     * @see #getMaxProgress()
     * @see #setMaxProgress(int)
     */
    public void setProgress(int progress) {
        if (isIndicatorStyleTimer()) {
            return;
        }

        if (progress < 0) {
            progress = 0;
        }

        if (progress > maxProgress) {
            progress = maxProgress;
        }

        synchronized (this) {
            if (progress != this.progress) {
                this.progress = progress;
                invalidate();
            }
        }
    }

    /**
     * Get the current progress. Returns 0 when the progress indicator style is
     * {@link #INDICATOR_STYLE_TIMER}.<br/>
     *
     * @return The current progress between 0 and {@link #getMaxProgress()}}.
     * @see #setProgress(int)
     * @see #getMaxProgress()
     * @see #setMaxProgress(int)
     */
    public synchronized int getProgress() {
        return this.progress;
    }

    /**
     * Sets the current secondary progress to the provided value. This does nothing if the current
     * indicator style is {@link #INDICATOR_STYLE_TIMER}.<br/>
     * The current secondary progress is ensured to be between [0 .. 100].
     *
     * @param progress The new progress (between [0 .. 100]).
     * @see #getSecondaryProgress()
     */
    public void setSecondaryProgress(int progress) {
        if (progress < 0) {
            progress = 0;
        }

        if (progress > 100) {
            progress = 100;
        }

        synchronized (this) {
            if (progress != this.secondaryProgress) {
                this.secondaryProgress = progress;
                invalidate();
            }
        }
    }

    /**
     * Get the current secondary progress. Returns 0 when the progress indicator style is
     * {@link #INDICATOR_STYLE_TIMER}.<br/>
     *
     * @return The current progress between 0 and {@link #getMaxProgress()}}.
     * @see #setProgress(int)
     * @see #getMaxProgress()
     * @see #setMaxProgress(int)
     */
    public synchronized int getSecondaryProgress() {
        return this.secondaryProgress;
    }

    /**
     * Sets the timeout value used when the indicator style is {@link #INDICATOR_STYLE_TIMER}. The
     * value must be in <b>milliseconds</b>.<br/>
     * The indicator will countdown from this value to 0. Upon reaching 0,
     * {@link com.barista.demitasse.widgets.RadialProgressIndicator.TimerListener#onTimeout()}
     * will be triggered if a valid TimerListener has been registered.
     *
     * @param timeout The timeout value in milliseconds.
     * @see #getTimeout()
     * @see #startTimer()
     * @see #stopTimer()
     * @see #resetTimer()
     */
    public synchronized void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Returns the current timeout value in milliseconds.
     *
     * @return Current timeout value in milliseconds.
     * @see #setTimeout(int)
     * @see #startTimer()
     * @see #stopTimer()
     * @see #resetTimer()
     */
    public synchronized int getTimeout() {
        return this.timeout;
    }

    /**
     * Starts the countdown timer. If the indicator style is not set to {@link #INDICATOR_STYLE_TIMER}
     * then this will not have any effect. If the timer has already been started then this will not
     * have any effect.
     *
     * @see #setTimeout(int)
     * @see #getTimeout()
     * @see #stopTimer()
     * @see #resetTimer()
     */
    public void startTimer() {
        if (getIndicatorStyle() != INDICATOR_STYLE_TIMER) {
            return;
        }

        if (this.timerStarted) {
            return;
        }

        this.showSuccess = false;
        this.showFailure = false;
        this.timerStarted = true;
        postInvalidate();
    }

    /**
     * Stops the countdown timer. If the indicator style is not set to {@link #INDICATOR_STYLE_TIMER}
     * then this will not have any effect. If the timer has not been started then this will not
     * have any effect.
     *
     * @see #setTimeout(int)
     * @see #getTimeout()
     * @see #startTimer()
     * @see #resetTimer()
     */
    public void stopTimer() {
        if (getIndicatorStyle() != INDICATOR_STYLE_TIMER) {
            return;
        }

        if (!this.timerStarted) {
            return;
        }

        this.timerStarted = false;
        animationHandler.removeCallbacks(primaryIndicatorTick);
    }

    /**
     * Resets the countdown timer. If the indicator style is not set to {@link #INDICATOR_STYLE_TIMER}
     * then this will not have any effect. This should be called prior to calling {@link #startTimer()}
     * when the application would like to restart the timer. This will also hide a success or
     * failure indicator if one was displayed.
     *
     * @see #setTimeout(int)
     * @see #getTimeout()
     * @see #startTimer()
     * @see #stopTimer()
     */
    public void resetTimer() {
        if (getIndicatorStyle() != INDICATOR_STYLE_TIMER) {
            return;
        }

        stopTimer();

        this.showSuccess = false;
        this.showFailure = false;
        this.progress = getTimeout();
        this.animationStartTime = 0;
        postInvalidate();
    }

    /**
     * Set the timer listener. This will trigger a notification to the application when the timer
     * reaches its timeout value.
     *
     * @param listener
     */
    public void setTimerListener(TimerListener listener) {
        this.timerListener = listener;
    }

    /**
     * Displays the failure drawable if one has been set.<br/>
     * This will hide the success drawable if it is currently visible.
     */
    public void showFailure() {
        if (failureDrawable == null) {
            return;
        }

        showSuccess = false;
        showFailure = true;

        postInvalidate();
    }

    /**
     * Displays the success drawable if one has been set.<br/>
     * This will hide the failure drawable if it is currently visible.
     */
    public void showSuccess() {
        if (successDrawable == null) {
            return;
        }

        showFailure = false;
        showSuccess = true;

        postInvalidate();
    }

    public synchronized boolean isSecondaryProgressStarted() {
        return this.secondaryProgressStarted;
    }

    public synchronized void startSecondaryProgress() {
        this.secondaryProgressStarted = true;
    }

    public synchronized void stopSecondaryProgress() {
        this.secondaryProgressStarted = false;
    }

    /**
     * Displays the progress text in the center of the progress indicator.<br/>
     * The progress text is either a percentage or fixed value of the progress.
     *
     * @see #hideProgressText()
     */
    public void showProgressText() {
        this.showProgressText = true;
        postInvalidate();
    }

    /**
     * Hides the progress text in the center of the progress indicator.<br/>
     *
     * @see #showProgressText
     */
    public void hideProgressText() {
        this.showProgressText = false;
        postInvalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        final int size = Math.min(w, h);
        centerX = size / 2.0f;
        centerY = size / 2.0f;

        final float left = getPaddingLeft() + thickness / 2.0f + secondaryThickness;
        final float right = size - getPaddingRight() - thickness / 2.0f - secondaryThickness;
        final float top = getPaddingTop() + thickness / 2.0f + secondaryThickness;
        final float bottom = size - getPaddingBottom() - thickness / 2.0f - secondaryThickness;

        bounds = new RectF(left, top, right, bottom);
        if (secondaryIndicatorStyle == SECONDARY_INDICATOR_STYLE_INSIDE) {
            final float margin = thickness / 2.0f + secondaryThickness / 2.0f;
            secondaryProgressBounds = new RectF(left + margin, top + margin, right - margin, bottom - margin);
        } else if (secondaryIndicatorStyle == SECONDARY_INDICATOR_STYLE_OVERLAY) {
            secondaryProgressBounds = new RectF(left, top, right, bottom);
        } else if (secondaryIndicatorStyle == SECONDARY_INDICATOR_STYLE_OUTSIDE) {
            secondaryProgressBounds = new RectF(getPaddingLeft() + secondaryThickness / 2.0f, getPaddingTop() + secondaryThickness / 2.0f, size - getPaddingRight() - secondaryThickness / 2.0f, size - getPaddingBottom() - secondaryThickness / 2.0f);
        }

        if (successDrawable != null) {
            updateDrawableBounds(successDrawable);
        }

        if (failureDrawable != null) {
            updateDrawableBounds(failureDrawable);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (animationStartTime == 0) {
            animationStartTime = SystemClock.uptimeMillis();
        }

        canvas.save();

        if (showSuccess) {
            drawSuccess(canvas);
        } else if (showFailure) {
            drawFailure(canvas);
        } else if (showProgressText) {
            drawProgressText(canvas);
        }

        drawIndicatorTrack(canvas);
        drawSecondaryIndicator(canvas);
        drawPrimaryIndicator(canvas);

        canvas.restore();
    }

    private void drawIndicatorTrack(Canvas canvas) {
        canvas.drawCircle(centerX, centerY, bounds.width() / 2.0f, trackPaint);
    }

    private void drawProgressText(Canvas canvas) {
        String progressText = getProgressText();
        Rect textBounds = new Rect();
        textPaint.getTextBounds(progressText, 0, progressText.length(), textBounds);
        canvas.drawText(progressText, centerX, centerY - textBounds.exactCenterY(), textPaint);
    }

    private void drawFailure(Canvas canvas) {
        drawDrawable(canvas, failureDrawable);
    }

    private void drawSuccess(Canvas canvas) {
        drawDrawable(canvas, successDrawable);
    }

    private void drawDrawable(Canvas canvas, Drawable drawable) {
        final int width = drawable.getBounds().width();
        final int height = drawable.getBounds().height();

        int saveCount = canvas.save();
        canvas.translate(bounds.left + (bounds.width() - width) / 2.0f, bounds.top + (bounds.height() - height) / 2.0f);
        successDrawable.draw(canvas);
        canvas.restoreToCount(saveCount);
    }

    private void drawPrimaryIndicator(Canvas canvas) {
        int saveCount = canvas.save();

        canvas.rotate(ANGLE_START, centerX, centerY);
        sweepAngle = getAngle();

        if (smoothAnimation && isIndicatorStylePercentage()) {
            currentSweepAngle = Math.min(currentSweepAngle, sweepAngle);
            canvas.drawArc(bounds, 0, currentSweepAngle, false, indicatorPaint);
            animationHandler.removeCallbacks(primaryIndicatorTick);

            if (currentSweepAngle < sweepAngle) {
                animationHandler.post(primaryIndicatorTick);
            }
        } else {
            canvas.drawArc(bounds, 0, sweepAngle, false, indicatorPaint);
        }

        if (isIndicatorStyleTimer() && timerStarted) {
            animationHandler.removeCallbacks(primaryIndicatorTick);
            animationHandler.post(primaryIndicatorTick);
        }

        canvas.restoreToCount(saveCount);
    }

    private void drawSecondaryIndicator(Canvas canvas) {
        int saveCount = canvas.save();
        secondarySweepAngle = getSecondaryAngle();

        canvas.rotate(ANGLE_START, centerX, centerY);

        if (smoothAnimation) {
            currentSecondarySweepAngle = Math.min(currentSecondarySweepAngle, secondarySweepAngle);
            canvas.drawArc(secondaryProgressBounds, 0, currentSecondarySweepAngle, false, secondaryIndicatorPaint);
            animationHandler.removeCallbacks(secondaryIndicatorTick);

            if (currentSecondarySweepAngle < secondarySweepAngle) {
                animationHandler.post(secondaryIndicatorTick);
            }
        } else {
            canvas.drawArc(secondaryProgressBounds, 0, secondarySweepAngle, false, secondaryIndicatorPaint);
        }

        canvas.restoreToCount(saveCount);
    }

    private void updateDrawableBounds(Drawable d) {
        final int padding = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics()) + 0.5f);
        final int width = Math.min(d.getIntrinsicWidth(), (int) bounds.width() - padding * 2);
        final int height = Math.min(d.getIntrinsicHeight(), (int) bounds.height() - padding * 2);

        d.setBounds(0, 0, width, height);
    }

    private float getAngle() {
        if (showFailure || showSuccess) {
            return 360.0f;
        }

        if (isIndicatorStylePercentage() || isIndicatorStyleFixed()) {
            final int progress = getProgress();
            if (progress == getMaxProgress()) {
                this.progress = getMaxProgress();
                return 360.0f;
            }

            return 360.0f * progress / (float) (getMaxProgress());
        } else if (isIndicatorStyleTimer()) {
            final int timeout = getTimeout();
            long now = SystemClock.uptimeMillis();
            long progress = now - animationStartTime;
            if (progress >= timeout && timerStarted) {
                timerStarted = false;
                resetTimer();

                if (timerListener != null) {
                    timerListener.onTimeout();
                }

                return 360.0f;
            }

            setProgress((int) (timeout - progress));
            return 360.0f * progress / (float) (timeout);
        } else {
            return 0;
        }
    }

    private float getSecondaryAngle() {
        final int progress = getSecondaryProgress();
        return Math.min(360.0f, 360.0f * progress / 100.0f);
    }

    private String getProgressText() {
        String progress = "";

        if (isIndicatorStyleTimer()) {
            progress = (int) ((getProgress() / 1000.0f + 0.9f)) + "s";
        } else if (isIndicatorStylePercentage()) {
            if (smoothAnimation) {
                progress = (int) ((currentSweepAngle / 360.0f) * 100) + "%";
            } else {
                progress = (int) ((getProgress() / (float) getMaxProgress()) * 100) + "%";
            }
        } else if (isIndicatorStyleFixed()) {
            if (isSecondaryProgressStarted()) {
                progress = getSecondaryProgress() + "%";
            } else {
                progress = NumberFormat.getNumberInstance(Locale.getDefault()).format(getProgress());
            }
        }

        return progress;
    }

    /**
     * Validates the provided indicator style to ensure it is one of the supported styles.<br/>
     * Valid indicator styles are:<br/>
     * {@link #INDICATOR_STYLE_PERCENTAGE}<br/>
     * {@link #INDICATOR_STYLE_FIXED}<br/>
     * {@link #INDICATOR_STYLE_TIMER}<br/>
     *
     * @param indicatorStyle
     * @return Returns true if the style is supported and false otherwise.
     */
    private boolean isIndicatorStyleSupported(int indicatorStyle) {
        return (indicatorStyle == INDICATOR_STYLE_FIXED || indicatorStyle == INDICATOR_STYLE_PERCENTAGE || indicatorStyle == INDICATOR_STYLE_TIMER);
    }

    /**
     * Validates the provided secondary indicator style to ensure it is one of the supported styles<br/>
     * Valid indicator styles are:<br/>
     * {@link #SECONDARY_INDICATOR_STYLE_INSIDE}<br/>
     * {@link #SECONDARY_INDICATOR_STYLE_OUTSIDE}<br/>
     * {@link #SECONDARY_INDICATOR_STYLE_OVERLAY}<br/>
     *
     * @param indicatorStyle
     * @return Returns true if the style is supported and false otherwise.
     */
    private boolean isSecondaryIndicatorStyleSupported(int indicatorStyle) {
        return (indicatorStyle == SECONDARY_INDICATOR_STYLE_INSIDE || indicatorStyle == SECONDARY_INDICATOR_STYLE_OUTSIDE || indicatorStyle == SECONDARY_INDICATOR_STYLE_OVERLAY);
    }
}
