package com.tiksem.pq.data.response.android;

import com.tiksem.pq.data.Message;
import com.tiksem.pq.data.User;

/**
 * Created by CM on 2/21/2015.
 */
public class MobileMessage {
    public String message;
    public long addingDate;
    public boolean sent;

    public MobileMessage(Message m, User signedInUser) {
        message = m.getMessage();
        addingDate = m.getAddingDate();
        sent = m.getFromUserId().equals(signedInUser.getId());
    }
}
