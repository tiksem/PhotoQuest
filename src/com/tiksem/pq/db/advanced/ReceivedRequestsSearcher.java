package com.tiksem.pq.db.advanced;

import com.tiksem.pq.data.Relationship;
import com.tiksem.pq.db.SqlFileExecutor;

/**
 * Created by CM on 2/12/2015.
 */
public class ReceivedRequestsSearcher extends RelationsSearcher {
    public ReceivedRequestsSearcher(SqlFileExecutor sqlFileExecutor, SearchUsersParams params,
                                    long userId) {
        super(sqlFileExecutor, params, userId);
    }

    @Override
    protected int getType() {
        return Relationship.FRIEND_REQUEST;
    }

    @Override
    protected String getSelectUserIdColumn() {
        return "fromUserId";
    }

    @Override
    protected String getUserIdColumn() {
        return "toUserId";
    }
}
