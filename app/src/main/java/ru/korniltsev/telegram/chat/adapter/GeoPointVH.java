package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.GeoPointView;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;

public class GeoPointVH extends BaseAvatarVH {

    private final GeoPointView map;

    public GeoPointVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        map = (GeoPointView) itemView.findViewById(R.id.geo_point);
    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
        super.bind(item, lastReadOutbox);
        TdApi.Message msg = ((MessageItem) item).msg;
        map.set((TdApi.MessageLocation) msg.message);
    }
}
