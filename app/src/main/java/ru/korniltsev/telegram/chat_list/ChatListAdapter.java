package ru.korniltsev.telegram.chat_list;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import ru.korniltsev.telegram.chat.adapter.ChatPhotoChangedVH;
import ru.korniltsev.telegram.chat.adapter.SingleTextViewVH;
import ru.korniltsev.telegram.chat_list.view.ChatListCell;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.recycler.BaseAdapter;
import ru.korniltsev.telegram.core.rx.ChatDB;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.core.rx.UserHolder;
import ru.korniltsev.telegram.core.utils.Colors;
import ru.korniltsev.telegram.core.views.AvatarView;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.AppUtils;
import rx.functions.Action1;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ChatListAdapter extends BaseAdapter<TdApi.Chat, ChatListAdapter.VH> {

    public static final ColorStateList COLOR_SYSTEM = ColorStateList.valueOf(0xff6b9cc2);

    private final Context ctx;
    private final int myId;
    private final Action1<TdApi.Chat> clicker;
    private final Resources res;
    private ColorStateList COLOR_TEXT = ColorStateList.valueOf(0xff8a8a8a);
    final ChatDB chatDb;
    final UserHolder userHolder;

    public ChatListAdapter(Context ctx, int myId, Action1<TdApi.Chat> clicker, ChatDB chat, UserHolder userHolder) {
        super(ctx);
        this.ctx = ctx;
        this.myId = myId;
        this.clicker = clicker;
        this.chatDb = chat;
        this.userHolder = userHolder;
        setHasStableIds(true);
        res = ctx.getResources();
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        final View v = getViewFactory().inflate(R.layout.chat_list_item_chat, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        TdApi.Chat chat = getItem(position);
        TdApi.MessageContent message = chat.topMessage.message;

        holder.time.setText(chat.topMessage.dateFormatted);
        holder.cell.setTime(chat.topMessage.dateFormatted);

        if (chat.type instanceof TdApi.PrivateChatInfo){
            //name
            TdApi.User u = ((TdApi.PrivateChatInfo) chat.type).user;
            final String uiName = AppUtils.uiName(u, ctx);
            holder.cell.setTitle(uiName);
            holder.name.setText(uiName);
            //group_icon
            holder.iconGroupChat.setVisibility(View.GONE);
            holder.cell.setDrawGroupChatIcon(false);
        } else {
            //name
            TdApi.GroupChatInfo group = (TdApi.GroupChatInfo) chat.type;
            holder.name.setText(group.groupChat.title);
            holder.cell.setTitle(group.groupChat.title);
            //group_icon
            holder.iconGroupChat.setVisibility(View.VISIBLE);
            holder.cell.setDrawGroupChatIcon(true);
        }





        if (message instanceof TdApi.MessageText) {
            TdApi.MessageText text = (TdApi.MessageText) message;
            holder.message.setText(text.textWithSmilesAndUserRefs);
            holder.message.setTextColor(COLOR_TEXT);
        } else {
            CharSequence t = getSystemText(message, chat.topMessage);
            holder.message.setText(t.toString());//todo object allocations!
            holder.message.setTextColor(COLOR_SYSTEM);
        }
        if (chat.unreadCount > 0){
            holder.iconBottom.setVisibility(View.INVISIBLE);
            holder.iconBottom.setBackgroundResource(R.drawable.ic_badge);
//            holder.iconBottom.setText(String.valueOf(chat.unreadCount));
        } else {
            holder.iconBottom.setVisibility(View.GONE);
        }
        holder.cell.setUnreadCount(chat.unreadCount);

        RxChat rxChat = chatDb.getRxChat(chat.id);
        int msgState = rxChat.getMessageState(chat.topMessage, chat.lastReadOutboxMessageId, myId);
        switch (msgState){
            case RxChat.MESSAGE_STATE_READ:
                holder.iconTop.setVisibility(View.GONE);
                holder.cell.iconTop.setSate(ChatListCell.STATE_IC_NULL);
                break;
            case RxChat.MESSAGE_STATE_SENT:
                holder.iconTop.setImageResource(R.drawable.ic_unread);
                holder.iconTop.setVisibility(View.VISIBLE);
                holder.cell.iconTop.setSate(ChatListCell.STATE_IC_UNREAD);
                break;
            case RxChat.MESSAGE_STATE_NOT_SENT:
                holder.iconTop.setImageResource(R.drawable.ic_clock);
                holder.iconTop.setVisibility(View.VISIBLE);
                holder.cell.iconTop.setSate(ChatListCell.STATE_IC_CLOCK);
                break;
        }

        loadAvatar(holder, chat);
    }

    private CharSequence getSystemText(TdApi.MessageContent m, TdApi.Message topMessage) {

        if (m instanceof TdApi.MessageAudio) {
            return ctx.getString(R.string.Аудио);
        } else if (m instanceof TdApi.MessageDocument){
            TdApi.Document doc = ((TdApi.MessageDocument) m).document;
            if (TextUtils.isEmpty(doc.fileName)) {
                return ctx.getString(R.string.file);
            } else {
                return doc.fileName;
            }
        } else if (m instanceof TdApi.MessageSticker) {
            return ctx.getString(R.string.sticker);
        } else if (m instanceof TdApi.MessagePhoto) {
            return ctx.getString(R.string.photo);
        } else if (m instanceof TdApi.MessageVideo) {
            return ctx.getString(R.string.video);
        } else if (m instanceof TdApi.MessageLocation) {
            return ctx.getString(R.string.geo);
        } else if (m instanceof TdApi.MessageContact) {
            return ctx.getString(R.string.contact);
        } else if (m instanceof TdApi.MessageChatChangePhoto) {
            return ChatPhotoChangedVH.getTextFor(res, topMessage, userHolder);
        } else if (m instanceof TdApi.MessageWebPage) {
            return ctx.getString(R.string.web_page);
        } else if (m instanceof TdApi.MessageVoice) {
            return ctx.getString(R.string.message_voice);
        }
        else {
            return SingleTextViewVH.getTextFor(ctx, topMessage, m, userHolder);
        }

//        return null;
    }

    private void loadAvatar(VH holder, TdApi.Chat chat) {
//        holder.avatar.loadAvatarFor(chat);
        holder.cell.avatarView.loadAvatarFor(chat);
    }


    @Override
    public long getItemId(int position) {
        return getItem(position).id;
    }

    class VH extends RecyclerView.ViewHolder {
//        final AvatarView avatar;
        final TextView message;
        private final TextView name;
        private final TextView time;
        private final ImageView iconTop;
        private final View iconBottom;
        private final View iconGroupChat;
        private final ChatListCell cell;

        public VH(View itemView) {
            super(itemView);
            cell = ((ChatListCell) itemView.findViewById(R.id.new_cell));
//            avatar = (AvatarView) itemView.findViewById(R.id.avatar);
            message = (TextView) itemView.findViewById(R.id.message);
            name = (TextView) itemView.findViewById(R.id.name);
            time = (TextView) itemView.findViewById(R.id.time);
            iconTop = (ImageView) itemView.findViewById(R.id.icon_top);
            iconBottom = (View) itemView.findViewById(R.id.icon_bottom);
            iconGroupChat = itemView.findViewById(R.id.group_chat_icon);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clicker.call(getItem(getPosition()));
                }
            });
            message.setLinkTextColor(Colors.USER_NAME_COLOR);

        }
    }


}
