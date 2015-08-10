package ru.korniltsev.telegram.chat.adapter;

import android.view.LayoutInflater;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.VoiceMessageView;
import ru.korniltsev.telegram.chat.debug.CustomCeilLayout;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;

import static ru.korniltsev.telegram.chat.adapter.TextMessageVH.newBind;

public class VoicdVH extends BaseAvatarVH  {
    private final VoiceMessageView audioView;


    public VoicdVH(CustomCeilLayout itemView, Adapter adapter) {
        super(itemView, adapter);
        audioView = (VoiceMessageView) LayoutInflater.from(itemView.getContext())
                .inflate(R.layout.chat_item_audio, root, false);
        root.addContentView(audioView);


    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
        super.bind(item, lastReadOutbox);

        TdApi.Message rawMsg = ((MessageItem) item).msg;
        TdApi.MessageVoice msg = (TdApi.MessageVoice) rawMsg.message;
        audioView.setAudio(msg.voice);
    }
}
