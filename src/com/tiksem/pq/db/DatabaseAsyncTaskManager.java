package com.tiksem.pq.db;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.*;

/**
 * Created by CM on 1/5/2015.
 */
public class DatabaseAsyncTaskManager {
    private static DatabaseAsyncTaskManager instance;
    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(), 1000,
            100000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

    private WeakHashMap<Thread, DatabaseManager> databaseManagers =
            new WeakHashMap<Thread, DatabaseManager>();

    private ThreadPoolExecutor lowPriorityExecutor = new ThreadPoolExecutor(1, 1,
            100000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

    private class ThreadFactoryImpl implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread();
            databaseManagers.put(thread, new DatabaseManager());
            return thread;
        }
    }

    public synchronized static DatabaseAsyncTaskManager getInstance() {
        if(instance == null){
            instance = new DatabaseAsyncTaskManager();
        }

        return instance;
    }

    private DatabaseAsyncTaskManager() {
        ThreadFactory threadFactory = new ThreadFactoryImpl();
        threadPoolExecutor.setThreadFactory(threadFactory);
        lowPriorityExecutor.setThreadFactory(threadFactory);
    }

    private void executeOnExecutor(Executor executor, final Task task) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Thread thread = Thread.currentThread();
                DatabaseManager databaseManager = databaseManagers.get(thread);
                task.run(databaseManager);
            }
        });
    }

    public void executeAsyncTask(final Task task) {
        executeOnExecutor(threadPoolExecutor, task);
    }

    public void executeLowPriorityAsyncTask(final Task task) {
        executeOnExecutor(lowPriorityExecutor, task);
    }
}
