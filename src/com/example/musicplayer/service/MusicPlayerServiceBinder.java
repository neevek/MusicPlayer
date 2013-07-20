package com.example.musicplayer.service;

import android.os.Binder;

/**
 * Created with IntelliJ IDEA.
 * User: neevek
 * Date: 7/20/13
 * Time: 6:00 PM
 */
public class MusicPlayerServiceBinder extends Binder {
    private MusicPlayerService service;
    public MusicPlayerServiceBinder (MusicPlayerService service) {
        this.service = service;
    }
    public MusicPlayerService getMusicPlayerService () {
        return service;
    }
}
