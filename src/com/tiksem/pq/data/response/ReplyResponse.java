package com.tiksem.pq.data.response;

import com.tiksem.pq.data.Comment;
import com.tiksem.pq.data.User;

/**
 * Created by CM on 12/3/2014.
 */
public class ReplyResponse {
    private int type;
    private Comment comment;
    private User user;

    public ReplyResponse() {
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }
}
