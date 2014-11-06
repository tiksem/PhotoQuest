package com.tiksem.pq.data.response;

import com.tiksem.pq.data.Photoquest;

import java.util.Collection;

/**
 * Created by CM on 11/5/2014.
 */
public class PhotoquestsList {
    public Collection<Photoquest> quests;

    public PhotoquestsList(Collection<Photoquest> quests) {
        this.quests = quests;
    }
}
