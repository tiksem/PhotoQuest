package com.tiksem.pq.db;

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

    PersistenceManager persistenceManager;

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

    public AdvancedRequestsManager(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }
}
