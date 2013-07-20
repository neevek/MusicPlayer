package com.example.musicplayer.util;

import com.example.musicplayer.pojo.Album;
import com.example.musicplayer.pojo.Artist;
import com.example.musicplayer.pojo.Song;
import com.example.musicplayer.pojo.SongGroup;

import java.text.Collator;
import java.text.RuleBasedCollator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: neevek
 * Date: 7/20/13
 * Time: 11:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class Util {
    private final static Pattern singleQuotePatern = Pattern.compile("'");


    public static String escapeDBSingleQuotes(String s) {
        if (s == null)
            return null;

        return singleQuotePatern.matcher(s).replaceAll("\''");
    }

    public static Object ensureNotNull (Object obj, Object defaultValue) {
        if (obj == null)
            obj = defaultValue;
        return obj;
    }

    public static String ensureNotNull (String obj, String defaultValue) {
        if (obj == null)
            obj = defaultValue;
        return obj;
    }

    public static String formatMilliseconds(int milliseconds, StringBuilder buffer) {
        int seconds = milliseconds / 1000;

        if (buffer == null)
            buffer = new StringBuilder();

        int minutes = seconds / 60;
        seconds = seconds % 60;

        if (minutes == 0) {
            buffer.append("00");
        } else {
            if (minutes < 10)
                buffer.append('0');
            buffer.append(minutes);
        }

        buffer.append(':');

        if (seconds == 0) {
            buffer.append("00");
        } else {
            if (seconds < 10)
                buffer.append('0');
            buffer.append(seconds);
        }

        return buffer.toString();
    }

    private static RuleBasedCollator mChineseSortCollator = (RuleBasedCollator)Collator.getInstance(Locale.CHINA);

    public static void sortSongList (List<Song> songList) {
        Collections.sort(songList, new Comparator<Song>() {
            @Override
            public int compare(Song lhs, Song rhs) {
                return mChineseSortCollator.compare(lhs.title, rhs.title);
            }
        });
    }

    public static void sortAlbumList (List<Album> albumList) {
        Collections.sort(albumList, new Comparator<Album>() {
            @Override
            public int compare(Album lhs, Album rhs) {
                return mChineseSortCollator.compare(lhs.name, rhs.name);
            }
        });
    }

    public static void sortArtistList (List<Artist> albumList) {
        Collections.sort(albumList, new Comparator<Artist>() {
            @Override
            public int compare(Artist lhs, Artist rhs) {
                return mChineseSortCollator.compare(lhs.name, rhs.name);
            }
        });
    }
}
