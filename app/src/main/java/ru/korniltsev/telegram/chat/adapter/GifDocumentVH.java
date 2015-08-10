package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.GifView;
import ru.korniltsev.telegram.chat.adapter.view.VideoView;
import ru.korniltsev.telegram.chat.debug.CustomCeilLayout;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;

public class GifDocumentVH extends BaseAvatarVH {
    private final GifView video;

    public GifDocumentVH(CustomCeilLayout itemView, final Adapter adapter) {
        super(itemView, adapter);

        video = (GifView) adapter.getViewFactory().inflate(R.layout.chat_item_gif, root, false);
        root.addContentView(video);

    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
        super.bind(item, lastReadOutbox);


        TdApi.Message rawMsg = ((MessageItem) item).msg;

        TdApi.MessageDocument msg = (TdApi.MessageDocument) rawMsg .message;
        video.set(msg.document);


    }
}
