package com.tiksem.pq.db.advanced;

import com.tiksem.mysqljava.OffsetLimit;
import com.tiksem.pq.data.User;
import com.tiksem.pq.db.SqlFileExecutor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by CM on 2/12/2015.
 */
class UsersSearcher {
    private static final long SEARCH_USERS_BY_QUERY_LIMIT = 200;
    private SqlFileExecutor sqlFileExecutor;
    private SearchUsersParams params;

    public UsersSearcher(SqlFileExecutor sqlFileExecutor, SearchUsersParams params) {
        this.sqlFileExecutor = sqlFileExecutor;
        this.params = params;
    }

    public <T> List<T> search(OffsetLimit offsetLimit, String orderBy) {
        return (List<T>) searchUsersOrGetCount(offsetLimit, false, orderBy);
    }

    public long getCount() {
        return (Long)searchUsersOrGetCount(null, true, null);
    }

    private Object searchUsersOrGetCount(OffsetLimit offsetLimit,
                                         boolean getCount, Object orderBy) {
        long innerLimit = SEARCH_USERS_BY_QUERY_LIMIT;

        if(params.query == null){
            params.query = "";
            innerLimit = Long.MAX_VALUE;
        }

        String[] queryParts = params.query.split(" +");
        String query;
        if(queryParts.length > 1){
            query = queryParts[0] + " " + queryParts[1];
        } else {
            query = params.query;
        }

        query = getLikeQuery(query);

        String sqlFile = getCount ? getCountSQLFileName() : getSearchSQLFileName();

        Map<String, Object> args = new HashMap<String, Object>();
        args.put("query", query);

        String where = "";
        if(params.gender != null){
            where += "AND gender = " + params.gender + " ";
        }

        if (params.cityId != null) {
            where += "AND cityId = " + params.cityId + " ";
        }

        if (params.countryId != null) {
            where += "AND countryId = " + params.countryId;
        }
        args.put("where", where);
        args.put("innerLimit", innerLimit);

        if (!getCount) {
            offsetLimit.addToMap(args);
            args.put("orderBy", orderBy);
        }

        addArgs(args, getCount);

        if (!getCount) {
            return sqlFileExecutor.executeSQLQuery(sqlFile, args, User.class);
        } else {
            return sqlFileExecutor.executeCountQuery(sqlFile, args);
        }
    }

    protected String getCountSQLFileName() {
        return "user/search/count/search_users_by_query.sql";
    }

    protected String getSearchSQLFileName() {
        return "user/search/search_users_by_query.sql";
    }

    protected String getLikeQuery(String query) {
        return query + "%";
    }

    protected void addArgs(Map<String, Object> args, boolean getCount) {

    }
}
