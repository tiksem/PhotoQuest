package com.tiksem.pq.db.advanced;

import com.tiksem.mysqljava.MysqlObjectMapper;
import com.tiksem.mysqljava.OffsetLimit;
import com.tiksem.pq.data.*;
import com.tiksem.pq.data.response.CitySuggestion;
import com.tiksem.pq.data.response.CountrySuggestion;
import com.tiksem.pq.db.RatingOrder;
import com.tiksem.pq.db.SqlFileExecutor;
import com.utils.framework.CollectionUtils;
import com.utils.framework.strings.Strings;

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

    public List<Photoquest> getPhotoquestsByQuery(String query, OffsetLimit offsetLimit, String orderBy) {
        Map<String, Object> args = new HashMap<String, Object>();
        offsetLimit.addToMap(args);
        args.put("query", query);
        args.put("orderBy", orderBy);
        String sqlFile = "photoquest/search_photoquests_order_by.sql";
        return sqlFileExecutor.executeSQLQuery(sqlFile, args, Photoquest.class, MysqlObjectMapper.ALL_FOREIGN);
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
        if(photos.isEmpty()){
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

    public List<CitySuggestion> getCitySuggestions(int countryId, String query, int limit) {
        List<City> cities;

        if(Strings.isEmpty(query)){
            City pattern = new City();
            pattern.setCountryId(countryId);
            cities = mapper.queryByPattern(pattern, new OffsetLimit(0, limit), "id desc");
        } else {
            query += "%";

            Map<String, Object> args = new HashMap<String, Object>();
            args.put("countryId", countryId);
            args.put("query", query);
            args.put("limit", limit);

            String sqlFile = "location/city_suggestions.sql";
            cities = sqlFileExecutor.executeSQLQuery(sqlFile, args, City.class);
        }

        return CollectionUtils.transform(cities, new CollectionUtils.Transformer<City, CitySuggestion>() {
            @Override
            public CitySuggestion get(City city) {
                CitySuggestion suggestion = new CitySuggestion();
                suggestion.id = city.getId();
                suggestion.value = city.getEnName();
                return suggestion;
            }
        });
    }

    public List<CountrySuggestion> getCountrySuggestions(String query, int limit) {
        List<Country> countries;

        if(Strings.isEmpty(query)){
            countries = mapper.queryAllObjects(Country.class, new OffsetLimit(0, limit), "id desc");
        } else {
            query = "%" + query + "%";

            Map<String, Object> args = new HashMap<String, Object>();
            args.put("query", query);
            args.put("limit", limit);

            String sqlFile = "location/country_suggestions.sql";
            countries = sqlFileExecutor.executeSQLQuery(sqlFile, args, Country.class);
        }

        return CollectionUtils.transform(countries, new CollectionUtils.Transformer<Country, CountrySuggestion>() {
            @Override
            public CountrySuggestion get(Country country) {
                CountrySuggestion suggestion = new CountrySuggestion();
                suggestion.id = country.getId();
                suggestion.value = country.getEnName();
                return suggestion;
            }
        });
    }
}