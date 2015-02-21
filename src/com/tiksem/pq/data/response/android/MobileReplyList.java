package com.tiksem.pq.data.response.android;

import com.tiksem.pq.data.response.ReplyResponse;
import com.utils.framework.CollectionUtils;

import java.util.Collection;

/**
 * Created by CM on 2/21/2015.
 */
public class MobileReplyList {
    public Collection<MobileReply> replies;

    public MobileReplyList(Collection<ReplyResponse> replies) {
        this.replies = CollectionUtils.transform(replies,
                new CollectionUtils.Transformer<ReplyResponse, MobileReply>() {
            @Override
            public MobileReply get(ReplyResponse replyResponse) {
                return new MobileReply(replyResponse);
            }
        });
    }
}
