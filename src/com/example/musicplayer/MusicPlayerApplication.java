package com.example.musicplayer;

import android.app.Application;
import com.example.musicplayer.db.MusicPlayerDAO;
import com.example.musicplayer.db.MusicPlayerDBHelper;
import com.example.musicplayer.handler.MainHandler;

/**
 * Created with IntelliJ IDEA.
 * User: neevek
 * Date: 7/20/13
 * Time: 1:20 PM
 */
public class MusicPlayerApplication extends Application {
    private static MusicPlayerApplication mInstance;

    private MainHandler mMainHandler;
    private MusicPlayerDAO mMusicPlayerDAO;

    @Override
    public void onCreate() {
        super.onCreate();

        mInstance = this;

        mMainHandler = new MainHandler();

        mMusicPlayerDAO = new MusicPlayerDAO(new MusicPlayerDBHelper(this));
    }

    public static MusicPlayerApplication getInstance () {
        return mInstance;
    }

    public MainHandler getMainHandler () {
        return mMainHandler;
    }

    public MusicPlayerDAO getMusicPlayerDAO () {
        return mMusicPlayerDAO;
    }
}
