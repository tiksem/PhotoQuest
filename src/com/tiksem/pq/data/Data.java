package com.tiksem.pq.data;

import com.utils.framework.Reflection;

import javax.jdo.InstanceCallbacks;
import javax.jdo.annotations.NotPersistent;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by CM on 11/13/2014.
 */
public class Data implements InstanceCallbacks {
    @Override
    public void jdoPreClear() {

    }

    @Override
    public void jdoPreDelete() {

    }

    @Override
    public void jdoPostLoad() {
        List<Field> fields = Reflection.getFieldsWithAnnotations(getClass(), NotPersistent.class);
        for(Field field : fields){
            Reflection.setValueOfField(this, field, null);
        }
    }

    @Override
    public void jdoPreStore() {

    }
}
