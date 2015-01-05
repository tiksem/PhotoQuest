package com.tiksem.mysqljava;

import com.tiksem.mysqljava.annotations.*;
import com.utils.framework.CollectionUtils;
import com.utils.framework.Predicate;
import com.utils.framework.Reflection;
import com.utils.framework.collections.map.ListValuesMultiMap;
import com.utils.framework.collections.map.MultiMap;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

/**
 * Created by CM on 12/27/2014.
 */
public class MysqlObjectMapper {
    static {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static final List<String> ALL_FOREIGN = new ArrayList<String>();

    private final Connection connection;

    public MysqlObjectMapper() {
        try {
            connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost/photoquest?" +
                    "user=root&password=fightforme");
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean executeNonSelectSQL(String sql) {
        try {
            Statement statement = connection.createStatement();
            return statement.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean executeNonSelectSQL(String sql, Map<String, Object> args) {
        try {
            NamedParameterStatement statement = new NamedParameterStatement(connection, sql);
            if (args != null) {
                statement.setObjects(args);
            }
            return statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Object executeInsertSQL(String sql, Map<String, Object> args) {
        try {
            NamedParameterStatement statement = new NamedParameterStatement(connection, sql,
                    Statement.RETURN_GENERATED_KEYS);
            if (args != null) {
                statement.setObjects(args);
            }
            statement.execute();
            ResultSet generatedKeys = statement.getGeneratedKeys();
            if(generatedKeys.next()){
                return generatedKeys.getObject(1);
            }

            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ResultSet executeSelectSqlGetResultSet(String sql) {
        try {
            Statement statement = connection.createStatement();
            return statement.executeQuery(sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, Map<String, Object>>> executeSelectSql(String sql) {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            return ResultSetUtilities.getMapList(resultSet);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public <T> List<T> executeSQLQuery(String sql,
                                       Map<String, Object> args,
                                       Class<T> resultClass) {
        return executeSQLQuery(sql, args, resultClass, new ArrayList<String>());
    }

    public <T> List<T> executeSQLQuery(String sql,
                                       Class<T> resultClass) {
        return executeSQLQuery(sql, new HashMap<String, Object>(),
                resultClass, new ArrayList<String>());
    }

    public <T> List<T> executeSQLQuery(String sql,
                                       Map<String, Object> args,
                                       Class<T> resultClass,
                                       final List<String> foreigns) {
        final List<Field> resultFields = SqlGenerationUtilities.getFields(resultClass);

        try {
            NamedParameterStatement statement = new NamedParameterStatement(connection, sql);
            if (args != null) {
                statement.setObjects(args);
            }
            ResultSet resultSet = statement.executeQuery();
            if (!foreigns.isEmpty() || foreigns == ALL_FOREIGN) {
                List<Object> objects = ResultSetUtilities.getListWithSeveralTables(resultSet,
                        new ResultSetUtilities.FieldsProvider() {
                            @Override
                            public List<Field> getFieldsOfClass(Class aClass) {
                                return resultFields;
                            }

                            @Override
                            public List<Field> getForeignValueFields(Class aClass) {
                                return getForeignValueFieldsToFill(aClass, foreigns);
                            };
                        }, resultClass);

                return (List<T>) objects;
            } else {
                return ResultSetUtilities.getList(resultFields, resultClass, resultSet);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T executeSingleRowSQLQuery(String sql,
                                          Map<String, Object> args,
                                          Class<T> resultClass) {
        return executeSingleRowSQLQuery(sql, args, resultClass, new ArrayList<String>());
    }

    public <T> T executeSingleRowSQLQuery(String sql,
                                          Map<String, Object> args,
                                          Class<T> resultClass,
                                          List<String> foreigns) {
        List<T> result = executeSQLQuery(sql, args, resultClass, foreigns);
        if(result.isEmpty()){
            return null;
        }

        return result.get(0);
    }

    private ResultSet getResultSetFromPattern(String sql, List<Field> fields, Object pattern) {
        try {
            NamedParameterStatement statement = new NamedParameterStatement(connection, sql);
            Map<String, Object> args = ResultSetUtilities.getArgs(pattern, fields);
            statement.setObjects(args);
            return statement.executeQuery();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Field> getForeignValueFieldsToFill(Class aClass, final List<String> foreignFields) {
        if(foreignFields == ALL_FOREIGN){
            return Reflection.getFieldsWithAnnotations(aClass, ForeignValue.class);
        } else if(!foreignFields.isEmpty()) {
            List<Field> candidates = Reflection.getFieldsWithAnnotations(aClass, ForeignValue.class);

            return CollectionUtils.findAll(candidates, new Predicate<Field>() {
                @Override
                public boolean check(Field field) {
                    return foreignFields.contains(field.getName());
                }
            });
        } else {
            return new ArrayList<Field>();
        }
    }

    private <T> List<T> getListFromPattern(String sql, Object pattern, Class resultClass,
                                           final List<String> foreignFields) {
        final List<Field> resultFields = SqlGenerationUtilities.getFields(resultClass);
        List<Field> fields = SqlGenerationUtilities.getFields(pattern);

        ResultSet resultSet = getResultSetFromPattern(sql, fields, pattern);

        List<Object> objects = ResultSetUtilities.getListWithSeveralTables(resultSet,
                new ResultSetUtilities.FieldsProvider() {
                    @Override
                    public List<Field> getFieldsOfClass(Class aClass) {
                        return resultFields;
                    }

                    @Override
                    public List<Field> getForeignValueFields(Class aClass) {
                        return getForeignValueFieldsToFill(aClass, foreignFields);
                    }

                    ;
                }, resultClass);
        return (List<T>) objects;
    }

    public <T, Foreign> List<T> queryByForeignPattern(
            Foreign pattern,
            Class<T> resultClass,
            String foreignFieldName,
            SelectParams selectParams) {
        List<String> foreignFieldNames = selectParams.foreignFieldsToFill;
        List<SqlGenerationUtilities.Foreign> foreigns = getForeignsFromClass(foreignFieldNames, resultClass);

        SqlGenerationUtilities.Foreign foreign = new SqlGenerationUtilities.Foreign();
        foreign.foreignField = Reflection.getFieldByNameOrThrow(pattern, foreignFieldName);
        foreign.childClass = pattern.getClass();
        foreigns.add(foreign);

        String sql =
                SqlGenerationUtilities.select(Arrays.<Class>asList(resultClass), foreigns, pattern, selectParams);
        return getListFromPattern(sql, pattern, resultClass,
                selectParams.foreignFieldsToFill);
    }

    private List<SqlGenerationUtilities.Foreign> getForeignsFromClass(List<String> foreignFieldNames,
                                                                final Class childClass) {
        if (foreignFieldNames != ALL_FOREIGN) {
            return CollectionUtils.transform(foreignFieldNames,
                    new CollectionUtils.Transformer<String, SqlGenerationUtilities.Foreign>() {
                        @Override
                        public SqlGenerationUtilities.Foreign get(String fieldName) {
                            SqlGenerationUtilities.Foreign foreign = new SqlGenerationUtilities.Foreign();
                            Field field = Reflection.getFieldByNameOrThrow(childClass, fieldName);
                            ForeignValue foreignValue = Reflection.getAnnotationOrThrow(field,
                                    ForeignValue.class);
                            foreign.foreignField = Reflection.getFieldByNameOrThrow(childClass,
                                    foreignValue.idField());
                            foreign.childClass = childClass;
                            return foreign;
                        }
                    });
        } else {
            return CollectionUtils.transform(Reflection.getFieldsWithAnnotations(childClass, ForeignValue.class),
                    new CollectionUtils.Transformer<Field, SqlGenerationUtilities.Foreign>() {
                        @Override
                        public SqlGenerationUtilities.Foreign get(Field field) {
                            SqlGenerationUtilities.Foreign foreign = new SqlGenerationUtilities.Foreign();
                            ForeignValue foreignValue = Reflection.getAnnotationOrThrow(field,
                                    ForeignValue.class);
                            foreign.foreignField = Reflection.getFieldByNameOrThrow(childClass,
                                    foreignValue.idField());
                            foreign.childClass = childClass;
                            return foreign;
                        }
                    });
        }
    }

    public <T> T getObjectById(Class<T> aClass, Object id) {
        String sql = SqlGenerationUtilities.getObjectByPrimaryKey(aClass, id);
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("id", id);
        return executeSingleRowSQLQuery(sql, args, aClass);
    }

    public <T> T getObjectById(Class<T> aClass, Object id, List<String> foreigns) {
        if(foreigns.isEmpty() && foreigns != ALL_FOREIGN){
            return getObjectById(aClass, id);
        }

        T pattern = Reflection.createObjectOfClass(aClass);
        Field primaryKey = SqlGenerationUtilities.getPrimaryKey(pattern);
        if(primaryKey == null){
            throw new IllegalArgumentException("Primary key is not defined");
        }

        Reflection.setFieldValueUsingSetter(pattern, primaryKey, id);

        SelectParams selectParams = new SelectParams();
        selectParams.foreignFieldsToFill = foreigns;
        selectParams.offsetLimit = new OffsetLimit(0, 1);
        List<T> result = queryByPattern(pattern, selectParams);
        if(result.isEmpty()){
            return null;
        }

        return result.get(0);
    }

    public <T> List<T> queryByPattern(final T pattern, OffsetLimit offsetLimit, String ordering) {
        SelectParams params = new SelectParams();
        params.offsetLimit = offsetLimit;
        params.ordering = ordering;
        return queryByPattern(pattern, params);
    }

    public <T> List<T> queryByPattern(final T pattern, OffsetLimit offsetLimit) {
        return queryByPattern(pattern, offsetLimit, null);
    }

    public <T> T getObjectByPattern(final T pattern) {
        List<T> list = queryByPattern(pattern, new OffsetLimit(0, 1));
        if(list.isEmpty()){
            return null;
        }

        return list.get(0);
    }

    public long getCountByPattern(final Object pattern) {
        String sql = SqlGenerationUtilities.countByPattern(pattern);
        ResultSet resultSet = getResultSetFromPattern(sql, SqlGenerationUtilities.getFields(pattern), pattern);
        try {
            resultSet.next();
            return resultSet.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T max(Class aClass, String fieldName) {
        return max(aClass, fieldName, null);
    }

    public <T> T getMaxByPattern(Object pattern, String fieldName, T defaultValue) {
        List<Field> fields = SqlGenerationUtilities.getFields(pattern);
        String sql = SqlGenerationUtilities.max(fields, pattern, fieldName);
        try {
            NamedParameterStatement statement = new NamedParameterStatement(connection, sql);
            Map<String, Object> args = ResultSetUtilities.getArgs(pattern, fields);
            statement.setObjects(args);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            Object result = resultSet.getObject(1);
            if(result == null){
                return defaultValue;
            }

            return (T) result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void changeValue(Object object, String fieldName, int diff) {
        List<Field> fields = SqlGenerationUtilities.getFields(object);
        Map<String, Object> args = ResultSetUtilities.getArgs(object, fields);
        String sql = SqlGenerationUtilities.changeValue(object, fields, fieldName, diff);
        executeNonSelectSQL(sql);
    }

    public void increment(Object object, String fieldName) {
        changeValue(object, fieldName, 1);
    }

    public void decrement(Object object, String fieldName) {
        changeValue(object, fieldName, -1);
    }

    public <T> T getObjectWithMaxFieldByPattern(T pattern, String fieldName, T defaultValue) {
        List<T> objects = queryByPattern(pattern, new OffsetLimit(0, 1), fieldName + " desc");
        if(objects.isEmpty()){
            return defaultValue;
        }

        return objects.iterator().next();
    }

    public <T> T max(Class aClass, String fieldName, T defaultValue) {
        String sql = SqlGenerationUtilities.max(aClass, fieldName);
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            resultSet.next();
            Object result = resultSet.getObject(1);
            if(result == null){
                return defaultValue;
            }

            return (T) result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> List<T> queryAllObjects(Class<T> aClass, SelectParams selectParams) {
        List<SqlGenerationUtilities.Foreign> foreignFields = getForeignsFromClass(selectParams.foreignFieldsToFill,
                aClass);
        String sql = SqlGenerationUtilities.selectAll(aClass, foreignFields, selectParams);
        return executeSQLQuery(sql, new HashMap<String, Object>(), aClass, selectParams.foreignFieldsToFill);
    }

    public <T> List<T> queryAllObjects(Class<T> aClass, OffsetLimit offsetLimit, String ordering) {
        SelectParams selectParams = new SelectParams();
        selectParams.offsetLimit = offsetLimit;
        selectParams.ordering = ordering;
        return queryAllObjects(aClass, selectParams);
    }

    public long getAllObjectsCount(Class aClass) {
        try {
            String sql = SqlGenerationUtilities.count(aClass);
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            resultSet.next();
            return resultSet.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public long getObjectPosition(Object object, Object pattern, String orderBy, boolean desc) {
        String sql = SqlGenerationUtilities.getPosition(pattern, object.getClass(), orderBy, desc);
        Map<String, Object> args = ResultSetUtilities.getArgs(pattern,
                SqlGenerationUtilities.getFields(pattern));
        args.put(orderBy, Reflection.getFieldValueUsingGetter(object, orderBy));
        return executeCountQuery(sql, args);
    }

    public long executeCountQuery(String sql, Map<String, Object> args) {
        try {
            NamedParameterStatement statement = new NamedParameterStatement(connection, sql);
            statement.setObjects(args);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            return resultSet.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> List<T> queryByPattern(final T pattern, SelectParams selectParams) {
        List<String> foreignFieldNames = selectParams.foreignFieldsToFill;
        List<SqlGenerationUtilities.Foreign> foreigns = getForeignsFromClass(foreignFieldNames, pattern.getClass());

        String sql = SqlGenerationUtilities.select(pattern, foreigns, selectParams);
        return getListFromPattern(sql, pattern, pattern.getClass(), foreignFieldNames);
    }

    private boolean hasUniqueMultiIndexes(Class<?> aClass) {
        MultipleIndex multipleIndex = aClass.getAnnotation(MultipleIndex.class);
        if(multipleIndex != null && multipleIndex.isUnique()){
            return true;
        }

        MultipleIndexes multipleIndexes = aClass.getAnnotation(MultipleIndexes.class);
        if(multipleIndexes == null){
            return false;
        } else {
            MultipleIndex[] indexes = multipleIndexes.indexes();
            for(MultipleIndex index : indexes){
                if(index.isUnique()){
                    return true;
                }
            }

            return false;
        }
    }

    public void insertAll(List<Object> objects) {
        InsertStatement insertStatement = new InsertStatement(objects);
        insertStatement.execute(connection);
    }

    public void insert(Object object) {
        insertAll(Collections.singletonList(object));
    }

    public void insertAll(Object... objects) {
        insertAll(Arrays.asList(objects));
    }

    public void replace(Object object) {
        replaceAll(Collections.singletonList(object));
    }

    public void replaceAll(final List<Object> objects) {
        ReplaceStatement replaceStatement = new ReplaceStatement(objects);
        replaceStatement.execute(connection);
    }

    public <T> void updateUsingPattern(T pattern, T values) {
        String sql = SqlGenerationUtilities.update(pattern, values);
        List<Field> patternFields = SqlGenerationUtilities.getFields(pattern);
        List<Field> valuesFields = SqlGenerationUtilities.getFields(values);

        Map<String, Object> args = ResultSetUtilities.getArgs(pattern, patternFields);
        Map<String, Object> valuesArgs = ResultSetUtilities.getArgs(values, valuesFields);
        for(String key : valuesArgs.keySet()){
            args.put(key + "_update", valuesArgs.get(key));
        }

        executeNonSelectSQL(sql, args);
    }

    public void deleteAll(Iterable<Object> objects) {
        for(Object object : objects){
            List<Field> fields = SqlGenerationUtilities.getFields(object);
            Map<String, Object> args = ResultSetUtilities.getArgs(object, fields);
            String sql = SqlGenerationUtilities.delete(object, fields);
            executeNonSelectSQL(sql, args);
        }
    }

    public void deleteAll(Object... objects) {
        deleteAll(Arrays.asList(objects));
    }

    public void delete(Object object) {
        deleteAll(Arrays.asList(object));
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        connection.close();
    }
}
