package com.tiksem.pq;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by CM on 1/5/2015.
 */
public class AsyncTaskManager {
    private static AsyncTaskManager instance;
    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 1,
            100000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

    public synchronized static AsyncTaskManager getInstance() {
        if(instance == null){
            instance = new AsyncTaskManager();
        }

        return instance;
    }

    public void executeAsync(Runnable runnable) {
        threadPoolExecutor.execute(runnable);
    }
}
