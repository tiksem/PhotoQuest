package com.tiksem.pq.db;

import com.tiksem.pq.data.annotations.AddingDate;
import com.tiksem.pq.data.annotations.ModificationDate;
import com.tiksem.pq.data.annotations.Relation;
import com.utils.framework.Reflection;
import com.utils.framework.collections.map.MultiMap;
import com.utils.framework.collections.map.SetValuesHashMultiMap;
import com.utils.framework.strings.Strings;

import javax.jdo.*;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Unique;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by CM on 10/30/2014.
 */
public class DBUtilities {
    public static PersistenceManagerFactory createMySQLConnectionFactory(String databaseName) {
        Properties properties = new Properties();
        properties.setProperty("javax.jdo.PersistenceManagerFactoryClass",
                "org.datanucleus.api.jdo.JDOPersistenceManagerFactory");
        properties.setProperty("javax.jdo.option.ConnectionDriverName","com.mysql.jdbc.Driver");
        properties.setProperty("javax.jdo.option.ConnectionURL",
                        "jdbc:mysql://localhost/" + databaseName + "?autoReconnect=true&relaxAutoCommit=true" +
                                "&autoCommit=false");
        properties.setProperty("javax.jdo.option.ConnectionUserName","root");
        properties.setProperty("javax.jdo.option.ConnectionPassword","fightforme");
        properties.setProperty("datanucleus.schema.autoCreateTables", "true");
        properties.setProperty("datanucleus.schema.autoCreateColumns", "true");
        properties.setProperty("datanucleus.schema.autoCreateAll", "true");

        return JDOHelper.getPersistenceManagerFactory(properties);
    }

    public static <T> T getObjectById(PersistenceManager manager, Class<T> aClass, long id) {
        Query query = manager.newQuery(aClass);
        query.setFilter("this.id==" + id);
        Collection<T> collection = (Collection<T>)query.execute();
        if(collection.size() <= 0){
            return null;
        }

        T result = collection.iterator().next();
        resetNotPersistentFields(result);
        return result;
    }

    public static <T> T executeQueryForOneInstance(Query query) {
        Collection<T> collection = (Collection<T>) query.execute();
        if(collection.isEmpty()){
            return null;
        }

        return collection.iterator().next();
    }

    public static <T> Collection<T> queryByPattern(PersistenceManager manager, T pattern, OffsetLimit offsetLimit) {
        QueryParams params = new QueryParams();
        params.offsetLimit = offsetLimit;
        return queryByPattern(manager, pattern, params);
    }

    public static <T> Collection<T> queryByExcludePattern(PersistenceManager manager, T pattern,
                                                          OffsetLimit offsetLimit) {
        QueryParams params = new QueryParams();
        params.offsetLimit = offsetLimit;
        params.asExcludePattern = true;
        return queryByPattern(manager, pattern, params);
    }

    private static MultiMap<String, Field> getRelations(Class aClass) {
        MultiMap<String, Field> result = new SetValuesHashMultiMap<String, Field>();
        List<Field> fields = Reflection.getFieldsWithAnnotations(aClass, Relation.class);

        for(Field field : fields){
            Relation relation = field.getAnnotation(Relation.class);
            String relationName = relation.relationName();
            if(relationName == null){
                throw new IllegalArgumentException("Relation name could not be null");
            }

            result.put(relationName, field);
        }

        return result;
    }

    private static Query getQueryByPattern(PersistenceManager manager,
                                           Object pattern,
                                           Map<String, Object> outArgs,
                                           boolean asExcludePattern,
                                           boolean ignoreRelations,
                                           List<String> outParameters,
                                           StringBuilder outFilter) {
        Class<?> patternClass = pattern.getClass();
        List<Class> relationsClasses = ignoreRelations ? Arrays.<Class>asList() : Arrays.<Class>asList(Relation.class);
        List<Field> fields =
                Reflection.getFieldsWithAndWithoutAnnotations(patternClass,
                        Arrays.asList(PrimaryKey.class, Index.class, Unique.class), relationsClasses);

        List<String> parameters = outParameters;
        if(parameters == null){
            parameters = new ArrayList<String>(fields.size());
        }

        List<String> filters = new ArrayList<String>();

        for(Field field : fields){
            Class type = field.getType();
            Object value = Reflection.getValueOfField(pattern, field);
            if(value == null){
                continue;
            }

            String parameterName = "a" + parameters.size();
            String parameter = type.getSimpleName() + " " + parameterName;
            parameters.add(parameter);

            String filter = "this." + field.getName() + "==" + parameterName;
            filters.add(filter);

            outArgs.put(parameterName, value);
        }

        StringBuilder filtersString = outFilter;
        if(filtersString == null){
            filtersString = new StringBuilder();
        }

        Strings.join(" && ", filters, filtersString);

        if(asExcludePattern){
            filtersString.insert(0, "!(");
            filtersString.append(")");
        }

        if (!ignoreRelations) {
            MultiMap<String, Field> relations = getRelations(patternClass);
            if(!relations.isEmpty()){
                for(String relationName : relations.getKeys()){
                    List<Field> relationFields = new ArrayList<Field>(relations.getValues(relationName));
                    if(relationFields.size() != 2){
                        throw new IllegalArgumentException("Each relation in " +
                                patternClass.getCanonicalName() + " should have" +
                                " 2 related fields");
                    }

                    String[] params = new String[2];
                    int count = 0;
                    for(Field field : relationFields){
                        Object value = Reflection.getValueOfField(pattern, field);
                        if(value == null){
                            continue;
                        }

                        String parameterName = "a" + parameters.size();
                        String parameter = field.getType().getSimpleName() + " " + parameterName;
                        parameters.add(parameter);
                        outArgs.put(parameterName, value);
                        params[count++] = parameterName;
                    }

                    String filter = null;
                    if(count == 1){
                        String param = params[0];
                        filter = "(this." + relationFields.get(0).getName() + " == " + param + " || this." +
                                relationFields.get(1).getName() + " == " + param + ")";
                    } else if(count == 2) {
                        String field1 = relationFields.get(0).getName();
                        String field2 = relationFields.get(1).getName();

                        String param1 = params[0];
                        String param2 = params[1];
                        filter = "(this." + field1 + " == " + param1 + " && this." + field2 + " == "
                                + param2 + ") || " +
                                "(this." + field1 + " == " + param2 + " && this." + field2 + " == " + param1 + ")";
                    }

                    if(filter != null){
                        if (filtersString.length() > 0) {
                            filtersString.insert(0, "(");
                            filtersString.append(") && (" + filter + ")");
                        } else {
                            filtersString.append(filter);
                        }
                    }
                }
            }
        }

        if(outParameters != null && outFilter != null){
            return null;
        }

        Query query = manager.newQuery(patternClass);
        if (outParameters == null) {
            String parametersString = Strings.join(",", parameters).toString();
            query.declareParameters(parametersString);
        }

        if (outFilter == null) {
            query.setFilter(filtersString.toString());
        }

        return query;
    }

    public static long queryCountByExcludePattern(PersistenceManager manager, Object pattern,
                                            boolean ignoreRelations) {
        return queryCountByPattern(manager, pattern, true, ignoreRelations);
    }

    public static long queryCountByExcludePattern(PersistenceManager manager, Object pattern) {
        return queryCountByPattern(manager, pattern, true, false);
    }

    public static long queryCountByPattern(PersistenceManager manager, Object pattern,
                                            boolean ignoreRelations) {
        return queryCountByPattern(manager, pattern, false, ignoreRelations);
    }

    public static long queryCountByPattern(PersistenceManager manager, Object pattern) {
        return queryCountByPattern(manager, pattern, false, false);
    }

    private static long queryCountByPattern(PersistenceManager manager, Object pattern,
                                            boolean asExcludePattern,
                                            boolean ignoreRelations) {
        Map<String, Object> args = new HashMap<String, Object>();
        Query query = getQueryByPattern(manager, pattern, args, asExcludePattern, ignoreRelations, null, null);
        query.setResult("count(this)");

        Long result = (Long)query.executeWithMap(args);
        if(result != null){
            return result;
        }

        return 0;
    }

    public static class QueryParams {
        public boolean asExcludePattern = false;
        public boolean ignoreRelations = false;
        public OffsetLimit offsetLimit = new OffsetLimit();
        public String ordering;
    }

    public static <T> Collection<T> queryByPattern(PersistenceManager manager, T pattern,
                                                              QueryParams queryParams) {
        Map<String, Object> args = new HashMap<String, Object>();
        Query query = getQueryByPattern(manager, pattern, args, queryParams.asExcludePattern,
                queryParams.ignoreRelations, null, null);
        queryParams.offsetLimit.applyToQuery(query);
        if (queryParams.ordering != null) {
            query.setOrdering(queryParams.ordering);
        }

        Collection<T> result = new ArrayList<T>((Collection < T >) query.executeWithMap(args));
        resetNotPersistentFields(result);
        return result;
    }

    public static <T> T getObjectByPattern(PersistenceManager manager, T pattern) {
        Collection<T> collection = queryByPattern(manager, pattern, new OffsetLimit(0, 1));
        if(collection.isEmpty()){
            return null;
        }

        return collection.iterator().next();
    }

    public static <T> Collection<T> getAllObjectsOfClass(PersistenceManager manager, Class<T> patternClass,
                                                         OffsetLimit offsetLimit) {
        return getAllObjectsOfClass(manager, patternClass, offsetLimit, null);
    }

    public static <T> Collection<T> getAllObjectsOfClass(PersistenceManager manager, Class<T> patternClass,
                                                         OffsetLimit offsetLimit, String ordering) {
        Query query = manager.newQuery(patternClass);
        offsetLimit.applyToQuery(query);

        if(ordering != null){
            query.setOrdering(ordering);
        }

        Collection<T> result = new ArrayList<T>((Collection < T >) query.execute());
        resetNotPersistentFields(result);
        return result;
    }

    public static long getAllObjectsOfClassCount(PersistenceManager manager, Class patternClass) {
        Query query = manager.newQuery(patternClass);
        query.setResult("count(this)");
        return (Long)query.execute();
    }

    public static  <T> void deleteAllObjectsOfClass(PersistenceManager manager, Class<T> aClass) {
        Transaction transaction = manager.currentTransaction();
        transaction.begin();
        manager.deletePersistentAll(getAllObjectsOfClass(manager, aClass, new OffsetLimit(0, OffsetLimit.MAX_LIMIT)));
        transaction.commit();
    }

    private static void setAddingAndModificationDateIfNeed(Object object, Long value) {
        Field addingDateField = Reflection.getFieldWithAnnotation(object.getClass(), AddingDate.class);
        if(addingDateField != null){
            Reflection.setValueOfFieldIfNull(object, addingDateField, value);
        }

        Field modificationDateField = Reflection.getFieldWithAnnotation(object.getClass(), ModificationDate.class);
        if(modificationDateField != null){
            Reflection.setFieldValueUsingSetter(object, addingDateField, value);
        }
    }

    public static <T> T[] makeAllPersistent(PersistenceManager persistenceManager, T... objects) {
        return makeAllPersistent(persistenceManager, Arrays.asList(objects));
    }

    public static <T> T[] makeAllPersistent(PersistenceManager persistenceManager, Collection<T> objects) {
        Object[] result = new Object[objects.size()];

        Transaction transaction = persistenceManager.currentTransaction();
        transaction.begin();

        int index = 0;
        for (Object object : objects) {
            FieldsCheckingUtilities.fixAndCheckFields(object);

            try {
                setAddingAndModificationDateIfNeed(object, System.currentTimeMillis());
                object = persistenceManager.makePersistent(object);
            } catch (RuntimeException e) {
                transaction.rollback();
                throw e;
            } catch (Error e) {
                transaction.rollback();
                throw e;
            }

            result[index++] = object;
        }

        transaction.commit();
        return (T[]) result;
    }

    public static <T> T makePersistent(PersistenceManager persistenceManager, T object) {
        return makeAllPersistent(persistenceManager, object)[0];
    }

    public static void deletePersistent(PersistenceManager persistenceManager, Object object) {
        Transaction transaction = persistenceManager.currentTransaction();
        transaction.begin();
        try {
            persistenceManager.deletePersistent(object);
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
        } catch (Error e) {
            transaction.rollback();
            throw e;
        }
        transaction.commit();
    }

    public static void deleteAllPersistent(PersistenceManager persistenceManager, Collection objects) {
        Transaction transaction = persistenceManager.currentTransaction();
        transaction.begin();
        try {
            persistenceManager.deletePersistentAll(objects);
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
        } catch (Error e) {
            transaction.rollback();
            throw e;
        }
        transaction.commit();
    }

    public static void enhanceClassesInPackage(String packageName) {
        List<Class<?>> classesInPackage = Reflection.findClassesInPackage(packageName);

        JDOEnhancer jdoEnhancer = new org.datanucleus.api.jdo.JDOEnhancer();
        jdoEnhancer.setVerbose(true);
        jdoEnhancer.addClasses(Reflection.classesCanonicalAsNamesStringArray(classesInPackage));
        jdoEnhancer.enhance();
    }

    public static void resetNotPersistentFields(Collection objects) {
        for(Object o : objects){
            resetNotPersistentFields(o);
        }
    }

    public static void resetNotPersistentFields(Object object) {
        for(Field field : Reflection.getFieldsWithAnnotations(object.getClass(), NotPersistent.class)) {
            Reflection.setValueOfField(object, field, null);
        }
    }

    private static void checkFields(Class aClass, String fieldNameA, String fieldNameB) {
        if(!Reflection.hasField(aClass, fieldNameA)){
            throw new IllegalArgumentException("Class " + aClass.getCanonicalName() +
                    " hasn't " + fieldNameA + " field");
        }

        if(!Reflection.hasField(aClass, fieldNameB)){
            throw new IllegalArgumentException("Class " + aClass.getCanonicalName() +
                    " hasn't " + fieldNameB + " field");
        }
    }

    public static  <T> T getMax(PersistenceManager manager, Class<T> aClass, String ordering){
        Collection<T> result = DBUtilities.getAllObjectsOfClass(manager, aClass, new OffsetLimit(0, 1),
                ordering);
        if(result.isEmpty()){
            return null;
        }

        return result.iterator().next();
    }

    public static  <T> T getMaxByPattern(PersistenceManager manager, T pattern, String ordering){
        QueryParams params = new QueryParams();
        params.offsetLimit = new OffsetLimit(0, 1);
        params.ordering = ordering;
        Collection<T> result = queryByPattern(manager, pattern, params);
        if(result.isEmpty()){
            return null;
        }

        return result.iterator().next();
    }

    private static class Ordering {
        OrderType type;
        String fieldName;
    }

    private static List<Ordering> parseOrdering(String orderingString) {
        String[] orderingArray = orderingString.split(", *");
        List<Ordering> result = new ArrayList<Ordering>();

        for(String order : orderingArray){
            String[] splitOrder = order.split(" +");
            if(splitOrder.length != 2){
                throw new RuntimeException("Syntax error");
            }

            Ordering ordering = new Ordering();
            ordering.type = OrderType.valueOf(splitOrder[1]);
            ordering.fieldName = splitOrder[0];
            result.add(ordering);
        }

        return result;
    }

    private static void generateFilterForPositionQuery(StringBuilder outResult,
                                                int index,
                                                List<Ordering> orderings,
                                                List<String> outDeclarations,
                                                Map<String, Object> args,
                                                Object object) {
        int length = orderings.size();
        if(index >= length){
            return;
        }

        Ordering ordering = orderings.get(index);

        outResult.append('(');
        String operator = ">";
        if(ordering.type == OrderType.ascending){
            operator = "<";
        }

        Field field = Reflection.getFieldByNameOrThrow(object, ordering.fieldName);
        outDeclarations.add(field.getType().getSimpleName() + " " + ordering.fieldName);

        args.put(ordering.fieldName, Reflection.getFieldValueUsingGetter(object, field));
        outResult.append("this." + ordering.fieldName + operator + ordering.fieldName);

        if(index < length - 1){
            outResult.append(" || (");
            outResult.append("this." + ordering.fieldName + "==" + ordering.fieldName + " && ");
            generateFilterForPositionQuery(outResult, index + 1, orderings, outDeclarations, args, object);
            outResult.append(")");
        }

        outResult.append(')');
    }

    public static <T> long getPosition(PersistenceManager manager, T object, String orderingString, T pattern) {
        StringBuilder filter = new StringBuilder();
        Map<String, Object> args = new HashMap<String, Object>();
        List<String> declarations = new ArrayList<String>();
        generateFilterForPositionQuery(filter,0, parseOrdering(orderingString), declarations, args, object);

        StringBuilder filterFromPattern = new StringBuilder();
        if (pattern != null) {
            getQueryByPattern(manager, pattern, args, false, false, declarations, filterFromPattern);
        }
        String filterResult;
        if(filterFromPattern.length() > 0){
            filterResult = "(" + filterFromPattern + ") && (" + filter + ")";
        } else {
            filterResult = filter.toString();
        }

        Query query = manager.newQuery(object.getClass());
        query.setResult("count(this)");
        query.setFilter(filterResult);
        query.declareParameters(Strings.join(", ", declarations).toString());

        return Long.valueOf(query.executeWithMap(args).toString());
    }

    // method for debug
    public static Object get(PersistenceManager manager) {
        Query query = manager.newQuery();
        return query.execute();
    }
}
