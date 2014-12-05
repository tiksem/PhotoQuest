package com.tiksem.pq.db;

import com.utils.framework.io.IOUtilities;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by CM on 12/5/2014.
 */
public class SqlFileExecutor {
    private static final String SQL_PATH = "com/tiksem/pq/sql/";

    private Map<String, String> queries = new HashMap<String, String>();
    private PersistenceManager persistenceManager;

    public SqlFileExecutor(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    public Object execute(String fileName, Map<String, Object> params,
                                     Class resultClass) {
        String sql = queries.get(fileName);
        if(sql == null){
            try {
                IOUtilities.readSourceFile(SQL_PATH + fileName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            queries.put(fileName, sql);
        }


        Query query = persistenceManager.newQuery("javax.jdo.query.SQL", sql);
        if (resultClass != null) {
            query.setClass(resultClass);
        }
        return query.executeWithMap(params);
    }
}
