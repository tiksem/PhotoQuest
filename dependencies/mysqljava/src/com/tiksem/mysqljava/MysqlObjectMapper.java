package com.tiksem.mysqljava;

import com.tiksem.mysqljava.annotations.ForeignValue;
import com.utils.framework.CollectionUtils;
import com.utils.framework.Predicate;
import com.utils.framework.Reflection;
import org.springframework.jdbc.support.SQLErrorCodes;

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
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void createTables(String packageName) {
        List<Class<?>> classesInPackage = Reflection.findClassesInPackage(packageName);
        for(Class aClass : classesInPackage){
            createTable(aClass);
        }
        try {
            connection.commit();
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

    public Object executeInsertSQL(String sql, Map<String, Object> args) {
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
                                       Class<T> resultClass,
                                       final List<String> foreigns) {
        final List<Field> resultFields = SqlGenerationUtilities.getFields(resultClass);

        try {
            NamedParameterStatement statement = new NamedParameterStatement(connection, sql);
            if (args != null) {
                statement.setObjects(args);
            }
            ResultSet resultSet = statement.executeQuery();
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
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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
            Map<String, Object> args = Reflection.fieldsToPropertyMap(pattern, fields);
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
            };
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

    public <T> List<T> queryByPattern(final T pattern, SelectParams selectParams) {
        List<String> foreignFieldNames = selectParams.foreignFieldsToFill;
        List<SqlGenerationUtilities.Foreign> foreigns = getForeignsFromClass(foreignFieldNames, pattern.getClass());

        String sql = SqlGenerationUtilities.select(pattern, foreigns, selectParams);
        return getListFromPattern(sql, pattern, pattern.getClass(), foreignFieldNames);
    }

    public void insertAll(Iterable<Object> objects) {
        for(Object object : objects){
            List<Field> fields = SqlGenerationUtilities.getFieldsExcludingPrimaryKey(object.getClass());
            Map<String, Object> args = Reflection.fieldsToPropertyMap(object, fields);
            String sql = SqlGenerationUtilities.insert(object, fields);
            Object id = executeInsertSQL(sql, args);
            if (id != null) {
                SqlGenerationUtilities.setObjectId(object, id);
            }
        }
        try {
            connection.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void insertAll(Object... objects) {
        insertAll(Arrays.asList(objects));
    }

    public void insert(Object object) {
        insertAll(Arrays.asList(object));
    }

    public void deleteAll(Iterable<Object> objects) {
        for(Object object : objects){
            List<Field> fields = Reflection.getAllFields(object);
            Map<String, Object> args = Reflection.fieldsToPropertyMap(object, fields);
            String sql = SqlGenerationUtilities.delete(object, fields);
            executeNonSelectSQL(sql, args);
        }
        try {
            connection.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteAll(Object... objects) {
        deleteAll(Arrays.asList(objects));
    }

    public void delete(Object object) {
        deleteAll(Arrays.asList(object));
    }

    public void createTable(Class aClass) {
        try {
            String sql = SqlGenerationUtilities.createTable(aClass);
            Statement statement = connection.createStatement();
            statement.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        connection.close();
    }
}
