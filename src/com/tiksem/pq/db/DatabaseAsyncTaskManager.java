package com.tiksem.pq.db;

import com.tiksem.mysqljava.MysqlObjectMapper;
import com.utils.framework.strings.Strings;

import java.util.Collections;
import java.util.Map;
import java.util.Queue;
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

    private Map<Thread, DatabaseManager> databaseManagers =
            Collections.synchronizedMap(new WeakHashMap<Thread, DatabaseManager>());

    private class ThreadFactoryImpl implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            databaseManagers.put(thread, new DatabaseManager(new MysqlObjectMapper(), null));
            return thread;
        }
    }

    public synchronized static DatabaseAsyncTaskManager getInstance() {
        if(instance == null){
            instance = new DatabaseAsyncTaskManager();
        }

        return instance;
    }

    public class Handler {
        private String lang;
        private Queue<Task> queue = new ConcurrentLinkedQueue<Task>();
        private int threadHashCode = System.identityHashCode(Thread.currentThread());

        public Handler(String lang) {
            this.lang = lang;
        }

        public void execute(final Task task) {
            if(System.identityHashCode(Thread.currentThread()) != threadHashCode){
                throw new IllegalStateException("Only one thread can use handler");
            }

            if(queue.isEmpty()){
                executeOnExecutor(threadPoolExecutor, new Task() {
                    @Override
                    public void run(DatabaseManager databaseManager) {
                        queue.add(task);
                        while (!queue.isEmpty()) {
                            Task task = queue.remove();
                            task.run(databaseManager);
                        }
                    }
                }, lang);
            } else {
                queue.add(task);
            }
        }
    }

    private DatabaseAsyncTaskManager() {
        ThreadFactory threadFactory = new ThreadFactoryImpl();
        threadPoolExecutor.setThreadFactory(threadFactory);
    }

    private void executeOnExecutor(Executor executor, final Task task, String lang) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Thread thread = Thread.currentThread();
                DatabaseManager databaseManager = databaseManagers.get(thread);
                task.run(databaseManager);
            }
        });
    }

    public Handler createHandler(String lang) {
        return new Handler(lang);
    }
}
