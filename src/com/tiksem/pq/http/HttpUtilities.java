package com.tiksem.pq.http;

import javax.servlet.http.Cookie;

/**
 * User: Tikhonenko.S
 * Date: 25.04.2014
 * Time: 17:03
 */
public class HttpUtilities {
    public static Cookie getCookie(String name, Cookie[] cookies) {
        if(cookies == null){
            return null;
        }

        for(Cookie cookie : cookies){
            if(cookie.getName().equals(name)){
                return cookie;
            }
        }

        return null;
    }
}
