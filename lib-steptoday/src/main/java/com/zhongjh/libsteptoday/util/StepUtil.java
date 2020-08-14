package com.zhongjh.libsteptoday.util;

import java.util.Date;

public class StepUtil {

    /**
     * 是否上传步数，23:55:50~00:05:50分无法上传步数
     * @return true可以上传，false不能上传
     */
    public static boolean isUploadStep() {

        Date curDate = new Date(System.currentTimeMillis());

        long mills2355 = DateUtils.getDateMillis(DateUtils.getCurrentDate("yyyy-MM-dd") + " 23:55:50", "yyyy-MM-dd HH:mm:ss");
        Date date2355 = new Date(mills2355);

        if (curDate.after(date2355)) {
            return false;
        }

        long mills0005 = DateUtils.getDateMillis(DateUtils.getCurrentDate("yyyy-MM-dd") + " 00:05:50", "yyyy-MM-dd HH:mm:ss");
        Date date0005 = new Date(mills0005);

        if (curDate.before(date0005)) {
            return false;
        }

        return true;
    }

}
