package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import flow.Flow;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.PhotoMessageView;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;
import ru.korniltsev.telegram.photoview.PhotoView;

class PhotoMessageVH extends BaseAvatarVH {
    private final PhotoMessageView image;


    public PhotoMessageVH(View itemView, final Adapter adapter) {
        super(itemView, adapter);
        image = (PhotoMessageView) itemView.findViewById(R.id.image);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MessageItem item = (MessageItem) adapter.getItem(getPosition());
                TdApi.MessagePhoto photo = (TdApi.MessagePhoto) item.msg.message;
                Flow.get(v.getContext())
                        .set(new PhotoView(photo.photo, item.msg.id, item.msg.chatId));
            }
        });
    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
        super.bind(item, lastReadOutbox);
        TdApi.Message msg = ((MessageItem) item).msg;
        final TdApi.Photo photo = ((TdApi.MessagePhoto) msg.message).photo;
        image.load(photo, msg);


    }
}
