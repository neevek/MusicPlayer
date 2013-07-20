package com.example.musicplayer.util;

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
}
