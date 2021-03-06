package com.zhongjh.steptoday;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zhongjh.libsteptoday.ISportStepInterface;
import com.zhongjh.libsteptoday.StepTodayForeground;
import com.zhongjh.libsteptoday.StepTodayManager;
import com.zhongjh.libsteptoday.StepTodayService;
import com.zhongjh.libsteptoday.sensor.OnStepTodayListener;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainServiceActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";

    private static final int REFRESH_STEP_WHAT = 0;

    // 循环取当前时刻的步数中间的间隔时间
    private long TIME_INTERVAL_REFRESH = 3000;

    private ViewHolder mViewHolder;
    private Handler mDelayHandler = new Handler(new TodayStepCounterCall());
    private int mStepSum;
    private ISportStepInterface iSportStepInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mViewHolder = new ViewHolder(MainServiceActivity.this);

        // 权限请求
        if (ContextCompat.checkSelfPermission(MainServiceActivity.this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            MainServiceActivityPermissionsDispatcher.permissionWriteExternalStorageWithPermissionCheck(MainServiceActivity.this);
        } else {
            init();
        }

    }

    private void init() {
        // 判断如果是4.4并且支持
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && getStepCounter()) {
            new StepTodayForeground(this, new OnStepTodayListener() {
                @Override
                public void onChangeStepCounter(int step) {
                    mStepSum = step;
                    updateStepCount();
                }

                @Override
                public void onStepCounterClean() {

                }
            });
        } else {
            // 初始化计步模块
            StepTodayManager.startStepTodayService(getApplication());

            // 开启计步Service，同时绑定Activity进行aidl通信
            Intent intent = new Intent(this, StepTodayService.class);
            startService(intent);
            bindService(intent, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    // Activity和Service通过aidl进行通信
                    iSportStepInterface = ISportStepInterface.Stub.asInterface(service);
                    try {
                        mStepSum = iSportStepInterface.getCurrentTimeSportStep();
                        updateStepCount();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    mDelayHandler.sendEmptyMessageDelayed(REFRESH_STEP_WHAT, TIME_INTERVAL_REFRESH);

                }

                @Override
                public void onServiceDisconnected(ComponentName name) {

                }
            }, Context.BIND_AUTO_CREATE);
        }

        // 计时器
        mhandmhandlele.post(timeRunable);
    }

    /**
     * @return 是否有stepcounter支持
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private boolean getStepCounter() {
        Sensor countSensor = ((SensorManager) this.getSystemService(SENSOR_SERVICE)).getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        return null != countSensor;
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.stepArrayButton: {
                //获取所有步数列表
                if (null != iSportStepInterface) {
                    try {
                        String stepArray = iSportStepInterface.getTodaySportStepArray();
                        mViewHolder.stepArrayTextView.setText(stepArray);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
            default:
                break;
        }
    }

    class TodayStepCounterCall implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH_STEP_WHAT: {
                    // 每隔500毫秒获取一次计步数据刷新UI
                    if (null != iSportStepInterface) {
                        int step = 0;
                        try {
                            step = iSportStepInterface.getCurrentTimeSportStep();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        if (mStepSum != step) {
                            mStepSum = step;
                            updateStepCount();
                        }
                    }
                    mDelayHandler.sendEmptyMessageDelayed(REFRESH_STEP_WHAT, TIME_INTERVAL_REFRESH);

                    break;
                }
            }
            return false;
        }
    }

    private void updateStepCount() {
        Log.d(TAG, "updateStepCount : " + mStepSum);
        TextView stepTextView = (TextView) findViewById(R.id.stepTextView);
        stepTextView.setText(mStepSum + "步");
    }

    /***************** 计时器 beginregion *******************/
    private Runnable timeRunable = new Runnable() {
        @Override
        public void run() {

            currentSecond = currentSecond + 1000;
            mViewHolder.timeTextView.setText(getFormatHMS(currentSecond));
            if (!isPause) {
                //递归调用本runable对象，实现每隔一秒一次执行任务
                mhandmhandlele.postDelayed(this, 1000);
            }
        }
    };
    //计时器
    private Handler mhandmhandlele = new Handler();
    private boolean isPause = false;//是否暂停
    private long currentSecond = 0;//当前毫秒数
    /***************** 计时器 endregion *******************/

    /**
     * 根据毫秒返回时分秒
     *
     * @param time
     * @return
     */
    public static String getFormatHMS(long time) {
        time = time / 1000;//总秒数
        int s = (int) (time % 60);//秒
        int m = (int) (time / 60);//分
        int h = (int) (time / 3600);//秒
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    /**
     * NeedsPermission 请求权限不然计步不了
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @NeedsPermission({Manifest.permission.ACTIVITY_RECOGNITION})
    public void permissionWriteExternalStorage() {
        // 申请成功，进行相应操作
        init();
    }

    /**
     * 注解在用于向用户解释为什么需要调用该权限的方法上，只有当第一次请求权限被用户拒绝，下次请求权限之前会调用
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @OnShowRationale({Manifest.permission.ACTIVITY_RECOGNITION})
    public void permissionWriteExternalStorageRationale(PermissionRequest request) {
        // 再次请求
        new AlertDialog.Builder(MainServiceActivity.this)
                .setMessage("说明：你必须给权限才能正常运行")
                .setPositiveButton("立即允许", (dialog, which) -> request.proceed())
                .show();
    }

    /**
     * OnPermissionDenied 注解在当用户拒绝了权限请求时需要调用的方法上
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @OnPermissionDenied({Manifest.permission.ACTIVITY_RECOGNITION})
    public void permissionWriteExternalStorageDenied() {
        // 再次请求
        MainServiceActivityPermissionsDispatcher.permissionWriteExternalStorageWithPermissionCheck(MainServiceActivity.this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        MainServiceActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    public static class ViewHolder {
        public View rootView;
        public Button stepArrayButton;
        public TextView stepArrayTextView;
        public TextView stepTextView;
        public TextView timeTextView;

        public ViewHolder(MainServiceActivity rootView) {
            this.stepArrayButton = (Button) rootView.findViewById(R.id.stepArrayButton);
            this.stepArrayTextView = (TextView) rootView.findViewById(R.id.stepArrayTextView);
            this.stepTextView = (TextView) rootView.findViewById(R.id.stepTextView);
            this.timeTextView = (TextView) rootView.findViewById(R.id.timeTextView);
        }

    }
}
