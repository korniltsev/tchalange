//package ru.korniltsev.telegram.core.emoji;
//
//import android.content.Context;
//import android.support.v7.widget.RecyclerView;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import com.squareup.picasso.Picasso;
//import com.tonicartos.superslim.GridSLM;
//import com.tonicartos.superslim.LinearSLM;
//import org.drinkless.td.libcore.telegram.TdApi;
//import ru.korniltsev.telegram.core.picasso.RxGlide;
//import ru.korniltsev.telegram.core.recycler.BaseAdapter;
//import ru.korniltsev.telegram.utils.R;
//import rx.functions.Action1;
//import rx.functions.Func0;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class StickersAdapter extends BaseAdapter<StickersAdapter.Item, RecyclerView.ViewHolder> {
//
//    public static final int VIEW_TYPE_SECTION = 0;
//    public static final int VIEW_TYPE_DATA = 1;
//    final RxGlide glide;
//    private final int numColumns;
//
//    public StickersAdapter(Context ctx, List<List<TdApi.Sticker>> stickers, RxGlide glide, int numColumns) {
//        super(ctx);
//        this.glide = glide;
//        this.numColumns = numColumns;
//        List<Item> is = new ArrayList<>();
//
//        for (List<TdApi.Sticker> stickerSet : stickers) {
//            final int firstPosition = is.size();
//            for (TdApi.Sticker s : stickerSet) {
//                is.add(new Data(firstPosition, s));
//            }
//            final int mod = stickerSet.size() % numColumns;
//            if (mod != 0){
//                for (int i = 0; i < numColumns - mod; ++i) {
//                    is.add(new Section(firstPosition));
//                }
//            }
//        }
//        addAll(is);
//
//    }
//
//    @Override
//    public int getItemViewType(int position) {
//        return getItem(position) instanceof Section ? VIEW_TYPE_SECTION : VIEW_TYPE_DATA;
//    }
//
//    @Override
//    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//        if (viewType == VIEW_TYPE_SECTION) {
//            final View v = getViewFactory().inflate(R.layout.view_sticker_section, parent, false);
//            return new RecyclerView.ViewHolder(v) {
//            };
//        } else {
//            final View v = getViewFactory().inflate(R.layout.view_sticker_data, parent, false);
//            return new VH(v);
//        }
//
//    }
//
//
//
//    @Override
//    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
//        final Item item = getItem(position);
////        GridSLM.LayoutParams params = GridSLM.LayoutParams.from(holder.itemView.getLayoutParams());
////        params.setNumColumns(-1);
////        params.setSlm("grid");
////        final int firstPosition = item.firstPosition;
////        params.setFirstPosition(firstPosition);
////        params.headerDisplay = GridSLM.LayoutParams.HEADER_STICKY | GridSLM.LayoutParams.HEADER_ALIGN_START;
////        params.isHeader = position == item.firstPosition;
////        holder.itemView.setLayoutParams(params);
//        if (item instanceof Data){
//            VH h = (VH) holder;
//            glide.loadPhoto(((Data) item).sticker.thumb.photo, true)
//                    .priority(Picasso.Priority.HIGH)
//                    .into(h.sticker);
//        }
//    }
//
//    abstract class Item {
//        final int firstPosition;
//
//        protected Item(int firstPosition) {
//            this.firstPosition = firstPosition;
//        }
//    }
//
//    class Section extends Item {
//
//        Section(int firstPosition) {
//            super(firstPosition);
//        }
//    }
//
//    class Data extends Item {
//        final TdApi.Sticker sticker;
//
//        Data(int firstPosition, TdApi.Sticker sticker) {
//            super(firstPosition);
//            this.sticker = sticker;
//        }
//    }
//
//    class VH extends RecyclerView.ViewHolder {
//        final ImageView sticker;
//        public VH(View itemView) {
//            super(itemView);
//            this.sticker = (ImageView) itemView;
//            sticker.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    final TdApi.Sticker s = ((Data) getItem(getAdapterPosition())).sticker;
//                    clickListner.call(s);
//                }
//            });
//        }
//    }
//    private Action1<TdApi.Sticker> clickListner;
//
//    public void setClickListner(Action1<TdApi.Sticker> clickListner) {
//        this.clickListner = clickListner;
//    }
//}
