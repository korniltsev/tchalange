package ru.korniltsev.telegram.profile.chat;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.recycler.BaseAdapter;
import ru.korniltsev.telegram.core.views.AvatarView;

import static ru.korniltsev.telegram.common.AppUtils.uiStatusColor;
import static ru.korniltsev.telegram.common.AppUtils.uiName;
import static ru.korniltsev.telegram.common.AppUtils.uiUserStatus;

public class ChatInfoAdapter extends BaseAdapter<ChatInfoAdapter.Item, RecyclerView.ViewHolder> {
    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_DATA_CHAT_PARTICIPANT = 1;
    public static final int VIEW_TYPE_BUTTON = 2;
    final CallBack cb;

    public ChatInfoAdapter(Context ctx, CallBack cb) {
        super(ctx);
        this.cb = cb;
    }

    @Override
    public int getItemViewType(int position) {
        final Item item = getItem(position);
        if (item instanceof HeaderItem) {
            return VIEW_TYPE_HEADER;
        } else if (item instanceof ButtonItem) {
            return VIEW_TYPE_BUTTON;
        } else if (item instanceof ParticipantItem){
            return VIEW_TYPE_DATA_CHAT_PARTICIPANT;
        }
        throw new RuntimeException("unknown item type " + item.getClass().getSimpleName());
        //        return position == 0 ? VIEW_TYPE_HEADER : VIEW_TYPE_DATA_CHAT_PARTICIPANT;//super.getItemViewType(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = getViewFactory().inflate(R.layout.profile_item_header, parent, false);
            return new RecyclerView.ViewHolder(view) {
            };
        } else if (viewType == VIEW_TYPE_BUTTON) {
            View view = getViewFactory().inflate(R.layout.profile_item_button, parent, false);
            return new ButtonAddMemberVH(view);
        } else if (viewType == VIEW_TYPE_DATA_CHAT_PARTICIPANT) {
            View view = getViewFactory().inflate(R.layout.profile_item_chat_participant, parent, false);
            return new PariticpantVH(view);
        }
        throw new RuntimeException("unknown viewType " + viewType);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position != 0) {
            final Item item = getItem(position);
            if (item instanceof ButtonItem){

            } else if (item instanceof ParticipantItem) {
                ParticipantItem p = (ParticipantItem) item;
                final PariticpantVH pVH = (PariticpantVH) holder;
                pVH.avatar.loadAvatarFor(p.user);
                pVH.nick.setText(
                        uiName(p.user, getCtx()));
                if (p.user.type instanceof TdApi.UserTypeBot){
                    pVH.status.setText(R.string.user_status_bot);
                } else {
                    pVH.status.setText(
                            uiUserStatus(getCtx(), p.user.status));
                }
                pVH.status.setTextColor(
                        uiStatusColor(p.user.status));
                if (p.showIcon){
                    pVH.icon.setVisibility(View.VISIBLE);
                } else {
                    pVH.icon.setVisibility(View.INVISIBLE);
                }
            } else {
                throw new RuntimeException("unknown item type " + item.getClass().getSimpleName());
            }
//            ButtonAddMemberVH h = (ButtonAddMemberVH) holder;
//            if (item.icon == 0) {
//                h.icon.setImageDrawable(null);
//            } else {
//                h.icon.setImageResource(item.icon);
//            }
//            h.data.setText(item.data);
//            h.dataType.setText(item.localizedDataType);
        }
    }

    public class ButtonAddMemberVH extends RecyclerView.ViewHolder {
        public ButtonAddMemberVH(View itemView) {
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cb.btnAddMemberClicked();
                }
            });
        }
    }

    public class PariticpantVH extends RecyclerView.ViewHolder {
        final AvatarView avatar;
        private final TextView nick;
        private final TextView status;
        final ImageView icon;

        public PariticpantVH(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.icon);
            avatar = (AvatarView) itemView.findViewById(R.id.avatar);
            nick = ((TextView) itemView.findViewById(R.id.nick));
            status = ((TextView) itemView.findViewById(R.id.status));

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final ParticipantItem item = (ParticipantItem) getItem(getAdapterPosition());
                    cb.participantClicked(item);
                }
            });
        }
    }

    public static class Item {

    }

    public static class HeaderItem extends Item {

    }

    public static class ButtonItem extends Item {

        public ButtonItem() {
        }
    }

    public static class ParticipantItem extends Item {
        final boolean showIcon;
        final TdApi.User user;

        public ParticipantItem(boolean showIcon, TdApi.User user) {
            this.showIcon = showIcon;
            this.user = user;
        }
    }

    //    public static class Item {
    //        final int icon;
    //        final String data;
    //        final String localizedDataType;
    //
    //        public Item(int icon, String data, String localizedDataType ) {
    //            this.icon = icon;
    //            this.data = data;
    //            this.localizedDataType = localizedDataType;
    //
    //        }
    //    }

    interface CallBack {
        void btnAddMemberClicked();

        void participantClicked(ParticipantItem item);
    }
}
