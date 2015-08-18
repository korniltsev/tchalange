package ru.korniltsev.telegram.profile.media;

import android.os.Bundle;
import com.crashlytics.android.core.CrashlyticsCore;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import javax.inject.Singleton;

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
        getView().bind(path);
//        try {
//            final TdApi.Chat chat = (TdApi.Chat) client.sendRx(new TdApi.GetChat(path.chatId)).toBlocking().first();
//            getView()
//                    .bindTitle(chat);
//        } catch (Exception e) {
//            CrashlyticsCore.getInstance().logException(e);
//        }

    }

//    public void editName(String title) {
//        cs.add(client.sendRx(new TdApi.ChangeChatTitle(path.chatId, title))
//                .observeOn(mainThread())
//                .subscribe(new ObserverAdapter<TdApi.TLObject>() {
//                    @Override
//                    public void onNext(TdApi.TLObject response) {
//                        Flow.get(getView())
//                                .goBack();
//                    }
//
//                }));
//    }



    @Override
    public void dropView(SharedMediaView view) {
        super.dropView(view);
        cs.unsubscribe();
    }
}
