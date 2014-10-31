package com.tiksem.pq.http;

import com.utils.framework.strings.Strings;

/**
 * User: Tikhonenko.S
 * Date: 29.04.2014
 * Time: 14:49
 */
public class RequestUtils {
    public static int getRequestIntParam(String paramName, String value, int defaultValue) {
        try {
            return Strings.isEmpty(value) ? defaultValue : Integer.valueOf(value);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(paramName + " should be integer");
        }
    }

    public static long getRequestLongParam(String paramName, String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(paramName + " should be integer, " + e.getMessage());
        }
    }
}
