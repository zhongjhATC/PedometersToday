package com.zhongjh.libsteptoday.store.db.helper;

import android.database.Cursor;

import com.zhongjh.libsteptoday.logger.JLoggerConstant;
import com.zhongjh.libsteptoday.logger.JLoggerWraper;
import com.zhongjh.libsteptoday.store.db.DbCore;
import com.zhongjh.libsteptoday.store.db.entity.StepToday;
import com.zhongjh.libsteptoday.store.db.dao.StepTodayDao;
import com.zhongjh.libsteptoday.util.DateUtils;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StepTodayDBHelper implements IStepTodayDBHelper {

    private static final String DATE_PATTERN_YYYY_MM_DD = "yyyy-MM-dd";

    private StepTodayDao stepTodayDao;
    // 只保留mLimit天的数据
    private int mLimit = -1;

    public StepTodayDBHelper() {
        if (this.stepTodayDao == null) {
            this.stepTodayDao = DbCore.getDaoSession().getStepTodayDao();
        }
    }

    @Override
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
        JLoggerWraper.onEventInfo(JLoggerConstant.JLOGGER_CLEARCAPACITY,date);

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

    @Override
    public boolean isExist(StepToday stepToday) {

        return false;
    }

    @Override
    public void insert(StepToday stepToday) {

    }

    @Override
    public StepToday getMaxStepByDate(long millis) {
        return null;
    }

    /**
     *
     * @return 查询所有
     */
    @Override
    public List<StepToday> getQueryAll() {
        return stepTodayDao.queryBuilder().list();
    }

    @Override
    public List<StepToday> getStepListByDate(String dateString) {
        return null;
    }

    @Override
    public List<StepToday> getStepListByStartDateAndDays(String startDate, int days) {
        return null;
    }

}
