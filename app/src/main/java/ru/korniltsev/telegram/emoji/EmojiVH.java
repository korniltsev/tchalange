package ru.korniltsev.telegram.emoji;

import android.view.View;
import android.widget.ImageView;
import ru.korniltsev.telegram.chat.R;

public class EmojiVH {
    Long o;
    final ImageView img;

    public EmojiVH(final EmojiKeyboardView emojiKeyboardView, View itemView) {
        img = (ImageView) itemView.findViewById(R.id.img);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (o instanceof TdApi.Sticker) {
//                    final TdApi.Sticker o = (TdApi.Sticker) EmojiVH.this.o;
//                    emojiKeyboardView.callback.stickerCLicked(o);
//                    emojiKeyboardView.recentStickers.count(o.sticker.persistentId);
//                    emojiKeyboardView.setLastClick(EmojiKeyboardView.LAST_CLICK_STICKER);
//                } else {
                    Long emojiCode = EmojiVH.this.o;
                    emojiKeyboardView.callback.emojiClicked(emojiCode);
                    emojiKeyboardView.recentEmoji.count(String.valueOf(emojiCode));
                    emojiKeyboardView.setLastClick(EmojiKeyboardView.LAST_CLICK_EMOJI);
//                }
            }
        });
    }
}
