package com.zhongjh.libsteptoday;

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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.zhongjh.libsteptoday.logger.JLoggerConstant;
import com.zhongjh.libsteptoday.logger.JLoggerWraper;
import com.zhongjh.libsteptoday.notification.BaseClickBroadcast;
import com.zhongjh.libsteptoday.notification.NotificationApiCompat;
import com.zhongjh.libsteptoday.sensor.OnStepTodayListener;
import com.zhongjh.libsteptoday.sensor.StepTodayCounter;
import com.zhongjh.libsteptoday.sensor.StepTodaySensor;
import com.zhongjh.libsteptoday.store.db.entity.StepToday;
import com.zhongjh.libsteptoday.store.db.helper.StepTodayDBHelper;
import com.zhongjh.libsteptoday.util.DateUtils;
import com.zhongjh.libsteptoday.util.SportStepUtils;
import com.zhongjh.libsteptoday.util.StepUtil;
import com.zhongjh.libsteptoday.util.WakeLockUtils;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.zhongjh.libsteptoday.util.SportStepUtils.getCalorieByStep;
import static com.zhongjh.libsteptoday.util.SportStepUtils.getDistanceByStep;

/**
 * 流程图 https://www.jianshu.com/p/cfc2a200e46d
 */
public class StepTodayService extends Service implements Handler.Callback {

    private static final int SAMPLING_PERIOD_US = SensorManager.SENSOR_DELAY_FASTEST; // 传感器刷新频率
    public static final String INTENT_NAME_BOOT = "intent_name_boot";
    public static final String INTENT_STEP_INIT = "intent_step_init";
    private static final int NOTIFY_ID = 1000; // 步数通知ID
    private static final int HANDLER_WHAT_SAVE_STEP = 0; // 运动停止保存步数
    private static final int DB_SAVE_COUNTER = 300; // 保存数据库频率
    private static final int LAST_SAVE_STEP_DURATION = 10 * 1000; // 如果走路如果停止，10秒钟后保存数据库
    private static final int HANDLER_WHAT_REFRESH_NOTIFY_STEP = 2; // 刷新通知栏步数
    private static final int REFRESH_NOTIFY_STEP_DURATION = 3 * 1000; // 刷新通知栏步数，3s一次
    private static final String STEP_CHANNEL_ID = "stepChannelId"; // 通知栏id
    private static final int BROADCAST_REQUEST_CODE = 100; // 点击通知栏广播requestCode
    private static int CURRENT_STEP = 0; // 当前步数
    private StepTodayCounter mStepTodayCounter; // Sensor.TYPE_STEP_COUNTER 计步传感器计算当天步数，不需要后台Service
    private StepTodaySensor mStepTodaySensor; // 加速度传感器计算当天步数，需要保持后台Service
    private int mDbSaveCount = 0; // 保存数据库计数器
    private boolean mIsSeparate = false; // 是否分隔的
    private boolean mIsBoot = false; // 是否开机启动

    private SensorManager mSensorManager; // 传感器管理者
    private NotificationManager notificationManager; // 通知栏管理者
    private NotificationApiCompat mNotificationApiCompat;

    private StepTodayDBHelper mStepTodayDBHelper; // 数据库
    private final Handler mHandler = new Handler(this); // handler

    @Override
    public void onCreate() {
        super.onCreate();
        mStepTodayDBHelper = new StepTodayDBHelper();
        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        initNotification(CURRENT_STEP);
//        getSensorRate();  反射API29会出现异常
        Map<String, String> map = getLogMap();
        map.put("current_step", String.valueOf(CURRENT_STEP));
        JLoggerWraper.onEventInfo(JLoggerConstant.JLOGGER_SERVICE_INITIALIZE_CURRSTEP, map);
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
            if (setStep != null && !TextUtils.isEmpty(setStep)) {
                try {
                    setSteps(Integer.parseInt(setStep));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }

        mDbSaveCount = 0;

        Map<String, String> map = getLogMap();
        map.put("current_step", String.valueOf(CURRENT_STEP));
        map.put("mSeparate", String.valueOf(mIsSeparate));
        map.put("mBoot", String.valueOf(mIsBoot));
        map.put("mDbSaveCount", String.valueOf(mDbSaveCount));
        JLoggerWraper.onEventInfo(JLoggerConstant.JLOGGER_SERVICE_ONSTARTCOMMAND, map);

        // 更新通知
        updateNotification(CURRENT_STEP);
        // 注册传感器
        startStepDetector();

        mHandler.removeMessages(HANDLER_WHAT_REFRESH_NOTIFY_STEP);
        mHandler.sendEmptyMessageDelayed(HANDLER_WHAT_REFRESH_NOTIFY_STEP, REFRESH_NOTIFY_STEP_DURATION);

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Map<String, String> map = getLogMap();
        map.put("current_step", String.valueOf(CURRENT_STEP));
        JLoggerWraper.onEventInfo(JLoggerConstant.JLOGGER_SERVICE_ONBIND, map);
        mHandler.removeMessages(HANDLER_WHAT_REFRESH_NOTIFY_STEP);
        mHandler.sendEmptyMessageDelayed(HANDLER_WHAT_REFRESH_NOTIFY_STEP, REFRESH_NOTIFY_STEP_DURATION);
        return mIBinder.asBinder();
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
                for (ActivityInfo activityInfo : activityInfos) {
                    String receiverName = activityInfo.name;
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
        switch (msg.what) {
            case HANDLER_WHAT_SAVE_STEP: {
                // 走路停止保存数据库
                mDbSaveCount = 0;
                saveDb(true, CURRENT_STEP);
                break;
            }
            case HANDLER_WHAT_REFRESH_NOTIFY_STEP: {
                // 刷新通知栏
                updateTodayStep(CURRENT_STEP);
                mHandler.removeMessages(HANDLER_WHAT_REFRESH_NOTIFY_STEP);
                mHandler.sendEmptyMessageDelayed(HANDLER_WHAT_REFRESH_NOTIFY_STEP, REFRESH_NOTIFY_STEP_DURATION);
                break;
            }
            default:
                break;
        }
        return false;
    }

    /**
     * 注册传感器
     */
    private void startStepDetector() {
        // android4.4以后如果有stepcounter可以使用计步传感器
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && getStepCounter()) {
            addStepCounterListener();
        } else {
            addBasePedoListener();
        }
    }

    /**
     * 使用Android自帶的计步传感器
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void addStepCounterListener() {
        if (null != mStepTodayCounter) {
            WakeLockUtils.getLock(this); // 锁定CPU
            CURRENT_STEP = mStepTodayCounter.getCurrentStep(); // 获取当前步数
            updateNotification(CURRENT_STEP); // 更新通知栏
            // 输出日志
            Map<String, String> map = getLogMap();
            map.put("current_step", String.valueOf(CURRENT_STEP));
            JLoggerWraper.onEventInfo(JLoggerConstant.JLOGGER_SERVICE_TYPE_STEP_COUNTER_HADREGISTER, map);
            return;
        }
        // 判断计步传感器Counter是否可用
        Sensor countSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (null == countSensor) {
            return;
        }
        mStepTodayCounter = new StepTodayCounter(getApplicationContext(), mOnStepTodayListener, mIsSeparate, mIsBoot);
        CURRENT_STEP = mStepTodayCounter.getCurrentStep();
        // 传感器注册
        boolean registerSuccess = mSensorManager.registerListener(mStepTodayCounter, countSensor, SAMPLING_PERIOD_US);

        // 打印日志
        Map<String, String> map = getLogMap();
        map.put("current_step", String.valueOf(CURRENT_STEP));
        map.put("current_step_registerSuccess", String.valueOf(registerSuccess));
        JLoggerWraper.onEventInfo(JLoggerConstant.JLOGGER_SERVICE_TYPE_STEP_COUNTER_REGISTER, map);
    }

    /**
     * 低版本，没有计步传感器，就用加速传感器
     */
    private void addBasePedoListener() {
        if (null != mStepTodaySensor) {
            WakeLockUtils.getLock(this); // 锁定CPU
            CURRENT_STEP = mStepTodaySensor.getCurrentStep(); // 获取当前步数
            updateNotification(CURRENT_STEP);
            Map<String, String> map = getLogMap();
            map.put("current_step", String.valueOf(CURRENT_STEP));
            JLoggerWraper.onEventInfo(JLoggerConstant.JLOGGER_SERVICE_TYPE_ACCELEROMETER_HADREGISTER, map);
            return;
        }
        // 没有计步器的时候开启定时器保存数据
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (null == sensor) {
            return;
        }
        mStepTodaySensor = new StepTodaySensor(this, mOnStepTodayListener);
        CURRENT_STEP = mStepTodaySensor.getCurrentStep();
        // 获得传感器的类型，这里获得的类型是加速度传感器 此方法用来注册，只有注册过才会生效，参数：SensorEventListener的实例，Sensor的实例，更新速率
        boolean registerSuccess = mSensorManager.registerListener(mStepTodaySensor, sensor, SAMPLING_PERIOD_US);
        // 日志
        Map<String, String> map = getLogMap();
        map.put("current_step", String.valueOf(CURRENT_STEP));
        map.put("current_step_registerSuccess", String.valueOf(registerSuccess));
        JLoggerWraper.onEventInfo(JLoggerConstant.JLOGGER_SERVICE_TYPE_ACCELEROMETER_REGISTER, map);
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
        Bitmap largeIconBitmap;
        if (0 != largeIcon) {
            largeIconBitmap = BitmapFactory.decodeResource(getResources(), largeIcon);
        } else {
            largeIconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_all_out_black_24dp);
        }

        // 通知的点击事件
        String receiverName = getReceiver(getApplicationContext());
        PendingIntent contentIntent = null;
        if (receiverName != null && !TextUtils.isEmpty(receiverName)) {
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
        String km = getDistanceByStep(currentStep);
        String calorie = getCalorieByStep(currentStep);
        String contentText = calorie + " 千卡  " + km + " 公里";

        NotificationApiCompat.Builder notificationApiCompatBuilder = new NotificationApiCompat.Builder(this,
                notificationManager, // 通知管理器
                STEP_CHANNEL_ID, // 通知id
                getString(R.string.step_channel_name), // 通道名称
                smallIcon) // 图标
                .setContentIntent(contentIntent) // 点击事件
                .setContentText(contentText) // 通知内容
                .setContentTitle(getString(R.string.title_notification_bar, String.valueOf(currentStep))) // 通知标题
                .setTicker(getString(R.string.app_name)) // 设置的是通知时在状态bai栏显示的通知内容，一般du是一段文字，例如在状态zhi栏显示“您有一条短信，dao待查收
                .setOngoing(true) // 设置为ture，表示它为一个正在进行的通知。他们通常是用来表示 一个后台任务,用户积极参与(如播放音乐)或以某种方式正在等待,因此占用设备(如一个文件下载, 同步操作,主动网络连接)
                .setLargeIcon(largeIconBitmap) // 设置通知的大图标，当下拉通知后显示的图标
                .setOnlyAlertOnce(true); // 以便您的通知仅在通知第一次出现时才会中断用户（有声音，振动或视觉线索），而不是以后的更新。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notificationApiCompatBuilder.setPriority(Notification.PRIORITY_MIN); // 通知的优先级，数值越大越高
        }
        mNotificationApiCompat = notificationApiCompatBuilder.builder();
        mNotificationApiCompat.startForeground(this, NOTIFY_ID);
    }

    /**
     * 更新通知
     *
     * @param stepCount 当前步数
     */
    private synchronized void updateNotification(int stepCount) {
        if (null != mNotificationApiCompat) {
            String km = getDistanceByStep(stepCount);
            String calorie = getCalorieByStep(stepCount);
            String contentText = calorie + " 千卡  " + km + " 公里";
            mNotificationApiCompat.updateNotification(NOTIFY_ID, getString(R.string.title_notification_bar, String.valueOf(stepCount)), contentText);
        }
    }

    /**
     * 步数每次回调的方法
     *
     * @param currentStep 当前步数
     */
    private void updateTodayStep(int currentStep) {
        CURRENT_STEP = currentStep;
        updateNotification(CURRENT_STEP);
        saveStep(currentStep);
    }

    private void saveStep(int currentStep) {
        mHandler.removeMessages(HANDLER_WHAT_SAVE_STEP);
        mHandler.sendEmptyMessageDelayed(HANDLER_WHAT_SAVE_STEP, LAST_SAVE_STEP_DURATION);

        if (DB_SAVE_COUNTER > mDbSaveCount) {
            mDbSaveCount++;

            return;
        }
        mDbSaveCount = 0;

        saveDb(false, currentStep);
    }

    /**
     * @param handler     true handler回调保存步数，否false
     * @param currentStep 当前步数
     */
    private void saveDb(boolean handler, int currentStep) {
        StepToday todayStepData = new StepToday();
        todayStepData.setToday(getTodayDate());
        todayStepData.setDate(System.currentTimeMillis());
        todayStepData.setStep(currentStep);
        if (null != mStepTodayDBHelper) {
            if (!handler || !mStepTodayDBHelper.isExist(todayStepData)) {
                mStepTodayDBHelper.insert(todayStepData);
                Map<String, String> map = getLogMap();
                map.put("saveDb_currentStep", String.valueOf(currentStep));
                JLoggerWraper.onEventInfo(JLoggerConstant.JLOGGER_SERVICE_INSERT_DB, map);
            }
        }
    }

    /**
     * 清空数据
     */
    private void cleanDb() {
        Map<String, String> map = getLogMap();
        map.put("cleanDB_current_step", String.valueOf(CURRENT_STEP));
        JLoggerWraper.onEventInfo(JLoggerConstant.JLOGGER_SERVICE_CLEAN_DB, map);
        mDbSaveCount = 0;

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

    /**
     * 设置步数初始值，目前只支持设置用加速度传感器进行计步
     *
     * @param steps 步数
     */
    private void setSteps(int steps) {
        if (null != mStepTodaySensor) {
            mStepTodaySensor.setCurrentStep(steps);
        }
    }

    /**
     * service的bind
     */
    private final ISportStepInterface.Stub mIBinder = new ISportStepInterface.Stub() {

        private JSONArray getSportStepJsonArray(List<StepToday> todayStepDataArrayList) {
            return SportStepUtils.getSportStepJsonArray(todayStepDataArrayList);
        }

        /**
         * @return 获取当前时间运动步数
         */
        @Override
        public int getCurrentTimeSportStep() {
            return CURRENT_STEP;
        }

        /**
         * @return 获取所有步数列表，json格式，如果数据过多建议在线程中获取，否则会阻塞UI线程
         */
        @Override
        public String getTodaySportStepArray() {
            if (null != mStepTodayDBHelper) {
                List<StepToday> todayStepDataArrayList = mStepTodayDBHelper.getQueryAll();
                JSONArray jsonArray = getSportStepJsonArray(todayStepDataArrayList);
                return jsonArray.toString();
            }
            return null;
        }
    };

    /**
     * 事件
     */
    private OnStepTodayListener mOnStepTodayListener = new OnStepTodayListener() {
        @Override
        public void onChangeStepCounter(int step) {
            if (StepUtil.isUploadStep()) {
                CURRENT_STEP = step;
            }
        }

        @Override
        public void onStepCounterClean() {
            CURRENT_STEP = 0;
            updateNotification(CURRENT_STEP);
            cleanDb();
        }

    };

    private String getTodayDate() {
        return DateUtils.getCurrentDate("yyyy-MM-dd");
    }

    private Map<String, String> map;

    /**
     * @return 日志map
     */
    private Map<String, String> getLogMap() {
        if (map == null) {
            map = new HashMap<>();
        } else {
            map.clear();
        }
        return map;
    }

}
