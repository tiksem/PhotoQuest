package com.tiksem.pq.data.response.android;

import com.tiksem.pq.data.Action;
import com.tiksem.pq.data.Dialog;
import com.utils.framework.CollectionUtils;

import java.util.Collection;

/**
 * Created by CM on 2/21/2015.
 */
public class MobileDialogList {
    public Collection<MobileDialog> dialogs;

    public MobileDialogList(Collection<Dialog> dialogs) {
        this.dialogs = CollectionUtils.transform(dialogs,
                new CollectionUtils.Transformer<Dialog, MobileDialog>() {
                    @Override
                    public MobileDialog get(Dialog dialog) {
                        return new MobileDialog(dialog);
                    }
                });
    }
}
