package com.tiksem.pq.data.response;

import com.tiksem.pq.data.Comment;

import java.util.Collection;

/**
 * Created by CM on 11/10/2014.
 */
public class CommentsList {
    public Collection<Comment> comments;

    public CommentsList(Collection<Comment> comments) {
        this.comments = comments;
    }
}
