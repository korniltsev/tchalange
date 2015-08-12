package ru.korniltsev.telegram.core.emoji;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.squareup.picasso.Picasso;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.strip.EmojiPagerStripView;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import ru.korniltsev.telegram.utils.R;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

public class EmojiKeyboardView extends LinearLayout {

    private final RecentSmiles recent;
    private final int displayWidth;
    private ViewPager pager;
    @Inject Emoji emoji;
    @Inject Stickers stickers;
    @Inject RxDownloadManager downloader;
    DpCalculator calc;
    @Inject RxGlide picasso;

//    private View backspace;
    private final LayoutInflater viewFactory;
    private EmojiPagerStripView tabs;

    public EmojiKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        recent = new RecentSmiles(context.getSharedPreferences("RecentEmoji", Context.MODE_PRIVATE));
        ObjectGraphService.inject(context, this);
        viewFactory = LayoutInflater.from(context);
        final MyApp from = MyApp.from(context);
        calc = from.dpCalculator;
        displayWidth = from.displayWidth;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        pager = ((ViewPager) findViewById(R.id.pager));

        tabs = ((EmojiPagerStripView) findViewById(R.id.tabs));
        tabs.init(pager, new Runnable() {
            @Override
            public void run() {
                callback.backspaceClicked();
            }
        }, stickers.getStickers());
        Adapter adapter = new Adapter(getContext());
        pager.setAdapter(adapter);

        //        backspace = findViewById(R.id.backspace);
//        backspace.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                callback.backspaceClicked();
//            }
//        });
//        if (adapter.recentIds.length == 0) {
//            pager.setCurrentItem(1);
//        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private CallBack callback;

    public void setCallback(CallBack callback) {
        this.callback = callback;
    }

    public interface CallBack {
        void backspaceClicked();

        void emojiClicked(long code);

        void stickerCLicked(String stickerFilePath, TdApi.Sticker sticker);
    }

    class Adapter extends PagerAdapter {

        private final LayoutInflater viewFactory;
        private Context context;
        private long[] recentIds;

        public Adapter(Context context) {
            this.context = context;
            recentIds = EmojiKeyboardView.this.recent.get();
            viewFactory = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return /*recent*/ 1 +
                    /*emoji*/ 5 +
                    /* stickers*/ 1;
        }

        @Override
        public View instantiateItem(ViewGroup container, int position) {
            if (position == 0) {
                final long[] longs = recentIds;
                return createGridPage(container, position, new EmojiPageAdapter(longs), R.dimen.emoji_size);
            } else if (position == getCount() - 1) {
//                final List<TdApi.Sticker> ss = EmojiKeyboardView.this.stickers.getStickers();
                final List<TdApi.StickerSet> stickers = EmojiKeyboardView.this.stickers.getStickers();
                List<List<TdApi.Sticker>> sets = new ArrayList<>();
                for (TdApi.StickerSet s : stickers) {
                    sets.add(
                            Arrays.asList(s.stickers));
                }
                final ArrayList<TdApi.Sticker> recentStickers = new ArrayList<>();
                GridView res = createGridPage(container, position, new StickerAdapter(sets, recentStickers), R.dimen.sticker_size);
                res.setVerticalSpacing(calc.dp(16));
                res.setClipToPadding(false);
                int dip8 = calc.dp(8);
                res.setPadding(dip8 / 2, dip8, dip8 / 2, dip8);
                tabs.initStickerScroll(res);
//                RecyclerView res = createStickersPage(container);
                return res;
            } else {
                final long[] data = Emoji.data[position];
                return createGridPage(container, position, new EmojiPageAdapter(data), R.dimen.emoji_size);
            }
        }

//        @NonNull
//        private RecyclerView createStickersPage(ViewGroup container) {
//            final List<TdApi.StickerSet> stickers = EmojiKeyboardView.this.stickers.getStickers();
//            RecyclerView res = (RecyclerView) viewFactory.inflate(R.layout.keyboard_page_stickers, container, false);
//            container.addView(res);
//            final int stickerWidth = getContext().getResources().getDimensionPixelSize(R.dimen.sticker_size);
//            int numcolumns = displayWidth / stickerWidth;
//            List<List<TdApi.Sticker>> sets = new ArrayList<>();
//            for (TdApi.StickerSet s : stickers) {
//                sets.add(
//                        Arrays.asList(s.stickers));
//            }
//            final StickersAdapter adapter = new StickersAdapter(getContext(), sets, picasso, numcolumns);
//            adapter.setClickListner(new Action1<TdApi.Sticker>() {
//                @Override
//                public void call(TdApi.Sticker sticker) {
//                    stickerClicked(sticker);//todo pass by id!
//                }
//            });
//            res.setAdapter(adapter);
//            final GridLayoutManager lm = new GridLayoutManager(context, numcolumns);
//            res.setLayoutManager(lm);
//
////            res.setVerticalSpacing(calc.dp(16));
//            res.setClipToPadding(false);
//            int dip8 = calc.dp(8);
//            final int dp16 = calc.dp(16);
//            res.setPadding(dip8 / 2, 0, dip8 / 2, 0);
//            res.addItemDecoration(new RecyclerView.ItemDecoration() {
//                @Override
//                public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
//
////                    outRect.set(0,dp16,0,dp16);
//                }
//            });
//
//            return res;
//        }

        private GridView createGridPage(ViewGroup container, int position1, BaseAdapter adapter, int columnSizeResId) {
            int columnWidth = getContext().getResources().getDimensionPixelSize(columnSizeResId);
            GridView view = (GridView) viewFactory.inflate(R.layout.keyboard_page_emoji, container, false);
            view.setColumnWidth(columnWidth);
            view.setAdapter(adapter);
            container.addView(view);
            view.setTag(position1);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }

    class EmojiPageAdapter extends BaseAdapter {

        private long[] longs;
        ;

        public EmojiPageAdapter(long[] longs) {

            this.longs = longs;
        }

        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = viewFactory.inflate(R.layout.grid_item_emoji, parent, false);
            return new VH(v);
        }

        public void onBindViewHolder(VH holder, int position) {
            holder.o = longs[position];
            Drawable d = emoji.getEmojiBigDrawable(longs[position]);
            holder.img.setImageDrawable(d);
        }

        @Override
        public int getCount() {
            return longs.length;
        }

        @Override
        public Object getItem(int position) {
            return longs[position];
        }

        @Override
        public long getItemId(int position) {
            return longs[position];
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            VH vh;
            if (convertView == null) {
                vh = onCreateViewHolder(parent, 0);
                vh.img.setTag(vh);
            } else {
                vh = (VH) convertView.getTag();
            }
            onBindViewHolder(vh, position);
            return vh.img;
        }
    }

    class VH {
        Object o;
        final ImageView img;

        public VH(View itemView) {
            img = (ImageView) itemView.findViewById(R.id.img);
            img.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (o instanceof TdApi.Sticker) {
                        stickerClicked((TdApi.Sticker) o);
                    } else {
                        Long emojiCode = (Long) VH.this.o;
                        callback.emojiClicked(emojiCode);
                        recent.emojiClicked(emojiCode);
                    }
                }
            });
        }
    }

    public class StickerAdapter extends BaseAdapter {
        final List<Item> data;

        public List<Item> getData() {
            return data;
        }

        private final int numColumns;

        StickerAdapter(List<List<TdApi.Sticker>> sets, List<TdApi.Sticker> recentStickers) {
            this.data = new ArrayList<>();
            numColumns = displayWidth / getContext().getResources().getDimensionPixelSize(R.dimen.sticker_size);

            final int recentCount = recentStickers.size();
            if (recentCount != 0){
                for (TdApi.Sticker sticker : recentStickers) {
                    this.data.add(new Data(true, sticker));
                }
                final int mod = recentCount % numColumns;
                if (mod != 0){
                    for (int i = 0; i < numColumns - mod; ++i) {
                        this.data.add(new Section());
                    }
                }
            }



            for (List<TdApi.Sticker> stickers : sets) {
                for (TdApi.Sticker sticker : stickers) {
                    this.data.add(new Data(false, sticker));
                }

                final int mod = stickers.size() % numColumns;
                if (mod != 0){
                    for (int i = 0; i < numColumns - mod; ++i) {
                        this.data.add(new Section());
                    }
                }
            }
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Item getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {
            return getItem(position) instanceof Data ? 0 : 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            VH vh;
            final Item item = getItem(position);
            if (convertView == null) {
                if (item instanceof Data){
                    View v = viewFactory.inflate(R.layout.grid_item_sticker, parent, false);
                    vh = new VH(v);
                    v.setTag(vh);
                } else {
                    View v = viewFactory.inflate(R.layout.view_sticker_section, parent, false);
                    return v;
                }

            } else {
                vh = (VH) convertView.getTag();
            }
            if (item instanceof Section){
                return convertView;
            }

            onBindVH(vh, position);
            return vh.img;
        }

        private void onBindVH(final VH vh, int position) {
            final TdApi.Sticker s = ((Data) getItem(position)).sticker;
            vh.o = s;
            picasso.loadPhoto(s.thumb.photo, true)
                    .priority(Picasso.Priority.HIGH)
                    .into(vh.img);

            picasso.fetch(s.sticker);
        }

        public abstract class Item {

            protected Item() {

            }
        }

        class Section extends Item {

            public Section() {

            }
        }

        public class Data extends Item {
            public final boolean recents;
            public final TdApi.Sticker sticker;

            Data(boolean recents, TdApi.Sticker sticker) {
                this.recents = recents;
                this.sticker = sticker;
            }
        }
    }

    private void stickerClicked(final TdApi.Sticker sticker) {
        downloader.downloadWithoutProgress(sticker.sticker)
                .observeOn(mainThread())
                .subscribe(new ObserverAdapter<TdApi.File>() {
                    @Override
                    public void onNext(TdApi.File fileLocal) {
                        callback.stickerCLicked(fileLocal.path, sticker);
                    }
                });
    }
}
