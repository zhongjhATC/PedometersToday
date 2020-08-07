package com.zhongjh.libpedometerstoday.store.db.dao;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.internal.DaoConfig;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;

import com.zhongjh.libpedometerstoday.store.db.PedometersToday;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "PEDOMETERS_TODAY".
*/
public class PedometersTodayDao extends AbstractDao<PedometersToday, Long> {

    public static final String TABLENAME = "PEDOMETERS_TODAY";

    /**
     * Properties of entity PedometersToday.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property Today = new Property(1, String.class, "today", false, "TODAY");
        public final static Property Date = new Property(2, long.class, "date", false, "DATE");
        public final static Property Step = new Property(3, long.class, "step", false, "STEP");
    }


    public PedometersTodayDao(DaoConfig config) {
        super(config);
    }
    
    public PedometersTodayDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"PEDOMETERS_TODAY\" (" + //
                "\"_id\" INTEGER PRIMARY KEY ," + // 0: id
                "\"TODAY\" TEXT," + // 1: today
                "\"DATE\" INTEGER NOT NULL ," + // 2: date
                "\"STEP\" INTEGER NOT NULL );"); // 3: step
    }

    /** Drops the underlying database table. */
    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"PEDOMETERS_TODAY\"";
        db.execSQL(sql);
    }

    @Override
    protected final void bindValues(DatabaseStatement stmt, PedometersToday entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
 
        String today = entity.getToday();
        if (today != null) {
            stmt.bindString(2, today);
        }
        stmt.bindLong(3, entity.getDate());
        stmt.bindLong(4, entity.getStep());
    }

    @Override
    protected final void bindValues(SQLiteStatement stmt, PedometersToday entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
 
        String today = entity.getToday();
        if (today != null) {
            stmt.bindString(2, today);
        }
        stmt.bindLong(3, entity.getDate());
        stmt.bindLong(4, entity.getStep());
    }

    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    @Override
    public PedometersToday readEntity(Cursor cursor, int offset) {
        PedometersToday entity = new PedometersToday( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
            cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1), // today
            cursor.getLong(offset + 2), // date
            cursor.getLong(offset + 3) // step
        );
        return entity;
    }
     
    @Override
    public void readEntity(Cursor cursor, PedometersToday entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setToday(cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1));
        entity.setDate(cursor.getLong(offset + 2));
        entity.setStep(cursor.getLong(offset + 3));
     }
    
    @Override
    protected final Long updateKeyAfterInsert(PedometersToday entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    @Override
    public Long getKey(PedometersToday entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasKey(PedometersToday entity) {
        return entity.getId() != null;
    }

    @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }
    
}