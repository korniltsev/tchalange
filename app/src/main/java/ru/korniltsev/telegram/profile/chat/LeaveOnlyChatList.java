package ru.korniltsev.telegram.profile.chat;

import ru.korniltsev.telegram.chat_list.ChatList;
import ru.korniltsev.telegram.common.FlowHistoryStripper;

public class LeaveOnlyChatList implements FlowHistoryStripper {
    @Override
    public boolean shouldRemovePath(Object path) {
        return !(path instanceof ChatList);
    }
}
