package com.zhongjh.libsteptoday.store.preferences;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesHelper {

    public static final String APP_SHARD = "step_today_share_prefs";

    // 上一次计步器步数的时间
    public static final String LAST_SENSOR_TIME = "last_sensor_time";
    // 步数补偿数值，每次传感器返回的步数-offset=当前步数
    public static final String STEP_OFFSET = "step_offset";
    // 当天，用来判断是否跨天
    public static final String STEP_TODAY = "step_today";
    // 是否清除步数
    public static final String CLEAN_STEP = "clean_step";
    // 当前步数
    public static final String CURR_STEP = "curr_step";
    // 手机关机监听
    public static final String SHUTDOWN = "shutdown";
    // 系统运行时间
    public static final String ELAPSED_REALTIMEl = "elapsed_realtime";

    /**
     * Get SharedPreferences
     */
    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(APP_SHARD, Context.MODE_PRIVATE);
    }

    public static void setLastSensorStep(Context context, float lastSensorStep) {
        getSharedPreferences(context).edit().putFloat(LAST_SENSOR_TIME, lastSensorStep).apply();
    }

    public static float getLastSensorStep(Context context) {
        return getSharedPreferences(context).getFloat(LAST_SENSOR_TIME, 0.0f);
    }

    public static void setStepOffset(Context context, float stepOffset) {
        getSharedPreferences(context).edit().putFloat(STEP_OFFSET, stepOffset).apply();
    }

    public static float getStepOffset(Context context) {
        return getSharedPreferences(context).getFloat(STEP_OFFSET, 0.0f);
    }

    /**
     * 当前步数
     */
    public static void setCurrentStep(Context context, float currStep) {
        getSharedPreferences(context).edit().putFloat(CURR_STEP, currStep).apply();
    }

    /**
     * 当前步数
     */
    public static float getCurrentStep(Context context) {
        return getSharedPreferences(context).getFloat(CURR_STEP, 0.0f);
    }

    /**
     * 当天，用来判断是否跨天
     */
    public static void setStepToday(Context context, String stepToday) {
        getSharedPreferences(context).edit().putString(STEP_TODAY, stepToday).apply();
    }

    /**
     * 当天，用来判断是否跨天
     */
    public static String getStepToday(Context context) {
        return getSharedPreferences(context).getString(STEP_TODAY, "");
    }

    /**
     * true清除步数从0开始，false否
     *
     * @param context 上下文
     * @param cleanStep 是否清除步数
     */
    public static void setCleanStep(Context context, boolean cleanStep) {
        getSharedPreferences(context).edit().putBoolean(CLEAN_STEP, cleanStep).apply();
    }

    /**
     * true 清除步数，false否
     *
     * @param context 上下文
     * @return 是否清除步数
     */
    public static boolean getCleanStep(Context context) {
        return getSharedPreferences(context).getBoolean(CLEAN_STEP, true);
    }

    public static void setShutdown(Context context, boolean shutdown) {
        getSharedPreferences(context).edit().putBoolean(SHUTDOWN, shutdown).apply();
    }

    public static boolean getShutdown(Context context) {
        return getSharedPreferences(context).getBoolean(SHUTDOWN, false);
    }

    public static void setElapsedRealtime(Context context, long elapsedRealtime) {
        getSharedPreferences(context).edit().putLong(ELAPSED_REALTIMEl, elapsedRealtime).commit();
    }

    public static long getElapsedRealtime(Context context) {
        return getSharedPreferences(context).getLong(ELAPSED_REALTIMEl, 0L);
    }

}
