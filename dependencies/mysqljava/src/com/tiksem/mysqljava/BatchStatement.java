package com.tiksem.mysqljava;

import com.utils.framework.collections.map.ListValuesMultiMap;
import com.utils.framework.collections.map.MultiMap;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by CM on 1/4/2015.
 */
public abstract class BatchStatement {
    private final List<Object> objects;
    private MultiMap<String, Map<String, Object>> sqlArgsMap =
            new ListValuesMultiMap<String, Map<String, Object>>();

    public BatchStatement(List<Object> objects) {
        this.objects = objects;
    }

    public int execute(Connection connection) {
        int sum = 0;

        for(Object object : objects){
            StatementInfo info = prepareStatementForObject(object);
            sqlArgsMap.put(info.sql, info.args);
        }

        List<NamedParameterStatement> statements = new ArrayList<NamedParameterStatement>();
        int[] statementObjectsCount = new int[objects.size()];
        int statementIndex = 0;
        for(Map.Entry<String, Collection<Map<String, Object>>> entry : sqlArgsMap.getMap().entrySet()){
            String sql = entry.getKey();
            try {
                NamedParameterStatement statement = createStatement(connection, sql);
                Collection<Map<String, Object>> value = entry.getValue();
                if (value.size() > 1) {
                    for (Map<String, Object> args : value) {
                        statement.setObjects(args);
                        statement.addBatch();
                        statementObjectsCount[statementIndex]++;
                    }
                } else {
                    statement.setObjects(value.iterator().next());
                    statementObjectsCount[statementIndex] = 1;
                }
                statements.add(statement);
                statementIndex++;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            int index = 0;
            statementIndex = 0;
            for(NamedParameterStatement statement : statements){
                int start = index;

                if (statementObjectsCount[statementIndex] > 1) {
                    int[] replaceResult = statement.executeBatch();
                    for (int i : replaceResult) {
                        if(i != 1){
                            onNotOneRowInserted(objects.get(index));
                        }
                        index++;
                        sum += i;
                    }
                } else {
                    int count = statement.executeUpdate();
                    if (count != 1) {
                        onNotOneRowInserted(objects.get(index));
                    }
                    index++;
                    sum += count;
                }

                List<Object> objects = this.objects.subList(start, index);
                onStatementExecutionFinished(objects, statement);
                statementIndex++;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return sum;
    }

    protected static class StatementInfo {
        public String sql;
        public Map<String, Object> args;
    }

    protected NamedParameterStatement createStatement(Connection connection, String sql) throws SQLException {
        return new NamedParameterStatement(connection, sql);
    }

    protected void onNotOneRowInserted(Object object) throws SQLException {

    }
    protected abstract StatementInfo prepareStatementForObject(final Object object);
    protected void onStatementExecutionFinished(List<Object> objects, NamedParameterStatement statement) {

    }
}
