package com.zhongjh.steptoday;

import android.app.Application;

import com.zhongjh.libsteptoday.logger.JLoggerWraper;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        JLoggerWraper.initXLog(this);
    }

}
