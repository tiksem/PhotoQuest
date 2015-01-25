package com.tiksem.pq.db.help;

import com.tiksem.mysqljava.MysqlObjectMapper;
import com.tiksem.mysqljava.OffsetLimit;
import com.tiksem.pq.data.User;
import com.tiksem.pq.db.SqlFileExecutor;

import java.util.List;

/**
 * Created by CM on 1/23/2015.
 */
public class RelationsSearcher extends UsersSearcher {
    public RelationsSearcher(MysqlObjectMapper mapper, SqlFileExecutor sqlFileExecutor, SearchUsersParams params, OffsetLimit offsetLimit) {
        super(mapper, sqlFileExecutor, params, offsetLimit);
    }

    @Override
    protected long getCountByGenderAndCityId(Boolean gender, Integer cityId) {
        return 0;
    }

    @Override
    protected List<User> searchByGenderAndCityId(Boolean gender, Integer cityId, String orderBy, OffsetLimit offsetLimit) {
        return null;
    }

    @Override
    protected String getSearchFilesPath() {
        return null;
    }

    @Override
    protected String getCountFilesPath() {
        return null;
    }
}
