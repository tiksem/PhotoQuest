package com.tiksem.pq.db;

import com.utils.framework.Reflection;
import com.utils.framework.strings.Strings;

import javax.jdo.*;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.Unique;
import javax.persistence.Id;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by CM on 10/30/2014.
 */
public class ObjectDBUtilities {
    public static PersistenceManagerFactory createLocalConnectionFactory(String databaseName) {
        Properties properties = new Properties();
        properties.setProperty(
                "javax.jdo.PersistenceManagerFactoryClass", "com.objectdb.jdo.PMF");
        properties.setProperty(
                "javax.jdo.option.ConnectionURL", databaseName);

        PersistenceManagerFactory persistenceManagerFactory =
                JDOHelper.getPersistenceManagerFactory(properties);

        return persistenceManagerFactory;
    }

    public static <T> T getObjectById(PersistenceManager manager, Class<T> aClass, long id) {
        Query query = manager.newQuery(aClass);
        query.setFilter("this.id==" + id);
        Collection<T> collection = (Collection<T>)query.execute();
        if(collection.size() <= 0){
            return null;
        }

        return collection.iterator().next();
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
                Reflection.getFieldsWithAnnotations(patternClass, Id.class, Index.class, Unique.class);

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

        return (Collection<T>) query.executeWithMap(args);
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
        return (Collection<T>) query.execute();
    }

    public static  <T> void deleteAllObjectsOfClass(PersistenceManager manager, Class<T> aClass) {
        Transaction transaction = manager.currentTransaction();
        transaction.begin();
        manager.deletePersistentAll(getAllObjectsOfClass(manager, aClass));
        transaction.commit();
    }
}
