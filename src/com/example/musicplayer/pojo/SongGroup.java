package com.example.musicplayer.pojo;

/**
 * Created with IntelliJ IDEA.
 * User: neevek
 * Date: 7/21/13
 * Time: 12:35 AM
 */
public class SongGroup {
    public int id;
    public String name;
    public int songCount;

    public SongGroup (int id, String name, int songCount) {
        this.id = id;
        this.name = name;
        this.songCount = songCount;
    }
}
