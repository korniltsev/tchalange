package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.debug.CustomCeilLayout;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;
import ru.korniltsev.telegram.core.utils.Colors;
import ru.korniltsev.telegram.core.views.AvatarView;
import ru.korniltsev.telegram.common.AppUtils;

import java.text.SimpleDateFormat;
import java.util.Locale;

abstract class BaseAvatarVH extends RealBaseVH {
//    private static final DateTimeFormatter MESSAGE_TIME_FORMAT = DateTimeFormat.forPattern("K:mm aa")
//            .withLocale(Locale.US);

    private final int myId = adapter.myId;
    protected final CustomCeilLayout root;

    public BaseAvatarVH(CustomCeilLayout itemView, final Adapter adapter) {
        super(itemView, adapter);
        root = itemView;
        if (adapter.isGroup){
            itemView.avatarView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final ChatListItem item = adapter.getItem(getAdapterPosition());
                    if (item instanceof MessageItem){
                        final TdApi.Message msg = ((MessageItem) item).msg;
                        adapter.cb.avatarOfMessageClicked(msg);
                    }
                }
            });
        }


    }

    public static void colorizeNick(TextView v) {
        v.setTextColor(Colors.USER_NAME_COLOR_STATE_LIST);
    }

    public void bind(ChatListItem item, long lastReadOutbox){
        TextMessageVH.newBind(root, adapter, item, lastReadOutbox);
    }

//    static SimpleDateFormat fuckRuFormatter = new SimpleDateFormat("kk:mm", Locale.US);
    public static String format(TdApi.Message msg) {
        return msg.dateFormatted;
//        Locale l = Locale.getDefault();
//        if (l)
//        long timeInMillis = Utils.dateToMillis(msg.date);
//        long local = DateTimeZone.UTC.convertUTCToLocal(timeInMillis);
//        if (Locale.getDefault().getCountry().equals("RU")){
//            return fuckRuFormatter.format(local);//todo wtf
//        } else {
//            return MESSAGE_TIME_FORMAT.print(local);
//        }
    }
}
