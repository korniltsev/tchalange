package ru.korniltsev.telegram.chat.adapter;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.debug.CustomCeilLayout;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.core.emoji.EmojiTextView;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;
import ru.korniltsev.telegram.core.utils.Colors;

public class TextMessageVH extends RealBaseVH {

    private final CustomCeilLayout root;
    private final EmojiTextView message;

    //    private final TextView message;

    public TextMessageVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        root = ((CustomCeilLayout) itemView);

        message = new EmojiTextView(itemView.getContext());
        message.setTextColor(Color.BLACK);
        message.setPadding(0, 0, root.calc.dp(8), 0);
        message.setMovementMethod(LinkMovementMethod.getInstance());
        applyTextStyle(message);
        root.addContentView(message);

        final Resources resources = itemView.getContext().getResources();
        Drawable[] ds = new Drawable[3];
        ds[STATE_IC_UNREAD] = resources.getDrawable(R.drawable.ic_unread);
        ds[STATE_IC_CLOCK] = resources.getDrawable(R.drawable.ic_clock);
        ds[STATE_IC_NULL] = null;
        root.iconRight2.init(ds);

    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
        newBind(root, adapter, item, lastReadOutbox);
        //        super.bind(item, lastReadOutbox);
        TdApi.Message rawMsg = ((MessageItem) item).msg;
        //
        TdApi.MessageContent msg = rawMsg.message;
        TdApi.MessageText text = (TdApi.MessageText) msg;
        message.setText(text.textWithSmilesAndUserRefs);
    }

    public static void newBind(CustomCeilLayout root, Adapter adapter, ChatListItem item, long lastReadOutbox) {
        TdApi.Message msg = ((MessageItem) item).msg;
        TdApi.User user = adapter.getUserHolder().getUser(msg.fromId);
        String print = BaseAvatarVH.format(msg);
        root.setTime(print);

        if (user != null) {
            //            avatar.loadAvatarFor(user);
            root.avatarView.loadAvatarFor(user);
            String name = AppUtils.uiName(user, root.getContext());
//            root.nick.setText(name);
            root.setNick(name);
        } else {
            root.setNick("");
        }
        //else {
//
//        }


//        root.time.setText(print);

        switch (adapter.chat.getMessageState(msg, lastReadOutbox, adapter.myId)) {
            case RxChat.MESSAGE_STATE_READ:
                root.iconRight2.setSate(STATE_IC_NULL);

                //root.iconRight.setVisibility(View.INVISIBLE);
                break;
            case RxChat.MESSAGE_STATE_SENT:
                root.iconRight2.setSate(STATE_IC_UNREAD);
//                root.iconRight.setImageResource(R.drawable.ic_unread);
//                root.iconRight.setVisibility(View.VISIBLE);
                break;
            case RxChat.MESSAGE_STATE_NOT_SENT:
                root.iconRight2.setSate(STATE_IC_CLOCK);
//                root.iconRight.setImageResource(R.drawable.ic_clock);
//                root.iconRight.setVisibility(View.VISIBLE);
                break;
        }
    }

    public static void applyTextStyle(TextView text) {
        text.setAutoLinkMask(Linkify.WEB_URLS);

        text.setLinkTextColor(Colors.USER_NAME_COLOR);//todo specify in theme/style
    }
}
