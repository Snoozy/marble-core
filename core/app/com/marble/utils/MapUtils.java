package com.marble.utils;

import java.util.Map;

import org.boon.Boon;
import org.boon.Boon.*;

public class MapUtils {

    public static Map<String, String> deserializeMap(String s) {
        return Boon.fromJson(s, Map.class);
    }

    public static String serializeMap(Map<String, String> m) {
        return Boon.toJson(m);
    }

}