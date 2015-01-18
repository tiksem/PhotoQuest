package com.tiksem.pq;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by CM on 1/18/2015.
 */
public class Settings {
    private static Settings settings = null;
    private Properties properties;

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

    public Settings() {
        update();
    }

    public long getRequestDelay() {
        return Long.parseLong(properties.getProperty("requestDelay"));
    }
}
