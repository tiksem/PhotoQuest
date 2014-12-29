package com.tiksem.pq.db;

import com.tiksem.mysqljava.MysqlObjectMapper;
import com.tiksem.pq.http.HttpUtilities;
import com.utils.framework.io.IOUtilities;
import org.apache.commons.io.IOUtils;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by CM on 12/5/2014.
 */
public class SqlFileExecutor {
    private static final String SQL_PATH = HttpUtilities.getWarClassesPath() + "/sql/";
    private MysqlObjectMapper mapper;
    private Map<String, String> queries = new HashMap<String, String>();

    public SqlFileExecutor(MysqlObjectMapper mapper) {
        this.mapper = mapper;
    }

    private String getSql(String fileName) {
        String sql = queries.get(fileName);
        if(sql == null){
            try {
                sql = IOUtilities.readStringFromUrl(SQL_PATH + fileName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            queries.put(fileName, sql);
        }

        return sql;
    }

    public <T> List<T> executeSQLQuery(String fileName,
                          Map<String, Object> params,
                          Class<T> resultClass,
                          List<String> foreigns) {
        String sql = getSql(fileName);
        return mapper.executeSQLQuery(sql, params, resultClass, foreigns);
    }
}
