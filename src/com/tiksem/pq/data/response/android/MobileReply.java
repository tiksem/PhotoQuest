package com.tiksem.pq.data.response.android;

import com.tiksem.pq.data.Comment;
import com.tiksem.pq.data.Like;
import com.tiksem.pq.data.Reply;
import com.tiksem.pq.data.User;
import com.tiksem.pq.data.response.ReplyResponse;

/**
 * Created by CM on 2/21/2015.
 */
public class MobileReply {
    public int type;
    public String name;
    public String comment;
    public Long photoId;
    public long avatarId;
    public long addingDate;

    public MobileReply(ReplyResponse reply) {
        type = reply.getType();

        User user = reply.getUser();
        name = user.getName() + " " + user.getLastName();
        avatarId = user.getAvatarId();

        Comment replyComment = reply.getComment();
        if (replyComment != null) {
            comment = replyComment.getMessage();
            photoId = replyComment.getPhotoId();
        } else {
            Like like = reply.getLike();
            if(like != null){
                photoId = like.getPhotoId();
            }
        }

        addingDate = reply.getAddingDate();
    }
}
