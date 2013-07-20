package com.example.musicplayer.handler;

import android.os.Handler;
import android.os.Message;

/**
 * Created with IntelliJ IDEA.
 * User: neevek
 * Date: 7/20/13
 * Time: 1:16 PM
 */
public class MainHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
        ((Runnable) msg.obj).run();
    }

    public void sendAction (Runnable runnable) {
        Message msg = Message.obtain();
        msg.obj = runnable;
        this.sendMessage(msg);
    }
}
