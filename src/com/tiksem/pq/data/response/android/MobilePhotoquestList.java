package com.tiksem.pq.data.response.android;

import com.tiksem.pq.data.Photoquest;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by CM on 1/21/2015.
 */
public class MobilePhotoquestList {
    public Collection<MobilePhotoquest> quests;

    public MobilePhotoquestList(Collection<Photoquest> photoquests) {
        this.quests = new ArrayList<MobilePhotoquest>(photoquests.size());
        for(Photoquest photoquest : photoquests){
            MobilePhotoquest mobilePhotoquest = new MobilePhotoquest(photoquest);
            this.quests.add(mobilePhotoquest);
        }
    }
}
