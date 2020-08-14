package com.zhongjh.libsteptoday.store.db.helper;

import com.zhongjh.libsteptoday.store.db.entity.StepToday;

import java.util.List;


interface IStepTodayDBHelper {

    void clearCapacity(String curDate, int limit);

    boolean isExist(StepToday stepToday);

    void insert(StepToday stepToday);

    StepToday getMaxStepByDate(long millis);

    List<StepToday> getQueryAll();

    List<StepToday> getStepListByDate(String dateString);

    List<StepToday> getStepListByStartDateAndDays(String startDate, int days);
}
