package ru.korniltsev.telegram.chat.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.rx.UserHolder;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;

import static junit.framework.Assert.assertTrue;

public abstract class RealBaseVH extends RecyclerView.ViewHolder {
    public final Adapter adapter;
    public RealBaseVH(View itemView, Adapter adapter) {
        super(itemView);
        this.adapter = adapter;
    }

    public abstract void bind(ChatListItem item, long lastReadOutbox);



    public static String sGetNameForSenderOf(UserHolder uh, TdApi.Message item) {
        int fromId = item.fromId;
        assertTrue(fromId != 0);
        TdApi.User user = uh.getUser(fromId);
        return AppUtils.uiName(user, uh.getContext());
    }


}
