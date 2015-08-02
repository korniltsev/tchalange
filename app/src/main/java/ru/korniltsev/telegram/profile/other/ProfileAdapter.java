package ru.korniltsev.telegram.profile.other;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import ru.korniltsev.telegram.attach_panel.ListChoicePopup;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.recycler.BaseAdapter;
import ru.korniltsev.telegram.profile.chat.ChatInfoAdapter;

import java.util.List;

public class ProfileAdapter extends BaseAdapter<ProfileAdapter.Item, RecyclerView.ViewHolder> {
    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_KEY_VALUE = 1;
    public static final int VIEW_TYPE_BUTTON = 2;
    final CallBack cb;

    public ProfileAdapter(Context ctx, CallBack cb) {
        super(ctx);
        this.cb = cb;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0){
            return VIEW_TYPE_HEADER;
        } else {
            final Item itm = getItem(position);
            if (itm instanceof KeyValueItem){
                return VIEW_TYPE_KEY_VALUE;
            } else {
                return VIEW_TYPE_BUTTON;
            }
        }
//        return position == 0 ? VIEW_TYPE_HEADER : VIEW_TYPE_KEY_VALUE;
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
        } else {
            View view = getViewFactory().inflate(R.layout.profile_item_data, parent, false);
            return new VH(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position != 0) {
            final Item item = getItem(position);
            if (item instanceof KeyValueItem) {
                final KeyValueItem k = (KeyValueItem) item;
                VH h = (VH) holder;
                h.icon.setImageResource(k.icon);
                h.data.setText(k.data);
                h.dataType.setVisibility(View.VISIBLE);
                h.dataType.setText(k.localizedDataType);
                h.itemView.setClickable(k.bottomSheetActions != null);
            } else if (item instanceof ButtonItem){
                final ButtonItem b = (ButtonItem) item;
                ButtonAddMemberVH h = (ButtonAddMemberVH) holder;
                h.icon.setImageResource(b.icon);
                h.text.setText(b.localizedText);
            }
        }
    }

    public class VH extends RecyclerView.ViewHolder {

        private final ImageView icon;
        private final TextView data;
        private final TextView dataType;

        public VH(View itemView) {
            super(itemView);
            icon = ((ImageView) itemView.findViewById(R.id.icon));
            data = ((TextView) itemView.findViewById(R.id.data));
            dataType = ((TextView) itemView.findViewById(R.id.data_type));
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Item item = getItem(
                            getAdapterPosition());
                    cb.clicked(
                            (KeyValueItem) item);
                }
            });
        }
    }

    public static class Item {

    }

    public static class KeyValueItem extends Item {
        final int icon;
        final String data;
        @NonNull final String localizedDataType;
        @Nullable final List<ListChoicePopup.Item> bottomSheetActions;

        public KeyValueItem(int icon, String data, String localizedDataType, @Nullable List<ListChoicePopup.Item> bottomSheetActions) {
            this.icon = icon;
            this.data = data;
            this.localizedDataType = localizedDataType;
            this.bottomSheetActions = bottomSheetActions;
        }
    }

    public static class ButtonItem extends Item {
        final int icon;
        final String localizedText;
        final Runnable action;

        public ButtonItem(int icon, String localizedText, Runnable action) {
            this.icon = icon;
            this.localizedText = localizedText;
            this.action = action;
        }
    }

    interface CallBack {
        void clicked(KeyValueItem item);
    }

    public class ButtonAddMemberVH extends RecyclerView.ViewHolder {

        private final ImageView icon;
        private final TextView text;

        public ButtonAddMemberVH(View itemView) {
            super(itemView);
            icon = ((ImageView) itemView.findViewById(R.id.icon));
            text = ((TextView) itemView.findViewById(R.id.text));

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final ButtonItem item = (ButtonItem) getItem(getAdapterPosition());
                    item.action.run();
                    //
                    //                    cb.btnAddMemberClicked();
                }
            });
        }
    }
}
