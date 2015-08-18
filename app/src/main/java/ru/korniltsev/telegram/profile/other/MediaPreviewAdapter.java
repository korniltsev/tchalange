package ru.korniltsev.telegram.profile.other;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import flow.Flow;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.recycler.BaseAdapter;
import ru.korniltsev.telegram.core.utils.PhotoUtils;
import ru.korniltsev.telegram.photoview.PhotoView;
import ru.korniltsev.telegram.profile.media.controllers.SquareImageView;

import java.util.List;

public class MediaPreviewAdapter extends BaseAdapter<TdApi.Message, MediaPreviewAdapter.VH> {

    private final RxGlide rxGlide;
    private final DpCalculator calc;
    private final int dip100;

    public MediaPreviewAdapter(Context ctx, List<TdApi.Message> messages) {
        super(ctx, messages);
        calc = MyApp.from(ctx).dpCalculator;
        rxGlide = ObjectGraphService.getObjectGraph(ctx).get(RxGlide.class);
        dip100 = calc.dp(100);

    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        return new VH(getViewFactory().inflate(R.layout.profile_media_preview_item, parent, false));
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        final TdApi.Message item = getItem(position);
        final TdApi.MessageContent message = item.message;
        if (message instanceof TdApi.MessagePhoto){
            final TdApi.Photo photo = ((TdApi.MessagePhoto) message).photo;
            final TdApi.File smallestBiggerThan = PhotoUtils.findSmallestBiggerThan(photo, dip100, dip100);
            rxGlide.loadPhoto(smallestBiggerThan, false)
                    .into(holder.img);
        } else {
            TdApi.MessageVideo v = (TdApi.MessageVideo) message;
            rxGlide.loadPhoto(v.video.thumb.photo, false)
                    .into(holder.img);
        }

    }

    class VH extends RecyclerView.ViewHolder{
        final SquareImageView img;
        public VH(final View itemView) {
            super(itemView);
            this.img = (SquareImageView) itemView;
            this.img.setHorizontal(false);
            img.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final TdApi.Message msg = getItem(getAdapterPosition());
                    if (msg.message instanceof TdApi.MessagePhoto){
                        final TdApi.Photo photo = ((TdApi.MessagePhoto) msg.message).photo;
                        Flow.get(itemView.getContext())
                                .set(new PhotoView(photo));
                    } else {
                        //video should be downloaded
                    }
                }
            });
        }

    }
}
