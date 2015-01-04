package com.tiksem.pq.db;

import com.tiksem.mysqljava.MysqlObjectMapper;
import com.tiksem.mysqljava.OffsetLimit;
import com.tiksem.pq.data.Action;
import com.tiksem.pq.data.Photoquest;
import com.tiksem.pq.data.User;
import com.utils.framework.CollectionUtils;
import com.utils.framework.Reflection;
import com.utils.framework.collections.iterator.AbstractIterator;
import com.utils.framework.strings.Strings;

import javax.jdo.PersistenceManager;
import java.util.*;

/**
 * Created by CM on 12/5/2014.
 */
public class AdvancedRequestsManager {
    public static final String CLEAR_USERS_RATING = "update user set rating = 0";
    public static final String CLEAR_PHOTOQUESTS_VIEWS = "update photoquest set viewsCount = 0";
    public static final String CLEAR_PHOTOS_VIEWS = "update photo set viewsCount = 0";

    private MysqlObjectMapper mapper;
    private SqlFileExecutor sqlFileExecutor;

    public AdvancedRequestsManager(MysqlObjectMapper mapper) {
        this.mapper = mapper;
        sqlFileExecutor = new SqlFileExecutor(mapper);
    }

    private Collection<Photoquest> getPhotoquestsByUserId(long userId,
                                                          RatingOrder order,
                                                          OffsetLimit offsetLimit,
                                                          String sqlFile) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("userId", userId);
        offsetLimit.addToMap(params);

        String orderBy = null;
        switch (order) {
            case newest:
                orderBy = "id";
                break;
            case hottest:
                orderBy = "viewsCount";
                break;
            case rated:
                orderBy = "likesCount";
                break;
        };
        params.put("orderBy", orderBy);

        return sqlFileExecutor.executeSQLQuery(sqlFile, params, Photoquest.class);
    }

    public Collection<Photoquest> getFollowingPhotoquests(long userId, RatingOrder order,
                                                          OffsetLimit offsetLimit) {
        String sqlFile = "photoquest/following_photoquests.sql";
        return getPhotoquestsByUserId(userId, order, offsetLimit, sqlFile);
    }

    public Collection<Photoquest> getPerformedPhotoquests(long userId, RatingOrder order,
                                                          OffsetLimit offsetLimit) {
        String sqlFile = "photoquest/performed_photoquests.sql";
        return getPhotoquestsByUserId(userId, order, offsetLimit, sqlFile);
    }

    public Collection<Action> getNews(long userId, OffsetLimit offsetLimit) {
        String sqlFile = "feed/select_news.sql";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("userId", userId);
        offsetLimit.addToMap(params);

        return sqlFileExecutor.executeSQLQuery(sqlFile, params, Action.class,
                MysqlObjectMapper.ALL_FOREIGN);
    }

    public List<Photoquest> getPhotoquestsByQuery(String query, OffsetLimit offsetLimit) {
        Map<String, Object> args = new HashMap<String, Object>();
        offsetLimit.addToMap(args);
        args.put("query", query);
        return sqlFileExecutor.executeSQLQuery(query, args, Photoquest.class);
    }

    public static class SearchUsersParams {
        public String query;
        public Boolean gender;
        public String location;
        public String orderBy;
    }

    public <T> List<T> searchUsers(SearchUsersParams params,
                                   OffsetLimit offsetLimit) {
        return (List<T>) searchUsersOrGetCount(params, offsetLimit, false);
    }

    public long getSearchUsersCount(SearchUsersParams params) {
        return (Long)searchUsersOrGetCount(params, null, true);
    }

    private Object searchUsersOrGetCount(SearchUsersParams params,
                                             OffsetLimit offsetLimit,
                                             boolean getCount) {
        if(Strings.isEmpty(params.query)){
            User user = new User();
            user.setGender(params.gender);
            user.setLocation(params.location);
            if (!getCount) {
                return mapper.queryByPattern(user, offsetLimit, params.orderBy + " desc");
            } else {
                return mapper.getCountByPattern(user);
            }
        }

        String[] queryParts = params.query.split(" +");
        String query;
        if(queryParts.length > 1){
            query = queryParts[0] + " " + queryParts[1];
        } else {
            query = params.query;
        }

        params.query = query;

        String sqlFile;
        if(params.location != null){
            if(params.gender != null){
                sqlFile = "search_users_by_gender_location_and_query.sql";
            } else {
                sqlFile = "search_users_by_location_and_query.sql";
            }
        } else if(params.gender != null) {
            sqlFile = "search_users_by_gender_and_query.sql";
        } else {
            sqlFile = "search_users_by_query.sql";
        }

        sqlFile += getCount ? "user/search/count/" : "user/search/";

        Map<String, Object> args = Reflection.objectToPropertyMap(params);
        offsetLimit.addToMap(args);
        if (!getCount) {
            return sqlFileExecutor.executeSQLQuery(sqlFile, args, User.class);
        } else {
            return sqlFileExecutor.executeCountQuery(sqlFile, args);
        }
    }

    public void deleteComment(long commentId) {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("commentId", commentId);
        sqlFileExecutor.executeNonSelectQuery("comment/delete_comment.sql", args);
    }

    public void clearRatingAndViews() {
        mapper.executeNonSelectSQL(CLEAR_PHOTOQUESTS_VIEWS);
        mapper.executeNonSelectSQL(CLEAR_PHOTOS_VIEWS);
        mapper.executeNonSelectSQL(CLEAR_USERS_RATING);
    }

    public void insertActionFeed(Action action) {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("userId", action.getUserId());
        args.put("actionId", action.getId());
        args.put("photoquestId", action.getPhotoquestId());
        String sqlFile = "feed/commit_add_photo.sql";
        sqlFileExecutor.executeNonSelectQuery(sqlFile, args);
    }
}