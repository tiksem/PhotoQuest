package com.tiksem.pq.db;

import com.tiksem.pq.data.Photoquest;
import com.tiksem.pq.data.User;
import com.utils.framework.Reflection;
import com.utils.framework.strings.Strings;

import javax.jdo.*;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Unique;
import java.lang.reflect.Field;
import java.sql.Ref;
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

    public static <T> Collection<T> queryByPattern(PersistenceManager manager, T pattern) {
        return queryByPattern(manager, pattern, false);
    }

    public static <T> Collection<T> queryByExcludePattern(PersistenceManager manager, T pattern) {
        return queryByPattern(manager, pattern, true);
    }

    private static <T> Collection<T> queryByPattern(PersistenceManager manager, T pattern, boolean asExcludePattern) {
        Class<?> patternClass = pattern.getClass();
        List<Field> fields =
                Reflection.getFieldsWithAnnotations(patternClass, PrimaryKey.class, Index.class, Unique.class);

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

        if(parameters.isEmpty()){
            return (Collection<T>)manager.newQuery(patternClass).execute();
        }

        String parametersString = Strings.join(",", parameters).toString();
        String filtersString = Strings.join(" && ", filters).toString();
        if(asExcludePattern){
            filtersString = "!(" + filtersString +")";
        }

        Query query = manager.newQuery(patternClass);
        query.declareParameters(parametersString);
        query.setFilter(filtersString);

        Collection<T> result = (Collection < T >) query.executeWithMap(args);
        resetNotPersistentFields(result);
        return result;
    }

    public static <T> T getObjectByPattern(PersistenceManager manager, T pattern) {
        Collection<T> collection = queryByPattern(manager, pattern);
        if(collection.isEmpty()){
            return null;
        }

        return collection.iterator().next();
    }

    public static <T> Collection<T> getAllObjectsOfClass(PersistenceManager manager, Class<T> patternClass) {
        Query query = manager.newQuery(patternClass);
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

    public static <T> T makePersistent(PersistenceManager persistenceManager, T object) {
        Transaction transaction = persistenceManager.currentTransaction();
        transaction.begin();

        try {
            object = persistenceManager.makePersistent(object);
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
        } catch (Error e) {
            transaction.rollback();
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
}
