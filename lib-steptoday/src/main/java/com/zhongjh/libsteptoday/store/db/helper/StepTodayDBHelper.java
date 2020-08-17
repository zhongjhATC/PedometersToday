package com.zhongjh.libsteptoday.store.db.helper;

import android.content.ContentValues;
import android.database.Cursor;

import com.zhongjh.libsteptoday.logger.JLoggerConstant;
import com.zhongjh.libsteptoday.logger.JLoggerWraper;
import com.zhongjh.libsteptoday.store.db.DbCore;
import com.zhongjh.libsteptoday.store.db.entity.StepToday;
import com.zhongjh.libsteptoday.store.db.dao.StepTodayDao;
import com.zhongjh.libsteptoday.util.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StepTodayDBHelper {

    private static final String DATE_PATTERN_YYYY_MM_DD = "yyyy-MM-dd";

    private StepTodayDao stepTodayDao;
    // 只保留mLimit天的数据
    private int mLimit = -1;

    public StepTodayDBHelper() {
        if (this.stepTodayDao == null) {
            this.stepTodayDao = DbCore.getDaoSession().getStepTodayDao();
        }
    }

    /**
     * 根据limit来清除数据库
     * 例如：
     * curDate = 2018-01-10 limit=0;表示只保留2018-01-10当天
     * curDate = 2018-01-10 limit=1;表示保留2018-01-10、2018-01-09当天和前天
     * 以此类推
     *
     * @param curDate 当前时间
     * @param limit   -1失效
     */
    public void clearCapacity(String curDate, int limit) {
        mLimit = limit;
        if (mLimit <= 0) {
            return;
        }

        // 传递的是当前时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(DateUtils.getDateMillis(curDate, DATE_PATTERN_YYYY_MM_DD));
        calendar.add(Calendar.DAY_OF_YEAR, -(mLimit));
        String date = DateUtils.dateFormat(calendar.getTimeInMillis(), DATE_PATTERN_YYYY_MM_DD);
        JLoggerWraper.onEventInfo(JLoggerConstant.JLOGGER_CLEARCAPACITY, date);

        // 把时间小于当前时间的加入 删除列表
        List<StepToday> todayStepDataList = getQueryAll();
        Set<String> delDateSet = new HashSet<>();
        for (StepToday item : todayStepDataList) {
            long dbTodayDate = DateUtils.getDateMillis(item.getToday(), DATE_PATTERN_YYYY_MM_DD);
            if (calendar.getTimeInMillis() >= dbTodayDate) {
                delDateSet.add(item.getToday());
            }
        }

        // 对这个删除列表进行循环删除
        for (String delDate : delDateSet) {
            stepTodayDao.getSession().getDatabase().
                    execSQL("DELETE FROM " + StepTodayDao.TABLENAME + " WHERE " + StepTodayDao.Properties.Today.columnName + " = ?",
                            new String[]{delDate});
        }
    }

    /**
     * 根据时间和当天步数判断是否存在
     *
     * @param stepToday today 和 step
     * @return 是否存在
     */
    public boolean isExist(StepToday stepToday) {
        Cursor cursor = stepTodayDao.getSession().getDatabase().rawQuery("SELECT * FROM " + StepTodayDao.TABLENAME
                        + " WHERE " + StepTodayDao.Properties.Today.columnName + " = ? AND " + StepTodayDao.Properties.Step.columnName + " = ?",
                new String[]{stepToday.getToday(), stepToday.getStep() + ""});
        boolean exist = cursor.getCount() > 0;
        cursor.close();
        return exist;
    }

    /**
     * @return 查询所有
     */
    public List<StepToday> getQueryAll() {
        return stepTodayDao.queryBuilder().list();
    }

    /**
     * 添加
     *
     * @param stepToday 实体
     */
    public void insert(StepToday stepToday) {
        stepTodayDao.insert(stepToday);
    }

    /**
     * 删除所有数据
     */
    public void deleteAll() {
        stepTodayDao.deleteAll();
    }

    /**
     * 根据时间获取最大步数
     *
     * @return stepToday
     */
    public StepToday getMaxStepByDate(long millis) {
        Cursor cursor = stepTodayDao.getSession().getDatabase().rawQuery("SELECT * FROM " + StepTodayDao.TABLENAME
                        + " WHERE " + StepTodayDao.Properties.Today.columnName + " = ? ORDER BY " + StepTodayDao.Properties.Step.columnName + " DESC",
                new String[]{DateUtils.dateFormat(millis, "yyyy-MM-dd")});
        StepToday stepToday = null;
        if (cursor.getCount() > 0) {
            cursor.moveToNext();
            stepToday = getTodayStepData(cursor);
        }
        return null;
    }

    /**
     * 根据时间获取步数列表
     *
     * @param dateString 格式yyyy-MM-dd
     * @return 步数列表
     */
    public List<StepToday> getStepListByDate(String dateString) {
        Cursor cursor = stepTodayDao.getSession().getDatabase().rawQuery("SELECT * FROM " + StepTodayDao.TABLENAME
                        + " WHERE " + StepTodayDao.Properties.Today.columnName + " = ?",
                new String[]{dateString});
        List<StepToday> todayStepDatas = getTodayStepDataList(cursor);
        cursor.close();
        return todayStepDatas;
    }

    /**
     * 查询时间范围的StepToday
     *
     * @param startDate 起始时间
     * @param days      结束时间天数
     * @return stepToday列表
     */
    public List<StepToday> getStepListByStartDateAndDays(String startDate, int days) {
        Calendar calendarStart = Calendar.getInstance();
        calendarStart.setTimeInMillis(DateUtils.getDateMillis(startDate, DATE_PATTERN_YYYY_MM_DD));
        Calendar calendarEnd = Calendar.getInstance();
        calendarEnd.setTimeInMillis(DateUtils.getDateMillis(startDate, DATE_PATTERN_YYYY_MM_DD));
        calendarEnd.add(Calendar.DAY_OF_YEAR, days);
        Cursor cursor = stepTodayDao.getSession().getDatabase().rawQuery("SELECT * FROM " + StepTodayDao.TABLENAME
                        + " WHERE " + StepTodayDao.Properties.Today.columnName + " >= ? and " + StepTodayDao.Properties.Today.columnName + " <= ?",
                new String[]{DateUtils.dateFormat(calendarStart.getTimeInMillis(), DATE_PATTERN_YYYY_MM_DD),
                        DateUtils.dateFormat(calendarEnd.getTimeInMillis(), DATE_PATTERN_YYYY_MM_DD)});
        List<StepToday> todayStepDatas = getTodayStepDataList(cursor);
        cursor.close();
        return todayStepDatas;
    }

    /**
     * 根据cursor设置实体
     *
     * @param cursor cursor
     * @return 实体
     */
    private StepToday getTodayStepData(Cursor cursor) {
        String today = cursor.getString(cursor.getColumnIndex(StepTodayDao.Properties.Today.columnName));
        long date = cursor.getLong(cursor.getColumnIndex(StepTodayDao.Properties.Date.columnName));
        long step = cursor.getLong(cursor.getColumnIndex(StepTodayDao.Properties.Step.columnName));
        StepToday stepToday = new StepToday();
        stepToday.setToday(today);
        stepToday.setDate(date);
        stepToday.setStep(step);
        return stepToday;
    }

    /**
     * 根据cursor设置列表
     *
     * @param cursor cursor
     * @return 实体列表
     */
    private List<StepToday> getTodayStepDataList(Cursor cursor) {
        List<StepToday> todayStepDatas = new ArrayList<>();
        while (cursor.moveToNext()) {
            StepToday todayStepData = getTodayStepData(cursor);
            todayStepDatas.add(todayStepData);
        }
        return todayStepDatas;
    }

}
