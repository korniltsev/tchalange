package ru.korniltsev.telegram.core.rx.items;

import org.drinkless.td.libcore.telegram.TdApi;

public class MessageItem extends ChatListItem {
    public final TdApi.Message msg;

    public MessageItem(TdApi.Message msg) {
        this.msg = msg;
    }
}
