package com.tiksem.pq.data;

import com.tiksem.mysqljava.annotations.*;

/**
 * Created by CM on 1/1/2015.
 */
@Table
@MultipleIndex(fields = {"userId", "actionId"}, isUnique = true)
public class Feed {
    @PrimaryKey
    private Long id;
    @Stored
    @NotNull
    private Long userId;
    @ForeignKey(parent = Action.class, field = "id")
    @NotNull
    private Long actionId;

    @ForeignValue(idField = "actionId")
    private Action action;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getActionId() {
        return actionId;
    }

    public void setActionId(Long actionId) {
        this.actionId = actionId;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
