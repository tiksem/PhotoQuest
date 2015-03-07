package com.tiksem.pq.db;

import com.tiksem.pq.Settings;
import com.utils.framework.Maps;
import org.apache.commons.dbcp2.BasicDataSource;

import java.io.IOException;
import java.util.Map;

/**
 * Created by CM on 3/7/2015.
 */
public class PhotoquestDataSource extends BasicDataSource {
    private static PhotoquestDataSource instance;

    private PhotoquestDataSource() {
        Settings settings = Settings.getInstance();
        setDriverClassName("com.mysql.jdbc.Driver");
        setUrl(settings.get("connectionUrl"));
        setUsername(settings.get("dbUsername"));
        setPassword(settings.get("dbPassword"));
        setMaxTotal(settings.getInt("dbConnectionPoolSize"));
        setMaxIdle(settings.getInt("dbConnectionIdlePoolSize"));
    }

    public synchronized static PhotoquestDataSource getInstance() {
        if(instance == null){
            instance = new PhotoquestDataSource();
        }

        return instance;
    }
}
