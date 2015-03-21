package com.tiksem.pq.db;

import com.tiksem.mysqljava.MysqlObjectMapper;
import com.tiksem.pq.http.HttpUtilities;
import com.utils.framework.io.IOUtilities;
import com.utils.framework.strings.Strings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by CM on 12/5/2014.
 */
public class SqlFileExecutor {
    private static final String SQL_PATH = HttpUtilities.getWarClassesPath() + "/sql/";
    private static Pattern PRE_ARG_PATTERN = Pattern.compile("::[^\\s]+");
    private static Pattern INCLUDE_PATTERN = Pattern.compile("#include\\('.+'\\)");

    private MysqlObjectMapper mapper;
    private static Map<String, String[]> queries = new ConcurrentHashMap<String, String[]>();

    public SqlFileExecutor(MysqlObjectMapper mapper) {
        this.mapper = mapper;
    }

    private String applyPreArgsReplacement(String string, String fileName, Map<String, Object> args) {
        while (true) {
            Matcher matcher = PRE_ARG_PATTERN.matcher(string);
            if(matcher.find()){
                String placeholder = matcher.group();
                Object value = args.get(placeholder.substring(2));
                if(value == null){
                    throw new IllegalArgumentException("Error while executing sql file " + fileName + ". " +
                            "The value of " + placeholder + " was not provided");
                }

                String valueAsString = value.toString();
                valueAsString = applyPreArgsReplacement(valueAsString, fileName, args);

                string = Strings.replace(string, matcher, valueAsString);
            } else {
                return string;
            }
        }
    }

    private String[] getSqls(String fileName, Map<String, Object> args) {
        return getSqls(fileName, args, false);
    }

    private String[] getSqls(String fileName, Map<String, Object> args, boolean included) {
        String[] sqls = queries.get(fileName);
        if(sqls == null){
            try {
                String filePath = fileName;
                if (FilenameUtils.getPrefixLength(fileName) == 0) {
                    filePath = SQL_PATH + fileName;
                }
                final File root = new File(filePath).getParentFile();
                String script = IOUtilities.readStringFromUrl(filePath);
                script = Strings.regexpReplace(script, INCLUDE_PATTERN, new Strings.Replacer() {
                    @Override
                    public String getReplacement(String source) {
                        String includeFileRelativePath = Strings.getFirstStringBetweenQuotes(source, "'");
                        String includeFileAbsolutePath = new File(root, includeFileRelativePath).getAbsolutePath();
                        return getSqls(includeFileAbsolutePath, null, true)[0];
                    }
                });

                if (!included) {
                    script = script.replaceAll("[;\\s]*$", "");
                    sqls = script.split(";\\n*");
                } else {
                    sqls = new String[]{script};
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            queries.put(fileName, sqls);
        }

        if (args != null) {
            sqls = sqls.clone();
            int i = 0;
            for(String sql : sqls){
                sqls[i++] = applyPreArgsReplacement(sql, fileName, args);
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
