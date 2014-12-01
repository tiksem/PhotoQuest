package com.tiksem.pq.data.response;

import com.utils.framework.google.places.AutoCompleteResult;

import java.util.List;

/**
 * Created by CM on 12/1/2014.
 */
public class LocationSuggestions {
    public List<AutoCompleteResult> suggestions;

    public LocationSuggestions(List<AutoCompleteResult> suggestions) {
        this.suggestions = suggestions;
    }
}
