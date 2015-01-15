package com.tiksem.mysqljava;

import com.tiksem.mysqljava.annotations.*;
import com.tiksem.mysqljava.exceptions.ObjectReplacementFailed;
import com.tiksem.mysqljava.help.SqlGenerationUtilities;
import com.utils.framework.CollectionUtils;
import com.utils.framework.Predicate;
import com.utils.framework.Reflection;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by CM on 1/2/2015.
 */
public class ReplaceStatement extends BatchStatement {
    private List<Field> multiplePatternFields;

    public ReplaceStatement(List<Object> objects) {
        super(objects);
    }

    @Override
    protected void onNotOneRowInserted(Object object) throws SQLException {
        throw new ObjectReplacementFailed(object);
    }

    private void throwAllKeysAreNull() {
        throw new IllegalArgumentException("Unable to replace object, all unique key fields are null");
    }

    private boolean checkMultiIndex(Object object, MultipleIndex multipleIndex) {
        String[] fields = multipleIndex.fields();
        multiplePatternFields.clear();
        for(String fieldName : fields){
            Field field = Reflection.getFieldByNameOrThrow(object, fieldName);
            if(Reflection.isNull(object, field)){
                return false;
            }

            multiplePatternFields.add(field);
        }

        return true;
    }

    private MultipleIndex checkMultiIndexes(final Object object, List<MultipleIndex> multipleIndexes) {
        return CollectionUtils.find(multipleIndexes, new Predicate<MultipleIndex>() {
            @Override
            public boolean check(MultipleIndex item) {
                return checkMultiIndex(object, item);
            }
        });
    }

    private MultipleIndex checkMultipleIndexes(final Object object) {
        Class<?> aClass = object.getClass();
        List<MultipleIndex> multipleIndexes = SqlGenerationUtilities.getUniqueMultiIndexes(aClass);

        return checkMultiIndexes(object, multipleIndexes);
    }

    private List<Field> getPatternFields(final Object object) {
        Class<?> aClass = object.getClass();

        List<Field> uniques = Reflection.getFieldsWithAnnotations(aClass, PrimaryKey.class, Unique.class);
        MultipleIndex multipleIndex = null;

        if(uniques.isEmpty()){
            List<MultipleIndex> multipleIndexes = SqlGenerationUtilities.getUniqueMultiIndexes(aClass);
            if(multipleIndexes.isEmpty()){
                throw new UnsupportedOperationException("Replace is not supported for tables " +
                        "without primary or unique keys");
            }

            multipleIndex = checkMultiIndexes(object, multipleIndexes);
            if(multipleIndex == null){
                throwAllKeysAreNull();
            }
        }

        Field keyField = CollectionUtils.find(uniques, new Predicate<Field>() {
            @Override
            public boolean check(Field item) {
                return !Reflection.isNull(object, item);
            }
        });

        if(keyField == null){
            multipleIndex = checkMultipleIndexes(object);
            if(multipleIndex == null){
                throwAllKeysAreNull();
            }
        } else {
            return Collections.singletonList(keyField);
        }

        return multiplePatternFields;
    }

    @Override
    protected StatementInfo prepareStatementForObject(final Object object) {
        multiplePatternFields = new ArrayList<Field>();

        List<Field> valueFields = SqlGenerationUtilities.getFields(object);
        List<Field> patternFields = getPatternFields(object);
        valueFields.removeAll(patternFields);

        long currentTimeMillis = System.currentTimeMillis();
        Reflection.setValuesOfFieldsWithAnnotation(object, currentTimeMillis,
                ModificationDate.class);
        FieldsCheckingUtilities.fixAndCheckFields(object);

        String sql = SqlGenerationUtilities.update(object, patternFields, object, valueFields);

        Map<String, Object> args = ResultSetUtilities.getArgs(object, patternFields);
        Map<String, Object> valuesArgs = ResultSetUtilities.getArgs(object, valueFields);
        for(String key : valuesArgs.keySet()){
            args.put(key + "_update", valuesArgs.get(key));
        }


        StatementInfo info = new StatementInfo();
        info.args = args;
        info.sql = sql;
        return info;
    }
}
