package com.tiksem.mysqljava;

import com.tiksem.mysqljava.annotations.ForeignKey;
import com.tiksem.mysqljava.annotations.ForeignValue;
import com.utils.framework.CollectionUtils;
import com.utils.framework.Reflection;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by CM on 12/27/2014.
 */
public class ResultSetUtilities {
    public static <T> List<T> getList(Class<T> aClass, ResultSet resultSet) {
        if (!SqlGenerationUtilities.isSupportedPrimitive(aClass)) {
            return getList(SqlGenerationUtilities.getFields(aClass), aClass, resultSet);
        } else {
            return getList(null, aClass, resultSet);
        }
    }

    public static List<Map<String, Map<String, Object>>> getMapList(ResultSet resultSet) {
        List<Map<String, Map<String, Object>>> maps = new ArrayList<Map<String, Map<String, Object>>>();
        try {
            while (resultSet.next()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                Map<String, Map<String, Object>> map = new LinkedHashMap<String, Map<String, Object>>();

                for (int i = 0; i < columnCount; i++) {
                    String tableName = metaData.getTableName(i + 1);
                    Map<String, Object> table = map.get(tableName);
                    if(table == null){
                        table = new LinkedHashMap<String, Object>();
                        map.put(tableName, table);
                    }

                    Object value = resultSet.getObject(i + 1);
                    String columnName = metaData.getColumnName(i + 1);
                    table.put(columnName, value);
                }

                maps.add(map);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return maps;
    }

    public static <T> List<T> getList(List<Field> fields, Class<T> aClass, ResultSet resultSet) {
        List<T> result = new ArrayList<T>();

        try {
            while (resultSet.next()) {
                T object;

                if (fields != null) {
                    object = Reflection.createObjectOfClass(aClass);

                    for(Field field : fields){
                        String fieldName = field.getName();
                        try {
                            resultSet.findColumn(fieldName);
                        } catch (SQLException e) {
                            continue;
                        }
                        Object value = resultSet.getObject(fieldName);
                        Reflection.setFieldValueUsingSetter(object, field, value);
                    }
                } else {
                    object = (T) resultSet.getObject(0);
                }

                result.add(object);
            }
        } catch (SQLException e) {
            throw new  RuntimeException(e);
        }

        return result;
    }

    public interface FieldsProvider {
        List<Field> getFieldsOfClass(Class aClass);
        List<Field> getForeignValueFields(Class aClass);
    }

    private static void fillForeignField(Field foreignField,
                                         Object object,
                                         Map<String, Map<String, Object>> map) {
        ForeignValue foreignValue = Reflection.getAnnotationOrThrow(foreignField, ForeignValue.class);
        String idFieldName = foreignValue.idField();
        Class aClass = object.getClass();
        Field idField = Reflection.getFieldByNameOrThrow(aClass, idFieldName);
        ForeignKey foreignKey = Reflection.getAnnotationOrThrow(idField, ForeignKey.class);
        Class parentClass = foreignKey.parent();
        String parentClassName = parentClass.getSimpleName().toLowerCase();

        Map<String, Object> parentValues = map.get(parentClassName);
        Object foreignObject = Reflection.getOrCreateFieldValue(object, foreignField);

        Reflection.setFieldsFromMap(foreignObject, parentValues);
    }

    public static List<Object> getListWithSeveralTables(ResultSet resultSet,
                                                        FieldsProvider fieldsProvider,
                                                        Class... resultClasses) {
        List<Object> result = new ArrayList<Object>();

        Map<String, Integer> classNameIndexMap = new HashMap<String, Integer>();
        List<List<Field>> fields = new ArrayList<List<Field>>();
        List<List<Field>> foreignValueFields = new ArrayList<List<Field>>();

        int index = 0;
        for(final Class aClass : resultClasses){
            String name = aClass.getSimpleName().toLowerCase();
            classNameIndexMap.put(name, index++);
            fields.add(fieldsProvider.getFieldsOfClass(aClass));

            foreignValueFields.add(fieldsProvider.getForeignValueFields(aClass));
        }

        List<Map<String, Map<String, Object>>> resultList = getMapList(resultSet);
        for(Map<String, Map<String, Object>> map : resultList){
            for(Map.Entry<String, Map<String, Object>> entry : map.entrySet()){
                String className = entry.getKey();
                Integer classIndex = classNameIndexMap.get(className);
                if(classIndex == null){
                    continue;
                }

                List<Field> fieldsInClass = fields.get(classIndex);
                Object object = Reflection.createObjectOfClass(resultClasses[classIndex]);
                Reflection.setFieldsFromMap(object, fieldsInClass, entry.getValue());
                result.add(object);

                List<Field> foreignFields = foreignValueFields.get(classIndex);
                for(Field foreignField : foreignFields){
                    fillForeignField(foreignField, object, map);
                }
            }
        }

        return result;
    }

    public static <T> T get(List<Field> fields, Class<T> aClass, ResultSet resultSet) {
        List<T> list = getList(fields, aClass, resultSet);
        if(list.isEmpty()){
            return null;
        }

        return CollectionUtils.getLast(list);
    }
}
