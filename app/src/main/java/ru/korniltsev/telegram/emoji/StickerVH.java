package ru.korniltsev.telegram.emoji;

import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;

public class StickerVH {
    TdApi.Sticker o;
    final PooledImageView img;

    public StickerVH(final EmojiKeyboardView emojiKeyboardView, View itemView) {
        img = (PooledImageView) itemView.findViewById(R.id.img);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (o instanceof TdApi.Sticker) {
                    final TdApi.Sticker o = StickerVH.this.o;
                    emojiKeyboardView.callback.stickerCLicked(o);
                    emojiKeyboardView.recentStickers.count(o.sticker.persistentId);
                    emojiKeyboardView.setLastClick(EmojiKeyboardView.LAST_CLICK_STICKER);
//                } else {
//                    Long emojiCode = (Long) StickerVH.this.o;
//                    emojiKeyboardView.callback.emojiClicked(emojiCode);
//                    emojiKeyboardView.recentEmoji.count(String.valueOf(emojiCode));
//                    emojiKeyboardView.setLastClick(EmojiKeyboardView.LAST_CLICK_EMOJI);
//                }
            }
        });
    }
}
