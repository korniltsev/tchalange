package ru.korniltsev.telegram.core.rx;

import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;

import java.util.List;

public class Portion {
    public final List<TdApi.Message> ms;
    public final List<ChatListItem> split;

    public Portion(List<TdApi.Message> ms, List<ChatListItem> split) {
        this.ms = ms;

        this.split = split;
    }
}
