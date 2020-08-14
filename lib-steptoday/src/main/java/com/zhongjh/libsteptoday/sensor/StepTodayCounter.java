package com.zhongjh.libsteptoday.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import com.zhongjh.libsteptoday.store.preferences.PreferencesHelper;

/**
 * Sensor.TYPE_STEP_COUNTER
 * 计步传感器计算当天步数，不需要后台Service
 */
public class StepTodayCounter implements SensorEventListener {

    private Context mContext;
    private int mCurrStep = 0;

    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public int getCurrentStep() {
        mCurrStep = (int) PreferencesHelper.getCurrentStep(mContext);
        return mCurrStep;
    }
}
