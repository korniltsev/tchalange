package ru.korniltsev.telegram.profile.my;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import ru.korniltsev.telegram.attach_panel.ListChoicePopup;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.recycler.BaseAdapter;

import java.util.List;

public class MyProfileAdapter extends BaseAdapter<MyProfileAdapter.Item, RecyclerView.ViewHolder> {
    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_KEY_VALUE = 1;
    public static final int VIEW_TYPE_PASSCODE = 2;
    public MyProfileAdapter(Context ctx) {
        super(ctx);

    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_HEADER;
        } else {
            final Item item = getItem(position);
            if (item instanceof KeyValueItem){
                return VIEW_TYPE_KEY_VALUE
                        ;
            } else if (item instanceof PasscodeItem) {
                return VIEW_TYPE_PASSCODE;
            }
            throw new RuntimeException("unknown item type " + item);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = getViewFactory().inflate(R.layout.profile_item_header, parent, false);
            return new RecyclerView.ViewHolder(view) {
            };
        } else if (viewType == VIEW_TYPE_KEY_VALUE){
            View view = getViewFactory().inflate(R.layout.profile_item_data, parent, false);
            return new KeyValueVH(view);
        } else if (viewType == VIEW_TYPE_PASSCODE){
            View view = getViewFactory().inflate(R.layout.profile_item_passcode, parent, false);
            return new PassCodeVH(view);
        }
        throw new RuntimeException("unknown viewType" + viewType);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position != 0) {

            final Item item = getItem(position);
            if (item instanceof KeyValueItem){
                KeyValueItem kvi = (KeyValueItem) item;
                KeyValueVH h = (KeyValueVH) holder;
                if (kvi.icon == 0) {
                    h.icon.setImageDrawable(null);
                } else {
                    h.icon.setImageResource(kvi.icon);
                }
                h.data.setText(kvi.data);
                h.dataType.setText(kvi.localizedDataType);
            } else if (item instanceof PasscodeItem){
                PassCodeVH vh = (PassCodeVH) holder;
                final int text = ((PasscodeItem) item).enabled ? R.string.passcode_enabled : R.string.passcode_disabled;
                vh.data.setText(text);
            }



        }
    }

    public  class KeyValueVH extends RecyclerView.ViewHolder {

        private final ImageView icon;
        private final TextView data;
        private final TextView dataType;

        public KeyValueVH(View itemView) {
            super(itemView);
            icon = ((ImageView) itemView.findViewById(R.id.icon));
            data = ((TextView) itemView.findViewById(R.id.data));
            dataType = ((TextView) itemView.findViewById(R.id.data_type));
        }
    }

    public  class PassCodeVH extends RecyclerView.ViewHolder {

        private final TextView data;

        public PassCodeVH(View itemView) {
            super(itemView);
            data = ((TextView) itemView.findViewById(R.id.pass_code_status));
        }
    }


    public static class Item {

    }
    public static class KeyValueItem extends Item {
        final int icon;
        final String data;
        final String localizedDataType;

        public KeyValueItem(int icon, String data, String localizedDataType, @Nullable List<ListChoicePopup.Item> bottomSheetActions) {
            this.icon = icon;
            this.data = data;
            this.localizedDataType = localizedDataType;

        }
    }

    public static class PasscodeItem extends Item {
        final boolean enabled;

        public PasscodeItem(boolean enabled) {
            this.enabled = enabled;
        }
        //        final int icon;
//        final String data;
//        final String localizedDataType;

//        public KeyValueVerticalItem(int icon, String data, String localizedDataType, @Nullable List<ListChoicePopup.Item> bottomSheetActions) {
//            this.icon = icon;
//            this.data = data;
//            this.localizedDataType = localizedDataType;
//
//        }
    }


}
