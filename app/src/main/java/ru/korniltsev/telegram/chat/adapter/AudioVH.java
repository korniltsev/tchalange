package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.AudioMessageView;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;

public class AudioVH extends BaseAvatarVH {

    private final AudioMessageView audioView;

    public AudioVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        audioView = ((AudioMessageView) itemView.findViewById(R.id.audio));
    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
        super.bind(item, lastReadOutbox);
        TdApi.Message rawMsg = ((MessageItem) item).msg;
        TdApi.MessageVoice msg = (TdApi.MessageVoice) rawMsg.message;
        audioView.setAudio(msg.voice);
    }
}
