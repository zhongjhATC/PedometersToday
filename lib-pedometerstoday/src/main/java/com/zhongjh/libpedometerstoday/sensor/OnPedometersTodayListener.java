package com.zhongjh.libpedometerstoday.sensor;

/**
 * 计步监听器
 */
public interface OnPedometersTodayListener {

    /**
     * 用于显示步数
     * @param step 步数
     */
    void onChangeStepCounter(int step);

    /**
     * 步数清零监听，由于跨越0点需要重新计步
     */
    void onStepCounterClean();

}
