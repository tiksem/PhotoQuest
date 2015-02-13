package com.tiksem.mysqljava.security;

/**
 * Created by CM on 2/13/2015.
 */
public class IpHasBeenBannedException extends IpSecurityException {
    private String ip;
    private Long untilTime;

    public IpHasBeenBannedException(String ip, Long untilTime) {
        super("Your ip has been banned until " + untilTime);
        this.ip = ip;
        this.untilTime = untilTime;
    }

    public String getIp() {
        return ip;
    }

    public Long getUntilTime() {
        return untilTime;
    }
}
