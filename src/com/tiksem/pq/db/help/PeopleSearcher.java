package com.tiksem.pq.db.help;

import com.tiksem.mysqljava.MysqlObjectMapper;
import com.tiksem.mysqljava.OffsetLimit;
import com.tiksem.pq.data.User;
import com.tiksem.pq.db.SqlFileExecutor;

import java.util.List;

/**
 * Created by CM on 1/22/2015.
 */
public class PeopleSearcher extends UsersSearcher {
    public PeopleSearcher(MysqlObjectMapper mapper, SqlFileExecutor sqlFileExecutor, SearchUsersParams params, OffsetLimit offsetLimit) {
        super(mapper, sqlFileExecutor, params, offsetLimit);
    }

    @Override
    protected long getCountByGenderAndCityId(Boolean gender, Integer cityId) {
        User user = getPattern(gender, cityId);
        return mapper.getCountByPattern(user);
    }

    @Override
    protected List<User> searchByGenderAndCityId(Boolean gender, Integer cityId, String orderBy, OffsetLimit offsetLimit) {
        User user = getPattern(gender, cityId);
        return mapper.queryByPattern(user, offsetLimit, orderBy);
    }

    private User getPattern(Boolean gender, Integer cityId) {
        User user = new User();
        user.setGender(gender);
        user.setCityId(cityId);
        return user;
    }

    @Override
    protected String getSearchFilesPath() {
        return "user/search/";
    }

    @Override
    protected String getCountFilesPath() {
        return "user/search/count/";
    }
}
