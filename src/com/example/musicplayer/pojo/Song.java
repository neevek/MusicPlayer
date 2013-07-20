package com.example.musicplayer.pojo;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: neevek
 * Date: 7/20/13
 * Time: 2:35 PM
 */
public class Song implements Serializable {
    private static final long serialVersionUID = -4181598996437174001L;

    public int id;
    public String title;
    public String artist;
    public String album;
    public int duration;
    public String filePath;

    public Song (int id, String title, String artist, String album, int duration, String filePath) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.filePath = filePath;
    }

    @Override
    public boolean equals(Object o) {
        return this.id == ((Song)o).id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
