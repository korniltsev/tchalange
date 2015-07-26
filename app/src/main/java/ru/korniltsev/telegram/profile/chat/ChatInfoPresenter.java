package ru.korniltsev.telegram.profile.chat;

import android.os.Bundle;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.Chat;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.common.FlowHistoryStripper;
import ru.korniltsev.telegram.contacts.ContactList;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.rx.NotificationManager;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.Observable;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

@Singleton
public class ChatInfoPresenter extends ViewPresenter<ChatInfoView> implements ChatInfoAdapter.CallBack {
    final ChatInfo path;
    final ActivityOwner owner;
    final NotificationManager notifications;
    final RXClient client;
    private CompositeSubscription subscriptions;

    @Inject
    public ChatInfoPresenter(ChatInfo path, ActivityOwner owner, NotificationManager notifications, RXClient client) {
        this.path = path;
        this.owner = owner;
        this.notifications = notifications;
        this.client = client;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        subscriptions = new CompositeSubscription();
        getView().bindUser(path);
        final boolean muted = notifications.isMuted(path.chat.notificationSettings);
        getView().bindMuteMenu(muted);
    }

    @Override
    public void dropView(ChatInfoView view) {
        super.dropView(view);
        subscriptions.unsubscribe();
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
        subscriptions.add(
                client.sendRx(new TdApi.DeleteChatHistory(path.chat.id))
                        .flatMap(new Func1<TdApi.TLObject, Observable<TdApi.User>>() {
                            @Override
                            public Observable<TdApi.User> call(TdApi.TLObject tlObject) {
                                return client.getMe();
                            }
                        })
                        .flatMap(new Func1<TdApi.User, Observable<TdApi.TLObject>>() {
                            @Override
                            public Observable<TdApi.TLObject> call(TdApi.User me) {
                                return client.sendRx(new TdApi.DeleteChatParticipant(path.chat.id, me.id));
                            }
                        })
                        .observeOn(mainThread())
                .subscribe(new ObserverAdapter<TdApi.TLObject>() {
                    @Override
                    public void onNext(TdApi.TLObject response) {
                        goBackTwice();
                    }
                }));
    }

    private void goBackTwice() {
        AppUtils.flowPushAndRemove(getView(), null, new FlowHistoryStripper() {
            @Override
            public boolean shouldRemovePath(Object path) {
                return path instanceof ChatInfo && ((ChatInfo) path).chat.id == ((ChatInfo) path).chat.id
                        || path instanceof Chat && ((Chat) path).chat.id == ((Chat) path).chat.id;
            }
        }, Flow.Direction.BACKWARD);

    }

    public void editChatName() {

    }

    public void muteFor(int durationSeconds) {
        notifications.muteChat(path.chat, durationSeconds);
        getView().bindMuteMenu(notifications.isMuted(path.chat));
    }

    public void changePhoto() {

    }
}
