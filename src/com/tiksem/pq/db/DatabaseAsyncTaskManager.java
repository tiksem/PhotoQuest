package com.tiksem.pq.db;

import com.tiksem.mysqljava.MysqlObjectMapper;
import com.utils.framework.strings.Strings;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
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
            Runtime.getRuntime().availableProcessors(), 30,
            50000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

    private ConcurrentMap<Thread, DatabaseManager> databaseManagers =
            new ConcurrentHashMap<Thread, DatabaseManager>();
    private Queue<RuntimeException> exceptions = new ConcurrentLinkedQueue<RuntimeException>();

    private class ThreadFactoryImpl implements ThreadFactory {
        @Override
        public Thread newThread(final Runnable runnable) {
            return new Thread(runnable){
                {
                    try {
                        databaseManagers.put(this, new DatabaseManager(null,
                                new MysqlObjectMapper(PhotoquestDataSource.getInstance().getConnection()), null));
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void run() {
                    super.run();
                    DatabaseManager databaseManager = databaseManagers.get(this);
                    if (databaseManager != null) {
                        databaseManager.destroy();
                        databaseManagers.remove(this);
                    }
                }
            };
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
        private String requestUrl;
        private Queue<Task> queue = new ConcurrentLinkedQueue<Task>();
        private int threadHashCode = System.identityHashCode(Thread.currentThread());

        public Handler(String lang, HttpServletRequest request) {
            this.lang = lang;
            requestUrl = request.getRequestURI();
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
                            try {
                                task.run(databaseManager);
                            } catch (Throwable e) {
                                exceptions.add(new AsyncTaskExecutionException(requestUrl, e));
                                e.printStackTrace();
                            }
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

    public Handler createHandler(String lang, HttpServletRequest request) {
        return new Handler(lang, request);
    }

    public Queue<RuntimeException> getExceptions() {
        return exceptions;
    }
}
