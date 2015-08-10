package ru.korniltsev.telegram.chat.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.rx.UserHolder;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;
import ru.korniltsev.telegram.core.utils.Colors;
import ru.korniltsev.telegram.common.AppUtils;

//something we can draw as a single textView

//MessageGroupChatCreate extends MessageContent {
//        MessageChatChangeTitle extends MessageContent {
//        MessageChatDeleteParticipant extends MessageContent {
//        MessageChatAddParticipant extends MessageContent {
//        MessageDeleted extends MessageContent {
//        MessageChatDeletePhoto extends MessageContent {
//        MessageUnsupported extends MessageContent {




public class SingleTextViewVH extends RealBaseVH {
    private final TextView text;
    private Resources resources;

    public SingleTextViewVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        text = (TextView) itemView.findViewById(R.id.text);
        resources = itemView.getContext().getResources();
    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
        TdApi.Message msgRaw = ((MessageItem) item).msg;
        TdApi.MessageContent msg = msgRaw.message;
        CharSequence textFor = getTextFor(itemView.getContext(), msgRaw, msg, adapter.getUserHolder());
        text.setText(textFor);
    }

    public   static CharSequence getTextFor(Context ctx, TdApi.Message msgRaw, TdApi.MessageContent msg, UserHolder uh) {
        Resources res = ctx.getResources();
        if (msg instanceof TdApi.MessageChatChangeTitle) {
            TdApi.MessageChatChangeTitle create = (TdApi.MessageChatChangeTitle) msgRaw.message;
            Spannable creator = userColor(sGetNameForSenderOf(uh, msgRaw));
            Spannable title = userColor(create.title);

            SpannableStringBuilder sb = new SpannableStringBuilder();
            sb.append(creator)
                    .append(" ")
                    .append(res.getString(R.string.message_renamed_group))
                    .append(" ")
                    .append(title);
            return sb;
        } else if (msg instanceof TdApi.MessageGroupChatCreate) {
            TdApi.MessageGroupChatCreate create = (TdApi.MessageGroupChatCreate) msgRaw.message;
            Spannable creator = userColor(sGetNameForSenderOf(uh, msgRaw));
            Spannable title = userColor(create.title);

            SpannableStringBuilder sb = new SpannableStringBuilder();
            sb.append(creator)
                    .append(" ")
                    .append(res.getString(R.string.message_created_group))
                    .append(" ")
                    .append(title);
            return sb;
            //        text.setText(sb);
        } else if (msg instanceof TdApi.MessageChatDeleteParticipant) {
            TdApi.MessageChatDeleteParticipant kick = (TdApi.MessageChatDeleteParticipant) msgRaw.message;

            Spannable inviter = userColor(sGetNameForSenderOf(uh, msgRaw));
            Spannable newUser = userColor(AppUtils.uiName(kick.user, ctx));

            SpannableStringBuilder sb = new SpannableStringBuilder();
            if (kick.user.id == msgRaw.fromId) {
                sb.append(inviter)
                        .append(" ")
                        .append(res.getString(R.string.message_kicked_self));
            } else {
                sb.append(inviter)
                        .append(" ")
                        .append(res.getString(R.string.message_kicked))
                        .append(" ")
                        .append(newUser);
            }

            return sb;
            //        text.setText(sb);
        } else if (msg instanceof TdApi.MessageChatAddParticipant) {

            TdApi.MessageChatAddParticipant create = (TdApi.MessageChatAddParticipant) msgRaw.message;

            //        String inviter = ;
            Spannable inviter = userColor(
                    sGetNameForSenderOf(uh, msgRaw));
            Spannable newUser = userColor(
                    AppUtils.uiName(create.user, ctx));

            SpannableStringBuilder sb = new SpannableStringBuilder();
            sb.append(inviter)
                    .append(" ")
                    .append(res.getString(R.string.message_ivited))
                    .append(" ")
                    .append(newUser);

            return sb;
        } else if (msg instanceof TdApi.MessageDeleted) {
            return res.getString(R.string.message_deleted);
        } else if (msg instanceof TdApi.MessageChatDeletePhoto) {
            Spannable name = userColor(sGetNameForSenderOf(uh, msgRaw));
            SpannableStringBuilder sb = new SpannableStringBuilder();
            sb.append(name)
                    .append(" ")
                    .append(res.getString(R.string.message_removed_group_photo));
            return sb;
        } else if (msg instanceof TdApi.MessageChatJoinByLink) {
            Spannable name = userColor(sGetNameForSenderOf(uh, msgRaw));
            SpannableStringBuilder sb = new SpannableStringBuilder();
            sb.append(name)
                    .append(" ")
                    .append(res.getString(R.string.message_joined_by_link));
            return sb;
        } else{
            return res.getString(R.string.message_unsupported);
        }
    }


    public static Spannable userColor(String str){
        Spannable.Factory factory = Spannable.Factory.getInstance();
        Spannable spannable = factory.newSpannable(str);
        spannable.setSpan(new ForegroundColorSpan(Colors.USER_NAME_COLOR), 0, spannable.length(), 0);
        spannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, spannable.length(), 0);
        return spannable;
    }




}
