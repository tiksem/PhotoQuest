package com.tiksem.pq.db;

import com.tiksem.mysqljava.MysqlObjectMapper;
import com.tiksem.pq.data.ProgressOperation;

/**
 * Created by CM on 2/10/2015.
 */
public class ProgressOperationManager {
    private ProgressOperation progressOperation;
    private MysqlObjectMapper mapper;

    public ProgressOperationManager(MysqlObjectMapper mapper) {
        this.mapper = mapper;
        progressOperation = new ProgressOperation();
        mapper.insert(progressOperation);
    }

    public void setProgress(int value, MysqlObjectMapper mapper) {
        progressOperation.setProgress(value);
        mapper.replace(progressOperation);
    }

    public void setProgress(int value) {
        setProgress(value, mapper);
    }

    public void newOperation(String name, int maxProgress) {
        progressOperation.setOperationName(name);
        progressOperation.setMaxProgress(maxProgress);
        mapper.replace(progressOperation);
    }

    public void destroy() {
        mapper.delete(progressOperation);
    }
}
