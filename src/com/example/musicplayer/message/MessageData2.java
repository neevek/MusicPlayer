package com.example.musicplayer.message;

/**
 * Created with IntelliJ IDEA.
 * User: xiejm
 * Date: 7/20/13
 * Time: 22:57 PM
 */
public class MessageData2<T1, T2> {
    public T1 o1;
    public T2 o2;
    public MessageData2(T1 o1, T2 o2) {
        this.o1 = o1;
        this.o2 = o2;
    }
    public MessageData2() { }
}
