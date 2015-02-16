package com.tiksem.mysqljava.security;

import com.tiksem.mysqljava.annotations.*;

/**
 * Created by CM on 2/13/2015.
 */
@Table
@MultipleIndex(fields = {"ip", "requestMethodId"}, isUnique = true)
public class ApiRequest {
    @Stored
    @NotNull
    private Integer ip;

    @Stored
    @NotNull
    private Integer requestMethodId;

    @AddingDate
    @Stored
    private Long lastRequestTime;

    @Stored
    private Long bannedUntilTime;

    @Stored
    private Integer attemptsCountBeforeBan;

    public Integer getIp() {
        return ip;
    }

    public void setIp(Integer ip) {
        this.ip = ip;
    }

    public Long getLastRequestTime() {
        return lastRequestTime;
    }

    public void setLastRequestTime(Long lastRequestTime) {
        this.lastRequestTime = lastRequestTime;
    }

    public Long getBannedUntilTime() {
        return bannedUntilTime;
    }

    public void setBannedUntilTime(Long bannedUntilTime) {
        this.bannedUntilTime = bannedUntilTime;
    }

    public Integer getAttemptsCountBeforeBan() {
        return attemptsCountBeforeBan;
    }

    public void setAttemptsCountBeforeBan(Integer attemptsCountBeforeBan) {
        this.attemptsCountBeforeBan = attemptsCountBeforeBan;
    }

    public Integer getRequestMethodId() {
        return requestMethodId;
    }

    public void setRequestMethodId(Integer requestMethodId) {
        this.requestMethodId = requestMethodId;
    }
}