package org.pg.util;

import org.pg.error.PGError;

public final class Sleep {
    public static void sleep(final long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new PGError(e, "Sleep was interrupted, timeout: %s", ms);
        }
    }
}
