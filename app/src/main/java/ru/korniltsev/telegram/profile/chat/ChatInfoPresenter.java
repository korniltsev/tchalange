package ru.korniltsev.telegram.profile.chat;

import android.os.Bundle;
import android.support.annotation.Nullable;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.attach_panel.ListChoicePopup;
import ru.korniltsev.telegram.contacts.ContactList;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.rx.NotificationManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class ChatInfoPresenter extends ViewPresenter<ChatInfoView> implements ChatInfoAdapter.CallBack {
    final ChatInfo path;
    final ActivityOwner owner;
    final NotificationManager notifications;
    private ListChoicePopup mutePopup;
    //    @Nullable private ListChoicePopup popup;

    @Inject
    public ChatInfoPresenter(ChatInfo path, ActivityOwner owner, NotificationManager notifications) {
        this.path = path;
        this.owner = owner;
        this.notifications = notifications;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        getView().bindUser(path);
        final boolean muted = notifications.isMuted(path.chat.notificationSettings);
        getView().bindMuteMenu(muted);
    }

    @Override
    public void btnAddMemberClicked() {
        Flow.get(getView())
                .set(new ContactList(createFilter(), path.chat));
    }

    private List<TdApi.User> createFilter() {
        final ArrayList<TdApi.User> res = new ArrayList<>();
        for (TdApi.ChatParticipant p : path.chatFull.participants) {
            res.add(p.user);
        }
        return res;
    }

    @Override
    public void participantClicked(ChatInfoAdapter.ParticipantItem item) {

    }

    public void deleteAndLeave() {

    }

    public void editChatName() {

    }


    public void muteFor(int durationSeconds) {
        notifications.muteChat(path.chat, durationSeconds);
        getView().bindMuteMenu(notifications.isMuted(path.chat));
    }


}
