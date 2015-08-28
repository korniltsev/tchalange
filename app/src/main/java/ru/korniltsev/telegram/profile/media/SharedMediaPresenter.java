package ru.korniltsev.telegram.profile.media;

import android.os.Bundle;
import android.support.annotation.Nullable;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.rx.RXAuthState;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.SharedMediaHelper;
import ru.korniltsev.telegram.profile.chatselection.SelectChatPath;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.Set;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

@Singleton
public class SharedMediaPresenter extends ViewPresenter<SharedMediaView> {
    final SharedMediaPath path;
    final RXClient client;
    private final RXAuthState auth;
    private CompositeSubscription cs;
    @Nullable private TdApi.User currentUser;

    @Inject
    public SharedMediaPresenter(SharedMediaPath path, RXClient client, RXAuthState auth) {
        this.path = path;
        this.client = client;
        this.auth = auth;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        cs = new CompositeSubscription();
        cs.add(
                auth.getMe(client)
                        .subscribe(new ObserverAdapter<RXAuthState.StateAuthorized>() {
                            @Override
                            public void onNext(RXAuthState.StateAuthorized response) {
                                currentUser = response.user;
                            }
                        }));
        path.loadCount++;
        if (path.loadCount == 1) {
            SharedMediaHelper.Holder helper = MyApp.from(getView().getContext()).sharedMediaHelper.getHolder(path.chatId);
            helper.clear();
        }

        getView().bind(path);
    }

    @Override
    public void dropView(SharedMediaView view) {
        super.dropView(view);
        cs.unsubscribe();
    }

    public void deleteMessages(Set<Integer> selectedMessages) {
        final int[] msgIds = asIntArray(selectedMessages);
        cs.add(
                client.sendRx(new TdApi.DeleteMessages(path.chatId, msgIds))
                        .observeOn(mainThread())
                        .subscribe(new ObserverAdapter<TdApi.TLObject>() {
                            @Override
                            public void onNext(TdApi.TLObject response) {
                                TdApi.Ok res = (TdApi.Ok) response;
                                getView()
                                        .messagesDeleted(msgIds);
                            }
                        }));
    }

    private static int[] asIntArray(Set<Integer> selectedMessages) {
        final int[] msgIds = new int[selectedMessages.size()];
        int i = 0;
        for (Integer msgId : selectedMessages) {
            msgIds[i++] = msgId;
        }
        return msgIds;
    }

    public void forwardMessages(Set<Integer> selected) {
        if (currentUser == null){
            return;
        }
        final int[] messageIds = asIntArray(selected);
        Flow.get(getView())
                .set(new SelectChatPath(null, messageIds, path.chatId, currentUser, false));
    }
}
