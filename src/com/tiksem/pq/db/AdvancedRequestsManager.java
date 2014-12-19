package com.tiksem.pq.db;

import com.tiksem.pq.data.Action;
import com.tiksem.pq.data.Photoquest;
import com.utils.framework.CollectionUtils;
import com.utils.framework.collections.iterator.AbstractIterator;

import javax.jdo.PersistenceManager;
import java.util.*;

/**
 * Created by CM on 12/5/2014.
 */
public class AdvancedRequestsManager {
    private static final String FOLLOWING_ADDING_DATE_QUEST_SQL =
            "SELECT * FROM photoquest WHERE photoquest.id in\n" +
                    "(SELECT photoquestId FROM FollowingPhotoquest WHERE userId = :userId ORDER BY addingDate DESC) " +
                    "LIMIT :offset, :limit";
    private static final String FOLLOWING_RATED_QUEST_SQL =
            "SELECT * FROM photoquest WHERE photoquest.id in\n" +
                    "(SELECT photoquestId FROM FollowingPhotoquest WHERE userId = :userId) " +
                    "ORDER BY likesCount DESC LIMIT :offset, :limit";
    private static final String FOLLOWING_HOTTEST_QUEST_SQL =
            "SELECT * FROM photoquest WHERE photoquest.id in\n" +
                    "(SELECT photoquestId FROM FollowingPhotoquest WHERE userId = :userId) " +
                    "ORDER BY viewsCount DESC LIMIT :offset, :limit";

    private static final String NEWS_SQL = " from `action` where (photoquestId in\n" +
            "(\n" +
            "SELECT photoquestId from followingphotoquest where userId=:userId and " +
            "followingphotoquest.addingDate <= `action`.addingDate\n" +
            "))\n" +
            "or userId in (select toUserId from relationship where fromUserId=:userId and " +
            "relationship.addingDate <= `action`.addingDate)";

    private static final String NEWS_SELECT_SQL = "select *" + NEWS_SQL +
            " order by addingDate desc LIMIT :offset, :limit";

    private static final String NEWS_COUNT_SQL = "select count(*)" + NEWS_SQL;

    public static final String CREATE_PHOTOQUEST_SEARCH_INDEX = "create fulltext index keyword " +
            "on PhotoquestSearch(keywords)";
    public static final String PHOTOQUEST_SEARCH_SQL = "SELECT photoquestId, MATCH (keywords) AGAINST " +
            "(:str IN NATURAL LANGUAGE MODE) as relevance FROM photoquestsearch\n" +
            "    WHERE MATCH (keywords)\n" +
            "    AGAINST (:str IN NATURAL LANGUAGE MODE) and photoquestId <> :exclude order by relevance desc " +
            "LIMIT :offset, :limit";

    public static final String PHOTOQUEST_SEARCH_SQL_WITHOUT_EXCLUDE_ID = "SELECT photoquestId, MATCH (keywords) AGAINST " +
            "(:str IN NATURAL LANGUAGE MODE) as relevance FROM photoquestsearch\n" +
            "    WHERE MATCH (keywords)\n" +
            "    AGAINST (:str IN NATURAL LANGUAGE MODE) order by relevance desc " +
            "LIMIT :offset, :limit";

    public static final String CREATE_PHOTOQUEST_SEARCH_TABLE = "" +
            "create table if not exists photoquestsearch\n" +
            "(\n" +
            "photoquestId bigint not null,\n" +
            "keywords varchar(255) not null,\n" +
            "fulltext index(keywords)\n" +
            ");";

    public static final String INSERT_INTO_PHOTOQUEST_SEARCH = "INSERT INTO photoquestsearch " +
            "(photoquestId, keywords) values(:photoquestId, ':keywords')";

    public static final String CLEAR_USERS_RATING = "update user set rating = 0";
    public static final String CLEAR_PHOTOQUESTS_VIEWS = "update photoquest set viewsCount = 0";
    public static final String CLEAR_PHOTOS_VIEWS = "update photo set viewsCount = 0";

    private PersistenceManager persistenceManager;

    public Collection<Photoquest> getFollowingPhotoquests(long userId, RatingOrder order,
                                                          OffsetLimit offsetLimit) {
        String sql;
        if(order == RatingOrder.newest){
            sql = FOLLOWING_RATED_QUEST_SQL;
        } else if(order == RatingOrder.rated) {
            sql = FOLLOWING_ADDING_DATE_QUEST_SQL;
        } else if(order == RatingOrder.hottest) {
            sql = FOLLOWING_HOTTEST_QUEST_SQL;
        } else {
            throw new UnsupportedOperationException();
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("userId", userId);
        offsetLimit.addToMap(params);

        return (Collection<Photoquest>)
                DBUtilities.executeSQL(persistenceManager, sql, params, Photoquest.class);
    }

    public Collection<Action> getNews(long userId, OffsetLimit offsetLimit) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("userId", userId);
        offsetLimit.addToMap(params);

        return (Collection<Action>)
                DBUtilities.executeSQL(persistenceManager, NEWS_SELECT_SQL, params, Action.class);
    }

    public long getNewsCount(long userId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("userId", userId);

        Collection result = (Collection)
                DBUtilities.executeSQL(persistenceManager, NEWS_COUNT_SQL, params, null);
        if(result == null){
            return 0;
        }

        return (Long) result.iterator().next();
    }

    public List<Long> getPhotoquestsByQuery(String query, Long excludePhotoQuestId, OffsetLimit offsetLimit) {
        String sql = PHOTOQUEST_SEARCH_SQL_WITHOUT_EXCLUDE_ID;
        Map<String, Object> args = new HashMap<String, Object>();
        offsetLimit.addToMap(args);
        args.put("str", query);
        if (excludePhotoQuestId != null) {
            args.put("exclude", excludePhotoQuestId);
            sql = PHOTOQUEST_SEARCH_SQL;
        }

        final Collection<Object[]> result = (Collection<Object[]>)
                DBUtilities.executeSQL(persistenceManager, sql, args, null);

        return CollectionUtils.transform(result,
                new CollectionUtils.Transformer<Object[], Long>() {
            @Override
            public Long get(Object[] objects) {
                return (Long) objects[0];
            }
        });
    }

    public void initDatabase() {
        DBUtilities.executeNotSelectSQL(persistenceManager,
                CREATE_PHOTOQUEST_SEARCH_TABLE);
    }

    public void insertPhotoquestSearch(long photoquestId, String keywords) {
        String sql = INSERT_INTO_PHOTOQUEST_SEARCH;
        sql = sql.replaceFirst(":photoquestId", String.valueOf(photoquestId));
        sql = sql.replaceFirst(":keywords", keywords);
        DBUtilities.executeNotSelectSQL(persistenceManager, sql);
    }

    public void clearRatingAndViews() {
        DBUtilities.executeNotSelectSQL(persistenceManager, CLEAR_PHOTOQUESTS_VIEWS,
                CLEAR_PHOTOS_VIEWS, CLEAR_USERS_RATING);
    }

    public AdvancedRequestsManager(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }
}