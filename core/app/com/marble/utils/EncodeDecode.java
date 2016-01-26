package com.marble.utils;

public class EncodeDecode {

    private static final String base64Chars =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    public static String encodeNum(int x) {
        char[] buf = new char[11];
        int p = buf.length;
        do {
            buf[--p] = base64Chars.charAt((x % 64));
            x /= 64;
        } while (x != 0);
        return new String(buf, p, buf.length - p);
    }

    public static long decodeNum(String s) {
        long x = 0;
        for (char c : s.toCharArray()) {
            int charValue = base64Chars.indexOf(c);
            if (charValue == -1) throw new NumberFormatException(s);
            x *= 64;
            x += charValue;
        }
        return x;
    }

}