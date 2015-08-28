package ru.korniltsev.telegram.profile.media.controllers;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
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
import ru.korniltsev.telegram.photoview.PhotoView;
import rx.functions.Action1;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class SharedMediaAdapter extends BaseAdapter<SharedMediaAdapter.Item, RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_SECTION = 0;
    public static final int VIEW_TYPE_MEDIA = 1;
    private final RxGlide rxGlide;
    private int dip100;

    public static final DateTimeFormatter MESSAGE_TIME_FORMAT = DateTimeFormat.forPattern("MMMM YYYY")
            .withLocale(Locale.US);

    public void dropSelection() {
        selectedIds.clear();
        animateAllViews(new Action1<SharedMediaItemView>() {
            @Override
            public void call(SharedMediaItemView sharedMediaItemView) {
                sharedMediaItemView.animateGreenCircle(false);
                sharedMediaItemView.animateWhiteCircle(false);
            }
        });
        cb.itemsSelected(0);

    }

    public interface Callback {
        void itemsSelected(int selecedItems);
    }
    final Callback cb;
    final RecyclerView list;
    public SharedMediaAdapter(Context ctx, Callback cb, RecyclerView list) {
        super(ctx);
        this.cb = cb;
        this.list = list;
        rxGlide = ObjectGraphService.getObjectGraph(ctx).get(RxGlide.class);
        dip100 = MyApp.from(ctx).calc.dp(100);
        setHasStableIds(true);

    }

    @Override
    public long getItemId(int position) {
        final Item item = getItem(position);
        if (item instanceof Section) {
            return ((Section) item).id;
        }
        final Media m = (Media) item;
        return m.msg.id;
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


            final MediaVH viewHolder = new MediaVH(v) ;

            return viewHolder;
        }
    }


    class MediaVH extends RecyclerView.ViewHolder{
        final SharedMediaItemView root;
        public MediaVH(View itemView) {
            super(itemView);
            this.root = (SharedMediaItemView) itemView;
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    toggle();
                    return true;
                }
            });
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selectedIds.size() > 0){
                        toggle();
                        return;
                    }
                    final TdApi.Message msg = ((Media) getItem(getAdapterPosition())).msg;
                    if (msg.message instanceof TdApi.MessagePhoto){
                        final TdApi.Photo photo = ((TdApi.MessagePhoto) msg.message).photo;
                        Flow.get(getCtx())
                                .set(new PhotoView(photo));
                    } else {
                        //video should be downloaded
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
                animateAllViews(new Action1<SharedMediaItemView>() {
                    @Override
                    public void call(SharedMediaItemView v) {
                        v.animateWhiteCircle(true);
                    }
                });
                //animate green circle in on current view
                root.animateGreenCircle(true);
                selectedIds.add(id);
            } else {
                final boolean wasSelected = selectedIds.contains(id);
                if (wasSelected){
                    selectedIds.remove(id);
                    //animate green circle out
                    root.animateGreenCircle(false);
                    if (selectedIds.isEmpty()) {
                        //animate all white circles out
                        animateAllViews(new Action1<SharedMediaItemView>() {
                            @Override
                            public void call(SharedMediaItemView v) {
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
    private void animateAllViews( Action1<SharedMediaItemView> f) {
        for (int i =0; i< list.getChildCount(); ++i){
            View child = list.getChildAt(i);
            if (child instanceof SharedMediaItemView) {
                SharedMediaItemView v = (SharedMediaItemView) child;
                f.call(v);
            }
        }
    }
    final Set<Integer> selectedIds = new HashSet<>();


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder h, int position) {
        final Item item = getItem(position);
        if (item instanceof Media){
            MediaVH vh = (MediaVH) h;
            final TdApi.Message rawMessage = ((Media) item).msg;
            final TdApi.MessageContent message = rawMessage.message;


            if (message instanceof TdApi.MessagePhoto){
                final TdApi.Photo photo = ((TdApi.MessagePhoto) message).photo;
                vh.root.bindPhoto(photo);
            } else {

                TdApi.MessageVideo v = (TdApi.MessageVideo) message;
                vh.root.bindVideo(v.video);

            }
            int id = rawMessage.id;
            final boolean selected = selectedIds.contains(id);
            vh.root.bindSelection(selected, selectedIds.size() > 0);
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
        private final long id;

        Section(DateTime time, long id) {
            this.time = time;
            this.id = id;
        }
    }

    static class Media extends Item {
        final TdApi.Message msg;

        Media(TdApi.Message msg) {
            this.msg = msg;
        }
    }
}
