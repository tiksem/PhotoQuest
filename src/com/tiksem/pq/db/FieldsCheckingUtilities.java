package com.tiksem.pq.db;

import com.tiksem.pq.data.annotations.FixFieldsMethod;
import com.tiksem.pq.data.annotations.NameField;
import com.tiksem.pq.db.exceptions.NameFieldPatternException;
import com.tiksem.pq.db.exceptions.NameFieldTypeMismatchException;
import com.tiksem.pq.db.exceptions.PasswordPatternException;
import com.tiksem.pq.db.exceptions.UserNamePatternException;
import com.utils.framework.strings.Strings;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Pattern;

/**
 * User: Tikhonenko.S
 * Date: 22.04.2014
 * Time: 18:31
 */
public class FieldsCheckingUtilities {
    public static final String CHECK_NAME_FIELD_PATTERN_STRING = "[a-zA-Z]+";
    public static final String CHECK_SENTENCE_FIELD_PATTERN_STRING = "([a-zA-Z] )*[a-zA-Z]";
    private static final Pattern CHECK_NAME_FIELD_PATTERN = Pattern.compile(CHECK_NAME_FIELD_PATTERN_STRING);

    public static final String CHECK_LOGIN_FIELD_PATTERN_STRING = "[a-zA-Z_][a-zA-Z_0-9]+";
    private static final Pattern CHECK_LOGIN_FIELD_PATTERN = Pattern.compile(CHECK_LOGIN_FIELD_PATTERN_STRING);

    public static final String CHECK_PASSWORD_FIELD_PATTERN_STRING =
            "^(?=.*\\d)(?=.*[a-zA-Z]).{4,8}$";
    private static final Pattern CHECK_PASSWORD_FIELD_PATTERN = Pattern.compile(CHECK_PASSWORD_FIELD_PATTERN_STRING);

    public static boolean checkNameField(String name){
        return CHECK_NAME_FIELD_PATTERN.matcher(name).matches();
    }

    public static void fixNameField(Field field, Object object) {
        Class clazz = field.getType();
        NameField nameField = field.getAnnotation(NameField.class);

        if(nameField != null){
            if(!String.class.isAssignableFrom(clazz)){
                throw new NameFieldTypeMismatchException(field, clazz);
            }

            try {
                String value = (String) field.get(object);
                if(value == null || value.isEmpty()){
                    return;
                }

                value = value.toLowerCase();

                if(!checkNameField(value)){
                    throw new NameFieldPatternException(clazz.getName(), field.getName(), value);
                }

                value = Strings.capitalize(value).toString();
                field.set(object, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void fixFields(Object object){
        Class clazz = object.getClass();
        Field[] fields = clazz.getFields();

        for (Field field : fields) {
            field.setAccessible(true);
            fixNameField(field, object);
        }

        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            FixFieldsMethod fixFieldsMethod = method.getAnnotation(FixFieldsMethod.class);
            if(fixFieldsMethod != null){
                try {
                    method.setAccessible(true);
                    method.invoke(object);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void fixFields(List<Object> objects) {
        for(Object object : objects){
            fixFields(object);
        }
    }

    public static boolean checkLogin(String login) {
        return CHECK_LOGIN_FIELD_PATTERN.matcher(login).matches();
    }

    public static boolean checkPassword(String password) {
        return CHECK_PASSWORD_FIELD_PATTERN.matcher(password).matches();
    }

    public static void checkLoginAndPassword(String login, String password) {
        if(!checkLogin(login)){
            throw new UserNamePatternException();
        }

        if(!checkPassword(password)){
            throw new PasswordPatternException();
        }
    }
}
