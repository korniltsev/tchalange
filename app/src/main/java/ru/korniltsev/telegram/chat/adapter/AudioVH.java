package ru.korniltsev.telegram.chat.adapter;

import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.AudioMessageView;
import ru.korniltsev.telegram.chat.debug.CustomCeilLayout;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;

public class AudioVH extends BaseAvatarVH {
    private final AudioMessageView contentView;

    public AudioVH(CustomCeilLayout itemView, Adapter adapter) {
        super(itemView, adapter);
        root.setBottomMarginEnabled(false);
        contentView = (AudioMessageView) adapter.getViewFactory().inflate(R.layout.chat_item_real_audio, root, false);
        final DpCalculator dpCalculator = MyApp.from(itemView).calc;
        final int dp6 = dpCalculator.dp(6);
        contentView.setPadding(0, 0, 0, dp6);
        root.addContentView(contentView);
    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
        super.bind(item, lastReadOutbox);
        final TdApi.Message msg = ((MessageItem) item).msg;
        final TdApi.MessageAudio a = (TdApi.MessageAudio) msg.message;
        contentView.bind(a, msg);
    }
}
