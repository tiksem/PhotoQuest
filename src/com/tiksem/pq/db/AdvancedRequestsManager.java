package com.tiksem.pq.db;

import com.tiksem.pq.data.Action;
import com.tiksem.pq.data.Photoquest;

import javax.jdo.PersistenceManager;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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

    private PersistenceManager persistenceManager;

    public Collection<Photoquest> getFollowingPhotoquests(long userId, RatingOrder order,
                                                          OffsetLimit offsetLimit) {
        String sql;
        if(order == RatingOrder.newest){
            sql = FOLLOWING_RATED_QUEST_SQL;
        } else if(order == RatingOrder.rated) {
            sql = FOLLOWING_ADDING_DATE_QUEST_SQL;
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

    public AdvancedRequestsManager(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }
}
