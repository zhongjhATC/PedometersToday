package com.zhongjh.libsteptoday.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.zhongjh.libsteptoday.store.preferences.PreferencesHelper;

/**
 * 关机广播
 */
public class StepTodayShutdownReceiver extends BroadcastReceiver {

    private static final String TAG = "StepTodayShutdownReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SHUTDOWN)) {
            PreferencesHelper.setShutdown(context, true);
        }
    }

}
