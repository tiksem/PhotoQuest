package com.tiksem.pq.data.response;

import com.tiksem.pq.data.Dialog;

import java.util.Collection;

/**
 * Created by CM on 11/15/2014.
 */
public class DialogsList {
    public Collection<Dialog> dialogs;

    public DialogsList(Collection<Dialog> dialogs) {
        this.dialogs = dialogs;
    }
}
