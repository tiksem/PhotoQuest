package com.tiksem.pq.data.response;

import com.tiksem.pq.data.Action;

import java.util.Collection;

/**
 * Created by CM on 12/6/2014.
 */
public class FeedList {
    public Collection<Action> feeds;

    public FeedList(Collection<Action> feeds) {
        this.feeds = feeds;
    }
}
