package com.tiksem.pq.data;

import com.tiksem.mysqljava.annotations.PrimaryKey;
import com.tiksem.mysqljava.annotations.Stored;
import com.tiksem.mysqljava.annotations.Table;

/**
 * Created by CM on 1/13/2015.
 */
@Table
public class Count {
    @PrimaryKey
    private String tableName;
    @Stored
    private Long count;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
