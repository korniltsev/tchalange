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

public class AudioMessagesAdapter extends BaseAdapter<AudioMessagesAdapter.Item, RecyclerView.ViewHolder> {

    public static final int AUDIO = 0;
    public static final int VIEW_TYPE_SECTION = AUDIO;
    public static final int VIEW_TYPE_AUDIO = 1;

    public AudioMessagesAdapter(Context ctx) {
        super(ctx);
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position) instanceof Section ? VIEW_TYPE_SECTION : VIEW_TYPE_AUDIO;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SECTION){
            final View v = getViewFactory().inflate(R.layout.profile_media_preview_section, parent, false);
            return new RecyclerView.ViewHolder(v) {
            };
        } else {
            final View v = getViewFactory().inflate(R.layout.profile_media_audio_item, parent, false);
            return new RecyclerView.ViewHolder(v) {
            };
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder h, int position) {
        final Item item = getItem(position);
        if (item instanceof Media){
            final TdApi.Message msg = ((Media) item).msg;
            final TdApi.MessageContent message = msg.message;
            final TdApi.MessageAudio a = (TdApi.MessageAudio) message;
            final AudioMessageView itemView = (AudioMessageView) h.itemView;
            itemView.bind(a, msg);
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
}
