package com.tiksem.mysqljava;

import com.tiksem.mysqljava.annotations.*;
import com.tiksem.mysqljava.help.SqlGenerationUtilities;
import com.utils.framework.CollectionUtils;
import com.utils.framework.Predicate;
import com.utils.framework.Reflection;

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

    private Connection connection;
    private OnRowSelectedListener onRowSelectedListener;

    public MysqlObjectMapper(Connection connection) {
        this.connection = connection;

        try {
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
            NamedParameterStatement statement = getNamedParameterStatement(sql, args);
            return statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private NamedParameterStatement getNamedParameterStatement(String sql, Map<String, Object> args) {
        NamedParameterStatement statement = null;
        try {
            statement = new NamedParameterStatement(connection, sql);
            if (args != null) {
                statement.setObjects(args);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return statement;
    }

    public int executeModifySQL(String sql, Map<String, Object> args) {
        try {
            NamedParameterStatement statement = getNamedParameterStatement(sql, args);
            return statement.executeUpdate();
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

    public ResultSet executeSelectSqlGetResultSet(String sql, Map<String, Object> args) {
        try {
            NamedParameterStatement statement = getNamedParameterStatement(sql, args);
            return statement.executeQuery();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> List<T> executeOneColumnValuesSql(String sql) {
        ResultSet resultSet = executeSelectSqlGetResultSet(sql);
        return ResultSetUtilities.getValuesOfColumn(resultSet, 0);
    }

    public <T> T executeFirstRowValueSelectSql(String sql, Class<T> resultClass) {
        ResultSet resultSet = executeSelectSqlGetResultSet(sql);
        try {
            if (resultSet.next()) {
                return (T) resultSet.getObject(1);
            } else {
                return null;
            }
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
            NamedParameterStatement statement = getNamedParameterStatement(sql, args);
            ResultSet resultSet = statement.executeQuery();
            List objects;
            if (!foreigns.isEmpty() || foreigns == ALL_FOREIGN) {
                objects = ResultSetUtilities.getListWithSeveralTables(resultSet,
                        new ResultSetUtilities.FieldsProvider() {
                            @Override
                            public List<Field> getFieldsOfClass(Class aClass) {
                                return resultFields;
                            }

                            @Override
                            public List<Field> getForeignValueFields(Class aClass) {
                                return getForeignValueFieldsToFill(aClass, foreigns);
                            }

                            ;
                        }, resultClass);
            } else {
                objects = ResultSetUtilities.getList(resultFields, resultClass, resultSet);
            }
            initList(objects);
            return objects;
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
        return getResultSetFromPattern(sql, fields, pattern, null);
    }

    private ResultSet getResultSetFromPattern(String sql, List<Field> fields, Object pattern,
                                              Map<String, Object> additionalArgs) {
        try {
            NamedParameterStatement statement = new NamedParameterStatement(connection, sql);
            Map<String, Object> args = ResultSetUtilities.getArgs(pattern, fields);
            if(additionalArgs != null){
                args.putAll(additionalArgs);
            }

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
        return getListFromPattern(sql, pattern, resultClass, foreignFields, null);
    }

    private <T> List<T> getListFromPattern(String sql, Object pattern, Class resultClass,
                                           final List<String> foreignFields, Map<String, Object> additionalArgs) {
        final List<Field> resultFields = SqlGenerationUtilities.getFields(resultClass);
        List<Field> fields = SqlGenerationUtilities.getFields(pattern);

        ResultSet resultSet = getResultSetFromPattern(sql, fields, pattern, additionalArgs);

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
            Class resultClass,
            String foreignFieldName,
            SelectParams selectParams) {
        List<String> foreignFieldNames = selectParams.foreignFieldsToFill;
        List<SqlGenerationUtilities.Foreign> foreigns = getForeignsFromClass(foreignFieldNames, resultClass);

        SqlGenerationUtilities.Foreign foreign = new SqlGenerationUtilities.Foreign();
        foreign.foreignField = Reflection.getFieldByNameOrThrow(pattern, foreignFieldName);
        foreign.childClass = pattern.getClass();
        foreigns.add(foreign);

        if(selectParams.ordering != null && !selectParams.ordering.contains(".")){
            selectParams.ordering = "`" + resultClass.getSimpleName() + "_" +
                    foreignFieldName +
                    "`." + selectParams.ordering;
        }

        String sql =
                SqlGenerationUtilities.select(
                        Arrays.<Class>asList(pattern.getClass()),
                        foreigns,
                        pattern,
                        selectParams);
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

    public <T> List<T> queryByPattern(final T pattern) {
        return queryByPattern(pattern, null, null);
    }

    public <T> T getObjectByPattern(final T pattern) {
        return getObjectByPattern(pattern, null);
    }

    public <T> T getObjectByPattern(final T pattern, String additionalWhere) {
        SelectParams params = new SelectParams();
        params.offsetLimit = new OffsetLimit(0, 1);
        params.additionalWhereClosure = additionalWhere;
        List<T> list = queryByPattern(pattern, params);
        if(list.isEmpty()){
            return null;
        }

        T object = list.get(0);
        initRow(object);
        return object;
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

    public void changeValue(Object object, String fieldName, long diff) {
        if(diff == 0){
            return;
        }

        List<Field> fields = SqlGenerationUtilities.getFields(object);
        Map<String, Object> args = ResultSetUtilities.getArgs(object, fields);
        String sql = SqlGenerationUtilities.changeValue(object, fields, fieldName, diff);
        int modifiedCount = executeModifySQL(sql, args);
        if (modifiedCount == 1) {
            Reflection.changeNumberUsingGetterAndSetter(object, fieldName, diff);
        } else if(modifiedCount != 0) {
            throw new IllegalStateException("More than one object have been modified");
        }
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

        T next = objects.iterator().next();
        initRow(next);
        return next;
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

    private <T> void initRow(T item) {
        if(onRowSelectedListener != null){
            onRowSelectedListener.onRowSelected(item);
            List<Field> foreigns = Reflection.getFieldsWithAnnotations(item.getClass(), ForeignValue.class);
            for(Field foreign : foreigns){
                if(!Reflection.isNull(item, foreign)){
                    onRowSelectedListener.onRowSelected(Reflection.getFieldValueUsingGetter(item, foreign));
                }
            }
        }
    }

    private <T> void initList(List<T> list) {
        for(T item : list){
            initRow(item);
        }
    }

    public <T> List<T> queryAllObjects(Class<T> aClass, OffsetLimit offsetLimit, String ordering) {
        SelectParams selectParams = new SelectParams();
        selectParams.offsetLimit = offsetLimit;
        selectParams.ordering = ordering;
        return queryAllObjects(aClass, selectParams);
    }

    public <T> T getNextPrev(T pattern, Object object, String orderBy, List<String> foreigns, boolean next,
                             boolean allowCircle) {
        List<SqlGenerationUtilities.Foreign> foreignList = getForeignsFromClass(foreigns, pattern.getClass());
        List<Field> orderByFields = new ArrayList<Field>();
        String sql = SqlGenerationUtilities.nextPrev(pattern, orderBy, foreignList, next, orderByFields);

        Map<String, Object> args = ResultSetUtilities.getArgs(object, orderByFields);
        List<T> list = getListFromPattern(sql, pattern, pattern.getClass(), foreigns, args);
        if(list.isEmpty()){
            if (!allowCircle) {
                return null;
            } else {
                if(!next){
                    orderBy = SqlGenerationUtilities.reverseOrderBy(orderBy);
                }

                SelectParams params = new SelectParams();
                params.ordering = orderBy;
                params.foreignFieldsToFill = foreigns;
                params.offsetLimit = new OffsetLimit(0, 1);
                list = queryByPattern(pattern, params);
                if(list.isEmpty()){
                    return null;
                }

                T item = list.get(0);
                initRow(item);
                return item;
            }
        }

        T item = list.get(0);
        initRow(item);
        return item;
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

    public long getObjectPosition(Object object, Object pattern, String orderBy) {
        List<Field> orderByFields = new ArrayList<Field>();
        String sql = SqlGenerationUtilities.getPosition(pattern, object.getClass(), orderBy, orderByFields);
        Map<String, Object> args = ResultSetUtilities.getArgs(pattern,
                SqlGenerationUtilities.getFields(pattern));
        args.putAll(ResultSetUtilities.getArgs(object, SqlGenerationUtilities.getFields(object)));
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
        List<T> list = getListFromPattern(sql, pattern, pattern.getClass(), foreignFieldNames);
        initList(list);
        return list;
    }

    public <Id, T> List<Id> queryIdesByPattern(final T pattern) {
        String sql = SqlGenerationUtilities.selectIdes(pattern);
        Map<String, Object> args = ResultSetUtilities.getArgs(pattern, SqlGenerationUtilities.getFields(pattern));
        ResultSet resultSet = executeSelectSqlGetResultSet(sql, args);
        return ResultSetUtilities.getValuesOfColumn(resultSet, "id");
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

    public <T> int insertAll(List<T> objects, boolean ignore) {
        InsertStatement insertStatement = new InsertStatement((List<Object>) objects);
        insertStatement.setIgnore(ignore);
        return insertStatement.execute(connection);
    }

    public <T> int insertAll(List<T> objects) {
        return insertAll(objects, false);
    }

    public int insert(Object object, boolean ignore) {
        return insertAll(Collections.singletonList(object), ignore);
    }

    public int insert(Object object) {
        return insert(object, false);
    }

    public void replace(Object object) {
        replaceAll(Collections.singletonList(object));
    }

    public void replaceAll(final List<Object> objects) {
        ReplaceStatement replaceStatement = new ReplaceStatement(objects);
        replaceStatement.execute(connection);
    }

    public <T> int updateUsingPattern(T pattern, T values) {
        String sql = SqlGenerationUtilities.update(pattern, values);
        List<Field> patternFields = SqlGenerationUtilities.getFields(pattern);
        List<Field> valuesFields = SqlGenerationUtilities.getFields(values);

        Map<String, Object> args = ResultSetUtilities.getArgs(pattern, patternFields);
        Map<String, Object> valuesArgs = ResultSetUtilities.getArgs(values, valuesFields);
        for(String key : valuesArgs.keySet()){
            args.put(key + "_update", valuesArgs.get(key));
        }

        return executeModifySQL(sql, args);
    }

    public int deleteAll(Iterable<Object> objects) {
        int sum = 0;

        for(Object object : objects){
            List<Field> fields = SqlGenerationUtilities.getFields(object);
            Map<String, Object> args = ResultSetUtilities.getArgs(object, fields);
            String sql = SqlGenerationUtilities.delete(object, fields);
            sum += executeModifySQL(sql, args);
        }

        return sum;
    }

    public int deleteAll(Object... objects) {
        return deleteAll(Arrays.asList(objects));
    }

    public int delete(Object object) {
        return deleteAll(Arrays.asList(object));
    }

    public OnRowSelectedListener getOnRowSelectedListener() {
        return onRowSelectedListener;
    }

    public void setOnRowSelectedListener(OnRowSelectedListener onRowSelectedListener) {
        this.onRowSelectedListener = onRowSelectedListener;
    }

    @Override
    protected void finalize() throws Throwable {
        destroy();
        super.finalize();
    }

    public void destroy() {
        if(connection != null){
            try {
                connection.close();
            } catch (SQLException e) {

            }

            connection = null;
        }
    }
}
