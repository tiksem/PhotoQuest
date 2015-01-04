package com.tiksem.mysqljava;

import com.tiksem.mysqljava.annotations.*;
import com.tiksem.mysqljava.metadata.ColumnInfo;
import com.tiksem.mysqljava.metadata.ForeignKeyInfo;
import com.tiksem.mysqljava.metadata.IndexInfo;
import com.utils.framework.CollectionUtils;
import com.utils.framework.Equals;
import com.utils.framework.Predicate;
import com.utils.framework.Reflection;
import com.utils.framework.strings.Strings;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.*;

/**
 * Created by CM on 1/2/2015.
 */
public class MysqlTablesCreator {
    private MysqlObjectMapper mapper;
    private OutputStream progress;
    private String endOfLine = "\n";

    public MysqlTablesCreator(MysqlObjectMapper mapper) {
        this.mapper = mapper;
    }

    private void writeProgress(String value) {
        if(progress != null){
            try {
                IOUtils.write(value + endOfLine, progress);
            } catch (IOException e) {
                throw new RuntimeException("Writing Progress failed");
            }
        }
    }

    public void updateAndCreateTables(String packageName, OutputStream progress, String endOfLine) {
        this.progress = progress;
        this.endOfLine = endOfLine;

        List<Class<?>> classesInPackage = Reflection.findClassesInPackage(packageName);
        classesInPackage = CollectionUtils.findAll(classesInPackage, new Predicate<Class<?>>() {
            @Override
            public boolean check(Class<?> item) {
                return item.getAnnotation(Table.class) != null;
            }
        });

        writeProgress("Creating tables...");
        for(Class aClass : classesInPackage){
            createTable(aClass);
            writeProgress(aClass.getSimpleName());
        }

        writeProgress("Updating columns...");
        for(Class aClass : classesInPackage){
            updateColumns(aClass);
            writeProgress(aClass.getSimpleName());
        }

        writeProgress("Updating indexes...");
        for(Class aClass : classesInPackage){
            updateIndexes(aClass);
            writeProgress(aClass.getSimpleName());
        }

        writeProgress("Updating foreign keys...");
        for(Class aClass : classesInPackage){
            updateForeignKeys(aClass);
            writeProgress(aClass.getSimpleName());
        }
        writeProgress("Success!");
    }

    public void createTable(Class aClass) {
        String sql = SqlGenerationUtilities.createTable(aClass);
        mapper.executeNonSelectSQL(sql);
    }

    public List<ColumnInfo> getTableColumns(String table) {
        String tableName = Strings.quote(table, "`");
        return mapper.executeSQLQuery("DESCRIBE " + tableName, ColumnInfo.class);
    }

    public List<IndexInfo> getTableKeys(String table) {
        String tableName = Strings.quote(table, "`");
        return mapper.executeSQLQuery("SHOW KEYS FROM " + tableName, IndexInfo.class);
    }

    public List<String> getTableNames() {
        String sql = "SHOW TABLES";
        ResultSet resultSet = mapper.executeSelectSqlGetResultSet(sql);
        return ResultSetUtilities.getValuesOfColumn(resultSet, "TABLE_NAME");
    }

    public void dropTables() {
        mapper.executeNonSelectSQL("drop database photoquest");
        mapper.executeNonSelectSQL("create database photoquest");
    }

    public void clearDatabase() {
        for(String tableName : getTableNames()){
            String sql = "DELETE FROM `" + tableName + "`";
            mapper.executeNonSelectSQL(sql);
        }
    }

    public List<ForeignKeyInfo> getForeignKeys(String tableName) {
        String sql = "select KEY_COLUMN_USAGE.*, REFERENTIAL_CONSTRAINTS.UPDATE_RULE, " +
                "REFERENTIAL_CONSTRAINTS.DELETE_RULE\n" +
                "from information_schema.KEY_COLUMN_USAGE, information_schema.REFERENTIAL_CONSTRAINTS\n" +
                "where KEY_COLUMN_USAGE.TABLE_SCHEMA = :databaseName and KEY_COLUMN_USAGE.table_name=:tableName\n" +
                "and REFERENTIAL_CONSTRAINTS.CONSTRAINT_SCHEMA = :databaseName and " +
                "REFERENTIAL_CONSTRAINTS.table_name = :tableName AND " +
                "KEY_COLUMN_USAGE.REFERENCED_TABLE_NAME is not null";
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("tableName", tableName);
        args.put("databaseName", "photoquest");
        List<ForeignKeyInfo> foreignKeys = mapper.executeSQLQuery(sql, args, ForeignKeyInfo.class);
        return CollectionUtils.unique(foreignKeys,
                new CollectionUtils.KeyProvider<Object, ForeignKeyInfo>() {
            @Override
            public Object getKey(ForeignKeyInfo value) {
                return value.getCONSTRAINT_NAME();
            }
        });
    }

    private void updateColumns(Class aClass) {
        String tableName = aClass.getSimpleName();
        List<ColumnInfo> columns = getTableColumns(tableName);
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
                    mapper.executeNonSelectSQL(info.sql);
                    continue;
                }

                boolean isNullable = column.getIS_NULLABLE().equals("YES");
                if(isNullable != info.isNullable){
                    mapper.executeNonSelectSQL(info.sql);
                    continue;
                }

                boolean hasAutoIncrement = column.getEXTRA().equals("auto_increment");
                if(hasAutoIncrement != info.autoIncrement){
                    mapper.executeNonSelectSQL(info.sql);
                    continue;
                }

                boolean isPrimaryKey = column.getCOLUMN_KEY().equals("PRI");
                boolean isUniqueKey = column.getCOLUMN_KEY().equals("UNI");
                if(isPrimaryKey != info.isPrimaryKey || isUniqueKey != info.isUniqueKey){
                    mapper.executeNonSelectSQL(info.sql);
                }
            }
        }

        for(ColumnInfo column : columns){
            dropColumn(column.getCOLUMN_NAME(), tableName);
        }

        for(Field field : fields){
            addColumn(field, tableName);
        }
    }

    private void addColumn(Field field, String tableName) {
        String sql = SqlGenerationUtilities.addColumn(field, tableName).sql;
        mapper.executeNonSelectSQL(sql);
    }

    private void dropColumn(String columnName, String tableName) {
        if (getColumn(tableName, columnName) != null) {
            String sql = "ALTER TABLE " + tableName + " DROP COLUMN " + columnName;
            mapper.executeNonSelectSQL(sql);
        }
    }

    private void modifyColumn(Field field, String tableName) {
        String sql = SqlGenerationUtilities.modifyColumn(field, tableName).sql;
        mapper.executeNonSelectSQL(sql);
    }

    private ColumnInfo getColumn(String tableName, final String fieldName) {
        return CollectionUtils.find(getTableColumns(tableName), new Predicate<ColumnInfo>() {
            @Override
            public boolean check(ColumnInfo item) {
                return item.getCOLUMN_NAME().equalsIgnoreCase(fieldName);
            }
        });
    }

    private void addOrModifyColumn(final Field field, String tableName) {
        final String fieldName = field.getName();

        boolean exists = getColumn(tableName, fieldName) != null;

        if(exists){
            modifyColumn(field, tableName);
        } else {
            addColumn(field, tableName);
        }
    }

    private void dropIndex(final String indexName, String tableName) {
        IndexInfo index = CollectionUtils.find(getTableKeys(tableName), new Predicate<IndexInfo>() {
            @Override
            public boolean check(IndexInfo item) {
                return item.getINDEX_NAME().equalsIgnoreCase(indexName);
            }
        });

        if (index != null) {
            String quotedTableName = Strings.quote(tableName, "`");
            mapper.executeNonSelectSQL("DROP INDEX " + indexName + " ON " + quotedTableName);
        }
    }

    private void addIndex(String tableName, MultipleIndex multipleIndex) {
        if(multipleIndex.fields().length < 2){
            throw new IllegalArgumentException("multipleIndex.fields().length < 2");
        }

        addIndex(multipleIndex.indexType(), tableName, Arrays.asList(multipleIndex.fields()), multipleIndex.isUnique());
    }

    private String getIndexName(List<String> fieldNames) {
        return Strings.join("_", fieldNames) + "_index";
    }

    private void addIndex(IndexType indexType, String tableName, List<String> fieldNames, boolean unique) {
        String quotedTableName = Strings.quote(tableName, "`");
        String sql;
        if (indexType != IndexType.FULLTEXT) {
            sql = unique ? "CREATE UNIQUE INDEX " : "CREATE INDEX ";
        } else {
            sql = "CREATE FULLTEXT INDEX ";
        }

        sql += getIndexName(fieldNames) + " ON " + quotedTableName +
                "(" + Strings.join(", ", fieldNames) + ")";
        if(indexType != IndexType.FULLTEXT){
            sql += " USING " + indexType;
        }

        mapper.executeNonSelectSQL(sql);
    }

    private void addIndex(IndexType indexType, String tableName, String fieldName, boolean unique) {
        addIndex(indexType, tableName, Collections.singletonList(fieldName), unique);
    }

    private void dropAndAddIndex(IndexType indexType, String indexName,
                                 String tableName, String fieldName, boolean unique) {
        dropIndex(indexName, tableName);
        addIndex(indexType, tableName, fieldName, unique);
    }

    private void updateMultipleIndexes(Class aClass, List<IndexInfo> multipleIndexes) {
        String tableName = aClass.getSimpleName();

        Map<String, IndexInfo> indexesMap =
                CollectionUtils.mapFromList(multipleIndexes,
                        new CollectionUtils.KeyProvider<String, IndexInfo>() {
                            @Override
                            public String getKey(IndexInfo value) {
                                return value.getINDEX_NAME();
                            }
                        });

        Map<String, MultipleIndex> indexesClassMap = new HashMap<String, MultipleIndex>();
        for(Annotation annotation : aClass.getAnnotations()){
            if(annotation instanceof MultipleIndex){
                MultipleIndex multipleIndex = (MultipleIndex)annotation;
                List<String> fieldNames = Arrays.asList(multipleIndex.fields());
                indexesClassMap.put(getIndexName(fieldNames), multipleIndex);
                continue;
            }

            if(annotation instanceof MultipleIndexes){
                MultipleIndexes indexes = (MultipleIndexes)annotation;
                for(MultipleIndex multipleIndex : indexes.indexes()){
                    List<String> fieldNames = Arrays.asList(multipleIndex.fields());
                    indexesClassMap.put(getIndexName(fieldNames), multipleIndex);
                }
                continue;
            }
        }

        Set<String> indexesMapKeys = indexesMap.keySet();
        Iterator<String> indexesMapKeysIterator = indexesMapKeys.iterator();
        while (indexesMapKeysIterator.hasNext()) {
            String indexName = indexesMapKeysIterator.next();
            IndexInfo index = indexesMap.get(indexName);
            MultipleIndex multipleIndex = indexesClassMap.get(indexName);
            if(multipleIndex == null){
                continue;
            }

            indexesMapKeysIterator.remove();
            indexesClassMap.remove(indexName);

            boolean sameIndexType = index.getINDEX_TYPE().equalsIgnoreCase(multipleIndex.indexType().toString());
            if(!sameIndexType){
                dropIndex(indexName, tableName);
                addIndex(tableName, multipleIndex);
                continue;
            }

            boolean indexUnique = index.getNON_UNIQUE().equals("0");
            if(indexUnique != multipleIndex.isUnique()){
                dropIndex(indexName, tableName);
                addIndex(tableName, multipleIndex);
            }
        }

        for(String indexName : indexesMap.keySet()){
            dropIndex(indexName, tableName);
        }

        for(MultipleIndex multipleIndex : indexesClassMap.values()){
            addIndex(tableName, multipleIndex);
        }
    }

    private static class UpdateIndexInfo {
        public boolean isUnique;
        public IndexType indexType;
    }

    private boolean shouldUpdateIndex(Field field, IndexInfo index, UpdateIndexInfo out) {
        out.isUnique = Reflection.hasOneOrMoreAnnotations(field, Unique.class, PrimaryKey.class);
        if(!out.isUnique){
            ForeignKey foreignKey = field.getAnnotation(ForeignKey.class);
            if(foreignKey != null){
                out.isUnique = foreignKey.unique();
            }
        }

        boolean currentIsUnique = index.getNON_UNIQUE().equals("0");
        out.indexType = SqlGenerationUtilities.getIndexType(field);

        if(out.isUnique != currentIsUnique){
            return true;
        }

        if(!index.getINDEX_TYPE().equals(out.indexType.toString())){
            return true;
        }

        return false;
    }

    private void updateIndexIfRequired(Field field, IndexInfo index, String tableName) {
        UpdateIndexInfo info = new UpdateIndexInfo();
        if(shouldUpdateIndex(field, index, info)){
            dropAndAddIndex(info.indexType, index.getINDEX_NAME(), tableName, field.getName(), info.isUnique);
        }
    }

    private void updateIndexes(Class aClass) {
        String tableName = aClass.getSimpleName();
        List<IndexInfo> indexes = getTableKeys(tableName);
        final List<ForeignKeyInfo> foreignKeys = getForeignKeys(tableName);

        List<IndexInfo> multipleIndexes = CollectionUtils.getRemovedItems(indexes, new Predicate<IndexInfo>() {
            @Override
            public boolean check(IndexInfo item) {
                String result = item.getINDEX_NAME().toLowerCase().replace(item.getCOLUMN_NAME().toLowerCase(), "");
                return !result.equals("_index") && !result.equals("primary");
            }
        });

        CollectionUtils.removeAll(indexes, new Predicate<IndexInfo>() {
            @Override
            public boolean check(final IndexInfo item) {
                return CollectionUtils.find(foreignKeys, new Predicate<ForeignKeyInfo>() {
                    @Override
                    public boolean check(ForeignKeyInfo keyInfo) {
                        return item.getCOLUMN_NAME().equalsIgnoreCase(keyInfo.getCOLUMN_NAME());
                    }
                }) != null;
            }
        });

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

                updateIndexIfRequired(field, index, tableName);
            }
        }

        for(final IndexInfo index : indexes){
            Field field = CollectionUtils.find(Reflection.getAllFieldsOfClass(aClass),
                    new Predicate<Field>() {
                @Override
                public boolean check(Field item) {
                    return item.getName().equalsIgnoreCase(index.getCOLUMN_NAME());
                }
            });
            if(field != null && field.getAnnotation(ForeignKey.class) != null){
                continue;
            }

            dropIndex(index.getINDEX_NAME(), tableName);
        }

        for(Field field : fields){
            IndexType indexType = SqlGenerationUtilities.getIndexType(field);
            boolean isUnique = Reflection.hasOneOrMoreAnnotations(field, Unique.class, PrimaryKey.class);
            addIndex(indexType, tableName, field.getName(), isUnique);
        }

        updateMultipleIndexes(aClass, multipleIndexes);
    }

    private void dropForeignKey(String tableName, String constraintName) {
        if (getForeignKey(tableName, constraintName) != null) {
            String sql = "ALTER TABLE `" + tableName + "` DROP FOREIGN KEY " + constraintName;
            mapper.executeNonSelectSQL(sql);
        }
    }

    private ForeignKeyInfo getForeignKey(String tableName, final String constraintName) {
        return CollectionUtils.find(getForeignKeys(tableName), new Predicate<ForeignKeyInfo>() {
            @Override
            public boolean check(ForeignKeyInfo item) {
                return item.getCONSTRAINT_NAME().equals(constraintName);
            }
        });
    }

    private IndexInfo getIndex(String tableName, final String fieldName) {
        return CollectionUtils.find(getTableKeys(tableName), new Predicate<IndexInfo>() {
            @Override
            public boolean check(IndexInfo item) {
                return item.getINDEX_NAME().equalsIgnoreCase(fieldName + "_index");
            }
        });
    }

    private void addForeignKey(String tableName, final Field field, ForeignKey foreignKey) {
        final String fieldName = field.getName();
        addOrModifyColumn(field, tableName);

        IndexInfo index = getIndex(tableName, fieldName);

        if(index == null){
            addIndex(foreignKey.indexType(), tableName, field.getName(), foreignKey.unique());
        } else {
            updateIndexIfRequired(field, index, tableName);
        }

        tableName = Strings.quote(tableName, "`");
        String sql = "ALTER TABLE " + tableName +
                " ADD FOREIGN KEY (`" + field.getName() + "`) REFERENCES `" +
                foreignKey.parent().getSimpleName() + "`(`" + foreignKey.field() + "`)" +
                " ON DELETE " + foreignKey.onDelete().toString().replace("_", " ") +
                " ON UPDATE " + foreignKey.onUpdate().toString().replace("_", " ");
        mapper.executeNonSelectSQL(sql);
    }

    private void updateForeignKeys(Class aClass) {
        String tableName = aClass.getSimpleName();
        List<ForeignKeyInfo> foreignKeys = getForeignKeys(tableName);
        List<Field> fields = Reflection.getFieldsWithAnnotations(aClass, ForeignKey.class);

        Iterator<ForeignKeyInfo> foreignKeyIterator = foreignKeys.iterator();
        while (foreignKeyIterator.hasNext()) {
            ForeignKeyInfo foreignKey = foreignKeyIterator.next();

            final String columnName = foreignKey.getCOLUMN_NAME();
            final Field field = CollectionUtils.find(fields, new Predicate<Field>() {
                @Override
                public boolean check(Field item) {
                    return item.getName().equalsIgnoreCase(columnName);
                }
            });

            if (field != null) {
                foreignKeyIterator.remove();
                fields.remove(field);

                addOrModifyColumn(field, tableName);

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
                    continue;
                }

                IndexInfo index = getIndex(tableName, field.getName());

                if(index == null){
                    dropForeignKey(tableName, foreignKey.getCONSTRAINT_NAME());
                    addForeignKey(tableName, field, key);
                    continue;
                }

                UpdateIndexInfo info = new UpdateIndexInfo();
                if(shouldUpdateIndex(field, index, info)){
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
}