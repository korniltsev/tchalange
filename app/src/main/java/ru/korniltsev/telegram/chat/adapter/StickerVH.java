package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.StickerView;
import ru.korniltsev.telegram.chat.debug.CustomCeilLayout;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;

import static ru.korniltsev.telegram.chat.adapter.TextMessageVH.newBind;

class StickerVH extends BaseAvatarVH {
    final StickerView image;

    public StickerVH( CustomCeilLayout itemView, Adapter adapter) {
        super(itemView, adapter);
        image = (StickerView) adapter.getViewFactory().inflate(R.layout.chat_item_sticker, itemView, false);
        root.addContentView(image);
    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
        super.bind(item, lastReadOutbox);

        TdApi.Message msg = ((MessageItem) item).msg;
        TdApi.MessageSticker sticker = (TdApi.MessageSticker) msg.message;

        image.bind(sticker.sticker);

    }
}
