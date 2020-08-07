package com.zhongjh.libpedometerstoday;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.zhongjh.libpedometerstoday.notification.BaseClickBroadcast;
import com.zhongjh.libpedometerstoday.notification.NotificationApiCompat;
import com.zhongjh.libpedometerstoday.sensor.PedometersTodaySensorEventListener;
import com.zhongjh.libpedometerstoday.util.SportStepUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class PedometersTodayService extends Service implements Handler.Callback {

    public static final String INTENT_NAME_BOOT = "intent_name_boot";
    public static final String INTENT_STEP_INIT = "intent_step_init";
    private static final int NOTIFY_ID = 1000; // 步数通知ID
    private static final String STEP_CHANNEL_ID = "stepChannelId"; // 通知栏id
    private static final int BROADCAST_REQUEST_CODE = 100; // 点击通知栏广播requestCode
    private static int CURRENT_STEP = 0; // 当前步数

    private PedometersTodaySensorEventListener mPedometersTodaySensorEventListener; // 加速度传感器计算当天步数，需要保持后台Service

    private boolean mIsBoot = false; // 是否开机启动

    private SensorManager mSensorManager; // 传感器管理者
    private NotificationManager notificationManager; // 通知栏管理者
    private NotificationApiCompat mNotificationApiCompat;

    @Override
    public void onCreate() {
        super.onCreate();
        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        initNotification(CURRENT_STEP);
//        getSensorRate();
    }

    /**
     * onCreate方法只有第一次会调用，onStartCommand和onStart每次都被调用。onStartCommand会告诉系统如何重启服务，如判断是否异常终止后重新启动，在何种情况下异常终止
     * 它们的含义分别是：
     * 1):START_STICKY：如果service进程被kill掉，保留service的状态为开始状态，但不保留递送的intent对象。随后系统会尝试重新创建service，由于服务状态为开始状态，所以创建服务后一定会调用onStartCommand(Intent,int,int)方法。如果在此期间没有任何启动命令被传递到service，那么参数Intent将为null。
     * 2):START_NOT_STICKY：“非粘性的”。使用这个返回值时，如果在执行完onStartCommand后，服务被异常kill掉，系统不会自动重启该服务
     * 3):START_REDELIVER_INTENT：重传Intent。使用这个返回值时，如果在执行完onStartCommand后，服务被异常kill掉，系统会自动重启该服务，并将Intent的值传入。
     * 4):START_STICKY_COMPATIBILITY：START_STICKY的兼容版本，但不保证服务被kill后一定能重启。
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null != intent) {
            mIsBoot = intent.getBooleanExtra(INTENT_NAME_BOOT, false);
            String setStep = intent.getStringExtra(INTENT_STEP_INIT);
            if (!TextUtils.isEmpty(setStep)) {
                try {
                    setSteps(Integer.parseInt(setStep));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 创建通知栏
     */
    private synchronized void initNotification(int currentStep) {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // 通知的icon
        int smallIcon = getResources().getIdentifier("icon_step_small", "mipmap", getPackageName());
        if (0 == smallIcon) {
            smallIcon = R.drawable.ic_all_out_black_24dp;
        }
        int largeIcon = getResources().getIdentifier("ic_launcher", "mipmap", getPackageName());
        Bitmap largeIconBitmap = null;
        if (0 != largeIcon) {
            largeIconBitmap = BitmapFactory.decodeResource(getResources(), largeIcon);
        } else {
            largeIconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_all_out_black_24dp);
        }

        // 通知的点击事件
        String receiverName = getReceiver(getApplicationContext());
        PendingIntent contentIntent = null;
        if (!TextUtils.isEmpty(receiverName)) {
            try {
                // 实例化使用者的通知栏点击
                contentIntent = PendingIntent.getBroadcast(this, BROADCAST_REQUEST_CODE, new Intent(this, Class.forName(receiverName)), PendingIntent.FLAG_UPDATE_CURRENT);
            } catch (Exception e) {
                e.printStackTrace();
                // 无通知栏点击
                contentIntent = PendingIntent.getBroadcast(this, BROADCAST_REQUEST_CODE, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
            }
        }

        // 通知内容
        String km = SportStepUtils.getDistanceByStep(currentStep);
        String calorie = SportStepUtils.getCalorieByStep(currentStep);
        String contentText = calorie + " 千卡  " + km + " 公里";

        mNotificationApiCompat = new NotificationApiCompat.Builder(this,
                notificationManager, // 通知管理器
                STEP_CHANNEL_ID, // 通知id
                getString(R.string.step_channel_name), // 通道名称
                smallIcon) // 图标
                .setContentIntent(contentIntent) // 点击事件
                .setContentText(contentText) // 通知内容
                .setContentTitle(getString(R.string.title_notification_bar, String.valueOf(currentStep))) // 通知标题
                .setTicker(getString(R.string.app_name)) // 设置的是通知时在状态bai栏显示的通知内容，一般du是一段文字，例如在状态zhi栏显示“您有一条短信，dao待查收
                .setOngoing(true) // 设置为ture，表示它为一个正在进行的通知。他们通常是用来表示 一个后台任务,用户积极参与(如播放音乐)或以某种方式正在等待,因此占用设备(如一个文件下载, 同步操作,主动网络连接)
                .setPriority(Notification.PRIORITY_MIN) // 通知的优先级，数值越大越高
                .setLargeIcon(largeIconBitmap) // 设置通知的大图标，当下拉通知后显示的图标
                .setOnlyAlertOnce(true) // 以便您的通知仅在通知第一次出现时才会中断用户（有声音，振动或视觉线索），而不是以后的更新。
                .builder();
        mNotificationApiCompat.startForeground(this, NOTIFY_ID);
    }

    /**
     * @return 获取使用者自定义的通知栏点击包，因为不知道具体是哪个点击包，所以递归查找基类属于BaseClickBroadcast的
     */
    public static String getReceiver(Context context) {
        try {
            // 获取该包下的所有接收器
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_RECEIVERS);
            ActivityInfo[] activityInfos = packageInfo.receivers;
            if (null != activityInfos && activityInfos.length > 0) {
                for (int i = 0; i < activityInfos.length; i++) {
                    String receiverName = activityInfos[i].name;
                    Class superClazz = Class.forName(receiverName).getSuperclass();
                    int count = 1;
                    while (null != superClazz) {
                        if (superClazz.getName().equals("java.lang.Object")) {
                            break;
                        }
                        if (superClazz.getName().equals(BaseClickBroadcast.class.getName())) {
                            return receiverName;
                        }
                        if (count > 20) {
                            // 用来做容错，如果20个基类还不到Object直接跳出防止while死循环
                            break;
                        }
                        count++;
                        superClazz = superClazz.getSuperclass();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        return false;
    }

    /**
     * 设置步数初始值，目前只支持设置用加速度传感器进行计步
     *
     * @param steps
     */
    private void setSteps(int steps) {
        if (null != mPedometersTodaySensorEventListener) {
            mPedometersTodaySensorEventListener.setCurrentStep(steps);
        }
    }

}
