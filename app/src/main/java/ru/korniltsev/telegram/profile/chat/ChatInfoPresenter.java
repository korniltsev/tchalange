package ru.korniltsev.telegram.profile.chat;

import android.os.Bundle;
import android.support.annotation.Nullable;
import mortar.ViewPresenter;
import ru.korniltsev.telegram.attach_panel.ListChoicePopup;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ChatInfoPresenter extends ViewPresenter<ChatInfoView> implements ChatInfoAdapter.CallBack {
    final ChatInfo path;
    final ActivityOwner owner;
//    @Nullable private ListChoicePopup popup;

    @Inject
    public ChatInfoPresenter(ChatInfo path, ActivityOwner owner) {
        this.path = path;
        this.owner = owner;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        getView().bindUser(path);

    }

    @Override
    public void btnAddMemberClicked() {

    }

    @Override
    public void participantClicked(ChatInfoAdapter.ParticipantItem item) {

    }
}
