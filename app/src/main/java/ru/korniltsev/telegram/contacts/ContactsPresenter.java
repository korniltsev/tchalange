package ru.korniltsev.telegram.contacts;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.Chat;
import ru.korniltsev.telegram.chat_list.ChatList;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.common.FlowHistoryStripper;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.flow.utils.Utils;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.profile.chat.ChatInfo;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

@Singleton
public class ContactsPresenter extends ViewPresenter<ContactListView> implements Action1<TdApi.User> {
    final RXClient client;
    private final Observable<List<Contact>> request;
    private CompositeSubscription subscription;
    private Observable<MeAndChat> requestOpenChat;
    private final Context appCtx;
    final ContactList path;
    private Subscription openChatSubscription = Subscriptions.empty();

    @Inject
    public ContactsPresenter(RXClient client, final Context appCtx, final ContactList path) {
        this.client = client;
        this.appCtx = appCtx;
        this.path = path;

        request = client.sendCachedRXUI(new TdApi.GetContacts())
                .map(new Func1<TdApi.TLObject, List<Contact>>() {
                    @Override
                    public List<Contact> call(TdApi.TLObject response) {
                        TdApi.Contacts contacts = (TdApi.Contacts) response;
                        final ArrayList<Contact> res = new ArrayList<>();
                        if (path.type == ContactList.TYPE_ADD_MEMBER
                                && path.filter != null) {
                            Set<Integer> filterIds = new HashSet<>();
                            for (TdApi.User it : path.filter) {
                                filterIds.add(it.id);
                            }
                            for (TdApi.User user : contacts.users) {
                                if (!filterIds.contains(user.id)) {
                                    res.add(newContact(user));
                                }
                            }
                        } else if (path.type == ContactList.TYPE_LIST
                                || path.type == ContactList.TYPE_SHARE_USER) {
                            for (TdApi.User user : contacts.users) {
                                res.add(newContact(user));
                            }
                        }

                        Collections.sort(res, new Comparator<Contact>() {
                            @Override
                            public int compare(Contact lhs, Contact rhs) {
                                return lhs.uiName.compareTo(rhs.uiName);
                            }
                        });
                        return res;
                    }
                })
                .observeOn(mainThread())
                .cache();
    }

    @NonNull
    private Contact newContact(TdApi.User user) {
        return new Contact(user, AppUtils.uiName(user, appCtx), AppUtils.uiUserStatus(appCtx, user.status));
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        subscription = new CompositeSubscription();
        subscription.add(
                request.subscribe(new ObserverAdapter<List<Contact>>() {
                    @Override
                    public void onNext(List<Contact> response) {
                        getView()
                                .showContacts(response);
                    }
                }));
        if (requestOpenChat != null) {
            subscribeOpenChat();
        }
    }

    @Override
    public void dropView(ContactListView view) {
        super.dropView(view);
        subscription.unsubscribe();
        openChatSubscription.unsubscribe();
    }

    @Override
    public void call(TdApi.User user) {
        if (path.type == ContactList.TYPE_LIST) {
            openConversation(user);//todo modify history
        } else if (path.type == ContactList.TYPE_ADD_MEMBER) {
            addChatMember(path.chat, user);
        } else if (path.type == ContactList.TYPE_SHARE_USER) {
            shareUser(user, path.sharedUser);
        }
    }

    private void shareUser(TdApi.User target, final TdApi.User sharedUser) {
        final Observable<TdApi.TLObject> me = client.sendRx(new TdApi.GetMe());
        final Observable<TdApi.TLObject> chat = client.sendRx(new TdApi.CreatePrivateChat(target.id));
        subscription.add(
                Observable.zip(me, chat, new Func2<TdApi.TLObject, TdApi.TLObject, Chat>() {
                    @Override
                    public Chat call(TdApi.TLObject me, TdApi.TLObject chat) {
                        return new Chat((TdApi.Chat) chat, (TdApi.User) me);
                    }
                }).observeOn(mainThread())
                        .subscribe(new ObserverAdapter<Chat>() {
                            @Override
                            public void onNext(Chat chat) {
                                shareUserImpl(sharedUser, chat);
                            }
                        }));
    }

    private void shareUserImpl(final TdApi.User sharedUser, Chat chat) {
        chat.sharedContact = sharedUser;
        AppUtils.flowPushAndRemove(getView(), chat, new FlowHistoryStripper() {
            @Override
            public boolean shouldRemovePath(Object path) {
                return !(path instanceof ChatList);
            }
        }, Flow.Direction.FORWARD);
    }

    private void addChatMember(TdApi.Chat chat, final TdApi.User user) {
        subscription.add(
                client.sendCachedRXUI(new TdApi.AddChatParticipant(chat.id, user.id, 0))
                        .subscribe(new ObserverAdapter<TdApi.TLObject>() {
                            @Override
                            public void onNext(TdApi.TLObject response) {
                                System.out.println(response);
                                final Flow flow = Flow.get(getView());
                                final ChatInfo previousPath = Utils.getPreviousPath(getView());
                                previousPath.addedUsers.add(user);

                                flow.goBack();
                            }
                        }));
    }

    private void openConversation(TdApi.User user) {
        final Observable<TdApi.TLObject> me = client.sendRx(new TdApi.GetMe());
        final Observable<TdApi.TLObject> chat = client.sendRx(new TdApi.CreatePrivateChat(user.id));
        requestOpenChat = Observable.zip(me, chat, new Func2<TdApi.TLObject, TdApi.TLObject, MeAndChat>() {
            @Override
            public MeAndChat call(TdApi.TLObject tlObject, TdApi.TLObject tlObject2) {
                return new MeAndChat((TdApi.User) tlObject, (TdApi.Chat) tlObject2);
            }
        }).observeOn(mainThread())
                .cache();
        subscribeOpenChat();
    }

    private void subscribeOpenChat() {
        openChatSubscription.unsubscribe();
        openChatSubscription = requestOpenChat.subscribe(new ObserverAdapter<MeAndChat>() {
            @Override
            public void onNext(MeAndChat response) {
                final Chat newTop = new Chat(response.tlObject2, response.tlObject);
                openChat(newTop);
            }
        });
    }

    private void openChat(Chat newTop) {
        AppUtils.flowPushAndRemove(getView(), newTop, new FlowHistoryStripper() {
            @Override
            public boolean shouldRemovePath(Object path) {
                return path instanceof ContactList;
            }
        }, Flow.Direction.FORWARD);
    }

    class MeAndChat {

        private final TdApi.User tlObject;
        private final TdApi.Chat tlObject2;

        public MeAndChat(TdApi.User tlObject, TdApi.Chat tlObject2) {

            this.tlObject = tlObject;
            this.tlObject2 = tlObject2;
        }
    }
}
