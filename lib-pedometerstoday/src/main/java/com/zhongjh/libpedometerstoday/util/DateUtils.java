package com.zhongjh.libpedometerstoday.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

    // 这种写法变成线程安全的，多个线程都用到SimpleDateFormat https://baijiahao.baidu.com/s?id=1669465540942472303&wfr=spider&for=pc
    private static ThreadLocal<SimpleDateFormat> SIMPLE_DATE_FORMAT = new ThreadLocal<>();

    public static SimpleDateFormat getDateFormat() {
        SimpleDateFormat df = SIMPLE_DATE_FORMAT.get();
        if (df == null) {
            df = new SimpleDateFormat();
            SIMPLE_DATE_FORMAT.set(df);
        }
        return df;
    }

    /**
     * 返回一定格式的当前时间
     *
     * @param pattern "yyyy-MM-dd HH:mm:ss E"
     * @return
     */
    public static String getCurrentDate(String pattern) {
        getDateFormat().applyPattern(pattern);
        Date date = new Date(System.currentTimeMillis());
        String dateString = getDateFormat().format(date);
        return dateString;

    }

}
