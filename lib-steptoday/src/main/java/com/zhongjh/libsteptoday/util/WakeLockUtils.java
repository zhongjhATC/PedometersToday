package com.zhongjh.libsteptoday.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.PowerManager;

import com.zhongjh.libsteptoday.StepTodayService;

/**
 * PowerManager 用来控制设备的电源状态. 而PowerManager.WakeLock 也称作唤醒锁, 是一种保持 CPU 运转防止设备休眠的方式.
 * <p>
 * 我们经常需要应用保持高亮, 比如看小说. 或者即使屏幕关闭后台也能保持运行, 比如播放MP3. 这里就需要使用 PowerManager 的 WakeLock 机制.
 * <p>
 * 如果只是需要保持屏幕开启, 比如阅读器应用或者游戏, 可以在 activity 中使用 FLAG_KEEP_SCREEN_ON. 唤醒锁更加倾向于后台服务, 运转 CPU 在休眠之前完成某些特定任务. 比如下载或者mp3播放.
 * <p>
 * 官方文档里也说明了在 activity 里从不使用唤醒锁, 只使用 FLAG_KEEP_SCREEN_ON.
 */
public class WakeLockUtils {

    private static PowerManager.WakeLock mWakeLock;

    @SuppressLint("WakelockTimeout")
    public synchronized static PowerManager.WakeLock getLock(Context context) {
        if (mWakeLock != null) {
            // isHeld() 返回锁的状态，true为正被持锁，false为被释放或未被持锁；
            if (mWakeLock.isHeld())
                // release()    释放锁；
                mWakeLock.release();
            mWakeLock = null;
        }

        PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        // 新建一个名为tag的levelAndFlags类型的wakelock 锁定资源
        mWakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, StepTodayService.class.getName());
        // 设置wakelock的计数机制，默认为计数。true为计数，false为不计数 计数：每一个acquire必须对应一个release； 不计数：无论有多少个acquire，一个release就可以释放。
        mWakeLock.setReferenceCounted(true);
        // 持锁，不限制时间无限锁定
        mWakeLock.acquire();

        return mWakeLock;
    }

}
