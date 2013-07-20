package com.example.musicplayer.handler;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Created with IntelliJ IDEA.
 * User: neevek
 * Date: 7/20/13
 * Time: 1:16 PM
 */
public class MessageQueueHandler extends Handler implements Runnable {

    private Handler mHandler;


    public MessageQueueHandler () {

        new Thread(this).start();
    }

    @Override
    public void handleMessage(Message msg) {
    }

    @Override
    public void run() {
        try {
            Looper.prepare();
            mHandler = new Handler(Looper.myLooper());
            Looper.loop();

        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
