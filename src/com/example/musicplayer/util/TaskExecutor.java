package com.example.musicplayer.util;

import java.util.concurrent.*;

/**
 * Created with IntelliJ IDEA.
 * User: xiejm
 * Date: 8/16/12
 * Time: 11:46 AM
 */

// an asynchronous task executor(thread pool)
public class TaskExecutor {
    private static ExecutorService service = null;

    public static void executeTask(Runnable task) {
        if (service == null) {
            service = new ThreadPoolExecutor(5, 100,
                    20L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>(),
                    Executors.defaultThreadFactory());
        }
        service.execute(task);
    }

    public static <T> Future<T> submitTask(Callable<T> task) {
        if (service == null) {
            service = new ThreadPoolExecutor(5, 100,
                    20L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>(),
                    Executors.defaultThreadFactory());
        }
        return service.submit(task);
    }

    public static void shutdown() {
        if (service != null) {
            service.shutdown();
            service = null;
        }
    }
}

