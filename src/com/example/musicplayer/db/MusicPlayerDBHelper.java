package com.example.musicplayer.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created with IntelliJ IDEA.
 * User: neevek
 * Date: 7/20/13
 * Time: 11:18 AM
 */
public class MusicPlayerDBHelper extends SQLiteOpenHelper {
    public static String dbName = "main.db";
    public final static int VERSION = 1;

    public MusicPlayerDBHelper(Context context) {
        super(context, dbName, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        initTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    private void initTable (SQLiteDatabase db) {
        String sql = "CREATE TABLE artist_info (_id integer primary key AUTOINCREMENT, name text, song_count integer default 0, UNIQUE(name) ON CONFLICT IGNORE)";
        db.execSQL(sql);
        sql = "CREATE TABLE album_info (_id integer primary key AUTOINCREMENT, name text, song_count integer default 0, UNIQUE(name) ON CONFLICT IGNORE)";
        db.execSQL(sql);
        sql = "CREATE TABLE song_info (_id integer primary key AUTOINCREMENT, title text, artist_id int, artist text, album_id int, album text, duration int, file_path text, UNIQUE(file_path) ON CONFLICT IGNORE)";
        db.execSQL(sql);
    }

}
