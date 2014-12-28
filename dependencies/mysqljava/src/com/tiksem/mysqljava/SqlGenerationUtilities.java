package com.tiksem.mysqljava;

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

    private static String getSqlType(Field field) {
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
                        ForeignKey foreignKey = field.getAnnotation(ForeignKey.class);
                        if(foreignKey != null){
                            Class parent = foreignKey.parent();
                            try {
                                Field parentField = parent.getDeclaredField(foreignKey.field());
                                return getSqlType(parentField);
                            } catch (NoSuchFieldException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        }

        return getSqlType(field, suggestedType);
    }

    private static String getSqlType(Field field, String suggestedType) {
        if(Strings.isEmpty(suggestedType)){
            Class<?> type = field.getType();

            if(type == Long.class){
                return "BIGINT";
            } else if(type == Integer.class) {
                return "INT";
            } else if(type == Short.class) {
                return "SMALLINT";
            } else if(type == Byte.class) {
                return "TINYINT";
            } else if(type == String.class) {
                return "VARCHAR(255)";
            } else if(type == Float.class) {
                return "FLOAT";
            } else if(type == Double.class) {
                return "DOUBLE";
            } else if(type == Boolean.class) {
                return "BIT(1)";
            }

            throw new IllegalArgumentException("Unsupported type " + type.getSimpleName());

        } else {
            return suggestedType;
        }
    }

    private static boolean isInt(Field field) {
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

            PrimaryKey primaryKey = field.getAnnotation(PrimaryKey.class);
            if(primaryKey != null){
                String columnDefinition = getColumnDefinition(field, primaryKey.type(),
                        primaryKey.autoincrement());
                columnDefinition += " PRIMARY KEY";

                String fieldName = field.getName();
                String indexDefinition = getIndexDefinition(fieldName, primaryKey.indexType());

                parts.add(columnDefinition);
                parts.add(indexDefinition);
                continue;
            }

            Unique unique = field.getAnnotation(Unique.class);
            if(unique != null){
                String columnDefinition = getColumnDefinition(field, unique.type(), false);

                String fieldName = field.getName();

                parts.add(columnDefinition);
                String uniqueKeyDefinition = "UNIQUE KEY " + fieldName + "_index " +
                        "USING " + unique.indexType()
                        + " (`" + fieldName + "`)";
                parts.add(uniqueKeyDefinition);
                continue;
            }

            Index index = field.getAnnotation(Index.class);
            if(index != null){
                String columnDefinition = getColumnDefinition(field, index.type(), false);

                String fieldName = field.getName();
                String indexDefinition = getIndexDefinition(fieldName, index.indexType());

                parts.add(columnDefinition);
                parts.add(indexDefinition);
                continue;
            }

            ForeignKey foreignKey = field.getAnnotation(ForeignKey.class);
            if(foreignKey != null){
                Class parent = foreignKey.parent();
                Field parentField;
                try {
                    parentField = parent.getDeclaredField(foreignKey.field());
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }

                PrimaryKey parentPrimaryKey = parentField.getAnnotation(PrimaryKey.class);
                if(parentPrimaryKey == null){
                    throw new IllegalStateException("Unable to define ForeignKey, parent should be " +
                            "PrimaryKey");
                }

                String columnDefinition = getColumnDefinition(field, parentPrimaryKey.type(), false);
                String fieldName = field.getName();
                String indexDefinition = "INDEX " + fieldName + "_index USING " + foreignKey.indexType()
                        + " (" + fieldName + ")";
                String keyDefinition = "FOREIGN KEY (" + fieldName + ") REFERENCES " + parent.getSimpleName() + "(" +
                        parentField.getName() + ")";

                keyDefinition += " ON DELETE " + foreignKey.onDelete();
                keyDefinition += " ON UPDATE " + foreignKey.onUpdate();

                parts.add(columnDefinition);
                parts.add(indexDefinition);
                parts.add(keyDefinition);
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
                ForeignKey.class, Unique.class);
    }

    public static List<Field> getFieldsExcludingPrimaryKey(Class aClass) {
        return Reflection.getFieldsWithAnnotations(aClass,
                Index.class, Stored.class,
                ForeignKey.class, Unique.class);
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

        if(parts.isEmpty()){
            throw new IllegalArgumentException("Invalid pattern, all fields are null");
        }

        return Strings.join(" AND ", parts).toString();
    }

    public static String generatePatternWhereClosure(Object pattern) {
        List<Field> fields = getFields(pattern);
        return generatePatternWhereClosure(pattern, fields);
    }

    public static String insert(final Object object) {
        return insert(object, getFields(object));
    }

    public static String insert(final Object object, List<Field> fields) {
        List<String> fieldNames = CollectionUtils.transform(fields,
                new CollectionUtils.Transformer<Field, String>() {
            @Override
            public String get(Field field) {
                return field.getName();
            }
        });

        List<String> values = CollectionUtils.transform(fields,
                new CollectionUtils.Transformer<Field, String>() {
            @Override
            public String get(Field field) {
                return ":" + field.getName();
            }
        });

        String result =  "INSERT INTO `" + object.getClass().getSimpleName() +
                "` (" + Strings.join(", ", fieldNames) + ")\nVALUES (" +
                Strings.join(", ", values)
                + ")";
        return result;
    }

    public static class Foreign {
        public Class childClass;
        public Field foreignField;
    }

    private static String quotedClassName(Class aClass) {
        return Strings.quote(aClass.getSimpleName(), "`");
    }

    public static String select(Object pattern,
                                List<Foreign> foreigns,
                                SelectParams selectParams) {
        return select(Arrays.<Class>asList(pattern.getClass()), foreigns, pattern, selectParams);
    }

    public static String select(List<Class> resultClasses,
                                List<Foreign> foreigns,
                                Object pattern,
                                SelectParams selectParams
                                ) {
        if(resultClasses.isEmpty()){
            throw new IllegalArgumentException("resultClasses are empty");
        }

        Set<Class> resultClassesSet = new HashSet<Class>(resultClasses);
        Set<Class> fromClasses = new HashSet<Class>(resultClasses);

        List<String> foreignWhereParts = new ArrayList<String>();

        for(Foreign foreign : foreigns){
            ForeignKey foreignKey = Reflection.getAnnotationOrThrow(foreign.foreignField, ForeignKey.class);
            String parentFieldName = foreignKey.field();
            Class parent = foreignKey.parent();
            fromClasses.add(parent);
            fromClasses.add(foreign.childClass);
            resultClassesSet.add(parent);

            String part = quotedClassName(parent) + "." + parentFieldName + "=" +
                    quotedClassName(foreign.childClass) + "." + foreign.foreignField.getName();
            foreignWhereParts.add(part);
        }

        String from = Strings.join(", ", CollectionUtils.transform(fromClasses,
                new CollectionUtils.Transformer<Class, CharSequence>() {
            @Override
            public CharSequence get(Class aClass) {
                return quotedClassName(aClass);
            }
        })).toString();

        String selectResult = Strings.join(", ",
                CollectionUtils.transform(resultClassesSet,
                        new CollectionUtils.Transformer<Class, CharSequence>() {
                            @Override
                            public CharSequence get(Class aClass) {
                                return quotedClassName(aClass) + ".*";
                            }
                        })).toString();

        String where = generatePatternWhereClosure(pattern, getFields(pattern));

        if(!foreignWhereParts.isEmpty()){
            where = "(" + where + ") AND (" + Strings.join(" AND ", foreignWhereParts) + ")";
        }

        if(selectParams.whereTransformer != null){
            where = selectParams.whereTransformer.get(where);
        }

        String query = "SELECT " + selectResult + " FROM " + from;
        if(!Strings.isEmpty(where)){
            query += " WHERE " + where;
        }

        if(selectParams.ordering != null){
            query += " ORDER BY " + selectParams.ordering;
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

    public static void setObjectId(Object object, Object id) {
        Field field = getPrimaryKey(object);
        Reflection.setFieldValueUsingSetter(object, field, id);
    }

    public static String delete(Object object,
                                List<Field> fields) {
        return delete(object, fields, null);
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
            where = generatePatternWhereClosure(object);
            if(whereTransformer != null){
                where = whereTransformer.get(where);
            }
        }

        String result = "DELETE FROM " + object.getClass().getSimpleName();
        if(!Strings.isEmpty(where)){
            result += " WHERE " + where;
        }

        return result;
    }
}
