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
            1000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

    private Queue<RuntimeException> exceptions = new ConcurrentLinkedQueue<RuntimeException>();

    public synchronized static DatabaseAsyncTaskManager getInstance() {
        if(instance == null){
            instance = new DatabaseAsyncTaskManager();
        }

        return instance;
    }

    public class Handler {
        private String lang;
        private String requestUrl;

        public Handler(String lang, HttpServletRequest request) {
            this.lang = lang;
            requestUrl = request.getRequestURI();
        }

        public void execute(final Task task) {
            try {
                executeOnExecutor(threadPoolExecutor, task, lang);
            } catch (RuntimeException e) {
                exceptions.add(new AsyncTaskExecutionException(requestUrl, e));
                e.printStackTrace();
            }
        }
    }

    private DatabaseAsyncTaskManager() {

    }

    private void executeOnExecutor(Executor executor, final Task task, final String lang) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                MysqlObjectMapper mapper = null;
                try {
                    mapper = new MysqlObjectMapper(PhotoquestDataSource.getInstance().getConnection());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                DatabaseManager databaseManager = new DatabaseManager(null, mapper, lang);
                task.run(databaseManager);
                databaseManager.destroy();
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
