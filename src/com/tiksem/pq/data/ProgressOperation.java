package com.tiksem.pq.data;

import com.tiksem.mysqljava.annotations.PrimaryKey;
import com.tiksem.mysqljava.annotations.Stored;
import com.tiksem.mysqljava.annotations.Table;

/**
 * Created by CM on 2/10/2015.
 */
@Table
public class ProgressOperation {
    @PrimaryKey
    private Long id;
    @Stored
    private String operationName;
    @Stored
    private Integer maxProgress;
    @Stored
    private Integer progress;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public Integer getMaxProgress() {
        return maxProgress;
    }

    public void setMaxProgress(Integer maxProgress) {
        this.maxProgress = maxProgress;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }
}
