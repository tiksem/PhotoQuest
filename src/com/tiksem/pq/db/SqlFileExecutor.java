package com.tiksem.pq.db;

import com.tiksem.mysqljava.MysqlObjectMapper;
import com.tiksem.pq.http.HttpUtilities;
import com.utils.framework.io.IOUtilities;
import com.utils.framework.strings.Strings;
import org.apache.commons.io.IOUtils;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by CM on 12/5/2014.
 */
public class SqlFileExecutor {
    private static final String SQL_PATH = HttpUtilities.getWarClassesPath() + "/sql/";
    private static Pattern PRE_ARG_PATTERN = Pattern.compile("::[^\\s]+");

    private MysqlObjectMapper mapper;
    private Map<String, String[]> queries = new HashMap<String, String[]>();

    public SqlFileExecutor(MysqlObjectMapper mapper) {
        this.mapper = mapper;
    }

    private String[] getSqls(String fileName, Map<String, Object> args) {
        String[] sqls = queries.get(fileName);
        if(sqls == null){
            try {
                String script = IOUtilities.readStringFromUrl(SQL_PATH + fileName);
                script = script.replaceAll("[;\\s]*$", "");
                sqls = script.split(";\\n*");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            queries.put(fileName, sqls);
        }

        if (args != null) {
            sqls = sqls.clone();
            int i = 0;
            for(String sql : sqls){
                List<String> argPlaceholders = Strings.findAll(sql, PRE_ARG_PATTERN);
                for(String placeholder : argPlaceholders){
                    Object value = args.get(placeholder.substring(2));
                    if(value == null){
                        throw new IllegalArgumentException("Error while executing sql file " + fileName + ". " +
                                "The value of " + placeholder + " was not provided");
                    }

                    sql = sql.replace(placeholder, value.toString());
                }
                sqls[i++] = sql;
            }
        }

        return sqls;
    }

    private String getSql(String fileName, Map<String, Object> args) {
        String[] sqls = getSqls(fileName, args);
        return sqls[0];
    }

    public long executeCountQuery(String fileName,
                                  Map<String, Object> params) {
        String sql = getSql(fileName, params);
        return mapper.executeCountQuery(sql, params);
    }

    public void executeNonSelectQuery(String fileName, Map<String, Object> args) {
        String[] sqls = getSqls(fileName, args);
        for (String sql : sqls) {
            mapper.executeNonSelectSQL(sql, args);
        }
    }

    public <T> List<T> executeSQLQuery(String fileName,
                          Map<String, Object> params,
                          Class<T> resultClass,
                          List<String> foreigns) {
        String sql = getSql(fileName, params);
        return mapper.executeSQLQuery(sql, params, resultClass, foreigns);
    }

    public <T> List<T> executeSQLQuery(String fileName,
                                       Map<String, Object> params,
                                       Class<T> resultClass) {
        String sql = getSql(fileName, params);
        return mapper.executeSQLQuery(sql, params, resultClass);
    }

    public <T> List<T> executeSQLQuery(String fileName,
                                      Class<T> resultClass) {
        String sql = getSql(fileName, null);
        return mapper.executeSQLQuery(sql, resultClass);
    }
}
