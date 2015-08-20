package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.AudioMessageView;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;

public class AudioVH2 extends RealBaseVH {
    private final AudioMessageView contentView;

    public AudioVH2(View itemView, Adapter adapter) {
        super(itemView, adapter);
        contentView = (AudioMessageView)itemView.findViewById(R.id.audio_message_view);// adapter.getViewFactory().inflate(R.layout.chat_item_real_audio, root, false);

    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
        final TdApi.Message msg = ((MessageItem) item).msg;
        final TdApi.MessageAudio a = (TdApi.MessageAudio) msg.message;
        contentView.bind(a, msg);

    }
}
