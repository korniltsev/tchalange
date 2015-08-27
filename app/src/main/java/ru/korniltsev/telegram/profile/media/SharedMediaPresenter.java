package ru.korniltsev.telegram.profile.media;

import android.os.Bundle;
import com.crashlytics.android.core.CrashlyticsCore;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.SharedMediaHelper;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.Set;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

@Singleton
public class SharedMediaPresenter extends ViewPresenter<SharedMediaView> {
    final SharedMediaPath path;
    final RXClient client;
    private CompositeSubscription cs;

    @Inject
    public SharedMediaPresenter(SharedMediaPath path, RXClient client) {
        this.path = path;
        this.client = client;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        cs = new CompositeSubscription();

        path.loadCount++;
        if (path.loadCount == 1){
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
        final int[] msgIds = new int[selectedMessages.size()];
        int i = 0;
        for (Integer msgId : selectedMessages) {
            msgIds[i++] = msgId;
        }
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

    public void forwardMessages(Set<Integer> selected) {

    }
}
