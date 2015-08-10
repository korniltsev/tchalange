package ru.korniltsev.telegram.chat.adapter;

import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.TextView;
import com.crashlytics.android.core.CrashlyticsCore;
import flow.Flow;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.Chat;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.rx.UserHolder;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;
import ru.korniltsev.telegram.core.views.AvatarView;
import ru.korniltsev.telegram.photoview.PhotoView;

import static ru.korniltsev.telegram.chat.adapter.SingleTextViewVH.userColor;

public class ChatPhotoChangedVH extends RealBaseVH {


    private final TextView text;
    private final AvatarView image;
    private final Resources res;

    public ChatPhotoChangedVH(View itemView, final Adapter adapter) {
        super(itemView, adapter);
        text = ((TextView) itemView.findViewById(R.id.text));
        image = ((AvatarView) itemView.findViewById(R.id.image));
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MessageItem item = (MessageItem) adapter.getItem(getPosition());
                TdApi.MessageChatChangePhoto change = (TdApi.MessageChatChangePhoto) item.msg.message;
                Flow.get(v.getContext())
                        .set(new PhotoView(change.photo));
            }
        });
        res = itemView.getResources();
    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
        TdApi.Message msg = ((MessageItem) item).msg;
        SpannableStringBuilder sb = getTextFor(res, msg, adapter.getUserHolder());
        //        String text =  this.text.getResources().getString(R.string.message_changed_group_photo, userName);
        this.text.setText(sb);
        TdApi.MessageChatChangePhoto changed = (TdApi.MessageChatChangePhoto) msg.message;
//        TdApi.PhotoSize smallSize = changed.photo.photos[0];
//        for (TdApi.PhotoSize photo : changed.photo.photos) {
//            if (photo.type.equals("a")) {
//                smallSize = photo;
//            }
//        }

        final Chat path = adapter.getChatPath();
        final TdApi.Chat orig = path.chat;
        //        final TdApi.Chat o = path.chat;// new TdApi.Chat();
        //сделать копию o
        if (!(orig.type instanceof TdApi.GroupChatInfo)) {
            logError("inconsistent state: trying to bind chat photo changed on private chat");
            return;
        }
        final TdApi.GroupChat origGroupChat = ((TdApi.GroupChatInfo) orig.type).groupChat;
        final TdApi.ProfilePhoto profilePhotoCopy = new TdApi.ProfilePhoto(origGroupChat.id, null, null);
        if (!match(profilePhotoCopy, changed.photo)){
            logError("could not match photo sizes");
            return;
        }
        final TdApi.GroupChat groupChatCopy = new TdApi.GroupChat(origGroupChat.id, origGroupChat.title, origGroupChat.participantsCount, profilePhotoCopy,origGroupChat.left);
        TdApi.GroupChatInfo chatInfoCopy = new TdApi.GroupChatInfo(groupChatCopy);

        final TdApi.Chat chatCopy = new TdApi.Chat(orig.id, orig.topMessage, orig.unreadCount, orig.lastReadInboxMessageId, orig.lastReadOutboxMessageId ,
                orig.notificationSettings, 0, chatInfoCopy);

        image.loadAvatarFor(chatCopy);


    }

    private void logError(String msg) {
        CrashlyticsCore.getInstance()
                .logException(new IllegalStateException(msg));
    }

    private boolean match(TdApi.ProfilePhoto to, TdApi.Photo fromChange) {
        //        GroupChat.photoSmall это "s" тип ?
        //                LikeMay 4, 2015 at 6:39 pm|Edit|Delete
        //
        //        Telegram Challenge
        final TdApi.PhotoSize small = get(fromChange, "a");
        final TdApi.PhotoSize big = get(fromChange, "c");

        if (small == null || big == null){
            return false;
        }
        to.small = small.photo;
        to.big = big.photo;
        return true;
    }

    @Nullable
    private TdApi.PhotoSize get(TdApi.Photo from, String type) {
        for (TdApi.PhotoSize photo : from.photos) {
            if (photo.type.equals(type)) {
                return photo;
            }
        }
        return null;
    }

    public static SpannableStringBuilder getTextFor(Resources res, TdApi.Message msg, UserHolder uh) {
        Spannable userName = userColor(sGetNameForSenderOf(uh, msg));
        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append(userName)
                .append(" ")
                .append(res.getString(R.string.message_changed_group_photo));
        return sb;
    }
}
