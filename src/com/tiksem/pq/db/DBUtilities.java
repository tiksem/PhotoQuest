package com.tiksem.pq.db;

import com.tiksem.pq.data.annotations.AddingDate;
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
                "com.objectdb.jdo.PMF");
//        properties.setProperty("javax.jdo.option.ConnectionDriverName","com.mysql.jdbc.Driver");
        properties.setProperty("javax.jdo.option.ConnectionURL",
                        databaseName);
//        properties.setProperty("javax.jdo.option.ConnectionUserName","root");
//        properties.setProperty("javax.jdo.option.ConnectionPassword","fightforme");
//        properties.setProperty("datanucleus.schema.autoCreateTables", "true");
        PersistenceManagerFactory persistenceManagerFactory = JDOHelper.getPersistenceManagerFactory(properties);

        return persistenceManagerFactory;
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
        return queryByPattern(manager, pattern, false, offsetLimit);
    }

    public static <T> Collection<T> queryByExcludePattern(PersistenceManager manager, T pattern,
                                                          OffsetLimit offsetLimit) {
        return queryByExcludePattern(manager, pattern, false, offsetLimit);
    }

    public static <T> Collection<T> queryByPattern(PersistenceManager manager, T pattern,
                                                   boolean ignoreRelations,
                                                   OffsetLimit offsetLimit) {
        return queryByPattern(manager, pattern, false, ignoreRelations, offsetLimit);
    }

    public static <T> Collection<T> queryByExcludePattern(PersistenceManager manager, T pattern,
                                                          boolean ignoreRelations,
                                                          OffsetLimit offsetLimit) {
        return queryByPattern(manager, pattern, true, ignoreRelations, offsetLimit);
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

    private static <T> Collection<T> queryByPattern(PersistenceManager manager, T pattern,
                                                              boolean asExcludePattern,
                                                              boolean ignoreRelations,
                                                              OffsetLimit offsetLimit) {
        Class<?> patternClass = pattern.getClass();
        List<Class> relationsClasses = ignoreRelations ? Arrays.<Class>asList() : Arrays.<Class>asList(Relation.class);
        List<Field> fields =
                Reflection.getFieldsWithAndWithoutAnnotations(patternClass,
                        Arrays.asList(PrimaryKey.class, Index.class, Unique.class), relationsClasses);

        List<String> parameters = new ArrayList<String>(fields.size());
        List<String> filters = new ArrayList<String>();
        Map<String, Object> args = new HashMap<String, Object>();

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

            args.put(parameterName, value);
        }

        String filtersString = Strings.join(" && ", filters).toString();
        if(asExcludePattern){
            filtersString = "!(" + filtersString +")";
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
                        args.put(parameterName, value);
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
                        if (!filtersString.isEmpty()) {
                            filtersString = "(" + filtersString + ") && (" + filter + ")";
                        } else {
                            filtersString = filter;
                        }
                    }
                }
            }
        }

        String parametersString = Strings.join(",", parameters).toString();

        Query query = manager.newQuery(patternClass);
        query.declareParameters(parametersString);
        query.setFilter(filtersString);
        offsetLimit.applyToQuery(query);

        Collection<T> result = (Collection < T >) query.executeWithMap(args);
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

    public static <T> Collection<T> getAllObjectsOfClass(PersistenceManager manager, Class<T> patternClass) {
        return getAllObjectsOfClass(manager, patternClass, new OffsetLimit(0, OffsetLimit.MAX_LIMIT));
    }

    public static <T> Collection<T> getAllObjectsOfClass(PersistenceManager manager, Class<T> patternClass,
                                                         OffsetLimit offsetLimit) {
        Query query = manager.newQuery(patternClass);
        offsetLimit.applyToQuery(query);
        Collection<T> result = (Collection < T >) query.execute();
        resetNotPersistentFields(result);
        return result;
    }

    public static  <T> void deleteAllObjectsOfClass(PersistenceManager manager, Class<T> aClass) {
        Transaction transaction = manager.currentTransaction();
        transaction.begin();
        manager.deletePersistentAll(getAllObjectsOfClass(manager, aClass));
        transaction.commit();
    }

    private static void setAddingDateIfNeed(Object object, Long value) {
        Field addingDateField = Reflection.getFieldWithAnnotation(object.getClass(), AddingDate.class);
        if(addingDateField != null){
            if (value != null) {
                Reflection.setValueOfFieldIfNull(object, addingDateField, value);
            } else {
                Reflection.setValueOfField(object, addingDateField, null);
            }
        }
    }

    public static <T> T makePersistent(PersistenceManager persistenceManager, T object) {
        Transaction transaction = persistenceManager.currentTransaction();
        transaction.begin();

        FieldsCheckingUtilities.fixAndCheckFields(object);

        try {
            setAddingDateIfNeed(object, System.currentTimeMillis());
            object = persistenceManager.makePersistent(object);
        } catch (RuntimeException e) {
            transaction.rollback();
            setAddingDateIfNeed(object, null);
            throw e;
        } catch (Error e) {
            transaction.rollback();
            setAddingDateIfNeed(object, null);
            throw e;
        }

        transaction.commit();
        return object;
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
}
