package com.tiksem.pq.db.exceptions;

/**
 * Created by CM on 11/14/2014.
 */
public class MessageNotOwnedByUserException extends RuntimeException {
    public MessageNotOwnedByUserException(long messageId, long userId) {
        super("Message " + messageId + " is not owned by " + userId + " user");
    }
}
