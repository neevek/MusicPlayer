package com.example.musicplayer.message;

import android.util.Log;
import com.example.musicplayer.MusicPlayerApplication;
import com.example.musicplayer.handler.MainHandler;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Created with IntelliJ IDEA.
 * User: neevek
 * Date: 7/20/13
 * Time: 14:50 AM
 */
public class MessagePump extends Thread {
    private final static boolean DEBUG = false;
    private final static String TAG = "MessagePump >>>";

    private PriorityBlockingQueue<Message> mMsgPump;
    private List<List<WeakReference<MessageCallback>>> mMsgAndObserverList;

    private MainHandler mMainHandler;

    public MessagePump() {
        mMainHandler = MusicPlayerApplication.getInstance().getMainHandler();
        mMsgPump = new PriorityBlockingQueue<Message>(100, new Comparator<Message>() {
            public int compare(Message o1, Message o2) {
                return o1.priority < o2.priority ? -1 : o1.priority > o2.priority ? 1 : 1;
            }

        });

        mMsgAndObserverList = new ArrayList<List<WeakReference<MessageCallback>>>(Collections.<List<WeakReference<MessageCallback>>>nCopies(Message.Type.values().length, null));

        // start the background thread to process messages.
        start();
        if (DEBUG) Log.d(TAG, "started MessagePump");
    }

    public void destroyMessagePump () {
        if (DEBUG) Log.d(TAG, "start destroying MessagePump");
        // this message is used to destroy the message center,
        // we use the "Poison Pill Shutdown" approach, see: http://stackoverflow.com/a/812362/668963
        broadcastMessage(Message.Type.DESTROY_MESSAGE_PUMP, null, Message.PRIORITY_EXTREMELY_HIGH);
    }

    @Override
    public void run() {
        Thread.currentThread().setPriority(MIN_PRIORITY);
        dispatchMessages();
        if (DEBUG) Log.d(TAG, "destroyed MessagePump");
    }

    public synchronized void register (Message.Type msgType, MessageCallback callback) {
        List<WeakReference<MessageCallback>> observerList = mMsgAndObserverList.get(msgType.ordinal());
        if (observerList == null) {
            observerList = new ArrayList<WeakReference<MessageCallback>>();
            mMsgAndObserverList.set(msgType.ordinal(), observerList);
        }
        if (indexOf(callback, observerList) == -1)
            observerList.add(new WeakReference<MessageCallback>(callback));
    }

    private int indexOf (MessageCallback callback, List<WeakReference<MessageCallback>> observerList) {
        for (int i = 0; i < observerList.size(); ++i) {
            if (observerList.get(i).get() == callback)
                return i;
        }
        return -1;
    }

    public synchronized void unregister (Message.Type msgType, MessageCallback callback) {
        List<WeakReference<MessageCallback>> observerList = mMsgAndObserverList.get(msgType.ordinal());
        if (observerList != null) {
            int index = indexOf(callback, observerList);
            if (index != -1) {
                observerList.remove(index);
            }
        }
    }


    public synchronized void unregister (MessageCallback callback) {
        Message.Type[] types = Message.Type.values();
        for (int i = 0; i < types.length; ++i) {
            unregister(types[i], callback);
        }
    }

    public void broadcastMessage(Message.Type msgType, Object data) {
        mMsgPump.put(Message.obtainMessage(msgType, data, Message.PRIORITY_NORMAL, null));
    }

    public void broadcastMessage(Message.Type msgType, Object data, int priority) {
        mMsgPump.put(Message.obtainMessage(msgType, data, priority, null));
    }

    public void broadcastMessage(Message.Type msgType, Object data, int priority, Object sender) {
        mMsgPump.put(Message.obtainMessage(msgType, data, priority, sender));
    }

    private void dispatchMessages() {
        while (true) {
            try {
                final Message message = mMsgPump.take();
                if (message.type == Message.Type.DESTROY_MESSAGE_PUMP)
                    break;

                final List<WeakReference<MessageCallback>> observerList = mMsgAndObserverList.get(message.type.ordinal());
                if (observerList != null && observerList.size() > 0) {
                    message.referenceCount = observerList.size();
                    for (int i = 0; i < observerList.size(); ++i) {
                        final MessageCallback callback = observerList.get(i).get();
                        if (callback == null) {
                            observerList.remove(i);
                            --i;

                            if (--message.referenceCount == 0) {
                                message.recycle();
                            }
                        } else {
                            mMainHandler.sendAction(new Runnable() {
                                @Override
                                public void run() {
                                    // call the target on the UI thread
                                    callback.onReceiveMessage(message);

                                    // recycle the Message object
                                    if (--message.referenceCount == 0) {
                                        message.recycle();
                                    }
                                }
                            });
                        }
                    }
                } else {
                    message.recycle();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}