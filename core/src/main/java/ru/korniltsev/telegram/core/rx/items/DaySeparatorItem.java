package ru.korniltsev.telegram.core.rx.items;

import org.joda.time.DateTime;

public class DaySeparatorItem extends ChatListItem {
    public final long id;
    public final DateTime day;//millis

    public DaySeparatorItem(long id, DateTime day) {
        this.id = id;
        this.day = day;
    }
}
