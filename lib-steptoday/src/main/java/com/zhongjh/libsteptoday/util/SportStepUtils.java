package com.zhongjh.libsteptoday.util;

import android.annotation.SuppressLint;

import com.zhongjh.libsteptoday.store.db.dao.StepTodayDao;
import com.zhongjh.libsteptoday.store.db.entity.StepToday;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * 运动工具类
 */
public class SportStepUtils {

    public static final String SPORT_DATE = "sportDate";
    public static final String STEP_NUM = "stepNum";
    public static final String DISTANCE = "km";
    public static final String CALORIE = "kaluli";
    public static final String TODAY = StepTodayDao.Properties.Today.columnName;

    public static JSONArray getSportStepJsonArray(List<StepToday> todayStepDataArrayList) {
        JSONArray jsonArray = new JSONArray();
        if (null == todayStepDataArrayList || 0 == todayStepDataArrayList.size()) {
            return jsonArray;
        }
        for (int i = 0; i < todayStepDataArrayList.size(); i++) {
            StepToday todayStepData = todayStepDataArrayList.get(i);
            try {
                JSONObject subObject = getJSONObject(todayStepData);
                jsonArray.put(subObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return jsonArray;
    }

    static JSONObject getJSONObject(StepToday todayStepData) throws JSONException {
        JSONObject subObject = new JSONObject();
        subObject.put(TODAY, todayStepData.getToday());
        subObject.put(SPORT_DATE, todayStepData.getDate() / 1000);
        subObject.put(STEP_NUM, todayStepData.getStep());
        subObject.put(DISTANCE, getDistanceByStep(todayStepData.getStep()));
        subObject.put(CALORIE, getCalorieByStep(todayStepData.getStep()));
        return subObject;
    }

    /**
     * 公里计算公式
     */
    @SuppressLint("DefaultLocale")
    public static String getDistanceByStep(long steps) {
        return String.format("%.2f", steps * 0.6f / 1000);
    }

    /**
     * 千卡路里计算公式
     */
    @SuppressLint("DefaultLocale")
    public static String getCalorieByStep(long steps) {
        return String.format("%.1f", steps * 0.6f * 60 * 1.036f / 1000);
    }

}
