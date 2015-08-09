package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.StickerView;
import ru.korniltsev.telegram.chat.debug.CustomCeilLayout;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;

import static ru.korniltsev.telegram.chat.adapter.TextMessageVH.newBind;

class StickerVH extends RealBaseVH {
    final StickerView image;
    private final CustomCeilLayout root;

    public StickerVH( CustomCeilLayout itemView, Adapter adapter) {
        super(itemView, adapter);
        this.root = itemView;
        image = (StickerView) adapter.getViewFactory().inflate(R.layout.chat_item_sticker, itemView, false);
        root.addContentView(image);

//        image = (StickerView) itemView.findViewById(R.id.image);
    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
        newBind(root, adapter, item, lastReadOutbox);

        TdApi.Message msg = ((MessageItem) item).msg;
        TdApi.MessageSticker sticker = (TdApi.MessageSticker) msg.message;

        image.bind(sticker.sticker);

    }
}
