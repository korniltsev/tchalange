package ru.korniltsev.telegram.chat_list;

import android.graphics.Bitmap;
import android.os.Bundle;
import com.crashlytics.android.core.CrashlyticsCore;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.Chat;
import ru.korniltsev.telegram.contacts.ContactList;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.emoji.Emoji;
import ru.korniltsev.telegram.core.passcode.PasscodeManager;
import ru.korniltsev.telegram.core.rx.ChatDB;
import ru.korniltsev.telegram.core.rx.RXAuthState;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.profile.my.MyProfilePath;
import rx.Observable;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static rx.Observable.concat;
import static rx.Observable.just;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

@Singleton
public class ChatListPresenter extends ViewPresenter<ChatListView> {
    private final ChatList cl;
    private final RXClient client;
    private final Emoji emoji;
    private final RXAuthState authState;
    private final PasscodeManager passcodeManager;
    private final ChatDB chatDB;
    private Observable<RXAuthState.StateAuthorized> meRequest;

//    private TdApi.User me;
    private Observable<TdApi.UpdateOption> networkState;
    private CompositeSubscription subscription;
    private RXAuthState.StateAuthorized me;

    @Inject
    public ChatListPresenter(ChatList cl, RXClient client, Emoji emoji, RXAuthState authState, PasscodeManager passcodeManager, ChatDB chatDB) {
        this.cl = cl;
        this.client = client;
        this.emoji = emoji;
        this.authState = authState;
        this.passcodeManager = passcodeManager;
        this.chatDB = chatDB;
        checkTlObjectIsSerializable();

        networkState = client.getConnectedState()
            .observeOn(mainThread());
    }



    private void checkTlObjectIsSerializable() {
        //do not forget to implement seralizable after libtd update
        //noinspection ConstantConditions
        assertTrue(new TdApi.UpdateFile() instanceof Serializable);
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        meRequest = authState.getMe(client);
        if (!chatDB.isRequestInProgress()) {
            if (!chatDB.isAtLeastOneResponseReturned()) {
                requestChats();
            } //else wait for scroll
        }


        List<TdApi.Chat> allChats = chatDB.getAllChats();
        getView().setData(allChats);
        if (!allChats.isEmpty()
                && !chatDB.isRequestInProgress()){
            chatDB.updateCurrentChatList();
        }

        subscribe();
        bindLockButton();
    }

    private void bindLockButton() {
        final boolean locked = passcodeManager.isLocked();
        final boolean enabled = passcodeManager.passCodeEnabled();
        getView().bindLockButton(locked, enabled);


    }

    private void requestChats() {
        chatDB.requestPortion();
    }

    private void subscribe() {
        subscription = new CompositeSubscription();
        subscription.add(
                chatDB.chatList().subscribe(new ObserverAdapter<List<TdApi.Chat>>() {
                    @Override
                    public void onNext(List<TdApi.Chat> chats) {
                        getView()
                                .setData(chats);
                    }
                }));

        subscription.add(
                meRequest.subscribe(new ObserverAdapter<RXAuthState.StateAuthorized>() {
                    @Override
                    public void onNext(RXAuthState.StateAuthorized state) {
                        //Log.e("ChatListPresenter", "show me " + state);
                        if (state.user == null){
                            return;
                        }
                        getView()
                                .showMe(state.user);
                        me = state;
//                        me = user;
                    }
                }));
        subscription.add(
                networkState.subscribe(new ObserverAdapter<TdApi.UpdateOption>() {
                    @Override
                    public void onNext(TdApi.UpdateOption o) {
                        if (o.value instanceof TdApi.OptionString){
                            TdApi.OptionString b = (TdApi.OptionString) o.value;
                            boolean connected;
                            switch (b.value) {
                                case "Waiting for network":
                                case "Connecting":
                                    connected = false;
                                    break;
                                default:
                                    connected = true;
                                    break;
                            }
                            ChatListPresenter.this.getView()
                                    .updateNetworkStatus(connected);
                        } else {
                            CrashlyticsCore.getInstance().logException(new IllegalStateException(o.name + " = " + o.value));
                        }

                    }
                }));
        subscription.add(emoji.pageLoaded()
                .subscribe(new ObserverAdapter<Bitmap>() {
                    @Override
                    public void onNext(Bitmap response) {
                        getView()
                                .invalidate();
                    }
                }));
    }

    public void openChat(TdApi.Chat chat) {
        if (me.user == null){
            CrashlyticsCore.getInstance()
                    .logException(new NullPointerException("me.user == null"));
            return;
        }
        if (supportedChats(chat)) {
            Flow.get(getView())
                    .set(new Chat(chat, me.user));
        } //else do nothing
    }

    private static boolean supportedChats(TdApi.Chat chat) {
        return chat.type instanceof TdApi.PrivateChatInfo
                || chat.type instanceof TdApi.GroupChatInfo;
    }

    public void logout() {
        client.logout();
    }

    @Override
    public void dropView(ChatListView view) {
        super.dropView(view);
        subscription.unsubscribe();
    }

    public void listScrolledToEnd() {
        if (chatDB.isRequestInProgress()) {
            return;
        }
        if (chatDB.isDownloadedAllChats()) {
            return;
        }
        chatDB.requestPortion();
    }

    public ChatList getCl() {
        return cl;
    }

    public void openContacts() {
        Flow.get(getView())
                .set(new ContactList());
    }

    public void openSettings() {
        final TdApi.User user = me.user;
        if (user != null){
            Flow.get(getView())
                    .set(new MyProfilePath(user));
        }
    }

    public void lockUnlock() {
        boolean locked = passcodeManager.isLocked();
        passcodeManager.setLocked(!locked);
        getView()
                .bindLockButton(!locked, passcodeManager.passCodeEnabled());
    }
}
