package com.tiksem.mysqljava.security;

import com.tiksem.mysqljava.MysqlObjectMapper;

/**
 * Created by CM on 3/2/2015.
 */
public interface RpsGuard {
    void commitUserRequest(MysqlObjectMapper mapper, String ip, String methodName);
    void clearUnbannedIPes(MysqlObjectMapper mapper);
}
