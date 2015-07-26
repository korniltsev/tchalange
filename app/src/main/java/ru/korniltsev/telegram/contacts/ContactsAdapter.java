package ru.korniltsev.telegram.contacts;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.tonicartos.superslim.LayoutManager;
import com.tonicartos.superslim.LinearSLM;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.common.recycler.sections.Item;
import ru.korniltsev.telegram.common.recycler.sections.Section;
import ru.korniltsev.telegram.common.recycler.sections.SectionVH;
import ru.korniltsev.telegram.core.recycler.BaseAdapter;
import ru.korniltsev.telegram.core.views.AvatarView;
import rx.functions.Action1;

public class ContactsAdapter extends BaseAdapter<Item<Contact>, RecyclerView.ViewHolder> {
    public static final int VIEW_TYPE_SECTION = 0;
    public static final int VIEW_TYPE_DATA = 1;
    final Action1<TdApi.User> clicked;
    public ContactsAdapter(Context ctx, Action1<TdApi.User> clicked) {
        super(ctx);
        this.clicked = clicked;
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position) instanceof Section) {
            return VIEW_TYPE_SECTION;
        } else {
            return VIEW_TYPE_DATA;
        }

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SECTION) {
            final View view = getViewFactory().inflate(R.layout.contact_item_section, parent, false);
            return new SectionVH(view);
        } else {
            final View view = getViewFactory().inflate(R.layout.contact_item_user, parent, false);
            return new VH(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final Item<Contact> item = getItem(position);
        final LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) holder.itemView.getLayoutParams();
        params.setSlm(LinearSLM.ID);
        params.setFirstPosition(item.firstPosition);
        holder.itemView.setLayoutParams(params);


        if (item instanceof Section) {
            ((SectionVH) holder)
                    .bind((Section) item);
        } else {
            ((VH) holder)
                    .bind(item.data);
        }
    }


    class VH extends RecyclerView.ViewHolder {

        private final TextView userName;
        private final AvatarView avatar;
        private final TextView userStatus;

        public VH(View itemView) {
            super(itemView);
            userName = ((TextView) this.itemView.findViewById(R.id.user_name));
            userStatus = ((TextView) this.itemView.findViewById(R.id.user_status));
            avatar = ((AvatarView) this.itemView.findViewById(R.id.avatar));
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Item<Contact> item = getItem(getPosition());
                    clicked.call(item.data.user);
                }
            });
        }

        public void bind(Contact u){
            avatar.loadAvatarFor(u.user);
            userName.setText(u.uiName);
            userStatus.setText(u.uiStatus);
            final int color;
            final TdApi.UserStatus status = u.user.status;
            color = AppUtils.uiStatusColor(status);
            userStatus.setTextColor(color);
        }


    }
}
