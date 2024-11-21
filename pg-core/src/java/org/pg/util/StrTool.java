package org.pg.util;

import java.util.ArrayList;
import java.util.List;

public class StrTool {

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
}
