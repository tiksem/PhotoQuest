package com.tiksem.pq.exceptions;

/**
 * Created by CM on 2/28/2015.
 */
public class AvatarDeletionException extends PermissionDeniedException {
    public AvatarDeletionException() {
        super("Unable to delete avatar");
    }
}
