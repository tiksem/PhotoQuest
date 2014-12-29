package com.tiksem.pq.http;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;

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

    public static Cookie createLocalhostUnexpiredCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setPath("/");
        return cookie;
    }

    public static String reencodePostParamString(String value) {
        try {
            return new String(value.getBytes("ISO-8859-1"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getWarClassesPath() {
        String path = HttpUtilities.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        return path.substring(0, path.indexOf("classes") + "classes".length());
    }
}
