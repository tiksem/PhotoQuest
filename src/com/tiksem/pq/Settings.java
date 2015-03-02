package com.tiksem.pq;

import com.tiksem.mysqljava.security.DatabaseRpsGuard;
import com.tiksem.mysqljava.security.MemoryRpsGuard;
import com.tiksem.mysqljava.security.RpsGuard;
import com.utils.framework.strings.Strings;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * Created by CM on 1/18/2015.
 */
public class Settings {
    private static Settings settings = null;
    private Properties properties;
    private RpsGuard rpsGuard;

    public synchronized static Settings getInstance() {
        if(settings == null){
            settings = new Settings();
        }

        return settings;
    }

    public void update() {
        properties = new Properties();
        try {
            properties.load(new FileInputStream("settings.txt"));
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
        try {
            return Long.parseLong(properties.getProperty("requestDelay"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public boolean isEnableRPS() {
        return Strings.equalsIgnoreCase(properties.getProperty("enableRPS"), "true");
    }
}
