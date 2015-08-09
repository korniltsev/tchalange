package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.AudioMessageView;
import ru.korniltsev.telegram.chat.debug.CustomCeilLayout;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;

public class AudioVH2 extends RealBaseVH {
//    private final CustomCeilLayout root;
    private final AudioMessageView contentView;

    public AudioVH2(View itemView, Adapter adapter) {
        super(itemView, adapter);
//        root = ((CustomCeilLayout) itemView);

        contentView = (AudioMessageView)itemView.findViewById(R.id.audio_message_view);// adapter.getViewFactory().inflate(R.layout.chat_item_real_audio, root, false);

//        root.addContentView(contentView);
    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
//        TextMessageVH.newBind(root, adapter, item, lastReadOutbox);
        final TdApi.MessageAudio a = (TdApi.MessageAudio) ((MessageItem) item).msg.message;
        contentView.bind(a);

    }
}
