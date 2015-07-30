package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;
import ru.korniltsev.telegram.core.views.AvatarView;
import ru.korniltsev.telegram.common.AppUtils;

class ForwardedTextMessage2VH extends RealBaseVH {

//    private final TextView message;
    private final TextView text;
    private final TextView message_time;
    private final TextView nick;
    private final AvatarView avatar;

    public ForwardedTextMessage2VH(View itemView, Adapter adapter) {
        super(itemView, adapter);
//        message = ((TextView) itemView.findViewById(R.id.message));
        text = ((TextView) itemView.findViewById(R.id.forward_text));
        TextMessageVH.applyTextStyle(text);
        message_time = ((TextView) itemView.findViewById(R.id.forward_time));
        nick = ((TextView) itemView.findViewById(R.id.forward_nick));
        BaseAvatarVH.colorizeNick(nick);
        avatar = ((AvatarView) itemView.findViewById(R.id.forward_avatar));
    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
//        super.bind(item);
        TdApi.Message rawMsg = ((MessageItem) item).msg;
        TdApi.MessageContent msg = rawMsg.message;
        TdApi.MessageText text = (TdApi.MessageText) msg;
        this.text.setText(text.textWithSmilesAndUserRefs);



        TdApi.User user = adapter.getUserHolder().getUser(rawMsg.forwardFromId);
        avatar.loadAvatarFor(user);
        nick.setText(
                AppUtils.uiName(user, itemView.getContext()));
//        long forwardDateInMillis = Utils.dateToMillis(rawMsg.forwardDate);
//        long localTime = DateTimeZone.UTC.convertUTCToLocal(forwardDateInMillis);
        message_time.setText(BaseAvatarVH.format(rawMsg));



    }
}
