package com.tiksem.mysqljava.help;

import com.tiksem.mysqljava.OffsetLimit;
import com.tiksem.mysqljava.SelectParams;
import com.tiksem.mysqljava.annotations.*;
import com.utils.framework.CollectionUtils;
import com.utils.framework.Reflection;
import com.utils.framework.strings.Strings;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by CM on 12/27/2014.
 */
public class SqlGenerationUtilities {
    public static boolean isSupportedPrimitive(Class aClass) {
        if(aClass == Long.class){
            return true;
        } else if(aClass == Integer.class) {
            return true;
        } else if(aClass == Short.class) {
            return true;
        } else if(aClass == Byte.class) {
            return true;
        } else if(aClass == String.class) {
            return true;
        } else if(aClass == Float.class) {
            return true;
        } else if(aClass == Double.class) {
            return true;
        } else if(aClass == Boolean.class) {
            return true;
        }

        return false;
    }

    public static int getDefaultTypeLength(String sqlType) {
        if(sqlType.equalsIgnoreCase("BIGINT")){
            return 20;
        } else if(sqlType.equalsIgnoreCase("INT")) {
            return 10;
        } else if(sqlType.equalsIgnoreCase("MEDIUMINT")) {
            return 8;
        } else if(sqlType.equalsIgnoreCase("SMALLINT")) {
            return 5;
        } else if(sqlType.equalsIgnoreCase("SMALLINT")) {
            return 3;
        } else if(sqlType.equalsIgnoreCase("BIT")) {
            return 1;
        } else if(sqlType.equalsIgnoreCase("CHAR")) {
            return 255;
        } else if(sqlType.equalsIgnoreCase("VARCHAR")) {
            return 255;
        }

        return -1;
    }

    public static boolean sqlTypeEquals(String a, String b) {
        if(a.equalsIgnoreCase(b)){
            return true;
        }

        String pattern = " *\\(\\d+\\)";
        String aType = a.replaceAll(pattern, "");
        String bType = b.replaceAll(pattern, "");
        if(!aType.equalsIgnoreCase(bType)){
            return false;
        }

        int aDigit = Strings.findUnsignedIntegerInString(a);
        int bDigit = Strings.findUnsignedIntegerInString(b);

        if(aDigit < 0) {
            aDigit = getDefaultTypeLength(aType);
        }

        if(bDigit < 0) {
            bDigit = getDefaultTypeLength(bType);
        }

        return aDigit == bDigit;
    }

    public static String getSqlType(Field field) {
        String suggestedType = null;
        Stored stored = field.getAnnotation(Stored.class);
        if(stored != null){
            suggestedType = stored.type();
        } else {
            PrimaryKey primaryKey = field.getAnnotation(PrimaryKey.class);
            if(primaryKey != null){
                suggestedType = primaryKey.type();
            } else {
                Unique unique = field.getAnnotation(Unique.class);
                if(unique != null){
                    suggestedType = unique.type();
                } else {
                    Index index = field.getAnnotation(Index.class);
                    if(index != null){
                        suggestedType = index.type();
                    } else {
                        Serialized serialized = field.getAnnotation(Serialized.class);
                        if(serialized != null){
                            return "BLOB";
                        } else {
                            ForeignKey foreignKey = field.getAnnotation(ForeignKey.class);
                            if(foreignKey != null){
                                Field parentField = Reflection.getFieldByNameOrThrow(foreignKey.parent(),
                                        foreignKey.field());
                                return getSqlType(parentField);
                            }
                        }
                    }
                }
            }
        }

        return getSqlType(field, suggestedType);
    }

    private static String getSqlType(Field field, String suggestedType) {
        String result = null;
        if(Strings.isEmpty(suggestedType)){
            Class<?> type = field.getType();

            if(type == Long.class){
                result = "BIGINT";
            } else if(type == Integer.class) {
                result = "INT";
            } else if(type == Short.class) {
                result = "SMALLINT";
            } else if(type == Byte.class) {
                result = "TINYINT";
            } else if(type == String.class) {
                result = "VARCHAR";
            } else if(type == Float.class) {
                result = "FLOAT";
            } else if(type == Double.class) {
                result = "DOUBLE";
            } else if(type == Boolean.class) {
                result = "BIT";
            }

            if (result == null) {
                throw new IllegalArgumentException("Unsupported type " + type.getSimpleName());
            }

        } else {
            result = suggestedType;
        }

        if (!result.equals("BLOB")) {
            int digit = Strings.findUnsignedIntegerInString(result);
            if(digit < 0){
                digit = getDefaultTypeLength(result);
                result += "(" + digit + ")";
            }
        }

        return result;
    }

    public static boolean isInt(Field field) {
        Class<?> type = field.getType();
        return type == Long.class || type == Integer.class || type == Short.class || type == Byte.class;
    }

    private static String getColumnDefinition(Field field,
                                       String suggestedType,
                                       boolean autoincrement) {
        StringBuilder result = new StringBuilder();
        result.append('`').append(field.getName()).append('`');
        String type = getSqlType(field, suggestedType);
        result.append(" ").append(type);
        autoincrement = autoincrement && isInt(field);

        if(field.getAnnotation(NotNull.class) != null){
            result.append(" NOT NULL");
        }

        if(autoincrement){
            result.append(" AUTO_INCREMENT");
        }

        return result.toString();
    }

    private static String getIndexDefinition(String fieldName, IndexType indexType) {
        return "INDEX " + fieldName + "_index USING " + indexType
                + " (" + fieldName + ")";
    }

    public static String createTable(Class aClass) {
        StringBuilder sql = new StringBuilder();
        sql.append("create table if not exists `").append(aClass.getSimpleName()).append("` (");
        List<Field> fields = Reflection.getAllFieldsOfClass(aClass);

        List<String> parts = new ArrayList<String>();
        for(Field field : fields){
            Stored stored = field.getAnnotation(Stored.class);
            if(stored != null){
                parts.add(getColumnDefinition(field, stored.type(), false));
                continue;
            }

            Serialized serialized = field.getAnnotation(Serialized.class);
            if(serialized != null){
                parts.add(getColumnDefinition(field, "BLOB", false));
                continue;
            }

            PrimaryKey primaryKey = field.getAnnotation(PrimaryKey.class);
            if(primaryKey != null){
                String columnDefinition = getColumnDefinition(field, primaryKey.type(),
                        primaryKey.autoincrement());
                columnDefinition += " PRIMARY KEY";

                parts.add(columnDefinition);
                continue;
            }

            Unique unique = field.getAnnotation(Unique.class);
            if(unique != null){
                String columnDefinition = getColumnDefinition(field, unique.type(), false);
                parts.add(columnDefinition);
                continue;
            }

            Index index = field.getAnnotation(Index.class);
            if(index != null){
                String columnDefinition = getColumnDefinition(field, index.type(), false);
                parts.add(columnDefinition);
                continue;
            }
        }

        Strings.join(",\n", parts, sql);
        sql.append(")");
        return sql.toString();
    }

    public static List<Field> getFields(Object object) {
        return getFields(object.getClass());
    }

    public static List<Field> getFields(Class aClass) {
        return Reflection.getFieldsWithAnnotations(aClass,
                Index.class, Stored.class, PrimaryKey.class,
                ForeignKey.class, Unique.class, Serialized.class);
    }

    public static IndexType getIndexType(Field field) {
        Index index = field.getAnnotation(Index.class);
        if(index != null){
            return index.indexType();
        }

        PrimaryKey primaryKey = field.getAnnotation(PrimaryKey.class);
        if(primaryKey != null){
            return primaryKey.indexType();
        }

        Unique unique = field.getAnnotation(Unique.class);
        if(unique != null){
            return unique.indexType();
        }

        ForeignKey foreignKey = field.getAnnotation(ForeignKey.class);
        if(foreignKey != null){
            return foreignKey.indexType();
        }

        throw new IllegalArgumentException("Field " + field.getName() + " should have " +
                "\nIndex.class, PrimaryKey.class,\n" +
                "ForeignKey.class, Unique.class annotation");
    }

    public static List<Field> getIndexedFields(Class aClass) {
        return Reflection.getFieldsWithAnnotations(aClass,
                Index.class, PrimaryKey.class, Unique.class);
    }

    public static List<Field> getFieldsExcludingPrimaryKey(Class aClass) {
        return Reflection.getFieldsWithAnnotations(aClass,
                Index.class, Stored.class, Unique.class, ForeignKey.class, Serialized.class);
    }

    public static List<MultipleIndex> getUniqueMultiIndexes(Class<?> aClass) {
        List<MultipleIndex> result = new ArrayList<MultipleIndex>();

        MultipleIndex multipleIndex = aClass.getAnnotation(MultipleIndex.class);
        if(multipleIndex != null && multipleIndex.isUnique()){
            result.add(multipleIndex);
        }

        MultipleIndexes multipleIndexes = aClass.getAnnotation(MultipleIndexes.class);
        if(multipleIndexes != null){
            MultipleIndex[] indexes = multipleIndexes.indexes();
            for(MultipleIndex index : indexes){
                if(index.isUnique()){
                    result.add(index);
                }
            }
        }

        return result;
    }

    public static String generatePatternWhereClosure(Object pattern, List<Field> fields) {
        Class aClass = pattern.getClass();
        List<String> parts = new ArrayList<String>();
        for(Field field : fields){
            Object value = Reflection.getFieldValueUsingGetter(pattern, field);
            if (value != null) {
                String fieldName = field.getName();
                String part;
                if(aClass == null){
                    part = "`" + fieldName + "` = :" + fieldName;
                } else {
                    part = quotedClassName(aClass) + "." + fieldName + " = :" + fieldName;
                }

                parts.add(part);
            }
        }

        return Strings.join(" AND ", parts).toString();
    }

    public static String generatePatternWhereClosure(Object pattern) {
        List<Field> fields = getFields(pattern);
        return generatePatternWhereClosure(pattern, fields);
    }

    public static String insert(final Object object, List<Field> fields, boolean replace) {
        List<String> fieldNames = CollectionUtils.transform(fields,
                new CollectionUtils.Transformer<Field, String>() {
            @Override
            public String get(Field field) {
                if (!Reflection.isNull(object, field)) {
                    return "`" + field.getName() + "`";
                }

                return null;
            }
        });

        List<String> values = CollectionUtils.transform(fields,
                new CollectionUtils.Transformer<Field, String>() {
            @Override
            public String get(Field field) {
                if (!Reflection.isNull(object, field)) {
                    return ":" + field.getName();
                }

                return null;
            }
        });

        String result = replace ? "REPLACE" : "INSERT";
        result += " INTO `" + object.getClass().getSimpleName() +
                "` (" + Strings.join(", ", fieldNames) + ")\nVALUES (" +
                Strings.join(", ", values)
                + ")";
        return result;
    }

    public static class Foreign {
        public Class childClass;
        public Field foreignField;
    }

    static String quotedClassName(Class aClass) {
        return Strings.quote(aClass.getSimpleName(), "`");
    }

    public static String selectAll(Class resultClass,
                                   List<Foreign> foreigns,
                                   SelectParams selectParams) {
        return select(Collections.singletonList(resultClass), foreigns, null, selectParams);
    }

    public static String select(Object pattern,
                                List<Foreign> foreigns,
                                SelectParams selectParams) {
        return select(Arrays.<Class>asList(pattern.getClass()), foreigns, pattern, selectParams);
    }

    public static String getPosition(Object pattern, Class aClass, String orderBy, List<Field> orderByFieldsOut) {
        String tableName = quotedClassName(aClass);
        String sql = "SELECT count(*) FROM " + tableName;
        String where = null;
        if (pattern != null) {
            where = generatePatternWhereClosure(pattern);
        }

        PositionOrderByGenerator orderByGenerator = new PositionOrderByGenerator(orderBy, aClass, orderByFieldsOut);
        String orderingWhere = orderByGenerator.generateWhere();

        sql += " WHERE (" + orderingWhere + ")";
        if(!Strings.isEmpty(where)){
            sql += " AND (" + where + ")";
        }

        sql += " ORDER BY " + orderBy;

        return sql;
    }

    public static String reverseOrderBy(String orderBy) {
        orderBy = orderBy.replace("desc", "ASC");
        orderBy = orderBy.replace("asc", "DESC");
        return orderBy.toLowerCase();
    }

    public static String nextPrev(Object pattern, String orderBy, List<Foreign> foreigns,
                                  boolean next, List<Field> orderByFieldsOut) {


        SelectParams params = new SelectParams();
        PositionOrderByGenerator orderByGenerator = new PositionOrderByGenerator(orderBy, pattern.getClass(),
                orderByFieldsOut);
        orderByGenerator.setReverse(next);
        params.additionalWhereClosure = orderByGenerator.generateWhere();
        params.offsetLimit = new OffsetLimit(0, 1);

        if(!next){
            orderBy = reverseOrderBy(orderBy);
        }

        params.ordering = orderBy;
        return select(pattern, foreigns, params);
    }

    public static String select(List<Class> fromClasses,
                                List<Foreign> foreigns,
                                Object pattern,
                                SelectParams selectParams) {
        return select(fromClasses, null, foreigns, pattern, selectParams);
    }

    public static String select(List<Class> fromClasses,
                                List<String> resultClasses,
                                List<Foreign> foreigns,
                                Object pattern,
                                SelectParams selectParams
                                ) {
        if(fromClasses.isEmpty()){
            throw new IllegalArgumentException("fromClasses are empty");
        }

        List<String> foreignJoinParts = new ArrayList<String>();

        Class orderByClass = null;
        if (selectParams.ordering != null) {
            orderByClass = pattern != null ? pattern.getClass() : fromClasses.get(0);
        }

        for(Foreign foreign : foreigns){
            ForeignKey foreignKey = Reflection.getAnnotationOrThrow(foreign.foreignField, ForeignKey.class);
            String parentFieldName = foreignKey.field();
            Class parent = foreignKey.parent();

            String parentName = parent.getSimpleName();

            String foreignFieldName = foreign.foreignField.getName();
            String parentAlias = parentName + "_" +
                    foreignFieldName;

            String part = "LEFT JOIN " + quotedClassName(parent) + " AS " + parentAlias + " ON " +
                    quotedClassName(foreign.childClass) + "." + foreignFieldName + "=" + parentAlias +
                    "." + parentFieldName;
            foreignJoinParts.add(part);
        }

        String from = Strings.join(", ", CollectionUtils.transform(fromClasses,
                new CollectionUtils.Transformer<Class, CharSequence>() {
            @Override
            public CharSequence get(Class aClass) {
                return quotedClassName(aClass);
            }
        })).toString();

        String where = "";
        if (pattern != null) {
            where = generatePatternWhereClosure(pattern, getFields(pattern));
        }

        if(selectParams.additionalWhereClosure != null){
            if(!Strings.isEmpty(where)){
                where = "(" + where + ") AND (" + selectParams.additionalWhereClosure + ")";
            } else {
                where = selectParams.additionalWhereClosure;
            }
        }

        if(selectParams.whereTransformer != null){
            where = selectParams.whereTransformer.get(where);
        }

        String result;
        if(resultClasses == null || resultClasses.isEmpty()){
            result = "*";
        } else {
            result = Strings.join(", ", CollectionUtils.transform(resultClasses,
                    new CollectionUtils.Transformer<String, CharSequence>() {
                @Override
                public CharSequence get(String aClass) {
                    return "`" + aClass + "`.*";
                }
            })).toString();
        }

        String query = "SELECT " + result + " FROM " + from;
        query += " " + Strings.join(" ", foreignJoinParts) + " ";

        if(!Strings.isEmpty(where)){
            query += " WHERE " + where;
        }

        if(selectParams.ordering != null){
            String[] orderCriteria = selectParams.ordering.split(", *");
            List<String> orderByParts = new ArrayList<String>();

            for(String order : orderCriteria){
                if(order.contains(".")){
                    orderByParts.add(order);
                    continue;
                }

                String[] args = order.split(" +", 2);
                String param = args[0];
                String part = quotedClassName(orderByClass) + "." + param;
                if(args.length > 1){
                    part += " " + args[1];
                }
                orderByParts.add(part);
            }

            query += " ORDER BY " + Strings.join(", ", orderByParts);
        }

        if (selectParams.offsetLimit != null) {
            query += " LIMIT " + selectParams.offsetLimit.getOffset() + ", " +
                    selectParams.offsetLimit.getLimit();
        }

        return query;
    }

    public static Field getPrimaryKey(Object object) {
        return Reflection.getFieldWithAnnotation(object.getClass(), PrimaryKey.class);
    }

    public static Field getPrimaryKey(Class aClass) {
        return Reflection.getFieldWithAnnotation(aClass, PrimaryKey.class);
    }

    public static void setObjectId(Object object, Object id) {
        Field field = getPrimaryKey(object);
        Reflection.setFieldValueUsingSetter(object, field, id);
    }

    public static String update(Object pattern, List<Field> patternFields, Object values, List<Field> valueFields) {
        String where = generatePatternWhereClosure(pattern, patternFields);
        String sql = "UPDATE " + quotedClassName(pattern.getClass());
        List<String> setParts = new ArrayList<String>();
        for(Field field : valueFields){
            Object value = Reflection.getFieldValueUsingGetter(values, field);
            if(value != null){
                String fieldName = field.getName();
                setParts.add("`" + fieldName + "` = :" + fieldName + "_update");
            }
        }

        if(setParts.isEmpty()){
            throw new IllegalArgumentException("Nothing to update, all fields are null");
        }

        sql += " SET " + Strings.join(", ", setParts);

        if(!Strings.isEmpty(where)){
            sql += " WHERE " + where;
        }

        return sql;
    }

    public static String update(Object pattern, Object values) {
        List<Field> patternFields = getFields(pattern);
        List<Field> valueFields = getFields(values);
        return update(pattern, patternFields, values, valueFields);
    }

    public static String delete(Object object,
                                List<Field> fields) {
        return delete(object, fields, null);
    }

    public static String changeValue(Object object,
                                     List<Field> fields,
                                     String fieldToChange,
                                     long value) {
        Field primaryKey = Reflection.getFieldWithAnnotation(fields, PrimaryKey.class);
        Object id = null;
        if (primaryKey != null) {
            id = Reflection.getFieldValueUsingGetter(object, primaryKey);
        }
        String where = null;

        if(id != null){
            String primaryKeyName = primaryKey.getName();
            if(primaryKeyName.equals(fieldToChange)){
                throw new IllegalArgumentException("fieldToChange should not be " +
                        "PrimaryKey");
            }

            where = "`" + primaryKeyName + "` = :" + primaryKeyName;
        } else {
            where = generatePatternWhereClosure(object, fields);
        }

        String result = "UPDATE " + quotedClassName(object.getClass());
        fieldToChange = Strings.quote(fieldToChange, "`");
        result += " SET " + fieldToChange + " = " + fieldToChange + " + " + value;

        if(!Strings.isEmpty(where)){
            result += " WHERE " + where;
        }

        return result;
    }

    public static String delete(Object object,
                                List<Field> fields,
                              CollectionUtils.Transformer<String, String>
                              whereTransformer) {
        Field primaryKey = Reflection.getFieldWithAnnotation(fields, PrimaryKey.class);
        Object id = null;
        if (primaryKey != null) {
            id = Reflection.getFieldValueUsingGetter(object, primaryKey);
        }
        String where = null;

        if(id != null){
            String primaryKeyName = primaryKey.getName();
            where = "`" + primaryKeyName + "` = :" + primaryKeyName;
        } else {
            where = generatePatternWhereClosure(object, fields);
            if(whereTransformer != null){
                where = whereTransformer.get(where);
            }
        }

        String result = "DELETE FROM " + quotedClassName(object.getClass());
        if(!Strings.isEmpty(where)){
            result += " WHERE " + where;
        }

        return result;
    }

    public static class ModifyInfo {
        public String sql;
        public boolean isNullable;
        public boolean autoIncrement;
        public String fieldType;
        public boolean isPrimaryKey;
        public boolean isUniqueKey;
    }

    public static ModifyInfo addColumn(Field field, String tableName) {
        return alterColumn(field, tableName, "ADD");
    }

    public static ModifyInfo alterColumn(Field field, String tableName, String action) {
        String alter = "ALTER TABLE `" + tableName + "`";

        NotNull notNull = field.getAnnotation(NotNull.class);

        String fieldType = SqlGenerationUtilities.getSqlType(field);
        String sql = alter + " " + action + " " + field.getName() + " " + fieldType;

        if(notNull != null){
            sql += " not null";
        } else {
            sql += " null";
        }

        boolean autoIncrement = false;
        boolean isUniqueKey = field.getAnnotation(Unique.class) != null;
        boolean isPrimaryKey = false;
        PrimaryKey primaryKey = field.getAnnotation(PrimaryKey.class);
        if(primaryKey != null){
            if(primaryKey.autoincrement() && isInt(field)){
                sql += " auto_increment";
                autoIncrement = true;
            }

            sql += " PRIMARY KEY";
            isPrimaryKey = true;
        }

        if(isUniqueKey){
            sql += " UNIQUE KEY";
        }

        ModifyInfo info = new ModifyInfo();
        info.sql = sql;
        info.autoIncrement = autoIncrement;
        info.isNullable = (notNull == null && !isPrimaryKey);
        info.fieldType = fieldType;
        info.isUniqueKey = isUniqueKey;
        info.isPrimaryKey = isPrimaryKey;
        return info;
    }

    public static ModifyInfo modifyColumn(Field field, String tableName) {
        return alterColumn(field, tableName, "MODIFY");
    }

    public static String getObjectByPrimaryKey(Class aClass, Object id) {
        Field primaryKey = getPrimaryKey(aClass);
        if(primaryKey == null){
            throw new IllegalArgumentException("Primary key is not defined");
        }

        return "SELECT * FROM `" + aClass.getSimpleName() + "` WHERE " + primaryKey.getName() + "= :id";
    }

    public static String countByPattern(Object pattern) {
        String sql = "SELECT count(*) FROM " + quotedClassName(pattern.getClass());
        String where = generatePatternWhereClosure(pattern);
        if(!Strings.isEmpty(where)){
            sql += " WHERE " + where;
        }

        return sql;
    }

    public static String count(Class aClass) {
        return  "SELECT count(*) FROM " + quotedClassName(aClass);
    }

    private static String max(List<Field> fields, Class aClass, Object pattern, String fieldName) {
        Reflection.getFieldByNameOrThrow(aClass, fieldName);
        String sql = "SELECT max(" + fieldName + ") FROM " + quotedClassName(aClass);
        String where = null;
        if (pattern != null) {
            where = generatePatternWhereClosure(pattern, fields);
        }

        if(!Strings.isEmpty(where)){
            sql += " WHERE " + where;
        }

        return sql;
    }

    public static String max(List<Field> fields, Object pattern, String fieldName) {
        return max(fields, pattern.getClass(), pattern, fieldName);
    }

    public static String max(Class aClass, String fieldName) {
        return max(null, aClass, null, fieldName);
    }
}
