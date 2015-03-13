package com.tiksem.pq.db;

/**
 * Created by CM on 3/13/2015.
 */
public class AsyncTaskExecutionException extends RuntimeException {
    public AsyncTaskExecutionException(String url, Throwable cause) {
        super("Previous async task exception occurs, while execution " +
                url + " request", cause);
    }
}
