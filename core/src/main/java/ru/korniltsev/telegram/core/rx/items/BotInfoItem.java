package ru.korniltsev.telegram.core.rx.items;

import android.text.Spannable;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.rx.DaySplitter;
import ru.korniltsev.telegram.core.rx.EmojiParser;

public class BotInfoItem extends ChatListItem {
    public final TdApi.BotInfoGeneral botInfo;
    public final Spannable descriptionWithEmoji;

    public final long id = DaySplitter.ID_BOT_INFO;

    public BotInfoItem(TdApi.BotInfoGeneral botInfo, Spannable descriptionWithEmoji) {
        this.botInfo = botInfo;

        this.descriptionWithEmoji = descriptionWithEmoji;
    }
}
