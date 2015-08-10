package ru.korniltsev.telegram.chat.adapter;

import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.VideoView;
import ru.korniltsev.telegram.chat.debug.CustomCeilLayout;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;

public class VideoVH extends BaseAvatarVH {
    private final VideoView video;

    public VideoVH(CustomCeilLayout itemView, final Adapter adapter) {
        super(itemView, adapter);
        video = (VideoView) adapter.getViewFactory().inflate(R.layout.chat_item_video, root, false);
        root.addContentView(video);
    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
        super.bind(item, lastReadOutbox);
        TdApi.Message rawMsg = ((MessageItem) item).msg;

        TdApi.MessageVideo msg = (TdApi.MessageVideo) rawMsg.message;
        video.set(msg.video);
    }
}
