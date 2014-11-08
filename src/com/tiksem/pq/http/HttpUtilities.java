package com.tiksem.pq.http;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

    public static String getBaseUrl( HttpServletRequest request ) {
        if ((request.getServerPort() == 80) ||
                (request.getServerPort() == 443))
            return request.getScheme() + "://" +
                    request.getServerName();
        else
            return request.getScheme() + "://" +
                    request.getServerName() + ":" + request.getServerPort();
    }

    public static void removeCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
