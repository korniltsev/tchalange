package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.GeoPointView;
import ru.korniltsev.telegram.chat.debug.CustomCeilLayout;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;

public class GeoPointVH extends RealBaseVH {

    private final GeoPointView map;
    private final CustomCeilLayout root;

    public GeoPointVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        root = (CustomCeilLayout) itemView;
        map = (GeoPointView) adapter.getViewFactory().inflate(R.layout.chat_item_geo, root, false);
        root.addContentView(map);

    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
        TextMessageVH.newBind(root, adapter, item, lastReadOutbox);
        TdApi.Message msg = ((MessageItem) item).msg;
        map.set((TdApi.MessageLocation) msg.message);
    }
}
