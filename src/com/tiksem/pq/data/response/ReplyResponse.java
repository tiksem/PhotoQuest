package com.tiksem.pq.data.response;

import com.tiksem.pq.data.Comment;
import com.tiksem.pq.data.Like;
import com.tiksem.pq.data.User;

/**
 * Created by CM on 12/3/2014.
 */
public class ReplyResponse {
    private int type;
    private Comment comment;
    private User user;
    private Like like;
    private long addingDate;

    public ReplyResponse() {
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

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Like getLike() {
        return like;
    }

    public void setLike(Like like) {
        this.like = like;
    }

    public long getAddingDate() {
        return addingDate;
    }

    public void setAddingDate(long addingDate) {
        this.addingDate = addingDate;
    }
}
