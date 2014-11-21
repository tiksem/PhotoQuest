package com.tiksem.pq.data;

import com.tiksem.pq.data.annotations.Relation;

import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceAware;
import javax.jdo.annotations.PersistenceCapable;

/**
 * Created by CM on 11/8/2014.
 */
@PersistenceCapable
@PersistenceAware
public class Friendship {
    @Relation(relationName = "user")
    @Index
    private Long user1;
    @Relation(relationName = "user")
    @Index
    private Long user2;

    public Friendship(Long user1, Long user2) {
        this.user1 = user1;
        this.user2 = user2;
    }

    public Friendship() {
    }

    public Long getUser1() {
        return user1;
    }

    public void setUser1(Long user1) {
        this.user1 = user1;
    }

    public Long getUser2() {
        return user2;
    }

    public void setUser2(Long user2) {
        this.user2 = user2;
    }
}
