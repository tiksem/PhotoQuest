package com.tiksem.pq.db.advanced;

import com.tiksem.pq.db.SqlFileExecutor;

import java.util.Map;

/**
 * Created by CM on 2/12/2015.
 */
public abstract class RelationsSearcher extends UsersSearcher {
    private long userId;

    public RelationsSearcher(SqlFileExecutor sqlFileExecutor, SearchUsersParams params,
                             long userId) {
        super(sqlFileExecutor, params);
        this.userId = userId;
    }

    @Override
    protected String getCountSQLFileName() {
        return getSearchSQLFileName();
    }

    @Override
    protected String getSearchSQLFileName() {
        return "user/search/relation/search_related_users_by_query.sql";
    }

    protected abstract int getType();

    protected String getSelectUserIdColumn() {
        return "toUserId";
    }

    protected String getUserIdColumn() {
        return "fromUserId";
    }

    @Override
    protected void addArgs(Map<String, Object> args, boolean getCount) {
        super.addArgs(args, getCount);
        if(getCount){
            args.put("select", "count(*)");
            args.put("orderByLimitExpression", "");
        } else {
            args.put("select", "user.*");
            args.put("orderByLimitExpression", "ORDER BY :orderBy LIMIT :offset, :limit");
        }

        args.put("selectUserIdColumn", getSelectUserIdColumn());
        args.put("userIdColumn", getUserIdColumn());
        args.put("userId", userId);
        args.put("type", getType());
    }

    @Override
    protected String getLikeQuery(String query) {
        return "%" + query + "%";
    }
}
