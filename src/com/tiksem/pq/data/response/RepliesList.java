package com.tiksem.pq.data.response;

import java.util.Collection;

/**
 * Created by CM on 12/3/2014.
 */
public class RepliesList {
    public Collection<ReplyResponse> replies;

    public RepliesList(Collection<ReplyResponse> replies) {
        this.replies = replies;
    }
}
