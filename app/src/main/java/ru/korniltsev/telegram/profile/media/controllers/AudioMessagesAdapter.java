package ru.korniltsev.telegram.profile.media.controllers;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import flow.Flow;
import org.drinkless.td.libcore.telegram.TdApi;
import org.joda.time.DateTime;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.AudioMessageView;
import ru.korniltsev.telegram.core.recycler.BaseAdapter;
import ru.korniltsev.telegram.core.utils.PhotoUtils;
import ru.korniltsev.telegram.photoview.PhotoView;
import rx.functions.Action1;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class AudioMessagesAdapter extends BaseAdapter<AudioMessagesAdapter.Item, RecyclerView.ViewHolder> {
    public static final int AUDIO = 0;
    public static final int VIEW_TYPE_SECTION = AUDIO;
    public static final int VIEW_TYPE_AUDIO = 1;

    final Set<Integer> selectedIds = new HashSet<>();
    final RecyclerView list;

    public Set<Integer> getSelectedIds() {
        return selectedIds;
    }

    public AudioMessagesAdapter(Context ctx, RecyclerView list, Callback cb) {
        super(ctx);
        this.list = list;
        this.cb = cb;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position) instanceof Section ? VIEW_TYPE_SECTION : VIEW_TYPE_AUDIO;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SECTION) {
            final View v = getViewFactory().inflate(R.layout.profile_media_preview_section, parent, false);
            return new RecyclerView.ViewHolder(v) {
            };
        } else {
            final View v = getViewFactory().inflate(R.layout.profile_media_audio_item, parent, false);
            return new VH(v);
        }
    }

    public void dropSelection() {
        selectedIds.clear();
        animateAllViews(new Action1<AudioMessageView>() {
            @Override
            public void call(AudioMessageView sharedMediaItemView) {
                sharedMediaItemView.animateGreenCircle(false);
                sharedMediaItemView.animateWhiteCircle(false);
            }
        });
        cb.itemsSelected(0);
    }

    public void deleteMessages(int[] msgIds) {
        for (Iterator<Item> iterator = getData().iterator(); iterator.hasNext(); ) {
            Item i = iterator.next();
            if (i instanceof Media) {
                final TdApi.Message msg = ((Media) i).msg;
                for (int msgId : msgIds) {
                    if (msgId == msg.id) {
                        iterator.remove();
                    }
                }
            }
        }
        notifyDataSetChanged();
        dropSelection();
    }

    class VH extends RecyclerView.ViewHolder {
        final AudioMessageView root;

        public VH(View itemView) {
            super(itemView);
            this.root = (AudioMessageView) itemView;
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    toggle();
                    return true;
                }
            });
            root.whiteCircle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selectedIds.size() > 0){
                        toggle();
                    }
                }
            });
            root.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selectedIds.size() > 0){
                        toggle();
                    }
                }
            });
        }

        private void toggle() {
            final int adapterPosition = getAdapterPosition();
            final Media item = (Media) getItem(adapterPosition);
            final int id = item.msg.id;

            //todo clear all animations
            if (selectedIds.isEmpty()) {
                //animate all views white circle in

                animateAllViews(new Action1<AudioMessageView>() {
                    @Override
                    public void call(AudioMessageView v) {
                        v.animateWhiteCircle(true);
                    }
                });
                //animate green circle in on current view
                root.animateGreenCircle(true);
                selectedIds.add(id);
            } else {
                final boolean wasSelected = selectedIds.contains(id);
                if (wasSelected) {
                    selectedIds.remove(id);
                    //animate green circle out
                    root.animateGreenCircle(false);
                    if (selectedIds.isEmpty()) {
                        //animate all white circles out
                        animateAllViews(new Action1<AudioMessageView>() {
                            @Override
                            public void call(AudioMessageView v) {
                                v.animateWhiteCircle(false);
                            }
                        });
                    }
                } else {
                    selectedIds.add(id);
                    //animate green circle in
                    root.animateGreenCircle(true);
                }
            }
            cb.itemsSelected(selectedIds.size());
        }
    }

    private void animateAllViews(Action1<AudioMessageView> action1) {
        for (int i = 0; i < list.getChildCount(); ++i) {
            View child = list.getChildAt(i);
            if (child instanceof AudioMessageView) {
                action1.call((AudioMessageView) child);
            }
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder h, int position) {
        final Item item = getItem(position);
        if (item instanceof Media) {
            final TdApi.Message msg = ((Media) item).msg;
            final TdApi.MessageContent message = msg.message;
            final TdApi.MessageAudio a = (TdApi.MessageAudio) message;
            final AudioMessageView itemView = (AudioMessageView) h.itemView;
            itemView.bind(a, msg);
            int id = msg.id;
            final boolean selected = selectedIds.contains(id);
            itemView.bindSelection(selected, selectedIds.size() > 0);
        } else {
            final Section section = (Section) item;
            final TextView sectionView = (TextView) h.itemView;
            sectionView.setText(
                    SharedMediaAdapter.MESSAGE_TIME_FORMAT.print(section.time));
        }
    }

    static class Item {

    }

    static class Section extends Item {
        final DateTime time;

        Section(DateTime time) {
            this.time = time;
        }
    }

    static class Media extends Item {
        final TdApi.Message msg;

        Media(TdApi.Message msg) {
            this.msg = msg;
        }
    }

    final Callback cb;

    public interface Callback {
        void itemsSelected(int count);
    }
}
