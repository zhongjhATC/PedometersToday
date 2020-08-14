package com.zhongjh.libsteptoday.store.db;

import android.annotation.SuppressLint;
import android.content.Context;

import com.zhongjh.libsteptoday.store.db.dao.DaoMaster;
import com.zhongjh.libsteptoday.store.db.dao.DaoSession;

import org.greenrobot.greendao.query.QueryBuilder;

/**
 * 数据库操作辅助类
 * Created by zhongjh on 2015/10/21.
 */
public class DbCore {

    public static final String DEFAULT_DB_NAME = "steptoday_db.db";
    private static DaoMaster daoMaster;
    private static DaoSession daoSession;

    @SuppressLint("StaticFieldLeak")
    private static Context mContext;
    private static String DB_NAME;

    public static void init(Context context) {
        init(context, DEFAULT_DB_NAME);
    }

    /**
     * 需要用的时候改成
     *
     * @param context 上下文
     * @param dbName  数据库名称
     */
    private static void init(Context context, String dbName) {
        if (context == null) {
            throw new IllegalArgumentException("context can't be null");
        }
        mContext = context.getApplicationContext();
        DB_NAME = dbName;
    }

    /**
     * 需要用的时候改成
     *
     * @return DaoMaster
     */
    private static DaoMaster getDaoMaster() {
        if (daoMaster == null) {
            //此处不可用 DaoMaster.DevOpenHelper, 那是开发辅助类，我们要自定义一个，方便升级
            DaoMaster.OpenHelper helper = new SQLiteOpenHelper(mContext, DB_NAME, null);
            //加密
//            daoMaster = new DaoMaster(helper.getEncryptedReadableDb("password"));
            daoMaster = new DaoMaster(helper.getWritableDatabase());
        }
        return daoMaster;
    }

    public static DaoSession getDaoSession() {
        if (daoSession == null) {
            if (daoMaster == null) {
                daoMaster = getDaoMaster();
            }
            daoSession = daoMaster.newSession();
        }
        return daoSession;
    }

    public static void enableQueryBuilderLog() {
        QueryBuilder.LOG_SQL = true;
        QueryBuilder.LOG_VALUES = true;
    }



}
