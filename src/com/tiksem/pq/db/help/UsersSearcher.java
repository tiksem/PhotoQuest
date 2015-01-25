package com.tiksem.pq.db.help;

import com.tiksem.mysqljava.MysqlObjectMapper;
import com.tiksem.mysqljava.OffsetLimit;
import com.tiksem.pq.data.User;
import com.tiksem.pq.db.SqlFileExecutor;
import com.utils.framework.Reflection;
import com.utils.framework.strings.Strings;

import java.util.List;
import java.util.Map;

/**
 * Created by CM on 1/22/2015.
 */
public abstract class UsersSearcher {
    protected MysqlObjectMapper mapper;
    private SearchUsersParams params;
    private OffsetLimit offsetLimit;
    protected SqlFileExecutor sqlFileExecutor;

    public UsersSearcher(MysqlObjectMapper mapper, SqlFileExecutor sqlFileExecutor, SearchUsersParams params,
                         OffsetLimit offsetLimit) {
        this.mapper = mapper;
        this.sqlFileExecutor = sqlFileExecutor;
        this.params = params;
        this.offsetLimit = offsetLimit;
    }

    protected abstract long getCountByGenderAndCityId(Boolean gender, Integer cityId);
    protected abstract List<User> searchByGenderAndCityId(Boolean gender, Integer cityId, String orderBy,
                                                          OffsetLimit offsetLimit);

    
    protected String getSearchByGenderCityQuerySqlFile() {
        return "search_users_by_gender_location_and_query.sql";
    }

    protected String getSearchByCityQuerySqlFile() {
        return "search_users_by_location_and_query.sql";
    }
    
    protected String getSearchByQuerySqlFile() {
        return "search_users_by_query.sql";
    }

    protected String getSearchByGenderQuerySqlFile() {
        return "search_users_by_gender_and_query.sql";
    }

    protected abstract String getSearchFilesPath();
    protected abstract String getCountFilesPath();

    private String fixQuery() {
        String[] queryParts = params.query.split(" +");
        String query;
        if(queryParts.length > 1){
            query = queryParts[0] + " " + queryParts[1];
        } else {
            query = params.query;
        }

        query += "%";
        return query;
    }
    
    private Object searchUsersOrGetCount(SearchUsersParams params,
                                         OffsetLimit offsetLimit,
                                         boolean getCount) {
        if(Strings.isEmpty(params.query)){
            if (!getCount) {
                return searchByGenderAndCityId(params.gender, params.cityId, params.orderBy, offsetLimit);
            } else {
                return getCountByGenderAndCityId(params.gender, params.cityId);
            }
        }

        params.query = fixQuery();

        String sqlFile;
        if(params.cityId != null){
            if(params.gender != null){
                sqlFile = getSearchByGenderCityQuerySqlFile();
            } else {
                sqlFile = getSearchByCityQuerySqlFile();
            }
        } else if(params.gender != null) {
            sqlFile = getSearchByGenderQuerySqlFile();
        } else {
            sqlFile = getSearchByQuerySqlFile();
        }

        sqlFile = getCount ? getCountFilesPath() + sqlFile : getSearchFilesPath() + sqlFile;

        Map<String, Object> args = Reflection.objectToPropertyMap(params);
        if (!getCount) {
            offsetLimit.addToMap(args);
        }
        if (!getCount) {
            return sqlFileExecutor.executeSQLQuery(sqlFile, args, User.class);
        } else {
            return sqlFileExecutor.executeCountQuery(sqlFile, args);
        }
    }

    public List<User> search() {
        return (List<User>) searchUsersOrGetCount(params, offsetLimit, false);
    }

    public long getCount() {
        return (Long) searchUsersOrGetCount(params, offsetLimit, true);
    }
}
