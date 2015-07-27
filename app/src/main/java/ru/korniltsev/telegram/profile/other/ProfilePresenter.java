package ru.korniltsev.telegram.profile.other;

import android.os.Bundle;
import android.support.annotation.Nullable;
import flow.Flow;
import mortar.ViewPresenter;
import ru.korniltsev.telegram.attach_panel.ListChoicePopup;
import ru.korniltsev.telegram.chat.Chat;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.common.FlowHistoryStripper;
import ru.korniltsev.telegram.contacts.ContactList;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.rx.NotificationManager;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ProfilePresenter extends ViewPresenter<ProfileView> implements ProfileAdapter.CallBack {
    final ProfilePath path;
    final ActivityOwner owner;
    @Nullable private ListChoicePopup popup;
    final NotificationManager nm;
    final RXClient client;
    private CompositeSubscription subscriptions;

    @Inject
    public ProfilePresenter(ProfilePath path, ActivityOwner owner, NotificationManager nm, RXClient client) {
        this.path = path;
        this.owner = owner;
        this.nm = nm;
        this.client = client;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        subscriptions = new CompositeSubscription();
        getView().bindUser(path.user);
        getView().bindMuteMenu(nm.isMuted(path.chat));
    }

    @Override
    public void dropView(ProfileView view) {
        super.dropView(view);
        subscriptions.unsubscribe();
    }

    @Override
    public void clicked(ProfileAdapter.Item item) {
        if (item.bottomSheetActions != null) {
            popup = ListChoicePopup.create(owner.expose(), item.bottomSheetActions);
        }
    }

    public boolean hidePopup() {
        if (popup != null && popup.isShowing()) {
            popup.dismiss();
            popup = null;
            return true;
        }
        popup = null;
        return false;
    }

    public void share() {
        Flow.get(getView())
                .set(new ContactList(path.user));

    }

    public void block() {

    }

    public void edit() {

    }

    public void delete() {

    }

    public void muteFor(final int duration) {
        nm.muteChat(path.chat, duration);
        getView().bindMuteMenu(nm.isMuted(path.chat));
    }

    public void startChat() {
        AppUtils.flowPushAndRemove(getView(), new Chat(path.chat, path.me),
                new FlowHistoryStripper() {
                    @Override
                    public boolean shouldRemovePath(Object o) {
                        return o instanceof Chat && ((Chat) o).chat.id == path.chat.id
                                || o instanceof ProfilePath && ((ProfilePath) o).chat.id == path.chat.id;
                    }
                }, Flow.Direction.FORWARD
        );


    }
}