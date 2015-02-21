package com.tiksem.pq.data.response.android;

import com.tiksem.pq.data.Dialog;
import com.tiksem.pq.data.Message;
import com.tiksem.pq.data.User;

/**
 * Created by CM on 2/21/2015.
 */
public class MobileDialog {
    public long userId;
    public String name;
    public String lastName;
    public long lastMessageTime;
    public String lastMessage;
    public Long avatarId;
    public boolean sent;
    public boolean read;

    public MobileDialog(Dialog dialog) {
        User user = dialog.getUser();
        userId = user.getId();
        name = user.getName();
        lastName = user.getLastName();
        lastMessageTime = dialog.getLastMessageTime();
        Message last = dialog.getLastMessage();
        lastMessage = last.getMessage();
        avatarId = user.getAvatarId();
        sent = dialog.getUser2Id().equals(userId);
        read = last.getRead();
    }
}
