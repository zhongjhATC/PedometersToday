package com.zhongjh.libpedometerstoday.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import com.zhongjh.libpedometerstoday.store.preferences.PreferencesHelper;
import com.zhongjh.libpedometerstoday.util.DateUtils;

/**
 * Sensor.TYPE_ACCELEROMETER
 * 加速度传感器计算当天步数，需要保持后台Service
 */
public class PedometersTodaySensor implements SensorEventListener {

    private Context mContext;
    // 当前时间
    private String mTodayDate;

    private int count = 0;
    // 当前步数
    private int mCount = 0;
    private OnPedometersTodayListener mOnPedometersTodayListener;
    // 最后高峰时间
    private long timeOfLastPeak = 0;
    // 当前高峰时间
    private long timeOfThisPeak = 0;

    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void setSteps(int initValue) {
        this.mCount = initValue;
        this.count = 0;
        timeOfLastPeak = 0;
        timeOfThisPeak = 0;
    }

    private String getTodayDate() {
        return DateUtils.getCurrentDate("yyyy-MM-dd");
    }

    /**
     * 设置步数初始值，目前只支持设置用加速度传感器进行计步
     * @param initStep
     */
    public void setCurrentStep(int initStep){
        setSteps(initStep);

        mCount = initStep;
        PreferencesHelper.setCurrentStep(mContext, mCount);

        mTodayDate = getTodayDate();
        PreferencesHelper.setStepToday(mContext, mTodayDate);

        if(null != mOnPedometersTodayListener){
            mOnPedometersTodayListener.onChangeStepCounter(mCount);
        }
    }

}