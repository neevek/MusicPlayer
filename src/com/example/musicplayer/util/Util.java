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
}
