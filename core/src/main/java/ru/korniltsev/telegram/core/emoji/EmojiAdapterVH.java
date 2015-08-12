package ru.korniltsev.telegram.core.emoji;

import android.view.View;
import android.widget.ImageView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.utils.R;

public class EmojiAdapterVH {
    Object o;
    final ImageView img;

    public EmojiAdapterVH(final EmojiKeyboardView emojiKeyboardView, View itemView) {
        img = (ImageView) itemView.findViewById(R.id.img);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (o instanceof TdApi.Sticker) {
                    final TdApi.Sticker o = (TdApi.Sticker) EmojiAdapterVH.this.o;
                    emojiKeyboardView.callback.stickerCLicked(o);
                    emojiKeyboardView.recentStickers.count(o.sticker.persistentId);
                    emojiKeyboardView.setLastClick(EmojiKeyboardView.LAST_CLICK_STICKER);
                } else {
                    Long emojiCode = (Long) EmojiAdapterVH.this.o;
                    emojiKeyboardView.callback.emojiClicked(emojiCode);
                    emojiKeyboardView.recentEmoji.count(String.valueOf(emojiCode));
                    emojiKeyboardView.setLastClick(EmojiKeyboardView.LAST_CLICK_EMOJI);
                }
            }
        });
    }
}
