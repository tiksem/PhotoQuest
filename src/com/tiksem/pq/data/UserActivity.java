package com.tiksem.pq.data;

import com.tiksem.mysqljava.annotations.*;

/**
 * Created by CM on 2/13/2015.
 */
@Table
@MultipleIndexes(indexes = {
        @MultipleIndex(fields = {"userId", "addingDate"}),
        @MultipleIndex(fields = {"userId", "cityId", "addingDate"}),
        @MultipleIndex(fields = {"userId", "countryId", "addingDate"})
})
public class UserActivity {
    @AddingDate
    private Long addingDate;

    @Index
    private Long userId;

    @Index
    private Integer cityId;

    @Index
    private Integer countryId;

    @Stored
    private String address;
}
