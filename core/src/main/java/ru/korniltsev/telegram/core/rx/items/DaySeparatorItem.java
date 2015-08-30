package ru.korniltsev.telegram.core.rx.items;

import org.joda.time.DateTime;

public class DaySeparatorItem extends ChatListItem {
    public final long id;
    public final DateTime day;//millis
    public final String dayFormatted;

    public DaySeparatorItem(long id, DateTime day, String dayFormatted) {
        this.id = id;
        this.day = day;
        this.dayFormatted = dayFormatted;
    }
}
