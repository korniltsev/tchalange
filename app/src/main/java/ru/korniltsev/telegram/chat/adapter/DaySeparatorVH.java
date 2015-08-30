package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import android.widget.TextView;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.DaySeparatorItem;

import java.util.Calendar;
import java.util.Locale;

public class DaySeparatorVH extends RealBaseVH {



    private final DaySeparatorView text;

    public DaySeparatorVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        text = (DaySeparatorView) itemView;
    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
        DaySeparatorItem s = (DaySeparatorItem) item;
        text.setText(s.dayFormatted);

    }





}
