package com.tiksem.pq.data.response.android;

import com.tiksem.pq.data.Action;
import com.utils.framework.CollectionUtils;

import java.util.Collection;

/**
 * Created by CM on 1/21/2015.
 */
public class MobileFeedList {
    public Collection<MobileFeed> feeds;

    public MobileFeedList(Collection<Action> actions) {
        feeds = CollectionUtils.transform(actions,
                new CollectionUtils.Transformer<Action, MobileFeed>() {
            @Override
            public MobileFeed get(Action action) {
                return new MobileFeed(action);
            }
        });
    }
}
