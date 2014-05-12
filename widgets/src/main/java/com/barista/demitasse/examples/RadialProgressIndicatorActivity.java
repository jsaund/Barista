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

package com.barista.demitasse.examples;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;

import com.barista.demitasse.widgets.R;
import com.barista.demitasse.widgets.RadialProgressIndicator;

import java.lang.ref.WeakReference;

public class RadialProgressIndicatorActivity extends ActionBarActivity {

    private RadialProgressIndicator indicator;
    private RadialProgressUpdateHandler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radial_progress_indicator);

        indicator = (RadialProgressIndicator) findViewById(R.id.radial_progress_indicator);
        handler = new RadialProgressUpdateHandler(this);

        Message updatePrimaryIndicator = handler.obtainMessage(RadialProgressUpdateHandler.MSG_ID_UPDATE_PRIMARY_INDICATOR, 100, 0);
        handler.sendMessageDelayed(updatePrimaryIndicator, 1500);

        Message updateSecondaryIndicator = handler.obtainMessage(RadialProgressUpdateHandler.MSG_ID_UPDATE_SECONDARY_INDICATOR, 5, 0);
        handler.sendMessageDelayed(updateSecondaryIndicator, 500);
    }

    private static class RadialProgressUpdateHandler extends Handler {
        private static final int MSG_ID_UPDATE_PRIMARY_INDICATOR = 1;
        private static final int MSG_ID_UPDATE_SECONDARY_INDICATOR = 2;

        private final WeakReference<RadialProgressIndicatorActivity> reference;

        RadialProgressUpdateHandler(RadialProgressIndicatorActivity activity) {
            this.reference = new WeakReference<RadialProgressIndicatorActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_ID_UPDATE_PRIMARY_INDICATOR) {
                int amount = msg.arg1 > 0 ? msg.arg1 : 100;

                final RadialProgressIndicatorActivity activity = this.reference.get();
                if (activity != null) {
                    final int max = activity.indicator.getMaxProgress();
                    final int progress = activity.indicator.getProgress() + amount;

                    activity.indicator.setProgress(progress);

                    if (progress < max) {
                        Message update = obtainMessage(RadialProgressUpdateHandler.MSG_ID_UPDATE_PRIMARY_INDICATOR, 100, 0);
                        sendMessageDelayed(update, 1500);
                    }
                }
            } else if (msg.what == MSG_ID_UPDATE_SECONDARY_INDICATOR) {
                int amount = msg.arg1 > 0 ? msg.arg1 : 10;

                final RadialProgressIndicatorActivity activity = this.reference.get();
                if (activity != null) {
                    final int max = 100;
                    final int progress = activity.indicator.getSecondaryProgress() + amount;

                    activity.indicator.setSecondaryProgress(progress);

                    if (progress < max) {
                        Message update = obtainMessage(RadialProgressUpdateHandler.MSG_ID_UPDATE_SECONDARY_INDICATOR, 5, 0);
                        sendMessageDelayed(update, 500);
                    }
                }
            }
        }
    }
}
