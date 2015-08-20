package ru.korniltsev.telegram.profile.other;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.attach_panel.ListChoicePopup;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.recycler.BaseAdapter;

import java.util.Arrays;
import java.util.List;

public class ProfileAdapter extends BaseAdapter<ProfileAdapter.Item, RecyclerView.ViewHolder> {
    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_KEY_VALUE = 1;
    public static final int VIEW_TYPE_BUTTON = 2;
    public static final int VIEW_TYPE_SHARED_MEDIA = 3;
    final CallBack cb;
    private final DpCalculator calc;

    public ProfileAdapter(Context ctx, CallBack cb) {
        super(ctx);
        calc = MyApp.from(ctx).calc;
        this.cb = cb;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_HEADER;
        } else {
            final Item itm = getItem(position);
            if (itm instanceof SharedMedia) {
                return VIEW_TYPE_SHARED_MEDIA;
            }
            if (itm instanceof KeyValueItem) {
                return VIEW_TYPE_KEY_VALUE;
            } else {
                return VIEW_TYPE_BUTTON;
            }
        }
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
            case VIEW_TYPE_SHARED_MEDIA: {
                View view = getViewFactory().inflate(R.layout.profile_item_shared_media, parent, false);
                return new MediaVH(view);
            }
            default: {
                View view = getViewFactory().inflate(R.layout.profile_item_data, parent, false);
                return new VH(view);
            }
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
            } else if (item instanceof ButtonItem) {
                final ButtonItem b = (ButtonItem) item;
                ButtonAddMemberVH h = (ButtonAddMemberVH) holder;
                h.icon.setImageResource(b.icon);
                h.text.setText(b.localizedText);
            } else if (item instanceof SharedMedia) {
                MediaVH h = (MediaVH) holder;
                h.bind((SharedMedia) item);
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

    public static class SharedMedia extends Item {
        final TdApi.Messages ms;

        public SharedMedia(TdApi.Messages ms) {
            this.ms = ms;
        }
    }

    interface CallBack {
        void clicked(KeyValueItem item);

        void sharedMediaClicked();
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
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cb.sharedMediaClicked();
                }
            });
        }

        public void bind(SharedMedia i) {
            if (i.ms.messages.length == 0) {
                mediaPreview.setVisibility(View.GONE);
                mediaCount.setText("0");
            } else {
                mediaPreview.setVisibility(View.VISIBLE);
                mediaPreview.setAdapter(new MediaPreviewAdapter(ctx, Arrays.asList(i.ms.messages)));
                mediaCount.setText(String.valueOf(i.ms.totalCount));
            }
        }
    }
}
