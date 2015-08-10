package ru.korniltsev.telegram.chat.adapter;

import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.rx.items.BotInfoItem;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;

public class BotInfoVH extends RealBaseVH {


    private final TextView botInfo;
    private final View botInfoRoot;

    public BotInfoVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        botInfo = ((TextView) itemView.findViewById(R.id.bot_info_description));
        botInfoRoot = itemView.findViewById(R.id.bot_info_root);
        final DpCalculator calc = ObjectGraphService.getObjectGraph(itemView.getContext())
                .get(DpCalculator.class);
        botInfoRoot.setPadding(0,calc.dp(16), 0, 0);
        botInfo.setMovementMethod(LinkMovementMethod.getInstance());
        TextMessageVH.applyTextStyle(botInfo);
    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
        BotInfoItem b = (BotInfoItem) item;
        botInfo.setText(b.descriptionWithEmoji);
    }
}
