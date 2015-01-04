package com.tiksem.pq.exceptions;

import com.tiksem.pq.data.Like;

/**
 * Created by CM on 11/10/2014.
 */
public class LikeExistsException extends RuntimeException {
    private static String createMessage(Like like) {
        Long photoId = like.getPhotoId();
        if(photoId != null){
            return "You have already liked photo with " + photoId + " id";
        }

        Long commentId = like.getCommentId();
        if(commentId != null){
            return "You have already liked comment with " + commentId + " id";
        }

        throw new RuntimeException("WTF?");
    }

    public LikeExistsException(Like like) {
        super(createMessage(like));
    }
}
