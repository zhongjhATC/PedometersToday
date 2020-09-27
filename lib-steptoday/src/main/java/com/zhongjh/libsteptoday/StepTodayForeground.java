package com.zhongjh.libsteptoday;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;

import com.zhongjh.libsteptoday.sensor.OnStepTodayListener;
import com.zhongjh.libsteptoday.sensor.StepTodayCounter;
import com.zhongjh.libsteptoday.store.db.helper.StepTodayDBHelper;
import com.zhongjh.libsteptoday.util.StepUtil;

import androidx.annotation.RequiresApi;

import static android.content.Context.SENSOR_SERVICE;

/**
 * 用于前端Activity计步，不用Service
 */
public class StepTodayForeground implements OnStepTodayListener {

    private Context mContext;
    private SensorManager mSensorManager; // 传感器管理者
    private static final int SAMPLING_PERIOD_US = SensorManager.SENSOR_DELAY_FASTEST; // 传感器刷新频率
    private StepTodayDBHelper mStepTodayDBHelper; // 数据库
    private StepTodayCounter mStepTodayCounter; // Sensor.TYPE_STEP_COUNTER 计步传感器计算当天步数，不需要后台Service
    private int mCurrentStep = 0; // 当前步数
    private OnStepTodayListener mOnStepTodayListener; // 步数事件


    public StepTodayForeground(Context context,OnStepTodayListener onStepTodayListener) {
        mContext = context;
        mOnStepTodayListener = onStepTodayListener;
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        mStepTodayDBHelper = new StepTodayDBHelper();
        mSensorManager = (SensorManager) mContext.getSystemService(SENSOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && getStepCounter()) {
            addStepCounterListener();
        }
    }

    /**
     * 使用Android自帶的计步传感器
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void addStepCounterListener() {
        if (null != mStepTodayCounter) {
            mCurrentStep = mStepTodayCounter.getCurrentStep(); // 获取当前步数
            return;
        }
        // 判断计步传感器Counter是否可用
        Sensor countSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (null == countSensor) {
            return;
        }
        mStepTodayCounter = new StepTodayCounter(mContext, this, false, false);
        mCurrentStep = mStepTodayCounter.getCurrentStep();
        // 传感器注册
        mSensorManager.registerListener(mStepTodayCounter, countSensor, SAMPLING_PERIOD_US);
    }

    @Override
    public void onChangeStepCounter(int step) {
        if (StepUtil.isUploadStep()) {
            mCurrentStep = step;
        }
        mOnStepTodayListener.onChangeStepCounter(mCurrentStep);
    }

    @Override
    public void onStepCounterClean() {
        mCurrentStep = 0;
        cleanDb();
        mOnStepTodayListener.onStepCounterClean();
    }

    /**
     * 清空数据
     */
    private void cleanDb() {
        if (null != mStepTodayDBHelper) {
            // 清空数据
            mStepTodayDBHelper.deleteAll();
        }
    }

    /**
     * @return 是否有stepcounter支持
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private boolean getStepCounter() {
        Sensor countSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        return null != countSensor;
    }


}
