package com.zhongjh.libpedometerstoday;

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
import com.zhongjh.libpedometerstoday.util.SportStepUtils;

public class PedometersTodayService extends Service implements Handler.Callback {

    public static final String INTENT_NAME_BOOT = "intent_name_boot";
    private static final int BROADCAST_REQUEST_CODE = 100; // 点击通知栏广播requestCode
    private static int CURRENT_STEP = 0; // 当前步数

    private SensorManager mSensorManager; // 传感器管理者
    private NotificationManager notificationManager; // 通知栏管理者
    private NotificationApiCompat mNotificationApiCompat;

    @Override
    public void onCreate() {
        super.onCreate();
        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        initNotification(CURRENT_STEP);
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
        PendingIntent contentIntent;
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

        mNotificationApiCompat

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


}
