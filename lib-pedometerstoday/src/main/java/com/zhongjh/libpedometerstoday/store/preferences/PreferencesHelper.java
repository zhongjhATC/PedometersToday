package com.zhongjh.libpedometerstoday.store.preferences;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesHelper {

    public static final String APP_SHARD = "pedometers_today_share_prefs";

    // 上一次计步器步数的时间
    public static final String LAST_SENSOR_TIME = "last_sensor_time";
    // 步数补偿数值，每次传感器返回的步数-offset=当前步数
    public static final String STEP_OFFSET = "step_offset";
    // 当天，用来判断是否跨天
    public static final String STEP_TODAY = "step_today";
    // 当前步数
    public static final String CURR_STEP = "curr_step";

    /**
     * Get SharedPreferences
     */
    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(APP_SHARD, Context.MODE_PRIVATE);
    }

    public static void setLastSensorStep(Context context, float lastSensorStep) {
        getSharedPreferences(context).edit().putFloat(LAST_SENSOR_TIME, lastSensorStep).apply();
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


}
