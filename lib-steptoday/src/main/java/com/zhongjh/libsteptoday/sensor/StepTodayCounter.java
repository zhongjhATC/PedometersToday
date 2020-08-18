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
import java.util.Map;

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
    private boolean mCounterStepReset = true; //用来标记对象第一次创建
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
                    Log.d("wcd_map", map.toString());
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
        // 计步器（记录历史步数累加值）
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            // 传感器步数
            int counterStep = (int) event.values[0];
            if (mIsCleanStep) {
                // TODO:只有传感器回调才会记录当前传感器步数，然后对当天步数进行清零，所以步数会少，少的步数等于传感器启动需要的步数，假如传感器需要10步进行启动，那么就少10步
                Map<String, String> map = new HashMap<>();
                map.put("clean_before_sCurrStep", String.valueOf(mCurrStep));
                map.put("clean_before_sOffsetStep", String.valueOf(mOffsetStep));
                map.put("clean_before_mCleanStep", String.valueOf(mIsCleanStep));
                cleanStep(counterStep);
                map.put("clean_after_sCurrStep", String.valueOf(mCurrStep));
                map.put("clean_after_sOffsetStep", String.valueOf(mOffsetStep));
                map.put("clean_after_mCleanStep", String.valueOf(mIsCleanStep));
                JLoggerWraper.onEventInfo(JLoggerConstant.JLOGGER_TYPE_STEP_CLEANSTEP, map);
            } else {
                // 处理关机启动
                if (mIsShutdown || shutdownByCounterStep(counterStep)) {
                    Map<String, String> map = new HashMap<>();
                    map.put("shutdown_before_mShutdown", String.valueOf(mIsShutdown));
                    map.put("shutdown_before_mCounterStepReset", String.valueOf(mCounterStepReset));
                    map.put("shutdown_before_sOffsetStep", String.valueOf(mOffsetStep));
                    shutdown(counterStep);
                    map.put("shutdown_after_mShutdown", String.valueOf(mIsShutdown));
                    map.put("shutdown_after_mCounterStepReset", String.valueOf(mCounterStepReset));
                    map.put("shutdown_after_sOffsetStep", String.valueOf(mOffsetStep));
                    JLoggerWraper.onEventInfo(JLoggerConstant.JLOGGER_TYPE_STEP_SHUTDOWN, map);
                }
            }
            mCurrStep = counterStep - mOffsetStep;

            if (mCurrStep < 0) {
                Map<String, String> map = new HashMap<>();
                map.put("tolerance_before_counterStep", String.valueOf(counterStep));
                map.put("tolerance_before_sCurrStep", String.valueOf(mCurrStep));
                map.put("tolerance_before_sOffsetStep", String.valueOf(mOffsetStep));
                // 容错处理，无论任何原因步数不能小于0，如果小于0，直接清零
                cleanStep(counterStep);
                map.put("tolerance_after_counterStep", String.valueOf(counterStep));
                map.put("tolerance_after_sCurrStep", String.valueOf(mCurrStep));
                map.put("tolerance_after_sOffsetStep", String.valueOf(mOffsetStep));
                JLoggerWraper.onEventInfo(JLoggerConstant.JLOGGER_TYPE_STEP_TOLERANCE, map);
            }

            PreferencesHelper.setCurrentStep(mContext, mCurrStep);
            PreferencesHelper.setElapsedRealtime(mContext, SystemClock.elapsedRealtime());
            PreferencesHelper.setLastSensorStep(mContext, counterStep);

            mJLoggerSensorStep = event.values[0];
            mJLoggerCounterStep = counterStep;
            mJLoggerCurrStep = mCurrStep;
            mJLoggerOffsetStep = mOffsetStep;
            updateStepCounter();
            if (mJLoggerSensorCount == 0) {
                sHandler.removeMessages(HANDLER_WHAT_TEST_JLOGGER);
                sHandler.sendEmptyMessageDelayed(HANDLER_WHAT_TEST_JLOGGER, 800);
            }
            // 用来判断传感器是否回调
            mJLoggerSensorCount++;
        }
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
     * 初始化时间改变广播
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
     * 清除步数
     *
     * @param counterStep 步数
     */
    private void cleanStep(int counterStep) {
        // 清除步数，步数归零，优先级最高
        mCurrStep = 0;
        mOffsetStep = counterStep;
        PreferencesHelper.setStepOffset(mContext, mOffsetStep);

        mIsCleanStep = false;
        PreferencesHelper.setCleanStep(mContext, mIsCleanStep);
        mJLoggerCurrStep = mCurrStep;
        mJLoggerOffsetStep = mOffsetStep;
    }

    /**
     * 当天关机后的处理
     *
     * @param counterStep 步数
     */
    private void shutdown(int counterStep) {
        int tmpCurrStep = (int) PreferencesHelper.getCurrentStep(mContext);
        // 重新设置offset
        mOffsetStep = counterStep - tmpCurrStep;
        //  TODO 只有在当天进行过关机，才会进入到这，直接置反??
        PreferencesHelper.setStepOffset(mContext, mOffsetStep);
        mIsShutdown = false;
        PreferencesHelper.setShutdown(mContext, mIsShutdown);
    }

    /**
     * 判断当前传感器步数是否小于上次的，如果是就重新启动了
     *
     * @param counterStep 传感器步数
     * @return 是否重新启动
     */
    private boolean shutdownByCounterStep(int counterStep) {
        if (mCounterStepReset) {
            // 只判断一次
            mCounterStepReset = false;
            if (counterStep < PreferencesHelper.getLastSensorStep(mContext)) {
                JLoggerWraper.onEventInfo(JLoggerConstant.JLOGGER_TYPE_STEP_SHUTDOWNBYCOUNTERSTEP, "当前传感器步数小于上次传感器步数");
                // 当前传感器步数小于上次传感器步数肯定是重新启动了，只是用来增加精度不是绝对的
                return true;
            }
        }
        return false;
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
            mJLoggerCurrStep = mCurrStep;

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
