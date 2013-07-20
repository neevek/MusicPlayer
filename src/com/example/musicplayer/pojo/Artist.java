package com.example.musicplayer.pojo;

/**
 * Created with IntelliJ IDEA.
 * User: neevek
 * Date: 7/20/13
 * Time: 2:45 PM
 */
public class Artist {
    public int id;
    public String name;
    public int songCount;

    public Artist (int id, String name, int songCount) {
        this.id = id;
        this.name = name;
        this.songCount = songCount;
    }
}
