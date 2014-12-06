package com.tiksem.pq.data.response;

import com.tiksem.pq.data.Photo;
import com.tiksem.pq.data.Photoquest;
import com.tiksem.pq.data.User;

/**
 * Created by CM on 12/6/2014.
 */
public class Feed {
    private Photoquest photoquest;
    private Photo photo;
    private User user;
    private long addingDate;

    public Photoquest getPhotoquest() {
        return photoquest;
    }

    public void setPhotoquest(Photoquest photoquest) {
        this.photoquest = photoquest;
    }

    public Photo getPhoto() {
        return photo;
    }

    public void setPhoto(Photo photo) {
        this.photo = photo;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public long getAddingDate() {
        return addingDate;
    }

    public void setAddingDate(long addingDate) {
        this.addingDate = addingDate;
    }
}
