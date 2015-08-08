package ru.korniltsev.telegram.core.rx;

import org.drinkless.td.libcore.telegram.TdApi;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.DaySeparatorItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;
import ru.korniltsev.telegram.core.rx.items.NewMessagesItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertTrue;

public class DaySplitter {

    public static final long ID_NEW_MESSAGES = -1;
    public static final long ID_BOT_INFO = -2;
    //guarded by lock
    private final Map<DateTime, DaySeparatorItem> cache = new HashMap<>();
    //guarded by lock
    private int counter = -10;

    public final Object lock = new Object();


    public boolean hasTheSameDay(TdApi.Message a, TdApi.Message b) {
        return hasTheSameDay(timInMillis(a), timInMillis(b));
    }

    private long timInMillis(TdApi.Message b) {
        return Utils.dateToMillis(b.date);
    }

    public boolean hasTheSameDay(long aTime, long bTime) {
        DateTime dateTimeA = localTime(aTime);
        DateTime dateTimeB = localTime(bTime);
        return dateTimeA.withTimeAtStartOfDay()
                .equals(dateTimeB.withTimeAtStartOfDay());
    }

    private DateTime localTime(long aTime) {

        DateTimeZone utcZone = DateTimeZone.UTC;
        long localTime = utcZone.convertUTCToLocal(aTime);
        return new DateTime(localTime);
    }

    //  0 item
    //  1 separator
    //  2 item
    //  3 separator
    public List<ChatListItem> split(List<TdApi.Message> ms) {
        if (ms.isEmpty()) {
            return new ArrayList<>();
        }

        ArrayList<ChatListItem> res = new ArrayList<>();
        TdApi.Message current = ms.get(0);
        res.add(new MessageItem(current));
        for (int i = 1; i < ms.size(); ++i) {
            TdApi.Message it = ms.get(i);
            if (hasTheSameDay(current, it)) {
                res.add(new MessageItem(it));
            } else {

                res.add(createSeparator(current));
                res.add(new MessageItem(it));
            }
            current = it;
        }
        res.add(createSeparator(current));
        return res;
    }




    public DaySeparatorItem createSeparator(TdApi.Message msg) {
        DateTime time = localTime(timInMillis(msg))
                .withTimeAtStartOfDay();
        synchronized (lock){
            DaySeparatorItem cached = cache.get(time);
            if (cached != null) {
                return cached;
            } else {
                DaySeparatorItem newSeparator = new DaySeparatorItem(counter--, time);
                cache.put(time, newSeparator);
                return newSeparator;
            }
        }

    }

    public List<ChatListItem> prepend(List<ChatListItem> data, List<ChatListItem> newMessages) {
        assertTrue(newMessages.size() > 1);//message item + date item

        if (data.isEmpty()) {
            return newMessages;
        } else {
            MessageItem firstMessage = (MessageItem) data.get(0);
            MessageItem lastNewMessage = (MessageItem) newMessages.get(newMessages.size() - 2);
            if (hasTheSameDay(lastNewMessage.msg, firstMessage.msg)) {
                final ChatListItem removedDateItem = newMessages.remove(newMessages.size() - 1);
                assertTrue(removedDateItem instanceof DaySeparatorItem);
            }
        }
        return newMessages;
//        RxChat.MessageItem newMessageItem = new RxChat.MessageItem(message);
//        if (addDateItem) {
//            return Arrays.asList(
//                    createSeparator(message),
//                    newMessageItem
//            );
//        } else {
//            return Collections.<RxChat.ChatListItem>singletonList(
//                    newMessageItem
//            );
//        }
    }

    //notice: may not insert the NewMessagesItem!!
    public NewMessagesItem insertNewMessageItem(List<ChatListItem> split, TdApi.Chat chat, int myId) {
        int lastReadIndex = -1;
        for (int i = 0, splitSize = split.size(); i < splitSize; i++) {
            ChatListItem it = split.get(i);
            if (it instanceof MessageItem) {
                final TdApi.Message msg = ((MessageItem) it).msg;
                if (msg.id == chat.lastReadInboxMessageId) {
                    lastReadIndex = i;
                }
            }
        }
        final NewMessagesItem result = new NewMessagesItem(chat.unreadCount, ID_NEW_MESSAGES);
        if (lastReadIndex == -1) {
            split.add(result);
            return result;
        }
        for (int i = lastReadIndex-1; i >= 0; i--) {
            ChatListItem it = split.get(i);
            if (it instanceof MessageItem){
                final MessageItem messageItem = (MessageItem) it;
                final TdApi.Message msg = messageItem.msg;
                if (msg.fromId != myId) {//income message
                    int insertIndex = i + 1;
                    if (insertIndex < split.size()//есть чо сверху
                            && split.get(insertIndex) instanceof DaySeparatorItem) {//и это сепаратор
                        insertIndex++;
                    }
                    split.add(insertIndex, result);
                    return result;
                }
            }
        }

        return result;
    }
}
