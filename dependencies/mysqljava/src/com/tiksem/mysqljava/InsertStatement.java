package com.tiksem.mysqljava;

import com.tiksem.mysqljava.annotations.AddingDate;
import com.tiksem.mysqljava.annotations.ModificationDate;
import com.tiksem.mysqljava.annotations.OnPrepareForStorage;
import com.tiksem.mysqljava.annotations.PrimaryKey;
import com.tiksem.mysqljava.help.SqlGenerationUtilities;
import com.utils.framework.Reflection;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * Created by CM on 1/4/2015.
 */
public class InsertStatement extends BatchStatement {
    public InsertStatement(List<Object> objects) {
        super(objects);
    }

    private boolean ignore;

    public boolean isIgnore() {
        return ignore;
    }

    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
    }

    @Override
    protected void onStatementExecutionFinished(List<Object> objects, NamedParameterStatement statement) {
        try {
            if (!ignore) {
                if (objects.size() == 1) {
                    ResultSet generatedKeys = statement.getGeneratedKeys();
                    for(Object object : objects){
                        Field field = Reflection.getFieldWithAnnotation(object.getClass(), PrimaryKey.class);
                        if (field != null && SqlGenerationUtilities.isInt(field)) {
                            generatedKeys.next();
                            Object id = generatedKeys.getObject(1);
                            SqlGenerationUtilities.setObjectId(object, id);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected NamedParameterStatement createStatement(Connection connection, String sql) throws SQLException {
        return new NamedParameterStatement(connection, sql, Statement.RETURN_GENERATED_KEYS);
    }

    @Override
    protected StatementInfo prepareStatementForObject(Object object) {
        Class<?> aClass = object.getClass();

        long currentTimeMillis = System.currentTimeMillis();
        Reflection.executeMethodsWithAnnotation(object, OnPrepareForStorage.class);
        FieldsCheckingUtilities.fixAndCheckFields(object);
        Reflection.setValuesOfFieldsWithAnnotationIfNull(object, currentTimeMillis,
                AddingDate.class);

        Reflection.setValuesOfFieldsWithAnnotation(object, currentTimeMillis,
                ModificationDate.class);

        List<Field> fields = SqlGenerationUtilities.getFieldsExcludingPrimaryKey(aClass);
        Field primaryKey = Reflection.getFieldWithAnnotation(aClass, PrimaryKey.class);
        if(primaryKey != null){
            PrimaryKey key = primaryKey.getAnnotation(PrimaryKey.class);
            if(!key.autoincrement() || !SqlGenerationUtilities.isInt(primaryKey)){
                fields.add(primaryKey);
            }
        }

        Map<String, Object> args = ResultSetUtilities.getArgs(object, fields);
        String sql = SqlGenerationUtilities.insert(object, fields, false, ignore);

        StatementInfo info = new StatementInfo();
        info.args = args;
        info.sql = sql;
        return info;
    }
}
