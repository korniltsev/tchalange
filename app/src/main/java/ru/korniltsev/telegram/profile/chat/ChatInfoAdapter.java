package ru.korniltsev.telegram.profile.chat;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.recycler.BaseAdapter;
import ru.korniltsev.telegram.core.views.AvatarView;
import ru.korniltsev.telegram.profile.other.MediaPreviewAdapter;

import java.util.List;

import static java.util.Arrays.asList;
import static ru.korniltsev.telegram.common.AppUtils.uiStatusColor;
import static ru.korniltsev.telegram.common.AppUtils.uiName;
import static ru.korniltsev.telegram.common.AppUtils.uiUserStatus;

public class ChatInfoAdapter extends BaseAdapter<ChatInfoAdapter.Item, RecyclerView.ViewHolder> {
    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_DATA_CHAT_PARTICIPANT = 1;
    public static final int VIEW_TYPE_BUTTON = 2;
    public static final int VIEW_TYPE_MEDIA= 3;
    final CallBack cb;
    private final DpCalculator calc;

    public ChatInfoAdapter(Context ctx, CallBack cb) {
        super(ctx);
        this.cb = cb;
        calc = MyApp.from(ctx).calc;
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
        } else if (item instanceof MediaItem) {
            return VIEW_TYPE_MEDIA;
        }
        throw new RuntimeException("unknown item type " + item.getClass().getSimpleName());
        //        return position == 0 ? VIEW_TYPE_HEADER : VIEW_TYPE_DATA_CHAT_PARTICIPANT;//super.getItemViewType(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_HEADER: {
                View view = getViewFactory().inflate(R.layout.profile_item_header, parent, false);
                return new RecyclerView.ViewHolder(view) {
                };
            }
            case VIEW_TYPE_BUTTON: {
                View view = getViewFactory().inflate(R.layout.profile_item_button, parent, false);
                return new ButtonAddMemberVH(view);
            }
            case VIEW_TYPE_DATA_CHAT_PARTICIPANT: {
                View view = getViewFactory().inflate(R.layout.profile_item_chat_participant, parent, false);
                return new PariticpantVH(view);
            }
            case VIEW_TYPE_MEDIA: {
                View view = getViewFactory().inflate(R.layout.profile_item_shared_media, parent, false);
                return new MediaVH(view);
            }
            default:
                throw new RuntimeException("unknown viewType " + viewType);
        }

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
            } else if (item instanceof MediaItem){
                MediaVH h = (MediaVH) holder;
                h.bind((MediaItem) item);
            } else {
                throw new RuntimeException("unknown item type " + item.getClass().getSimpleName());
            }

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


    public static class MediaItem extends Item {
        private final int totalCount;
        final List<TdApi.Message> ms;

        public MediaItem(int totalCount, List<TdApi.Message> ms) {
            this.totalCount = totalCount;
            this.ms = ms;
        }
    }
    interface CallBack {
        void btnAddMemberClicked();

        void participantClicked(ParticipantItem item);
    }

    class MediaVH extends RecyclerView.ViewHolder {

        private final TextView mediaCount;
        private final RecyclerView mediaPreview;
        private final Context ctx;

        public MediaVH(View itemView) {
            super(itemView);
            mediaCount = ((TextView) itemView.findViewById(R.id.media_count));
            mediaPreview = ((RecyclerView) itemView.findViewById(R.id.media_preview));
            ctx = itemView.getContext();
            mediaPreview.setLayoutManager(new LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false));
            final int dp8 = calc.dp(8);
            mediaPreview.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                    final int pos = parent.getChildViewHolder(view).getAdapterPosition();
                    if (pos == 0) {
                        outRect.set(0, 0, 0, 0);
                    } else {

                        outRect.set(dp8, 0, 0, 0);
                    }
                }
            });
        }

        public void bind(MediaItem i) {
            if (i.ms.isEmpty()) {
                mediaPreview.setVisibility(View.GONE);
                mediaCount.setText("0");
            } else {
                mediaPreview.setVisibility(View.VISIBLE);
                mediaPreview.setAdapter(new MediaPreviewAdapter(ctx, i.ms));
                mediaCount.setText(String.valueOf(i.totalCount));
            }
        }
    }
}
