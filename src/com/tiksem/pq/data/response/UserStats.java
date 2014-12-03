package com.tiksem.pq.data.response;

/**
 * Created by CM on 12/3/2014.
 */
public class UserStats {
    private long unreadMessagesCount = 0;
    private long friendRequestsCount = 0;
    private long unreadRepliesCount = 0;

    public long getUnreadMessagesCount() {
        return unreadMessagesCount;
    }

    public void setUnreadMessagesCount(long unreadMessagesCount) {
        if(unreadMessagesCount < 0){
            unreadMessagesCount = 0;
        }

        this.unreadMessagesCount = unreadMessagesCount;
    }

    public long getFriendRequestsCount() {
        return friendRequestsCount;
    }

    public void setFriendRequestsCount(long friendRequestsCount) {
        if(friendRequestsCount < 0){
            friendRequestsCount = 0;
        }

        this.friendRequestsCount = friendRequestsCount;
    }

    public long getUnreadRepliesCount() {
        return unreadRepliesCount;
    }

    public void setUnreadRepliesCount(long unreadRepliesCount) {
        this.unreadRepliesCount = unreadRepliesCount;
    }
}
