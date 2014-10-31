package com.tiksem.pq.db;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import java.util.Collection;
import java.util.Properties;

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
        return (T)query.execute();
    }

    public static <T> T executeQueryForOneInstance(Query query) {
        Collection<T> collection = (Collection<T>) query.execute();
        if(collection.isEmpty()){
            return null;
        }

        return collection.iterator().next();
    }
}
