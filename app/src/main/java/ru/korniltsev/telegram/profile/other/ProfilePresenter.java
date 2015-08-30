package ru.korniltsev.telegram.profile.other;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.attach_panel.ListChoicePopup;
import ru.korniltsev.telegram.chat.Chat;
import ru.korniltsev.telegram.chat_list.ChatList;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.common.FlowHistoryStripper;
import ru.korniltsev.telegram.contacts.ContactList;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.rx.ChatDB;
import ru.korniltsev.telegram.core.rx.NotificationManager;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.core.rx.UserHolder;
import ru.korniltsev.telegram.profile.chatselection.SelectChatPath;
import ru.korniltsev.telegram.profile.media.SharedMediaPath;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import javax.inject.Singleton;

import static rx.Observable.combineLatest;
import static rx.Observable.zip;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

@Singleton
public class ProfilePresenter extends ViewPresenter<ProfileView> implements ProfileAdapter.CallBack {

    final ProfilePath path;
    final ActivityOwner owner;
    private final RxChat rxChat;
    private final UserHolder userHolder;
    @Nullable private ListChoicePopup popup;
    final NotificationManager nm;
    final RXClient client;
    private CompositeSubscription subscriptions;

    private boolean blocked;

    @Inject
    public ProfilePresenter(Context ctx, ProfilePath path, ActivityOwner owner, NotificationManager nm, RXClient client, ChatDB chat) {
        this.path = path;
        this.owner = owner;
        this.nm = nm;
        this.client = client;
        rxChat = chat.getRxChat(path.chat.id);
        userHolder = MyApp.from(ctx).userHolder;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        subscriptions = new CompositeSubscription();
        //        getView().bindUser(path.user);
        final ProfileView view = getView();
        view.bindMuteMenu(nm.isMuted(path.chat));
        view.bindUserAvatar(path.user);
        view.bindBlockMenu(blocked);
        subscriptions.add(
                blockInfo()
                        .subscribe(new ObserverAdapter<Boolean>() {
                            @Override
                            public void onNext(Boolean blocked) {
                                ProfilePresenter.this.blocked = blocked;
                                view.bindBlockMenu(blocked);
                            }
                        }));

        final Observable<UserInfo> userInfo = combineLatest(
                rxChat.getMediaPreview(),
                userHolder.getUserFull(client, path.user.id),
                new Func2<TdApi.Messages, TdApi.UserFull, UserInfo>() {
                    @Override
                    public UserInfo call(TdApi.Messages messages, TdApi.UserFull userFull) {
                        return new UserInfo(messages, userFull);
                    }
                });

        subscriptions.add(
                userInfo.subscribe(new ObserverAdapter<UserInfo>() {
                    @Override
                    public void onNext(UserInfo response) {
                        bindUser(response);
                    }
                })
        );
    }

    private void bindUser(final UserInfo response) {
        AppUtils.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                ProfileView v = getView();
                if (v == null) {
                    return;
                }
                v.bindUser(response.user, response.ms);
            }
        });
    }

    @NonNull
    private Observable<Boolean> blockInfo() {
        final Observable<Boolean> getUserFull = client.sendCachedRXUI(new TdApi.GetUserFull(path.user.id))
                .map(new Func1<TdApi.TLObject, Boolean>() {
                    @Override
                    public Boolean call(TdApi.TLObject tlObject) {
                        return ((TdApi.UserFull) tlObject).isBlocked;
                    }
                });
        final Observable<Boolean> updates = client.updateUserBlocked(path.user.id);
        return Observable.concat(getUserFull, updates)
                .observeOn(mainThread());
    }

    @Override
    public void dropView(ProfileView view) {
        super.dropView(view);
        subscriptions.unsubscribe();
    }

    @Override
    public void clicked(ProfileAdapter.KeyValueItem item) {
        if (item.bottomSheetActions != null) {
            popup = ListChoicePopup.create(owner.expose(), item.bottomSheetActions);
        }
    }

    @Override
    public void sharedMediaClicked() {
        Flow.get(getView())
                .set(new SharedMediaPath(path.chat.id, SharedMediaPath.TYPE_MEDIA));
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
        if (blocked) {
            client.sendSilently(new TdApi.UnblockUser(path.user.id));
        } else {
            client.sendSilently(new TdApi.BlockUser(path.user.id));
        }
        blocked = !blocked;
        getView().bindBlockMenu(blocked);
    }



    public void delete() {
        final int id = path.user.id;
        final int[] ids = new int[]{id};
        subscriptions.add(
                client.sendRx(new TdApi.DeleteContacts(ids))
                        .observeOn(mainThread())
                        .subscribe(new ObserverAdapter<TdApi.TLObject>() {
                            @Override
                            public void onNext(TdApi.TLObject response) {
                                AppUtils.flowPushAndRemove(getView(), null, new FlowHistoryStripper() {
                                    @Override
                                    public boolean shouldRemovePath(Object path) {
                                        return !(path instanceof ChatList);
                                    }
                                }, Flow.Direction.BACKWARD);
                            }
                        }));
    }

    public void muteFor(final int duration) {
        nm.muteChat(path.chat, duration);
        getView().bindMuteMenu(nm.isMuted(path.chat));
    }

    public void startChat() {
        AppUtils.flowPushAndRemove(getView(), new Chat(path.chat, path.me, /* messages to forward */ null),
                new FlowHistoryStripper() {
                    @Override
                    public boolean shouldRemovePath(Object o) {
                        return o instanceof Chat && ((Chat) o).chat.id == path.chat.id
                                || o instanceof ProfilePath && ((ProfilePath) o).chat.id == path.chat.id;
                    }
                }, Flow.Direction.FORWARD
        );
    }

    public void addBotToGroup() {
        Flow.get(getView())
                .set(new SelectChatPath(path.user, null, 0, path.me, true));
    }

    class UserInfo {
        final TdApi.Messages ms;
        final TdApi.UserFull user;

        public UserInfo(TdApi.Messages ms, TdApi.UserFull user) {
            this.ms = ms;
            this.user = user;
        }
    }
}
