package com.zhongjh.libsteptoday.store.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.zhongjh.libsteptoday.store.db.dao.DaoMaster;

import org.greenrobot.greendao.database.Database;

public class SQLiteOpenHelper extends DaoMaster.OpenHelper {

    Context mContext;

    public SQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory) {
        super(context, name, factory);
        mContext = context;
    }

    @Override
    public void onCreate(Database db) {
        super.onCreate(db);
    }



}
