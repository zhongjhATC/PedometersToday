package com.zhongjh.libsteptoday.util;

import java.text.ParseException;
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
     * @param pattern "yyyy-MM-dd HH:mm:ss E"
     * @return 返回一定格式的当前时间
     */
    public static String getCurrentDate(String pattern) {
        getDateFormat().applyPattern(pattern);
        Date date = new Date(System.currentTimeMillis());
        return getDateFormat().format(date);
    }

    /**
     * @param dateString 時間的文本
     * @param pattern    格式
     * @return 毫秒
     */
    public static long getDateMillis(String dateString, String pattern) {
        long millionSeconds = 0;
        getDateFormat().applyPattern(pattern);
        try {
            millionSeconds = getDateFormat().parse(dateString).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        // 毫秒
        return millionSeconds;
    }

    /**
     *
     *
     * @param millis 毫秒时间
     * @param pattern yyyy-MM-dd HH:mm:ss E
     * @return 格式化输入的millis
     */
    public static String dateFormat(long millis, String pattern) {
        getDateFormat().applyPattern(pattern);
        Date date = new Date(millis);
        return getDateFormat().format(date);
    }

}
