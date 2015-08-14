package ru.korniltsev.telegram.emoji;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.squareup.picasso.Picasso;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.utils.R;

import java.util.ArrayList;
import java.util.List;

public class StickerAdapter extends BaseAdapter {
    private EmojiKeyboardView emojiKeyboardView;
    final List<Item> data;

    public List<Item> getData() {
        return data;
    }

    private final int numColumns;

    StickerAdapter(EmojiKeyboardView emojiKeyboardView, List<List<TdApi.Sticker>> sets, List<TdApi.Sticker> recentStickers) {
        this.emojiKeyboardView = emojiKeyboardView;
        this.data = new ArrayList<>();
        numColumns = emojiKeyboardView.displayWidth / emojiKeyboardView.getContext().getResources().getDimensionPixelSize(R.dimen.sticker_size);

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
        EmojiAdapterVH vh;
        final Item item = getItem(position);
        if (convertView == null) {
            if (item instanceof Data){
                View v = emojiKeyboardView.viewFactory.inflate(R.layout.grid_item_sticker, parent, false);
                vh = new EmojiAdapterVH(emojiKeyboardView, v);
                v.setTag(vh);
            } else {
                View v = emojiKeyboardView.viewFactory.inflate(R.layout.view_sticker_section, parent, false);
                return v;
            }

        } else {
            vh = (EmojiAdapterVH) convertView.getTag();
        }
        if (item instanceof Section){
            return convertView;
        }

        onBindVH(vh, position);
        return vh.img;
    }

    private void onBindVH(final EmojiAdapterVH vh, int position) {
        final TdApi.Sticker s = ((Data) getItem(position)).sticker;
        vh.o = s;
        emojiKeyboardView.picasso.loadPhoto(s.thumb.photo, true)
                .priority(Picasso.Priority.HIGH)
                .into(vh.img);

        emojiKeyboardView.picasso.fetch(s.sticker);
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
