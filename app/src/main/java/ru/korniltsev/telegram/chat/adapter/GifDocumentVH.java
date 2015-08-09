package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.VideoView;
import ru.korniltsev.telegram.chat.debug.CustomCeilLayout;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;

public class GifDocumentVH extends RealBaseVH {
    private final VideoView video;
    private final CustomCeilLayout root;

    public GifDocumentVH(View itemView, final Adapter adapter) {
        super(itemView, adapter);

        root = (CustomCeilLayout) itemView;
        video = (VideoView) adapter.getViewFactory().inflate(R.layout.chat_item_video, root, false);
        root.addContentView(video);

    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {

        TextMessageVH.newBind(root, adapter, item, lastReadOutbox);

        TdApi.Message rawMsg = ((MessageItem) item).msg;

        TdApi.MessageDocument msg = (TdApi.MessageDocument) rawMsg .message;
        video.set(msg.document);


    }
}
