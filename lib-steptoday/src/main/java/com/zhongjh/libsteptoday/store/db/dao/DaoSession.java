package com.zhongjh.libsteptoday.store.db.dao;

import java.util.Map;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.AbstractDaoSession;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.identityscope.IdentityScopeType;
import org.greenrobot.greendao.internal.DaoConfig;

import com.zhongjh.libsteptoday.store.db.entity.StepToday;

import com.zhongjh.libsteptoday.store.db.dao.StepTodayDao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see org.greenrobot.greendao.AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig stepTodayDaoConfig;

    private final StepTodayDao stepTodayDao;

    public DaoSession(Database db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        stepTodayDaoConfig = daoConfigMap.get(StepTodayDao.class).clone();
        stepTodayDaoConfig.initIdentityScope(type);

        stepTodayDao = new StepTodayDao(stepTodayDaoConfig, this);

        registerDao(StepToday.class, stepTodayDao);
    }
    
    public void clear() {
        stepTodayDaoConfig.clearIdentityScope();
    }

    public StepTodayDao getStepTodayDao() {
        return stepTodayDao;
    }

}
