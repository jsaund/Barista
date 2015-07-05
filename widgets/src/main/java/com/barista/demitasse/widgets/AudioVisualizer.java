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

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.media.audiofx.Visualizer;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import java.util.Arrays;

/**
 * The AudioVisualizer view transforms audio playback capture from the time domain
 * to the frequency domain and provides its child render views with the necessary
 * information to graphically display the information captured.
 */
public class AudioVisualizer extends LinearLayout {
    private static final String TAG = AudioVisualizer.class.getSimpleName();

    // The first two values in the FFT byte array returned by the visualizer represent
    // the amplitude of the frequency at DC and 1/2 the sampling rate.
    // We're interested in all values after these amplitudes.
    private static final int FFT_OFFSET = 2;

    // The minimum smoothing factor used to smooth the rendering of the audio FFT
    // representation.
    private static final float MIN_SMOOTHING_FACTOR = 0.0f;

    // The maximum smoothing factor used to smooth the rendering of the audio FFT
    // representation.
    private static final float MAX_SMOOTHING_FACTOR = 1.0f;

    // Represents the minimum number of elements in the FFT array to skip for each capture.
    private static final int MIN_FFT_BUCKET_SIZE = 2;

    // Represents the maximum number of elements in the FFT array to skip for each capture.
    private static final int MAX_FFT_BUCKET_SIZE = 10;

    // The maximum decibel value we expect to support.
    // The Visualizer class will provide a real and imaginary component restricted to
    // a maximum value of 256.
    private static final float MAX_DB = (float) (10 * Math.log10(256 * 256 + 256 * 256));

    private class VisualizerOnDataCaptureListener implements Visualizer.OnDataCaptureListener {

        @Override
        public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
            // Ignore. We are not interested in waveform capture.
        }

        @Override
        public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
            updateRenderAmplitude(fft);
        }
    }

    private Visualizer visualizer;

    private int sessionID;

    private Visualizer.OnDataCaptureListener dataCaptureListener;

    private float smoothingFactor = 0.1f;
    private int fftBucketSize = MIN_FFT_BUCKET_SIZE;
    private byte[] prevFFT;

    private int height;

    public AudioVisualizer(Context context) {
        this(context, null);
    }

    public AudioVisualizer(Context context, AttributeSet attrs) {
        super(context, attrs);

        dataCaptureListener = new VisualizerOnDataCaptureListener();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        height = getMeasuredHeight();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        final int count = getChildCount();
        if (count == 0) {
            return;
        }

        for (int i = 0; i < count; i++) {
            View v = getChildAt(i);
            if (v instanceof BarRender && ((BarRender) v).isAnimating()) {
                v.invalidate();
            }
        }

        super.dispatchDraw(canvas);
    }

    /**
     * Attaches an existing audio session to capture audio playback from.
     *
     * @param id
     */
    public void setAudioSessionID(int id) {
        if (sessionID == id && visualizer != null) {
            return;
        }

        sessionID = id;
    }

    /**
     * Returns true if the AudioVisualizer is enabled and capturing audio and
     * false otherwise.
     *
     * @return
     */
    public boolean isEnabled() {
        return visualizer != null && visualizer.getEnabled();
    }

    /**
     * Set the smoothing factor used during audio visualization.
     * The value must be between {@value #MIN_SMOOTHING_FACTOR} and {@value #MAX_SMOOTHING_FACTOR}
     *
     * @param f
     * @throws IllegalArgumentException
     */
    public void setSmoothingFactor(float f) throws IllegalArgumentException {
        if (f > MAX_SMOOTHING_FACTOR || f < MIN_SMOOTHING_FACTOR) {
            Log.e(TAG, "Failed to set smoothing factor to: " + f + ". Value must be between " + MIN_SMOOTHING_FACTOR + " to " + MAX_SMOOTHING_FACTOR);
            throw new IllegalArgumentException("Smoothing factor must be between " + MIN_SMOOTHING_FACTOR + " and " + MAX_SMOOTHING_FACTOR);
        }
        smoothingFactor = f;
    }

    /**
     * Returns the smoothing factor currently used during audio visualization.
     *
     * @return
     */
    public float getSmoothingFactor() {
        return smoothingFactor;
    }

    /**
     * Start initializes the audio visualizer and begins capturing audio data and
     * plotting it visually.
     * Once audio playback has completed or is stopped, it is recommended that
     * {@link #stop()} be called to release resources.
     *
     * @throws IllegalStateException
     */
    public void start() throws IllegalStateException {
        Log.d(TAG, "Starting AudioVisualizer");
        if (visualizer != null && visualizer.getEnabled()) {
            return;
        }

        if (visualizer == null) {
            initVisualizer();
        }

        enableVisualizer(true);
    }

    /**
     * Stop releases expensive resources associated with the visualizer. It is
     * recommended this be called when audio playback is either halted or has completed.
     */
    public void stop() {
        Log.d(TAG, "Stopping AudioVisualizer");
        if (visualizer == null || !visualizer.getEnabled()) {
            return;
        }

        shutdownAudioVisualizer();
    }

    /**
     * Sets the FFT bucket size range. A larger bucket size will yield a higher
     * frequency range to be represented during visualization.
     * The bucket size range must be between {@value #MIN_FFT_BUCKET_SIZE} and {@value #MAX_FFT_BUCKET_SIZE}
     *
     * @param size
     * @throws IllegalArgumentException
     */
    public void setFFTBucketSize(int size) throws IllegalArgumentException {
        if (fftBucketSize < MIN_FFT_BUCKET_SIZE || fftBucketSize > MAX_FFT_BUCKET_SIZE) {
            throw new IllegalArgumentException("Cannot set FFT interval to " + size + ". Interval must be between: " + MIN_FFT_BUCKET_SIZE + " and " + MAX_FFT_BUCKET_SIZE);
        }
        fftBucketSize = size;
    }

    /**
     * Returns the current FFT bucket size.
     *
     * @return
     */
    public int getFFTBucketSize() {
        return fftBucketSize;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void initVisualizer() throws IllegalStateException, IllegalArgumentException {
        visualizer = new Visualizer(sessionID);

        visualizer.setScalingMode(Visualizer.SCALING_MODE_NORMALIZED);

        final int captureSize = Visualizer.getCaptureSizeRange()[0];

        if (visualizer.setCaptureSize(captureSize) != Visualizer.SUCCESS) {
            Log.e(TAG, "Failed to set capture size to: " + captureSize);
            throw new IllegalArgumentException("Cannot set capture size to: " + captureSize + ". Must be in range: " + Arrays.toString(Visualizer.getCaptureSizeRange()));
        }

        visualizer.setDataCaptureListener(dataCaptureListener, Visualizer.getMaxCaptureRate(), false, true);
    }

    private void enableVisualizer(boolean enable) throws IllegalStateException {
        int enableStatus = visualizer.setEnabled(enable);
        String enableFailureReason = "Unknown";

        if (enableStatus != Visualizer.SUCCESS) {
            if (enableStatus == Visualizer.ERROR_INVALID_OPERATION) {
                enableFailureReason = "Operation failed because it was requested in wrong state.";
            } else if (enableStatus == Visualizer.ERROR_NO_INIT) {
                enableFailureReason = "Operation failed due to bad object initialization.";
            }

            Log.e(TAG, "Failed to enable visualizer. " + enableFailureReason);
            throw new IllegalStateException(enableFailureReason);
        }
    }

    private void shutdownAudioVisualizer() {
        if (visualizer != null) {
            enableVisualizer(false);

            visualizer.release();
            visualizer = null;

            resetAmplitude();
        }
    }

    private void resetAmplitude() {
        final int numChildren = getChildCount();

        for (int i = 0; i < numChildren; i++) {
            View v = getChildAt(i);
            if (v instanceof Render) {
                Render r = (Render) v;
                r.setAmplitude(0);
            }
        }
    }

    private void updateRenderAmplitude(byte[] fft) {
        if (fft == null) {
            return;
        }

        final int numChildren = getChildCount();

        if (numChildren == 0 || fft.length < numChildren) {
            return;
        }

        if (prevFFT == null) {
            prevFFT = new byte[fft.length];
        }

        for (int i = 0; i < numChildren; i++) {
            View v = getChildAt(i);
            if (v instanceof Render) {
                Render r = (Render) v;
                int db = complexToDB(fft, i * fftBucketSize + FFT_OFFSET);

                r.setAmplitude((int) (db * height / MAX_DB));
            }
        }

        prevFFT = fft;

        invalidate();
    }

    /**
     * Converts the value provided by the FFT byte array from it's complex
     * constituents to a decibel representation of it's amplitude.
     * The values used to convert from complex to DB are provided by the index.
     *
     * @param fft   The byte array containing the FFT in the complex representation
     * @param index Which real and imaginary value to convert
     * @return The decibel amplitude
     */
    private int complexToDB(byte[] fft, int index) {
        byte currR = fft[index];
        byte currI = fft[index + 1];

        byte prevR = fft[index];
        byte prevI = fft[index + 1];

        float deltaR = (currR - prevR) * smoothingFactor;
        float deltaI = (currI - prevI) * smoothingFactor;

        float r = currR + deltaR;
        float i = currI + deltaI;

        float amplitude = r * r + i * i;
        return (int) (10 * Math.log10(amplitude));
    }
}