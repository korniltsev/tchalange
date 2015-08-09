package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.ForwardedMessageView;
import ru.korniltsev.telegram.chat.debug.CustomCeilLayout;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;
import ru.korniltsev.telegram.core.views.AvatarView;
import ru.korniltsev.telegram.common.AppUtils;

class ForwardedTextMessageVH extends RealBaseVH {

    private final TextView text;
    private final TextView message_time;
    private final TextView nick;
    private final AvatarView avatar;
    private final CustomCeilLayout root;

    public ForwardedTextMessageVH(CustomCeilLayout itemView, Adapter adapter) {
        super(itemView, adapter);
        this.root = itemView;
        root.setBottomMarginEnabled(false);
        ForwardedMessageView contentView = (ForwardedMessageView) adapter.getViewFactory()
                .inflate(R.layout.chat_item_message_forward, root, false);
        root.addContentView(contentView);
        contentView.disableBlueMargin();

        text = ((TextView) contentView.findViewById(R.id.forward_text));
        TextMessageVH.applyTextStyle(text);
        message_time = ((TextView) contentView.findViewById(R.id.forward_time));
        nick = ((TextView) contentView.findViewById(R.id.forward_nick));
        BaseAvatarVH.colorizeNick(nick);

        avatar = ((AvatarView) itemView.findViewById(R.id.forward_avatar));
    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
        TextMessageVH.newBind(root, adapter, item, lastReadOutbox);

        TdApi.Message rawMsg = ((MessageItem) item).msg;
        TdApi.MessageContent msg = rawMsg.message;
        TdApi.MessageText text = (TdApi.MessageText) msg;
        this.text.setText(text.textWithSmilesAndUserRefs);



        TdApi.User user = adapter.getUserHolder().getUser(rawMsg.forwardFromId);
        avatar.loadAvatarFor(user);
        nick.setText(
                AppUtils.uiName(user, itemView.getContext()));
        message_time.setText(BaseAvatarVH.format(rawMsg));



    }
}
