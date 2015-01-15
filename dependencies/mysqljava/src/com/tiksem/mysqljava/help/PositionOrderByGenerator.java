package com.tiksem.mysqljava.help;

import com.utils.framework.Reflection;
import com.utils.framework.strings.Strings;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by CM on 1/15/2015.
 */
public class PositionOrderByGenerator {
    private String orderBy;
    private Class aClass;
    private List<Field> orderByFieldsOut;
    private boolean reverse;

    public boolean isReverse() {
        return reverse;
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    public PositionOrderByGenerator(String orderBy, Class aClass, List<Field> orderByFieldsOut) {
        this.orderBy = orderBy;
        this.aClass = aClass;
        this.orderByFieldsOut = orderByFieldsOut;
    }

    private String generateOrderByPair(String operator, String param) {
        return SqlGenerationUtilities.quotedClassName(aClass) + "." +
                param + operator + ":" + param;
    }

    public String generateWhere() {
        List<String[]> orderByParts = new ArrayList<String[]>();
        String[] orderCriteria = orderBy.split(", *");
        for(String order : orderCriteria){
            orderByParts.add(order.split(" +", 2));
        }

        return generateWhere(orderByParts, 0);
    }

    private String generateWhere(List<String[]> orderBy, int index) {
        if(index >= orderBy.size()){
            return "";
        }

        String[] args = orderBy.get(index);

        String param = args[0];
        orderByFieldsOut.add(Reflection.getFieldByNameOrThrow(aClass, param));

        boolean desc = args.length > 1 && args[1].equalsIgnoreCase("desc");
        if(reverse){
            desc = !desc;
        }

        String operator = desc ? ">" : "<";

        String part = generateOrderByPair(operator, param);

        String childWhere = generateWhere(orderBy, index + 1);
        if(Strings.isEmpty(childWhere)){
            return part;
        } else {
            return part + " OR (" +  generateOrderByPair("=", param) + " AND (" + childWhere + "))";
        }
    }
}
