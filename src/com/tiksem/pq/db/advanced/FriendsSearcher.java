package com.tiksem.pq.db.advanced;

import com.tiksem.pq.data.Relationship;
import com.tiksem.pq.db.SqlFileExecutor;

/**
 * Created by CM on 2/12/2015.
 */
public class FriendsSearcher extends RelationsSearcher {
    public FriendsSearcher(SqlFileExecutor sqlFileExecutor, SearchUsersParams params,
                           long userId) {
        super(sqlFileExecutor, params, userId);
    }

    @Override
    protected int getType() {
        return Relationship.FRIENDSHIP;
    }
}
