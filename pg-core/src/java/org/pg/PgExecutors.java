package org.pg;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class PgExecutors {
    final private static AtomicLong cachedThreadPoolCounter = new AtomicLong(0);

    @SuppressWarnings("unused")
    volatile public static Executor directExecutor = new DirectExecutor();

    @SuppressWarnings("unused")
    volatile public static ExecutorService threadPoolExecutor = new ThreadPoolExecutor(
        1,
        Runtime.getRuntime().availableProcessors(),
        60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>(),
        createThreadFactory("pg2-pool-%d", cachedThreadPoolCounter));

    private static ThreadFactory createThreadFactory(final String format, final AtomicLong threadPoolCounter) {
        return new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                thread.setName(String.format(format, threadPoolCounter.getAndIncrement()));
                return thread;
            }
        };
    }

    private static class DirectExecutor implements Executor {
        public void execute(Runnable r) {
            r.run();
        }
    }

    public static void shutdown(){
        threadPoolExecutor.shutdown();
    }
}
