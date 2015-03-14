package com.tiksem.pq.data.response;

/**
 * Created by CM on 12/3/2014.
 */
public class UserStats {
    private long unreadMessagesCount = 0;
    private long receivedRequestsCount = 0;
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

    public long getReceivedRequestsCount() {
        return receivedRequestsCount;
    }

    public void setReceivedRequestsCount(long receivedRequestsCount) {
        if(receivedRequestsCount < 0){
            receivedRequestsCount = 0;
        }

        this.receivedRequestsCount = receivedRequestsCount;
    }

    public long getUnreadRepliesCount() {
        return unreadRepliesCount;
    }

    public void setUnreadRepliesCount(long unreadRepliesCount) {
        this.unreadRepliesCount = unreadRepliesCount;
    }
}
