package com.tiksem.mysqljava.security;

import com.google.common.util.concurrent.Striped;
import com.tiksem.mysqljava.MysqlObjectMapper;
import com.utils.framework.Maps;
import com.utils.framework.io.Network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

/**
 * Created by CM on 3/2/2015.
 */
public abstract class AbstractRpsGuard implements RpsGuard {
    private static final int DEFAULT_DELAY = 100;
    private static final int DEFAULT_ATTEMPTS_COUNT = 100;
    private static final int DEFAULT_BAN_TIME = 1000 * 60 * 15; //15 minutes
    private Map<String, String> settings;
    private ConcurrentHashMap<String, Integer> methodIdes = new ConcurrentHashMap<String, Integer>();
    private int defaultDelay;
    protected int maxAttemptsCountBeforeBan;
    protected int banTime;
    private Striped<Lock> stripedLock = Striped.lock(64);

    protected int getDelay(String methodName) {
        return Maps.getInt(settings, methodName, defaultDelay);
    }

    public AbstractRpsGuard(Map<String, String> settings) {
        this.settings = settings;
        defaultDelay = Maps.getInt(settings, "defaultDelay", DEFAULT_DELAY);
        maxAttemptsCountBeforeBan = Maps.getInt(settings, "attemptsCountBeforeBan", DEFAULT_ATTEMPTS_COUNT);
        banTime = Maps.getInt(settings, "banTime", DEFAULT_BAN_TIME);
    }

    @Override
    public void commitUserRequest(MysqlObjectMapper mapper, String ip, String methodName) {
        int methodId = Maps.getOrPut(methodIdes, methodName, methodIdes.size());
        int delay = getDelay(methodName);

        ApiRequest pattern = new ApiRequest();
        pattern.setIp(Network.ip4StringToInt(ip));
        pattern.setRequestMethodId(methodId);

        try {
            onPreCommit(pattern);

            ApiRequest request = getObjectByPattern(mapper, pattern);
            boolean shouldThrowIpBanned = false;

            if(request == null){
                pattern.setLastRequestTime(System.currentTimeMillis());
                insert(mapper, pattern);
            } else {
                long currentTime = System.currentTimeMillis();

                Long bannedUntilTime = request.getBannedUntilTime();
                if(bannedUntilTime == null){
                    long lastRequestTime = request.getLastRequestTime();
                    if(currentTime - lastRequestTime < delay){
                        Integer attemptsCountBeforeBan = request.getAttemptsCountBeforeBan();
                        if(attemptsCountBeforeBan == null){
                            attemptsCountBeforeBan = maxAttemptsCountBeforeBan;
                        }

                        attemptsCountBeforeBan--;
                        if(attemptsCountBeforeBan <= 0){
                            request.setBannedUntilTime(currentTime + banTime);
                            shouldThrowIpBanned = true;
                        }
                    } else {
                        request.setBannedUntilTime(null);
                        request.setAttemptsCountBeforeBan(null);
                    }
                } else {
                    if (bannedUntilTime > currentTime) {
                        shouldThrowIpBanned = true;
                    } else {
                        request.setBannedUntilTime(null);
                        request.setAttemptsCountBeforeBan(null);
                    }
                }

                request.setLastRequestTime(currentTime);
                replace(mapper, request);
            }

            if(shouldThrowIpBanned){
                throw new IpHasBeenBannedException(ip, request.getBannedUntilTime());
            }
        } finally {
            onPostCommit(pattern);
        }
    }

    protected void onPostCommit(ApiRequest pattern) {
        stripedLock.get(pattern).unlock();
    }

    protected void onPreCommit(ApiRequest pattern) {
        stripedLock.get(pattern).lock();
    }

    protected abstract void replace(MysqlObjectMapper mapper, ApiRequest request);
    protected abstract void insert(MysqlObjectMapper mapper, ApiRequest request);
    protected abstract ApiRequest getObjectByPattern(MysqlObjectMapper mapper, ApiRequest pattern);
}
