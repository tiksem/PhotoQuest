package com.tiksem.mysqljava.security;

import com.tiksem.mysqljava.MysqlObjectMapper;
import com.utils.framework.Maps;
import com.utils.framework.io.Network;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by CM on 2/13/2015.
 */
public class RpsGuard {
    private static final int DEFAULT_DELAY = 100;
    private static final int DEFAULT_BAN_TIME = 1000 * 60 * 15; //15 minutes
    private Map<String, String> settings;
    private ConcurrentHashMap<String, Integer> methodIdes = new ConcurrentHashMap<String, Integer>();
    private int defaultDelay;
    private int maxAttemptsCountBeforeBan;
    private int banTime;

    private int getDelay(String methodName) {
        return Maps.getInt(settings, methodName, defaultDelay);
    }

    public RpsGuard(Map<String, String> settings) {
        this.settings = settings;
        defaultDelay = Maps.getInt(settings, "defaultDelay", DEFAULT_DELAY);
        maxAttemptsCountBeforeBan = Maps.getInt(settings, "attemptsCountBeforeBan", 1);
        banTime = Maps.getInt(settings, "banTime", DEFAULT_BAN_TIME);
    }

    public static void clearRequests() {
        MysqlObjectMapper mapper = new MysqlObjectMapper();
        clearRequests(mapper);
    }

    public static void clearRequests(MysqlObjectMapper mapper) {
        mapper.executeNonSelectSQL("DELETE FROM ApiRequest");
    }

    public void commitUserRequest(MysqlObjectMapper mapper, String ip, String methodName) {
        int delay = getDelay(methodName);
        int methodId = Maps.getOrPut(methodIdes, methodName, methodIdes.size());
        ApiRequest pattern = new ApiRequest();
        pattern.setIp(Network.ip4StringToInt(ip));
        pattern.setRequestMethodId(methodId);
        ApiRequest request = mapper.getObjectByPattern(pattern);
        boolean shouldThrowIpBanned = false;

        if(request == null){
            mapper.insert(pattern);
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
            mapper.replace(request);
        }

        if(shouldThrowIpBanned){
            throw new IpHasBeenBannedException(ip, request.getBannedUntilTime());
        }
    }
}
