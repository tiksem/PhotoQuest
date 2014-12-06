package com.tiksem.pq.data.response;

import java.util.Collection;

/**
 * Created by CM on 12/6/2014.
 */
public class FeedList {
    public Collection<Feed> feeds;

    public FeedList(Collection<Feed> feeds) {
        this.feeds = feeds;
    }
}
