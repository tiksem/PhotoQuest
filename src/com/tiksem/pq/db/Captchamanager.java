package com.tiksem.pq.db;

import java.io.InputStream;

/**
 * Created by CM on 1/18/2015.
 */
public interface CaptchaManager {
    public long saveCaptcha();
    public InputStream getCaptchaImage(long key);
    public boolean checkCaptcha(long key, String answer);
    public void clearOldCaptchas(long delay);
}
