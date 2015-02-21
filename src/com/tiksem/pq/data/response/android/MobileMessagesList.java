package com.tiksem.pq.data.response.android;

import com.tiksem.pq.data.Message;
import com.tiksem.pq.data.User;
import com.utils.framework.CollectionUtils;

import java.util.Collection;

/**
 * Created by CM on 2/21/2015.
 */
public class MobileMessagesList {
    public Collection<MobileMessage> messages;

    public MobileMessagesList(Collection<Message> messages, final User signedInUser) {
        this.messages = CollectionUtils.transform(messages,
                new CollectionUtils.Transformer<Message, MobileMessage>() {
                    @Override
                    public MobileMessage get(Message message) {
                        return new MobileMessage(message, signedInUser);
                    }
                });
    }
}
