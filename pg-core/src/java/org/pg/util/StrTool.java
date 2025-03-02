package org.pg.util;

import java.util.ArrayList;
import java.util.List;

public class StrTool {

    private final static int TRUNCATE_LIMIT = 128;

    public static String truncate(final String text, final int limit) {
        final int len = text.length();
        final int pos = Math.min(len, limit);
        final boolean isTruncated = pos < len;
        return text.substring(0, pos) + (isTruncated ? "..." : "");
    }

    public static String truncate(final String text) {
        return truncate(text, TRUNCATE_LIMIT);
    }

    public static List<String> split(final String text, final String regex) {
        final String[] partsRaw = text.split(regex);
        final List<String> partsClear = new ArrayList<>();
        for (String part: partsRaw) {
            String partClear = part.strip();
            if (!partClear.isEmpty()) {
                partsClear.add(partClear);
            }
        }
        return partsClear;
    }

    public static void main(String... args) {
        System.out.println(truncate("hello", 45));
    }
}
