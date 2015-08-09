package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.Adapter;
import ru.korniltsev.telegram.chat.adapter.RealBaseVH;
import ru.korniltsev.telegram.chat.adapter.TextMessageVH;
import ru.korniltsev.telegram.chat.adapter.view.AudioMessageView;
import ru.korniltsev.telegram.chat.debug.CustomCeilLayout;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.DaySeparatorItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;

public class AudioVH extends RealBaseVH {
    private final CustomCeilLayout root;
    private final AudioMessageView contentView;

    public AudioVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        root = ((CustomCeilLayout) itemView);
        root.setBottomMarginEnabled(false);
        contentView = (AudioMessageView) adapter.getViewFactory().inflate(R.layout.chat_item_real_audio, root, false);
        final DpCalculator dpCalculator = MyApp.from(itemView).dpCalculator;
        final int dp6 = dpCalculator.dp(6);
        contentView.setPadding(0, 0, 0, dp6);
        root.addContentView(contentView);
    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
        TextMessageVH.newBind(root, adapter, item, lastReadOutbox);
        final TdApi.MessageAudio a = (TdApi.MessageAudio) ((MessageItem) item).msg.message;
        contentView.bind(a);

    }
}
