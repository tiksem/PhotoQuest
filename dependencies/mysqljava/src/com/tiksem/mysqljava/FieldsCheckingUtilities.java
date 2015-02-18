package com.tiksem.mysqljava;

import com.tiksem.mysqljava.annotations.*;
import com.tiksem.mysqljava.exceptions.*;
import com.utils.framework.Reflection;
import com.utils.framework.strings.Strings;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.regex.Pattern;

/**
 * User: Tikhonenko.S
 * Date: 22.04.2014
 * Time: 18:31
 */
public class FieldsCheckingUtilities {
    public static final String CHECK_NAME_FIELD_PATTERN_STRING = "\\w+";
    public static final String CHECK_SENTENCE_FIELD_PATTERN_STRING = "([a-zA-Z] )*[a-zA-Z]";
    private static final Pattern CHECK_NAME_FIELD_PATTERN = Pattern.compile(CHECK_NAME_FIELD_PATTERN_STRING,
            Pattern.UNICODE_CHARACTER_CLASS);

    public static final String CHECK_LOGIN_FIELD_PATTERN_STRING = "[a-zA-Z_][a-zA-Z_0-9]+";
    private static final Pattern CHECK_LOGIN_FIELD_PATTERN = Pattern.compile(CHECK_LOGIN_FIELD_PATTERN_STRING);

    public static final String CHECK_PASSWORD_FIELD_PATTERN_STRING =
            ".+";
    private static final Pattern CHECK_PASSWORD_FIELD_PATTERN = Pattern.compile(CHECK_PASSWORD_FIELD_PATTERN_STRING);

    public static boolean checkNameField(String name){
        return CHECK_NAME_FIELD_PATTERN.matcher(name).matches();
    }

    public static String getFixedNameField(String value, Class aClass, String fieldName) {
        value = value.toLowerCase();

        if(!checkNameField(value)){
            throw new NameFieldPatternException(aClass.getName(), fieldName, value);
        }

        value = Strings.capitalize(value).toString();

        return value;
    }

    public static void fixNameField(Field field, Object object) {
        Class clazz = field.getType();
        NameField nameField = field.getAnnotation(NameField.class);

        if(nameField != null){
            if(!String.class.isAssignableFrom(clazz)){
                throw new NameFieldTypeMismatchException(field, clazz);
            }

            String value = (String) Reflection.getFieldValueUsingGetter(object, field);
            if(value == null || value.isEmpty()){
                return;
            }

            value = getFixedNameField(value, clazz, field.getName());
            Reflection.setFieldValueUsingSetter(object, field, value);
        }
    }

    private static void checkNotNullField(Field field, Object object) {
        NotNull notNull = field.getAnnotation(NotNull.class);

        if(notNull != null){
            Object value = Reflection.getFieldValueUsingGetter(object, field);
            if(value == null){
                throw new NullPointerException(field.getName() + " should not be null");
            }
        }
    }

    private static void checkLogin(Field field, Object object) {
        Class clazz = field.getType();
        Login loginField = field.getAnnotation(Login.class);

        if(loginField != null){
            if(!String.class.isAssignableFrom(clazz)){
                throw new IllegalArgumentException("Login field should be string");
            }

            String value = (String) Reflection.getFieldValueUsingGetter(object, field);
            checkLoginThrow(value);
        }
    }

    private static void checkEmail(Field field, Object object) {
        Class clazz = field.getType();
        Email email = field.getAnnotation(Email.class);

        if(email != null){
            if(!String.class.isAssignableFrom(clazz)){
                throw new IllegalArgumentException("Email field should be string");
            }

            String value = (String) Reflection.getFieldValueUsingGetter(object, field);
            validateEmailAddress(value);
        }
    }

    private static void checkPassword(Field field, Object object) {
        Class clazz = field.getType();
        Password passwordField = field.getAnnotation(Password.class);

        if(passwordField != null){
            if(!String.class.isAssignableFrom(clazz)){
                throw new IllegalArgumentException("Password field should be string");
            }

            String value = (String) Reflection.getFieldValueUsingGetter(object, field);
            checkPasswordThrow(value);
        }
    }

    public static void fixAndCheckFields(Object object){
        Class clazz = object.getClass();
        List<Field> fields = Reflection.getAllFieldsOfClass(clazz);

        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                fixNameField(field, object);
                checkLogin(field, object);
                checkPassword(field, object);
                checkEmail(field, object);
                checkNotNullField(field, object);
            }
        }

        List<Method> methods = Reflection.getAllMethods(object);
        for (Method method : methods) {
            OnPrepareForStorage onPrepareForStorage = method.getAnnotation(OnPrepareForStorage.class);
            if(onPrepareForStorage != null){
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
            fixAndCheckFields(object);
        }
    }

    public static boolean checkLogin(String login) {
        return CHECK_LOGIN_FIELD_PATTERN.matcher(login).matches();
    }

    public static void checkLoginThrow(String login) {
        if(!checkLogin(login)){
            throw new LoginPatternException();
        }
    }

    public static void checkPasswordThrow(String password) {
        if(!checkPassword(password)){
            throw new PasswordPatternException();
        }
    }

    public static boolean checkPassword(String password) {
        return CHECK_PASSWORD_FIELD_PATTERN.matcher(password).matches();
    }

    public static void checkLoginAndPassword(String login, String password) {
        if(!checkLogin(login)){
            throw new LoginPatternException();
        }

        if(!checkPassword(password)){
            throw new PasswordPatternException();
        }
    }

    public static void validateEmailAddress(String email) {
        if(!isValidEmailAddress(email)){
            throw new InvalidEmailException(email);
        }
    }

    public static boolean isValidEmailAddress(String email) {
        boolean result = true;
        try {
            InternetAddress emailAddress = new InternetAddress(email);
            emailAddress.validate();
        } catch (AddressException ex) {
            result = false;
        }
        return result;
    }
}
