package com.tiksem.mysqljava;

import com.utils.framework.CollectionUtils;
import com.utils.framework.Predicate;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by CM on 12/27/2014.
 */
public class SelectParams {
    public CollectionUtils.Transformer<String, String> whereTransformer;
    public String additionalWhereClosure;
    public String ordering;
    public OffsetLimit offsetLimit = new OffsetLimit();
    public List<String> foreignFieldsToFill = new ArrayList<String>();
    public FieldIncludePredicate includePredicate;
}
