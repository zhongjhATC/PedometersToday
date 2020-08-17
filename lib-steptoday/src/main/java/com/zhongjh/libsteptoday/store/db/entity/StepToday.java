package com.zhongjh.libsteptoday.store.db.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class StepToday {
    @Id
    private Long id;
    // 当天时间，只显示到天 yyyy-MM-dd
    private String today;
    // 步数时间，显示到毫秒
    private long date;
    // 对应date时间的步数
    private long step;
    @Generated(hash = 1396544854)
    public StepToday(Long id, String today, long date, long step) {
        this.id = id;
        this.today = today;
        this.date = date;
        this.step = step;
    }
    @Generated(hash = 292367325)
    public StepToday() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getToday() {
        return this.today;
    }
    public void setToday(String today) {
        this.today = today;
    }
    public long getDate() {
        return this.date;
    }
    public void setDate(long date) {
        this.date = date;
    }
    public long getStep() {
        return this.step;
    }
    public void setStep(long step) {
        this.step = step;
    }

}
