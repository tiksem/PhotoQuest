package com.tiksem.pq;

import com.tiksem.mysqljava.MysqlObjectMapper;
import com.tiksem.mysqljava.security.RpsGuard;
import com.tiksem.pq.db.DatabaseManager;
import com.tiksem.pq.db.PhotoquestDataSource;
import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by CM on 12/18/2014.
 */
@EnableScheduling
@Component
@Scope("singleton")
public class SchedulingTasks {
    private DatabaseManager getDatabaseManager() {
        Connection connection = null;
        try {
            connection = PhotoquestDataSource.getInstance().getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return new DatabaseManager(null, new MysqlObjectMapper(connection), "en");
    }

    @Scheduled(cron = "0 0/30 * * * ?")
    public void updateNewFlag() {
        getDatabaseManager().updateNewFlag();
    }

    @Scheduled(cron = "0 0/15 * * * *")
    public void clearRps() {
        RpsGuard rpsGuard = Settings.getInstance().getRpsGuard();
        rpsGuard.clearUnbannedIPes(getDatabaseManager().getMapper());
    }

    @Scheduled(cron = "0 0/5 * * * *")
    public void clearCaptcha() {
        getDatabaseManager().clearOldCaptchas(5 * 60 * 1000);
    }
}
