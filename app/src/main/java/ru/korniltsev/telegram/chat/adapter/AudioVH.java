package ru.korniltsev.telegram.chat.adapter;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.AudioMessageView;
import ru.korniltsev.telegram.chat.debug.CustomCeilLayout;
import ru.korniltsev.telegram.core.emoji.EmojiTextView;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;

import static ru.korniltsev.telegram.chat.adapter.TextMessageVH.newBind;

public class AudioVH extends RealBaseVH {
    private final CustomCeilLayout root;
    private final AudioMessageView audioView;

    //    private final AudioMessageView audioView;

    public AudioVH(CustomCeilLayout itemView, Adapter adapter) {
        super(itemView, adapter);
        root = ( itemView);
        audioView = (AudioMessageView) LayoutInflater.from(itemView.getContext())
                .inflate(R.layout.chat_item_audio, root, false);
        root.addContentView(audioView);

        final Resources resources = itemView.getContext().getResources();
        Drawable[] ds = new Drawable[3];
        ds[STATE_IC_UNREAD] = resources.getDrawable(R.drawable.ic_unread);
        ds[STATE_IC_CLOCK] = resources.getDrawable(R.drawable.ic_clock);
        ds[STATE_IC_NULL] = null;
        root.iconRight2.init(ds);
    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
//        super.bind(item, lastReadOutbox);
        newBind(root, adapter, item, lastReadOutbox);

        TdApi.Message rawMsg = ((MessageItem) item).msg;
        TdApi.MessageVoice msg = (TdApi.MessageVoice) rawMsg.message;
        audioView.setAudio(msg.voice);
    }
}
