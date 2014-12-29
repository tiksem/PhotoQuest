package com.tiksem.mysqljava;

import com.tiksem.mysqljava.annotations.*;
import com.tiksem.mysqljava.metadata.ColumnInfo;
import com.tiksem.mysqljava.metadata.ForeignKeyInfo;
import com.tiksem.mysqljava.metadata.IndexInfo;
import com.utils.framework.CollectionUtils;
import com.utils.framework.Predicate;
import com.utils.framework.Reflection;
import com.utils.framework.strings.Strings;
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
            updateTable(aClass);
        }
        try {
            connection.commit();
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

    private void alterColumnDefinition() {

    }

    public List<ColumnInfo> getTableColumns(Class table) {
        return executeSQLQuery("DESCRIBE " + table.getSimpleName(), ColumnInfo.class);
    }

    public List<IndexInfo> getTableKeys(Class table) {
        return executeSQLQuery("SHOW KEYS FROM " + table.getSimpleName(), IndexInfo.class);
    }

    public List<ForeignKeyInfo> getForeignKeys(Class table) {
        String sql = "select KEY_COLUMN_USAGE.*, REFERENTIAL_CONSTRAINTS.UPDATE_RULE, " +
                "REFERENTIAL_CONSTRAINTS.DELETE_RULE\n" +
                "from information_schema.KEY_COLUMN_USAGE, information_schema.REFERENTIAL_CONSTRAINTS\n" +
                "where KEY_COLUMN_USAGE.TABLE_SCHEMA = :databaseName and KEY_COLUMN_USAGE.table_name=:tableName\n" +
                "and REFERENTIAL_CONSTRAINTS.CONSTRAINT_SCHEMA = :databaseName and " +
                "REFERENTIAL_CONSTRAINTS.table_name = :tableName";
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("tableName", table.getSimpleName());
        args.put("databaseName", "photoquest");
        return executeSQLQuery(sql, args, ForeignKeyInfo.class);
    }

    private void updateColumns(Class aClass) {
        String tableName = aClass.getSimpleName();
        List<ColumnInfo> columns = getTableColumns(aClass);
        List<Field> fields = SqlGenerationUtilities.getFields(aClass);

        Iterator<ColumnInfo> columnIterator = columns.iterator();
        while (columnIterator.hasNext()) {
            final ColumnInfo column = columnIterator.next();
            Field field = CollectionUtils.find(fields, new Predicate<Field>() {
                @Override
                public boolean check(Field item) {
                    return item.getName().equalsIgnoreCase(column.getCOLUMN_NAME());
                }
            });

            if(field != null){
                SqlGenerationUtilities.ModifyInfo info =
                        SqlGenerationUtilities.modifyColumn(field, tableName);

                fields.remove(field);
                columnIterator.remove();

                if(!SqlGenerationUtilities.sqlTypeEquals(info.fieldType, column.getCOLUMN_TYPE())){
                    executeNonSelectSQL(info.sql);
                    continue;
                }

                boolean isNullable = column.getIS_NULLABLE().equals("YES");
                if(isNullable != info.isNullable){
                    executeNonSelectSQL(info.sql);
                    continue;
                }

                boolean hasAutoIncrement = column.getEXTRA().equals("auto_increment");
                if(hasAutoIncrement != info.autoIncrement){
                    executeNonSelectSQL(info.sql);
                    continue;
                }

                boolean isPrimaryKey = column.getCOLUMN_KEY().equals("PRI");
                boolean isUniqueKey = column.getCOLUMN_KEY().equals("UNI");
                if(isPrimaryKey != info.isPrimaryKey || isUniqueKey != info.isUniqueKey){
                    executeNonSelectSQL(info.sql);
                }
            }
        }

        for(ColumnInfo column : columns){
            String sql = "DROP COLUMN " + column.getCOLUMN_NAME();
            executeNonSelectSQL(sql);
        }

        for(Field field : fields){
            String sql = SqlGenerationUtilities.addColumn(field, tableName).sql;
            executeNonSelectSQL(sql);
        }
    }

    private void dropIndex(String indexName, String tableName) {
        String quotedTableName = Strings.quote(tableName, "`");
        executeNonSelectSQL("DROP INDEX " + indexName + " ON " + quotedTableName);
    }

    private void addIndex(IndexType indexType, String tableName, String fieldName) {
        String quotedTableName = Strings.quote(tableName, "`");
        String sql = "CREATE INDEX " + fieldName + "_index ON " + quotedTableName +
                "(" + fieldName + ") USING " + indexType;
        executeNonSelectSQL(sql);
    }

    private void dropAndAddIndex(IndexType indexType, String indexName, String tableName, String fieldName) {
        dropIndex(indexName, tableName);
        addIndex(indexType, tableName, fieldName);
    }

    private void updateIndexes(Class aClass) {
        String tableName = aClass.getSimpleName();
        List<IndexInfo> indexes = getTableKeys(aClass);
        List<Field> fields = SqlGenerationUtilities.getIndexedFields(aClass);

        Iterator<IndexInfo> indexIterator = indexes.iterator();
        while (indexIterator.hasNext()) {
            IndexInfo index = indexIterator.next();
            final String columnName = index.getCOLUMN_NAME();

            Field field = CollectionUtils.find(fields, new Predicate<Field>() {
                @Override
                public boolean check(Field item) {
                    return item.getName().equalsIgnoreCase(columnName);
                }
            });

            if(field != null){
                indexIterator.remove();
                fields.remove(field);

                IndexType indexType = SqlGenerationUtilities.getIndexType(field);
                if(!index.getINDEX_TYPE().equals(indexType.toString())){
                    dropAndAddIndex(indexType, index.getINDEX_NAME(), tableName, field.getName());
                }
            }
        }

        for(IndexInfo index : indexes){
            dropIndex(index.getINDEX_NAME(), tableName);
        }

        for(Field field : fields){
            IndexType indexType = SqlGenerationUtilities.getIndexType(field);
            addIndex(indexType, tableName, field.getName());
        }
    }

    private void dropForeignKey(String tableName, String constraintName) {
        String sql = "ALTER TABLE `" + tableName + "` DROP FOREIGN KEY " + constraintName;
    }

    private void addForeignKey(String tableName, Field field, ForeignKey foreignKey) {
        tableName = Strings.quote(tableName, "`");
        String sql = "ALTER TABLE " + tableName +
                " ADD FOREIGN KEY (`" + field.getName() + "`) REFERENCES " +
                foreignKey.parent().getSimpleName() + "(`" + foreignKey.field() + "`)" +
                " ON DELETE " + foreignKey.onDelete().toString().replace("_", " ") +
                " ON UPDATE " + foreignKey.onUpdate().toString().replace("_", " ");
        executeNonSelectSQL(sql);
    }

    private void updateForeignKeys(Class aClass) {
        String tableName = aClass.getSimpleName();
        List<ForeignKeyInfo> foreignKeys = getForeignKeys(aClass);
        List<Field> fields = Reflection.getFieldsWithAnnotations(aClass, ForeignKey.class);

        Iterator<ForeignKeyInfo> foreignKeyIterator = foreignKeys.iterator();
        while (foreignKeyIterator.hasNext()) {
            ForeignKeyInfo foreignKey = foreignKeyIterator.next();

            final String columnName = foreignKey.getCOLUMN_NAME();
            Field field = CollectionUtils.find(fields, new Predicate<Field>() {
                @Override
                public boolean check(Field item) {
                    return item.getName().equalsIgnoreCase(columnName);
                }
            });

            if (field != null) {
                foreignKeyIterator.remove();
                fields.remove(field);

                ForeignKey key = Reflection.getAnnotationOrThrow(field, ForeignKey.class);
                boolean sameUpdateRule = foreignKey.getUPDATE_RULE().replaceAll(" +", "_").
                        equals(key.onUpdate().toString());
                if(!sameUpdateRule){
                    dropForeignKey(tableName, foreignKey.getCONSTRAINT_NAME());
                    addForeignKey(tableName, field, key);
                    continue;
                }

                boolean sameDeleteRule = foreignKey.getDELETE_RULE().replaceAll(" +", "_").
                        equals(key.onDelete().toString());
                if(!sameDeleteRule){
                    dropForeignKey(tableName, foreignKey.getCONSTRAINT_NAME());
                    addForeignKey(tableName, field, key);
                    continue;
                }

                boolean sameParentTable = foreignKey.getREFERENCED_TABLE_NAME().
                        equalsIgnoreCase(key.parent().getSimpleName());
                if(!sameParentTable){
                    dropForeignKey(tableName, foreignKey.getCONSTRAINT_NAME());
                    addForeignKey(tableName, field, key);
                    continue;
                }

                boolean sameParentFieldName = foreignKey.getREFERENCED_COLUMN_NAME().
                        equalsIgnoreCase(key.field());
                if(!sameParentFieldName){
                    dropForeignKey(tableName, foreignKey.getCONSTRAINT_NAME());
                    addForeignKey(tableName, field, key);
                }
            }
        }

        for(ForeignKeyInfo key : foreignKeys){
            dropForeignKey(tableName, key.getCONSTRAINT_NAME());
        }

        for(Field field : fields){
            ForeignKey foreignKey = field.getAnnotation(ForeignKey.class);
            addForeignKey(tableName, field, foreignKey);
        }
    }

    public void updateTable(Class aClass) {
        updateColumns(aClass);
        updateIndexes(aClass);
        updateForeignKeys(aClass);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        connection.close();
    }
}
