package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.StickerView;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;

class StickerVH extends BaseAvatarVH {
    final StickerView image;
    public StickerVH( View itemView, Adapter adapter) {
        super(itemView, adapter);
        image = (StickerView) itemView.findViewById(R.id.image);
    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
        super.bind(item, lastReadOutbox);
        TdApi.Message msg = ((MessageItem) item).msg;
        TdApi.MessageSticker sticker = (TdApi.MessageSticker) msg.message;
        image.bind(sticker.sticker);

    }
}
