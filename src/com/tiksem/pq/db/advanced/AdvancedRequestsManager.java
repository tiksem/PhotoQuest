package com.tiksem.pq.db.advanced;

import com.tiksem.mysqljava.MysqlObjectMapper;
import com.tiksem.mysqljava.OffsetLimit;
import com.tiksem.pq.data.*;
import com.tiksem.pq.data.response.LocationSuggestion;
import com.tiksem.pq.db.RatingOrder;
import com.tiksem.pq.db.SqlFileExecutor;
import com.utils.framework.Maps;
import com.utils.framework.strings.Strings;

import java.util.*;

/**
 * Created by CM on 12/5/2014.
 */
public class AdvancedRequestsManager {
    public static final String CLEAR_USERS_RATING = "update user set rating = 0";
    public static final String CLEAR_PHOTOQUESTS_VIEWS = "update photoquest set viewsCount = 0";
    public static final String CLEAR_PHOTOS_VIEWS = "update photo set viewsCount = 0";
    public static final String PERFORMED_PHOTOQUESTS_WHERE = "AND photoquest.ID IN \n" +
            "      (SELECT photoquestId FROM performedphotoquest WHERE userId = :userId)";
    public static final String FOLLOWING_PHOTOQUESTS_WHERE = "AND photoquest.ID IN \n" +
            "      (SELECT photoquestId FROM followingphotoquest WHERE userId = :userId)";
    public static final String CREATED_PHOTOQUESTS_WHERE = "AND photoquest.userId = :userId";

    private MysqlObjectMapper mapper;
    private SqlFileExecutor sqlFileExecutor;

    public AdvancedRequestsManager(MysqlObjectMapper mapper) {
        this.mapper = mapper;
        sqlFileExecutor = new SqlFileExecutor(mapper);
    }

    private Collection<Photoquest> getPhotoquestsByUserId(long userId,
                                                          String orderBy,
                                                          OffsetLimit offsetLimit,
                                                          String sqlFile) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("userId", userId);
        offsetLimit.addToMap(params);
        params.put("orderBy", orderBy);

        return sqlFileExecutor.executeSQLQuery(sqlFile, params, Photoquest.class);
    }

    public Collection<Photoquest> getFollowingPhotoquests(long userId, String orderBy,
                                                          String filter,
                                                          OffsetLimit offsetLimit) {
        if (!Strings.isEmpty(filter)) {
            String where = FOLLOWING_PHOTOQUESTS_WHERE;
            Map<String, Object> args = new HashMap<String, Object>();
            args.put("userId", userId);
            return getPhotoquestsByQuery(filter, offsetLimit, where, orderBy, args);
        } else {
            String sqlFile = "photoquest/following_photoquests.sql";
            return getPhotoquestsByUserId(userId, orderBy, offsetLimit, sqlFile);
        }
    }

    public Collection<Photoquest> getPerformedPhotoquests(long userId, String orderBy,
                                                          String filter,
                                                          OffsetLimit offsetLimit) {
        if (!Strings.isEmpty(filter)) {
            String where = PERFORMED_PHOTOQUESTS_WHERE;
            Map<String, Object> args = new HashMap<String, Object>();
            args.put("userId", userId);
            return getPhotoquestsByQuery(filter, offsetLimit, where, orderBy, args);
        } else {
            String sqlFile = "photoquest/performed_photoquests.sql";
            return getPhotoquestsByUserId(userId, orderBy, offsetLimit, sqlFile);
        }
    }

    public Collection<Action> getNews(long userId, OffsetLimit offsetLimit) {
        String sqlFile = "feed/select_news.sql";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("userId", userId);
        offsetLimit.addToMap(params);

        return sqlFileExecutor.executeSQLQuery(sqlFile, params, Action.class,
                MysqlObjectMapper.ALL_FOREIGN);
    }

    public List<Photoquest> getPhotoquestsByQuery(String query, OffsetLimit offsetLimit,
                                                  String where,
                                                  String orderBy,
                                                  Map<String, Object> args) {
        orderBy = orderBy.replace("likesCount", "Photoquest.likesCount");
        orderBy = orderBy.replace("viewsCount", "Photoquest.viewsCount");
        orderBy = orderBy.replace("id", "Photoquest.id");

        offsetLimit.addToMap(args);
        args.put("query", query);
        args.put("orderBy", orderBy);
        args.put("where", where);
        String sqlFile = "photoquest/search_photoquests_order_by.sql";
        return sqlFileExecutor.executeSQLQuery(sqlFile, args, Photoquest.class, MysqlObjectMapper.ALL_FOREIGN);
    }

    public long getPhotoquestsByQueryCount(String query) {
        Map<String, Object> args = Collections.singletonMap("query", (Object)query);
        String sqlFile = "photoquest/search_photoquests_count.sql";
        return sqlFileExecutor.executeCountQuery(sqlFile, args);
    }

    private long getUserPhotoquestsByQueryCount(String query, long userId, String where) {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("query", query);
        args.put("userId", userId);
        args.put("where", where);
        String sqlFile = "photoquest/user_photoquests_search_count.sql";
        return sqlFileExecutor.executeCountQuery(sqlFile, args);
    }

    public long getCreatedPhotoquestsByQueryCount(String query, long userId) {
        String where = CREATED_PHOTOQUESTS_WHERE;
        return getUserPhotoquestsByQueryCount(query, userId, where);
    }

    public long getPerformedPhotoquestsByQueryCount(String query, long userId) {
        String where = PERFORMED_PHOTOQUESTS_WHERE;
        return getUserPhotoquestsByQueryCount(query, userId, where);
    }

    public long getFollowingPhotoquestsByQueryCount(String query, long userId) {
        String where = FOLLOWING_PHOTOQUESTS_WHERE;
        return getUserPhotoquestsByQueryCount(query, userId, where);
    }

    public List<Photoquest> getPhotoquestsByQuery(String query, OffsetLimit offsetLimit,
                                                  String orderBy) {
        return getPhotoquestsByQuery(query, offsetLimit, "", orderBy, new HashMap<String, Object>());
    }

    public List<Photoquest> getCreatedPhotoquestsByQuery(String query, OffsetLimit offsetLimit,
                                                         String orderBy, long userId) {
        String where = CREATED_PHOTOQUESTS_WHERE;
        return getPhotoquestsByQuery(query, offsetLimit, where, orderBy, Maps.hashMap("userId", (Object)userId));
    }

    public <T> List<T> searchUsers(SearchUsersParams params,
                                   String orderBy,
                                   OffsetLimit offsetLimit) {
        UsersSearcher searcher = new UsersSearcher(sqlFileExecutor, params);
        return searcher.search(offsetLimit, orderBy);
    }

    public long getSearchUsersCount(SearchUsersParams params) {
        UsersSearcher searcher = new UsersSearcher(sqlFileExecutor, params);
        return searcher.getCount();
    }

    public <T> List<T> searchFriends(SearchUsersParams params,
                                     OffsetLimit offsetLimit,
                                     String orderBy,
                                     long userId) {
        FriendsSearcher searcher = new FriendsSearcher(sqlFileExecutor, params, userId);
        return searcher.search(offsetLimit, orderBy);
    }

    public long getSearchFriendsCount(SearchUsersParams params, long userId) {
        FriendsSearcher searcher = new FriendsSearcher(sqlFileExecutor, params, userId);
        return searcher.getCount();
    }

    public <T> List<T> searchSentRequests(SearchUsersParams params,
                                          OffsetLimit offsetLimit,
                                          String orderBy,
                                          long userId) {
        SentRequestsSearcher searcher = new SentRequestsSearcher(sqlFileExecutor, params, userId);
        return searcher.search(offsetLimit, orderBy);
    }

    public long getSearchReceivedRequestsCount(SearchUsersParams params, long userId) {
        ReceivedRequestsSearcher searcher = new ReceivedRequestsSearcher(sqlFileExecutor, params, userId);
        return searcher.getCount();
    }

    public long getSearchSentRequestsCount(SearchUsersParams params, long userId) {
        SentRequestsSearcher searcher = new SentRequestsSearcher(sqlFileExecutor, params, userId);
        return searcher.getCount();
    }

    public <T> List<T> searchReceivedRequests(SearchUsersParams params,
                                              OffsetLimit offsetLimit,
                                              String orderBy,
                                              long userId) {
        ReceivedRequestsSearcher searcher = new ReceivedRequestsSearcher(sqlFileExecutor, params, userId);
        return searcher.search(offsetLimit, orderBy);
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

    public Photo getNextPrevPhotoOfFriends(long userId,
                                           long photoquestId,
                                           Object orderByValue,
                                           String orderBy,
                                           boolean next) {
        String operator = next ? "<" : ">";
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("operator", operator);
        args.put("orderBy", orderBy);
        args.put("orderByValue", orderByValue);
        args.put("photoquestId", photoquestId);
        args.put("userId", userId);
        List<Photo> photos = sqlFileExecutor.executeSQLQuery("photo/next_prev_photo_of_friends.sql", args, Photo.class);
        if (photos.isEmpty()) {
            return null;
        }

        return photos.get(0);
    }

    public List<Photo> getPhotosOfFriendsByPhotoquest(long userId, long photoquestId, String orderBy,
                                                      OffsetLimit offsetLimit) {
        Map<String, Object> args = new HashMap<String, Object>();
        offsetLimit.addToMap(args);
        args.put("userId", userId);
        args.put("orderBy", orderBy);
        args.put("photoquestId", photoquestId);
        return sqlFileExecutor.executeSQLQuery("photo/photos_of_friends.sql", args, Photo.class);
    }

    public long getPhotosOfFriendsByPhotoquestCount(long userId, long photoquestId) {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("userId", userId);
        args.put("photoquestId", photoquestId);
        return sqlFileExecutor.executeCountQuery("photo/photos_of_friends_count.sql", args);
    }

    public void insertActionFeed(Action action) {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("userId", action.getUserId());
        args.put("actionId", action.getId());
        args.put("photoquestId", action.getPhotoquestId());
        String sqlFile = "feed/commit_add_photo.sql";
        sqlFileExecutor.executeNonSelectQuery(sqlFile, args);
    }

    public List<LocationSuggestion> getCitySuggestions(int countryId, String query, int limit, String lang) {
        List<City> cities;
        String sqlFile;
        Map<String, Object> args = new HashMap<String, Object>();

        if (Strings.isEmpty(query)) {
            sqlFile = "location/city_suggestions_without_query.sql";
            args.put("nameField", lang.equals("ru") ? "ruName" : "enName");
        } else {
            query += "%";
            args.put("query", query);
            sqlFile = "location/city_suggestions.sql";
        }

        args.put("limit", limit);
        args.put("countryId", countryId);

        return sqlFileExecutor.executeSQLQuery(sqlFile, args, LocationSuggestion.class);
    }

    public List<LocationSuggestion> getCountrySuggestions(String query, int limit, String lang) {
        String sqlFile;
        Map<String, Object> args = new HashMap<String, Object>();
        if (Strings.isEmpty(query)) {
            sqlFile = "location/country_suggestions_without_query.sql";
            args.put("nameField", lang.equals("ru") ? "ruName" : "enName");
        } else {
            query += "%";
            args.put("query", query);
            sqlFile = "location/country_suggestions.sql";
        }

        args.put("limit", limit);

        return sqlFileExecutor.executeSQLQuery(sqlFile, args, LocationSuggestion.class);
    }
}