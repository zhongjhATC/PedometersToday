package com.zhongjh.libsteptoday.sensor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import com.zhongjh.libsteptoday.logger.JLoggerConstant;
import com.zhongjh.libsteptoday.logger.JLoggerWraper;
import com.zhongjh.libsteptoday.store.preferences.PreferencesHelper;
import com.zhongjh.libsteptoday.util.DateUtils;
import com.zhongjh.libsteptoday.util.WakeLockUtils;

import java.util.HashMap;

import androidx.annotation.RequiresApi;

import static com.zhongjh.libsteptoday.logger.ConstantDef.HANDLER_WHAT_TEST_JLOGGER;
import static com.zhongjh.libsteptoday.logger.ConstantDef.WHAT_TEST_JLOGGER_DURATION;

/**
 * Sensor.TYPE_STEP_COUNTER
 * 计步传感器计算当天步数，不需要后台Service
 */
public class StepTodayCounter implements SensorEventListener {

    private Context mContext;
    private int mOffsetStep = 0; // 补偿值步数
    private int mCurrStep = 0; // 当前步数
    private String mTodayDate; // 当天时间日期
    private boolean mIsCleanStep = true; // 是否清除了步数
    private boolean mIsShutdown = false; // 是否曾经关机过
    private OnStepTodayListener mOnStepTodayListener;
    private boolean mIsSeparate = false; // 是否隔天
    private boolean mIsBoot = false; // 是否开机启动

    private float mJLoggerSensorStep = 0f;
    private int mJLoggerCounterStep = 0;
    private int mJLoggerCurrStep = 0; // 当前步数
    private int mJLoggerOffsetStep = 0; // 补偿值步数
    private long mJLoggerSensorCount = 0; // 传感器回调次数

    /**
     * 用于循环一定时间内发送日志
     */
    private final Handler sHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLER_WHAT_TEST_JLOGGER: {
                    HashMap<String, String> map = new HashMap<>();
                    map.put("sCurrStep", String.valueOf(mJLoggerCurrStep));
                    map.put("counterStep", String.valueOf(mJLoggerCounterStep));
                    map.put("SensorStep", String.valueOf(mJLoggerSensorStep));
                    map.put("sOffsetStep", String.valueOf(mJLoggerOffsetStep));
                    map.put("SensorCount", String.valueOf(mJLoggerSensorCount));
                    // 增加电量、息屏状态
                    int battery = getBattery();
                    if (battery != -1) {
                        map.put("battery", String.valueOf(battery));
                    }
                    map.put("isScreenOn", String.valueOf(getScreenState()));
                    Log.e("wcd_map", map.toString());
                    JLoggerWraper.onEventInfo(JLoggerConstant.JLOGGER_TYPE_STEP_COUNTER_TIMER, map);
                    sHandler.removeMessages(HANDLER_WHAT_TEST_JLOGGER);
                    sHandler.sendEmptyMessageDelayed(HANDLER_WHAT_TEST_JLOGGER, WHAT_TEST_JLOGGER_DURATION);
                    break;
                }
            }
            return false;
        }
    });

    public StepTodayCounter(Context context, OnStepTodayListener onStepTodayListener, boolean isSeparate, boolean isBoot) {
        this.mContext = context;
        this.mIsSeparate = isSeparate;
        this.mIsBoot = isBoot;
        this.mOnStepTodayListener = onStepTodayListener;

        WakeLockUtils.getLock(mContext);
        mCurrStep = (int) PreferencesHelper.getCurrentStep(mContext);
        mIsCleanStep = PreferencesHelper.getCleanStep(mContext);
        mTodayDate = PreferencesHelper.getStepToday(mContext);
        mOffsetStep = (int) PreferencesHelper.getStepOffset(mContext);
        mIsShutdown = PreferencesHelper.getShutdown(mContext);
        // 开机启动监听到，一定是关机开机了
        boolean isShutdown = shutdownBySystemRunningTime();
        if (mIsBoot || isShutdown) {
            mIsShutdown = true;
            PreferencesHelper.setShutdown(mContext, mIsShutdown);
        }

        HashMap<String, String> map = new HashMap<>();
        map.put("mCurrStep", String.valueOf(mCurrStep));
        map.put("mIsCleanStep", String.valueOf(mIsCleanStep));
        map.put("mTodayDate", String.valueOf(mTodayDate));
        map.put("mOffsetStep", String.valueOf(mOffsetStep));
        map.put("mIsShutdown", String.valueOf(mIsShutdown));
        map.put("isShutdown", String.valueOf(isShutdown));
        map.put("lastSensorStep", String.valueOf(PreferencesHelper.getLastSensorStep(mContext)));
        JLoggerWraper.onEventInfo(JLoggerConstant.JLOGGER_TYPE_STEP_CONSTRUCTOR, map);

        dateChangeCleanStep();

        initBroadcastReceiver();

        updateStepCounter();

        // 启动JLogger日志打印
        sHandler.removeMessages(HANDLER_WHAT_TEST_JLOGGER);
        sHandler.sendEmptyMessageDelayed(HANDLER_WHAT_TEST_JLOGGER, WHAT_TEST_JLOGGER_DURATION);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * @return 获取当前步数
     */
    public int getCurrentStep() {
        mCurrStep = (int) PreferencesHelper.getCurrentStep(mContext);
        return mCurrStep;
    }

    /**
     *
     */
    private void initBroadcastReceiver() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK); // 广播：当前时间已经变化（正常的时间流逝）
        filter.addAction(Intent.ACTION_DATE_CHANGED); // 设备日期发生改变时会发出此广播
        BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                if (Intent.ACTION_TIME_TICK.equals(intent.getAction())
                        || Intent.ACTION_TIME_CHANGED.equals(intent.getAction())) {
                    // service存活做0点分隔
                    dateChangeCleanStep();
                }
            }
        };
        mContext.registerReceiver(mBatInfoReceiver, filter);
    }

    /**
     * @return 是否已经重启过了
     */
    private boolean shutdownBySystemRunningTime() {
        // 上次运行的时间大于当前运行时间判断为重启，只是增加精度，极端情况下连续重启，会判断不出来
        // SystemClock.elapsedRealtime()它是系统启动到当前时刻经过的时间，包括了系统睡眠经过的时间。在CPU休眠之后，它依然保持增长。所以它适合做更加广泛通用的时间间隔的统计。
        if (PreferencesHelper.getElapsedRealtime(mContext) > SystemClock.elapsedRealtime()) {
            JLoggerWraper.onEventInfo(JLoggerConstant.JLOGGER_TYPE_STEP_SHUTDOWNBYSYSTEMRUNNINGTIME, "本地记录的时间，判断进行了关机操作");
            return true;
        }
        return false;
    }

    /**
     * 判断当前日期是否改变或者是否0点分隔，如果是就执行清空
     */
    private synchronized void dateChangeCleanStep() {
        // 当前日期改变了清零，或者0点分隔回调
        if (!getTodayDate().equals(mTodayDate) || mIsSeparate) {
            HashMap<String, String> map = new HashMap<>();
            map.put("getTodayDate()", getTodayDate());
            map.put("mTodayDate", mTodayDate);
            map.put("mSeparate", String.valueOf(mIsSeparate));
            JLoggerWraper.onEventInfo(JLoggerConstant.JLOGGER_TYPE_STEP_COUNTER_DATECHANGECLEANSTEP, map);
            WakeLockUtils.getLock(mContext);

            mIsCleanStep = true;
            PreferencesHelper.setCleanStep(mContext, mIsCleanStep);

            mTodayDate = getTodayDate();
            PreferencesHelper.setStepToday(mContext, mTodayDate);

            mIsShutdown = false;
            PreferencesHelper.setShutdown(mContext, mIsShutdown);

            mIsBoot = false;

            mIsSeparate = false;

            mCurrStep = 0;
            PreferencesHelper.setCurrentStep(mContext, mCurrStep);

            mJLoggerSensorCount = 0;
            mJLoggerCurrStep = 0;

            if (null != mOnStepTodayListener) {
                mOnStepTodayListener.onStepCounterClean();
            }
        }
    }

    /**
     * 更新回调步数
     */
    private void updateStepCounter() {
        // 每次回调都判断一下是否跨天
        dateChangeCleanStep();

        if (null != mOnStepTodayListener) {
            mOnStepTodayListener.onChangeStepCounter(mCurrStep);
        }
    }

    /**
     * @return 电量
     */
    private int getBattery() {
        int battery = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            BatteryManager batteryManager = (BatteryManager) mContext.getSystemService(Context.BATTERY_SERVICE);
            battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
        return battery;
    }

    /**
     * @return 是否息屏
     */
    private boolean getScreenState() {
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        return pm.isScreenOn();
    }

    private String getTodayDate() {
        return DateUtils.getCurrentDate("yyyy-MM-dd");
    }

}
