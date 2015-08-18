package ru.korniltsev.telegram.profile.media.controllers;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import flow.Flow;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.recycler.BaseAdapter;
import ru.korniltsev.telegram.core.rx.SharedMediaHelper;
import ru.korniltsev.telegram.core.utils.PhotoUtils;
import ru.korniltsev.telegram.photoview.PhotoView;

import java.util.Locale;

public class SharedMediaAdapter extends BaseAdapter<SharedMediaAdapter.Item, RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_SECTION = 0;
    public static final int VIEW_TYPE_MEDIA = 1;
    private final RxGlide rxGlide;
    private int dip100;

    final DateTimeFormatter MESSAGE_TIME_FORMAT = DateTimeFormat.forPattern("MMMM YYYY")
            .withLocale(Locale.US);

    public SharedMediaAdapter(Context ctx) {
        super(ctx);
        rxGlide = ObjectGraphService.getObjectGraph(ctx).get(RxGlide.class);
        dip100 = MyApp.from(ctx).dpCalculator.dp(100);


    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position) instanceof Section ? VIEW_TYPE_SECTION : VIEW_TYPE_MEDIA;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SECTION){
            final View v = getViewFactory().inflate(R.layout.profile_media_preview_section, parent, false);
            return new RecyclerView.ViewHolder(v) {
            };
        } else {
            final View v = getViewFactory().inflate(R.layout.profile_media_preview_item, parent, false);
            final RecyclerView.ViewHolder viewHolder = new RecyclerView.ViewHolder(v) {
            };
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final TdApi.Message msg = ((Media) getItem(viewHolder.getAdapterPosition())).msg;
                    if (msg.message instanceof TdApi.MessagePhoto){
                        final TdApi.Photo photo = ((TdApi.MessagePhoto) msg.message).photo;
                        Flow.get(getCtx())
                                .set(new PhotoView(photo));
                    } else {
                        //video should be downloaded
                    }
                }
            });
            return viewHolder;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder h, int position) {
        final Item item = getItem(position);
        if (item instanceof Media){
            final TdApi.MessageContent message = ((Media) item).msg.message;
            final ImageView img = (ImageView) h.itemView;
            if (message instanceof TdApi.MessagePhoto){
                final TdApi.Photo photo = ((TdApi.MessagePhoto) message).photo;
                final TdApi.File smallestBiggerThan = PhotoUtils.findSmallestBiggerThan(photo, dip100, dip100);
                rxGlide.loadPhoto(smallestBiggerThan, false)
                        .into(img);
            } else {
                TdApi.MessageVideo v = (TdApi.MessageVideo) message;
                rxGlide.loadPhoto(v.video.thumb.photo, false)
                        .into(img);
            }
        } else {
            final Section section = (Section) item;
            final TextView sectionView = (TextView) h.itemView;
            sectionView.setText(
                    MESSAGE_TIME_FORMAT.print(section.time));
        }
    }




    public GridLayoutManager.SpanSizeLookup createLookup() {
        return new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                final boolean section = getItem(position) instanceof Section;
                return section ? 3 : 1;
            }
        };
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
