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

package com.barista.demitasse.examples;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.barista.demitasse.widgets.AudioVisualizer;
import com.barista.demitasse.widgets.R;
import java.io.IOException;

/**
 * AudioVisualizerActivity demonstrates using two of the prebuilt audio visualization renders.
 * The classic audio visualizer displays the frequency buckets similar to the typical audio
 * spectrum with vertical bars increasing / decreasing according to the magnitude of the frequency.
 * The modern audio visualizer is similar to the classic but centers the bars vertically and grows
 * in both the up and down vertical directions.
 */
public class AudioVisualizerActivity extends AppCompatActivity {
    private static final String TAG = AudioVisualizerActivity.class.getSimpleName();

    private static final Uri MEDIA_URL = Uri.parse("http://www.djdivsa.com/uploads/podcasts/MidnightCityII.mp3");

    private static class OnMediaPreparedListener implements MediaPlayer.OnPreparedListener {
        private ActiveAnalyzerHolder v;

        OnMediaPreparedListener(ActiveAnalyzerHolder v) {
            this.v = v;
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            mp.start();
            v.activeAnalyzer.setAudioSessionID(mp.getAudioSessionId());
            v.activeAnalyzer.start();
        }
    }

    private static class OnMediaCompletedListener implements MediaPlayer.OnCompletionListener {
        private ActiveAnalyzerHolder v;

        OnMediaCompletedListener(ActiveAnalyzerHolder v) {
            this.v = v;
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            v.activeAnalyzer.stop();
        }
    }

    /**
     * Active Analyzer Holder allows us to swap the active analyzer reference
     * while still maintaining a static class implementation of the media playback
     *
     */
    private static class ActiveAnalyzerHolder {
        private AudioVisualizer activeAnalyzer;

        ActiveAnalyzerHolder() {
        }

        void setActiveVisualizer(AudioVisualizer visualizer) {
            activeAnalyzer = visualizer;
        }
    }

    private ActiveAnalyzerHolder activeAnalyzerHolder;

    private AudioVisualizer classicAnalyzer;
    private AudioVisualizer modernAnalyzer;

    private MediaPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_audio_visualizer);

        Toolbar toolbar = (Toolbar) findViewById(R.id.audio_visualizer_toolbar);
        setSupportActionBar(toolbar);

        classicAnalyzer = (AudioVisualizer) findViewById(R.id.classic_analyzer);
        classicAnalyzer.setVisibility(View.VISIBLE);
        classicAnalyzer.setFFTBucketSize(10);

        modernAnalyzer = (AudioVisualizer) findViewById(R.id.modern_analyzer);
        modernAnalyzer.setVisibility(View.GONE);
        modernAnalyzer.setSmoothingFactor(0.5f);

        final Button toggleAnalyzer = (Button) findViewById(R.id.button_toggle_analyzer);
        toggleAnalyzer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (classicAnalyzer.getVisibility() == View.VISIBLE) {
                    classicAnalyzer.stop();
                    classicAnalyzer.setVisibility(View.GONE);

                    modernAnalyzer.setVisibility(View.VISIBLE);
                    modernAnalyzer.setAudioSessionID(player.getAudioSessionId());
                    modernAnalyzer.start();

                    toggleAnalyzer.setText(R.string.analyzer_toggle_show_classic);

                    activeAnalyzerHolder.setActiveVisualizer(modernAnalyzer);
                } else {
                    modernAnalyzer.stop();
                    modernAnalyzer.setVisibility(View.GONE);

                    classicAnalyzer.setVisibility(View.VISIBLE);
                    classicAnalyzer.setAudioSessionID(player.getAudioSessionId());
                    classicAnalyzer.start();

                    toggleAnalyzer.setText(R.string.analyzer_toggle_show_modern);

                    activeAnalyzerHolder.setActiveVisualizer(classicAnalyzer);
                }
            }
        });

        activeAnalyzerHolder = new ActiveAnalyzerHolder();
        activeAnalyzerHolder.setActiveVisualizer(classicAnalyzer);

        MediaPlayer.OnPreparedListener onPreparedListener = new OnMediaPreparedListener(activeAnalyzerHolder);
        MediaPlayer.OnCompletionListener onCompletionListener = new OnMediaCompletedListener(activeAnalyzerHolder);

        player = new MediaPlayer();
        player.setOnPreparedListener(onPreparedListener);
        player.setOnCompletionListener(onCompletionListener);

        try {
            player.setDataSource(this, MEDIA_URL);
            player.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, "Failed to set media player URL.", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (player.isPlaying()) {
            activeAnalyzerHolder.activeAnalyzer.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        activeAnalyzerHolder.activeAnalyzer.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            try {
                player.stop();
                player.release();
            } catch (Exception ignore) {
            }
        }
    }
}
