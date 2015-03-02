package com.tiksem.mysqljava.security;

import com.tiksem.mysqljava.MysqlObjectMapper;

import java.util.Map;

/**
 * Created by CM on 2/13/2015.
 */
public class DatabaseRpsGuard extends AbstractRpsGuard {
    public DatabaseRpsGuard(Map<String, String> settings) {
        super(settings);
    }

    @Override
    protected void replace(MysqlObjectMapper mapper, ApiRequest request) {
        mapper.replace(request);
    }

    @Override
    protected void insert(MysqlObjectMapper mapper, ApiRequest request) {
        mapper.insert(request);
    }

    @Override
    protected ApiRequest getObjectByPattern(MysqlObjectMapper mapper, ApiRequest pattern) {
        return mapper.getObjectByPattern(pattern);
    }

    @Override
    public void clearUnbannedIPes(MysqlObjectMapper mapper) {
        mapper.executeNonSelectSQL("DELETE from ApiRequest WHERE bannedUntilTime is null " +
                "AND attemptsCountBeforeBan is NULL");
    }
}
