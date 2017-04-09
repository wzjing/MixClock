package com.infinitytech.classicalmix;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class Db extends SQLiteOpenHelper {

    public static final String STEP_TABLE = "user_steps";
    public static final String STEP_DB = "step_db";

    public Db(Context context, int version){
        super(context, STEP_DB, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = String.format(
                "create table if not exists %s(" +
                        "_id INTEGER primary key auto_increment," +
                        "date DATE NOT NULL," +
                        "device TEXT NOT NULL," +
                        "step INTEGER NOT NULL)", STEP_TABLE);
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = String.format("drap table %s if exists", STEP_TABLE);
        db.execSQL(sql);
        onCreate(db);
    }
}
