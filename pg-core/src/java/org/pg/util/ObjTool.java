package org.pg.util;

public class ObjTool {
    public static <T> T coalesce(final T obj1, final T obj2) {
        return (obj1 == null) ? obj2 : obj1;
    }
}
