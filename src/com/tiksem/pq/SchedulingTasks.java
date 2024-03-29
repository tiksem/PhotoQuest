package com.tiksem.pq;

import com.tiksem.pq.db.DatabaseManager;
import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by CM on 12/18/2014.
 */
@EnableScheduling
@Component
@Scope("singleton")
public class SchedulingTasks {
    private DatabaseManager databaseManager = new DatabaseManager();

    @Scheduled(cron = "0 * * * * *")
    public void clearRatingAndViews() {
        //databaseManager.clearRatingAndViews();
    }
}
