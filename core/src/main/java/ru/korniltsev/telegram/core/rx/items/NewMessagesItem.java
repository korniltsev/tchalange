package ru.korniltsev.telegram.core.rx.items;

public class NewMessagesItem extends ChatListItem {
    public final int newMessagesCount;
    public final long id;

    public NewMessagesItem(int newMessagesCount, long id) {
        this.newMessagesCount = newMessagesCount;
        this.id = id;
    }
}
