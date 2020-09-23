package com.zhongjh.libsteptoday.logger;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import com.apkfuns.log2file.LogFileEngineFactory;
import com.apkfuns.logutils.LogLevel;
import com.apkfuns.logutils.LogUtils;
import com.apkfuns.logutils.file.LogFileFilter;

import java.util.HashMap;
import java.util.Map;

public class JLoggerWraper {

    /**
     * 初始化日志配置
     *
     * @param application application初始化
     */
    public static void initXLog(Application application) {
        LogUtils.getLogConfig()
                .configAllowLog(true)  // 是否在Logcat显示日志
                .configTagPrefix("LogUtilsDemo") // 配置统一的TAG 前缀
                .configFormatTag("%d{HH:mm:ss:SSS} %t %c{-5}") // 首行显示信息(可配置日期，线程等等)
                .configShowBorders(true) // 是否显示边框
                .configLevel(LogLevel.TYPE_VERBOSE); // 配置可展示日志等级

        // 支持输入日志到文件
        String filePath;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            filePath = application.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/LogUtils/logs/";
        } else {
            filePath = application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/LogUtils/logs/";
        }
        LogUtils.getLog2FileConfig()
                .configLog2FileEnable(true)  // 是否输出日志到文件
                .configLogFileEngine(new LogFileEngineFactory(application)) // 日志文件引擎实现
                .configLog2FilePath(filePath)  // 日志路径
                .configLog2FileNameFormat("app-%d{yyyyMMdd}.txt") // 日志文件名称
                .configLog2FileLevel(LogLevel.TYPE_VERBOSE) // 文件日志等级
                .configLogFileFilter(new LogFileFilter() {  // 文件日志过滤
                    @Override
                    public boolean accept(int level, String tag, String logContent) {
                        return true;
                    }
                });
    }

    public synchronized static void onEventInfo(String eventID, String label) {
        if (!TextUtils.isEmpty(label)) {
            LogUtils.d(String.format("%s : %s", eventID, label));
        } else {
            LogUtils.d(eventID);
        }
    }

    public synchronized static final void onEventInfo(String eventID) {
        onEventInfo(eventID, "");
    }

    /**
     * 以map形式写日志
     *
     * @param eventID eventID
     * @param map     map
     */
    public synchronized static void onEventInfo(String eventID, Map<String, String> map) {
        onEventInfo(eventID, map.toString());
    }

    public synchronized static final void flush() {
//        JLogger.flush();
    }

    public synchronized static final void deviceInfo(Context context) {
        Map<String, String> map = new HashMap<>();
        map.put("BRAND", Build.BRAND);  //samsung
        map.put("MANUFACTURER", Build.MANUFACTURER);//samsung
        map.put("MODEL", Build.MODEL);//SM-G9500
        map.put("PRODUCT", Build.PRODUCT);//dreamqltezc
        map.put("RELEASE", android.os.Build.VERSION.RELEASE);//8.0.0
        map.put("SDK_INT", String.valueOf(Build.VERSION.SDK_INT));//26
        map.put("APP_Version", Utils.getAppVersion(context));
        map.put("APP_Build", Utils.getAppVersionCode(context));
        //1. 手机具体型号，设备信息
        //2. 早上打开，晚上打开
        onEventInfo(JLoggerConstant.JLOGGER_DEVICE_INFO, map);
    }
}
