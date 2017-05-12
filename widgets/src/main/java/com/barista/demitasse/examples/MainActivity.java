package com.barista.demitasse.examples;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import com.barista.demitasse.widgets.R;

public class MainActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.start_audio_visualizer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent = new Intent(MainActivity.this, AudioVisualizerActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.start_progress_indicator).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent =
                        new Intent(MainActivity.this, RadialProgressIndicatorActivity.class);
                startActivity(intent);
            }
        });
    }
}
