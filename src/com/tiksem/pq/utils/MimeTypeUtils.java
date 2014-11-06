package com.tiksem.pq.utils;

import org.springframework.http.MediaType;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

/**
 * Created by CM on 11/5/2014.
 */
public class MimeTypeUtils {
    public static String getMimeTypeFromByteArray(byte[] bytes) throws IOException {
        InputStream is = new BufferedInputStream(new ByteArrayInputStream(bytes));
        return URLConnection.guessContentTypeFromStream(is);
    }

    public static MediaType getMediaTypeFromByteArray(byte[] bytes) throws IOException {
        return MediaType.valueOf(getMimeTypeFromByteArray(bytes));
    }
}
