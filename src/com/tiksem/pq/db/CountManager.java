package com.tiksem.pq.db;

import com.tiksem.mysqljava.MysqlObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by CM on 1/13/2015.
 */
public class CountManager {
    private SqlFileExecutor sqlFileExecutor;
    private MysqlObjectMapper mapper;

    public CountManager(MysqlObjectMapper mapper) {
        this.mapper = mapper;
        sqlFileExecutor = new SqlFileExecutor(mapper);
    }

    private String getTableNameWhere(String tableName) {
        return "WHERE tableName='" + tableName + "'";
    }

    public void changeCount(Class aClass, int value) {
        changeCount(aClass.getSimpleName(), value);
    }

    public void changeCount(String tableName, int value) {
        String sql = "UPDATE `count` SET `count`=`count`+" + value + " " + getTableNameWhere(tableName);
        mapper.executeNonSelectSQL(sql);
    }

    public void increment(Class aClass) {
        changeCount(aClass, 1);
    }

    public void decrement(Class aClass) {
        changeCount(aClass, -1);
    }

    public long updateCount(String tableName) {
        long count = mapper.getAllObjectsCount(tableName);
        String sql = "UPDATE `count` SET `count`=" + count + " " + getTableNameWhere(tableName);
        mapper.executeNonSelectSQL(sql);
        return count;
    }

    public long updateCount(Class aClass) {
        return updateCount(aClass.getSimpleName());
    }

    public long getCount(String tableName) {
        String sql = "SELECT `count` FROM `count` " + getTableNameWhere(tableName);
        ResultSet resultSet = mapper.executeSelectSqlGetResultSet(sql);
        try {
            if(resultSet.next()){
                return resultSet.getLong(1);
            }

            return updateCount(tableName);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public long getCount(Class aClass) {
        return getCount(aClass.getSimpleName());
    }
}
