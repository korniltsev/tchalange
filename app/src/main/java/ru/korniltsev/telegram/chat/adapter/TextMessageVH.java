package ru.korniltsev.telegram.chat.adapter;

import android.graphics.Color;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;
import ru.korniltsev.telegram.core.utils.Colors;

public class TextMessageVH extends BaseAvatarVH {

    private final TextView message;

    public TextMessageVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        message = ((TextView) itemView.findViewById(R.id.message));

        message.setMovementMethod(LinkMovementMethod.getInstance());

        applyTextStyle(message);
        message.setTextColor(Color.BLACK);


    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
        super.bind(item, lastReadOutbox);
        TdApi.Message rawMsg = ((MessageItem) item).msg;

        TdApi.MessageContent msg = rawMsg.message;
        TdApi.MessageText text = (TdApi.MessageText) msg;
        message.setText(text.textWithSmilesAndUserRefs);


    }

    public static void applyTextStyle(TextView text) {
        text.setAutoLinkMask(Linkify.WEB_URLS | Linkify.PHONE_NUMBERS);
//        text.setAutoLinkMask(0);
        text.setLinkTextColor(Colors.USER_NAME_COLOR);//todo specify in theme/style
    }
}
