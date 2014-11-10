package com.tiksem.pq.db.exceptions;

/**
 * Created by CM on 11/10/2014.
 */
public class CommentNotFoundException extends RuntimeException {
    public CommentNotFoundException(long commentId) {
        super("Comment with id " + commentId + " does not exist");
    }
}
