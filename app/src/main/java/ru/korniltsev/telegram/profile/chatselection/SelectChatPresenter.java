package ru.korniltsev.telegram.profile.chatselection;

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
import ru.korniltsev.telegram.core.rx.ChatDB;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

@Singleton
public class SelectChatPresenter extends ViewPresenter<SelectChatView> {
    final SelectChatPath path;
    private CompositeSubscription subscriptions;
    private final ChatDB chats;
    final RXClient client;

    @Inject
    public SelectChatPresenter(SelectChatPath path, ChatDB chats, RXClient client) {
        this.path = path;
        this.chats = chats;
        this.client = client;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        subscriptions = new CompositeSubscription();
        getView()
                .init(path.me, chats);

        if (path.filterOnlyGroupChats){
            final List<TdApi.Chat> filtered = filter(chats.getAllChats());
            getView()
                    .setData(filtered);
            if (filtered.isEmpty()) {
                tryRequestNewPortion();
            }
        } else {
            final List<TdApi.Chat> allChats = chats.getAllChats();
            getView().setData(allChats);

            if (allChats.isEmpty()) {
                tryRequestNewPortion();
            }
        }



        subscriptions.add(
                chats.chatList()
                        .subscribe(new ObserverAdapter<List<TdApi.Chat>>() {
                            @Override
                            public void onNext(List<TdApi.Chat> response) {
                                final List<TdApi.Chat> onlyGroupChats = filter(response);

                                final List<TdApi.Chat> oldData = getView().getData();
                                if (oldData.size() == onlyGroupChats.size()) { // хуевое решение
                                    tryRequestNewPortion();
                                }
                                getView()
                                        .setData(onlyGroupChats);
                            }
                        }));
    }

    @NonNull
    private List<TdApi.Chat> filter(List<TdApi.Chat> response) {
        final List<TdApi.Chat> onlyGroupChats = new ArrayList<TdApi.Chat>();
        for (TdApi.Chat chat : response) {
            if (chat.type instanceof TdApi.GroupChatInfo) {
                onlyGroupChats.add(chat);
            }
        }
        return onlyGroupChats;
    }

    @Override
    public void dropView(SelectChatView view) {
        super.dropView(view);
        subscriptions.unsubscribe();
    }

    public void listScrolledToEnd() {
        tryRequestNewPortion();
    }

    private void tryRequestNewPortion() {
        if (chats.isRequestInProgress()) {
            return;
        }
        if (chats.isDownloadedAllChats()) {
            return;
        }
        chats.requestPortion();
    }

    Subscription s = Subscriptions.empty();

    public void chatSelected(final TdApi.Chat chat) {
        if (path.bot != null){
            s = client.sendRx(new TdApi.AddChatParticipant(chat.id, path.bot.id, 0))
                    .observeOn(mainThread())
                    .subscribe(new ObserverAdapter<TdApi.TLObject>() {
                        @Override
                        public void onNext(TdApi.TLObject response) {
                            open(chat);
                        }
                    });
        }
        if (path.messagesToForward != null) {
            final TdApi.ForwardMessages f = new TdApi.ForwardMessages(chat.id, path.forwardedMessaesFromChatId, path.messagesToForward);
            final Chat newHead = new Chat(chat, path.me, f);
            AppUtils.flowPushAndRemove(getView(), newHead, new FlowHistoryStripper() {
                @Override
                public boolean shouldRemovePath(Object path) {
                    return !(path instanceof ChatList);
                }
            }, Flow.Direction.FORWARD);

//            s = client.sendRx(

//                    .observeOn(mainThread())
//                    .subscribe(new ObserverAdapter<TdApi.TLObject>() {
//                        @Override
//                        public void onNext(TdApi.TLObject response) {
//                            open(chat);
//                        }
//                    });
        }

    }

    private void open(TdApi.Chat chat) {
        final Chat newHead = new Chat(chat, path.me, /* messages to forward */ null);
        AppUtils.flowPushAndRemove(getView(), newHead, new FlowHistoryStripper() {
            @Override
            public boolean shouldRemovePath(Object path) {
                return !(path instanceof ChatList);
            }
        }, Flow.Direction.FORWARD);
    }
}
