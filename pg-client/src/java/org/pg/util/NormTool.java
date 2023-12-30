package org.pg.util;

import java.text.Normalizer;

public class NormTool {
    public static String normalizeNfc(final String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFC);
    }
}
