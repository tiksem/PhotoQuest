package com.tiksem.pq;

import com.tiksem.mysqljava.security.DatabaseRpsGuard;
import com.tiksem.mysqljava.security.MemoryRpsGuard;
import com.tiksem.mysqljava.security.RpsGuard;
import com.tiksem.pq.db.ImageManagerSettings;
import com.utils.framework.Maps;
import com.utils.framework.strings.Strings;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by CM on 1/18/2015.
 */
public class Settings {
    private static Settings settings = null;
    private Map<String, String> properties;
    private RpsGuard rpsGuard;
    private ImageManagerSettings imageManagerSettings;
    private ImageManagerSettings captchaSettings;

    public synchronized static Settings getInstance() {
        if(settings == null){
            settings = new Settings();
        }

        return settings;
    }

    public void update() {
        properties = new LinkedHashMap<String, String>();
        try {
            Properties loaded = new Properties();
            loaded.load(new FileInputStream("settings.txt"));
            properties = new LinkedHashMap<String, String>((Map)loaded);

            imageManagerSettings = new ImageManagerSettings();
            imageManagerSettings.imageMagickPath = Maps.get(properties, "imageMagickPath", "magic");
            imageManagerSettings.imageDirectory = Maps.get(properties, "imageDirectory", "images");
            imageManagerSettings.maxAspectRatioK = Maps.getDouble(properties, "imageMaxAspectRatioK", -1.0);
            imageManagerSettings.maxHeight = Maps.getInt(properties, "maxImageHeight", 0);
            imageManagerSettings.minHeight = Maps.getInt(properties, "minImageHeight", 0);
            imageManagerSettings.maxWidth = Maps.getInt(properties, "maxImageWidth", 0);
            imageManagerSettings.minWidth = Maps.getInt(properties, "minImageWidth", 0);

            captchaSettings = new ImageManagerSettings();
            captchaSettings.imageMagickPath = imageManagerSettings.imageMagickPath;
            captchaSettings.imageDirectory = Maps.get(properties, "captchaDirectory", "captcha");
        } catch (IOException e) {

        }
    }

    public void updateRps() {
        Properties rpsSettings = new Properties();
        try {
            rpsSettings.load(new FileInputStream("rps.txt"));
        } catch (IOException e) {

        }
        rpsGuard = new MemoryRpsGuard((Map)rpsSettings);
    }

    public Settings() {
        update();
        updateRps();
    }

    public RpsGuard getRpsGuard() {
        return rpsGuard;
    }

    public long getRequestDelay() {
        return Maps.getLong(properties, "requestDelay", 0);
    }

    public ImageManagerSettings getImageManagerSettings() {
        return imageManagerSettings;
    }

    public ImageManagerSettings getCaptchaSettings() {
        return captchaSettings;
    }

    public boolean isEnableRPS() {
        return Maps.getBoolean(properties, "enableRPS");
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public int getInt(String key) {
        return Maps.getIntOrThrow(properties, key);
    }

    public String get(String key) {
        return Maps.getOrThrow(properties, key);
    }
}
