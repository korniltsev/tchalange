package ru.korniltsev.telegram.core.emoji.strip;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.emoji.StickerAdapter;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.utils.R;

import javax.inject.Inject;
import java.lang.ref.WeakReference;
import java.util.List;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class EmojiPagerStripView extends ViewGroup {
    static final int[] icons = new int[]{
            R.drawable.ic_smiles_recent_selector,
            R.drawable.ic_smiles_smiles_selector,
            R.drawable.ic_smiles_flower_selector,
            R.drawable.ic_smiles_bell_selector,
            R.drawable.ic_smiles_car_selector,
            R.drawable.ic_smiles_grid_selector,
            R.drawable.ic_smiles_stickers_selector,
            R.drawable.ic_smiles_backspace_selector,
    };

    static final int emoji_page_count = icons.length - 2;
    private final LinearLayoutWithStrip emoji;
    private final HorizontalScrollView stickers;
    private final Paint divider;
    private final DpCalculator calc;
    private final int dp1;
    private final int stripWidth;
    private int currentPosition;
    private float currentPositionOffset;

    @Inject RxGlide glide;
    private List<TdApi.StickerSet> stickerSets;
    private WeakReference<GridView> stickerGrid;
    private LinearLayout stickerSetsButtonContainer;

    public EmojiPagerStripView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
        final MyApp app = MyApp.from(context);
        calc = app.dpCalculator;

        emoji = new LinearLayoutWithStrip(context);
        for (int icon : icons) {
            final ImageButton button = new ImageButton(context);
            final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, MATCH_PARENT);
            lp.weight = 1;
            button.setLayoutParams(lp);
            button.setBackgroundResource(R.drawable.bg_keyboard_tab);
            button.setImageResource(icon);
            emoji.addView(button);
        }
        addView(emoji);

        stickers = new HorizontalScrollView(context);
        stickers.setHorizontalScrollBarEnabled(false);

        addView(stickers);

        stripWidth = app.displayWidth / icons.length;

        divider = new Paint();
        divider.setColor(0xFFE2E5E7);
        dp1 = calc.dp(1);

        setWillNotDraw(false);
    }

    final Rect r = new Rect();

    @Override
    protected void onDraw(Canvas canvas) {
        int bottom = getBottom();
        r.set(getLeft(), bottom - dp1, getRight(), bottom);
        canvas.drawRect(r, divider);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (widthMode != EXACTLY || heightMode != EXACTLY) {
            throw new RuntimeException("unsupported");
        }

        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int height = MeasureSpec.getSize(heightMeasureSpec);
        emoji.measure(widthMeasureSpec, heightMeasureSpec);
        stickers.measure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        emoji.layout(l, t, r, b);
        int w = r - l;
        stickers.layout(l + w, t, r + w, b);
    }

    public void init(final ViewPager pager, final Runnable onBackspaceClicked, List<TdApi.StickerSet> stickers) {
        pager.addOnPageChangeListener(new MyAnimator());
        initEmoji(pager, onBackspaceClicked);
        initStickers(pager, stickers);
    }

    private void initStickers(final ViewPager pager, List<TdApi.StickerSet> data) {
        final Context ctx = getContext();
        stickerSetsButtonContainer = new LinearLayout(ctx);
        final int dp7 = calc.dp(7);
        final ImageButton smilesButton = new SquareImageButton(ctx);
        smilesButton.setScaleType(ImageView.ScaleType.CENTER);
        smilesButton.setImageResource(R.drawable.ic_smiles_smile);
        smilesButton.setBackgroundResource(R.drawable.bg_keyboard_tab);
        smilesButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                pager.setCurrentItem(0, true);
            }
        });

        final ImageButton recent = new SquareImageButton(ctx);
        recent.setScaleType(ImageView.ScaleType.CENTER);
        recent.setImageResource(R.drawable.ic_smiles_recent);
        recent.setBackgroundResource(R.drawable.bg_sticker_set_selector);
        stickerSetsButtonContainer.addView(smilesButton, 0, MATCH_PARENT);
        stickerSetsButtonContainer.addView(recent, 0, MATCH_PARENT);
        recent.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollToRecentStickers();
            }
        });

        for (final TdApi.StickerSet sticker : data) {
            final ImageButton imageButton = new SquareImageButton(ctx);
            imageButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageButton.setPadding(dp7, dp7, dp7, dp7);
            imageButton.setBackgroundResource(R.drawable.bg_sticker_set_selector);
            glide.loadPhoto(sticker.stickers[0].thumb.photo, true)
                    .into(imageButton);
            imageButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    scrollToStickerSet(sticker);
                }
            });
            stickerSetsButtonContainer.addView(imageButton, WRAP_CONTENT, MATCH_PARENT);
        }
        stickers.addView(stickerSetsButtonContainer, WRAP_CONTENT, MATCH_PARENT);
        this.stickerSets = data;
    }

    private void scrollToRecentStickers() {
        if (stickerGrid == null) {
            return;
        }
        final GridView gridView = stickerGrid.get();
        if (gridView == null) {
            return;
        }
        gridView.smoothScrollToPosition(0);
    }

    private void scrollToStickerSet(TdApi.StickerSet set) {
        if (stickerGrid == null) {
            return;
        }
        final GridView gridView = stickerGrid.get();
        if (gridView == null) {
            return;
        }
        final StickerAdapter a = (StickerAdapter) gridView.getAdapter();
        final List<StickerAdapter.Item> data = a.getData();
        for (int i = 0; i < data.size(); i++) {
            StickerAdapter.Item item = data.get(i);
            if (item instanceof StickerAdapter.Data) {
                final StickerAdapter.Data d = (StickerAdapter.Data) item;
                if (!d.recents) {
                    if (d.sticker.setId == set.id) {
                        gridView.setSelection(i);
                        return;
                    }
                }
            }
        }
    }

    private void initEmoji(final ViewPager pager, final Runnable onBackspaceClicked) {
        for (int i = 0; i < icons.length - 1; i++) {
            final int finalI = i;
            emoji.getChildAt(i).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    pager.setCurrentItem(finalI, true);
                }
            });
        }
        emoji.getChildAt(icons.length - 1).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackspaceClicked.run();
            }
        });
    }

    private void translate(int positionOffsetPixels) {
        emoji.setTranslationX(-positionOffsetPixels);
        stickers.setTranslationX(-positionOffsetPixels);
    }

    public void initStickerScroll(GridView res) {
        this.stickerGrid = new WeakReference<>(res);
        res.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                final GridView view1 = (GridView) view;
                final Object itemAtPosition = view1.getItemAtPosition(firstVisibleItem);
                if (itemAtPosition instanceof StickerAdapter.Data) {
                    final StickerAdapter.Data d = (StickerAdapter.Data) itemAtPosition;
                    setSelectedStickerPack(d);
                }
            }
        });
        //todo select first
    }

    int currentStickerSetPosition = 0;
    private void setSelectedStickerPack(StickerAdapter.Data setId) {
        int position = 1;
        if (setId.recents) {
            position = 1;
        } else {
            for (int i = 0, stickerSetsSize = stickerSets.size(); i < stickerSetsSize; i++) {
                TdApi.StickerSet s = stickerSets.get(i);
                if (s.id == setId.sticker.setId){
                    position = i + 2;
                }
            }
        }
        if (currentStickerSetPosition == position){
            return;
        }
        currentStickerSetPosition = position;
        for (int i = 0; i < stickerSetsButtonContainer.getChildCount(); i++) {
            final View childAt = stickerSetsButtonContainer.getChildAt(i);
            final boolean selected = i == position;
            childAt.setSelected(selected);
            if (selected) {
                checkVisible(childAt, i);
            }
        }
    }

    final int[] tmpRect = new int[2];
    private void checkVisible(View childAt, int position) {
        final int right = childAt.getRight();
        final int width = stickers.getWidth();
//        childAt.getLocationOnScreen(tmpRect);
//        int x = tmpRect[0];
//        int left = x;
//        int right = x + childAt.getWidth();
        if (right > width) {
            int diff = right - width;
            diff += calc.dp(32f);
            stickers.smoothScrollBy(diff, 0);
        } else {
            final int left = childAt.getLeft() - stickers.getScrollX();
            if (position == 2){
                stickers.smoothScrollTo(0, 0);
            } else if (left < 0 ) {
                stickers.smoothScrollTo(childAt.getLeft() - calc.dp(16), 0);
            }
        }

//        else {
//            if (left < 0){
//                final int diff = left - calc.dp(64f);
//                stickers.smoothScrollBy(diff, 0);
//            }
//        }
    }

    class LinearLayoutWithStrip extends LinearLayout {

        private final Paint strip;

        public LinearLayoutWithStrip(Context context) {
            super(context);
            setWillNotDraw(false);
            strip = new Paint();
            strip.setColor(0xFF66ACDF);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int bottom = getBottom();
            int left = (int) (currentPosition * stripWidth + currentPositionOffset * stripWidth);
            int right = left + stripWidth;
            int top = bottom - dp1 * 2;
            r.set(left, top, right, bottom);
            canvas.drawRect(r, strip);
        }
    }

    private class MyAnimator extends OnPageChangeListenerAdapter {
        @Override
        public void onPageSelected(int position) {
            for (int i = 0; i < emoji.getChildCount(); ++i) {
                View child = emoji.getChildAt(i);
                child.setSelected(i == position);
            }
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            Log.d("EmojiPagerStripView", String.format("position %d, positionOffset %f, positionOffsetPx %d", position, positionOffset, positionOffsetPixels));

            currentPosition = position;
            currentPositionOffset = positionOffset;

            if (position == emoji_page_count - 1) {
                translate(positionOffsetPixels);
            } else if (position == emoji_page_count) {
                translate(getWidth());
            } else {
                translate(0);
            }

            emoji.invalidate();
        }
    }
}
