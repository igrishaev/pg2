package org.pg;

import java.util.Timer;
import java.util.TimerTask;


public final class CancelTimer implements AutoCloseable {

    private final Timer timer = new Timer();

    public CancelTimer(final Connection conn) {
        this(conn, conn.getConfig().msCancelTimeout());
    }

    public CancelTimer(final Connection conn, final long msTimeout) {
        final TimerTask task = new TimerTask() {
            public void run() {
                Connection.cancelRequest(conn);
            }
        };
        timer.schedule(task, msTimeout);
    }

    @Override
    public void close () {
        timer.cancel();
    }
}