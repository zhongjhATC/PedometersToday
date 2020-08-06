package com.zhongjh.libpedometerstoday.util;

import android.annotation.SuppressLint;

public class SportStepUtils {

    /**
     * 公里计算公式
     */
    @SuppressLint("DefaultLocale")
    public static String getDistanceByStep(long steps) {
        return String.format("%.2f", steps * 0.6f / 1000);
    }

    /**
     * 千卡路里计算公式
     */
    @SuppressLint("DefaultLocale")
    public static String getCalorieByStep(long steps) {
        return String.format("%.1f", steps * 0.6f * 60 * 1.036f / 1000);
    }

}
