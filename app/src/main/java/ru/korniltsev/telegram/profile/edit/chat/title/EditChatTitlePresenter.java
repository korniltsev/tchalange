package ru.korniltsev.telegram.profile.edit.chat.title;

import android.os.Bundle;
import android.widget.Toast;
import com.crashlytics.android.core.CrashlyticsCore;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import javax.inject.Singleton;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

@Singleton
public class EditChatTitlePresenter extends ViewPresenter<EditChatTitleView> {
    final EditChatTitlePath path;
    final RXClient client;
    private CompositeSubscription cs;

    @Inject
    public EditChatTitlePresenter(EditChatTitlePath path, RXClient client) {
        this.path = path;
        this.client = client;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        cs = new CompositeSubscription();
        try {
            final TdApi.Chat chat = (TdApi.Chat) client.sendRx(new TdApi.GetChat(path.chatId)).toBlocking().first();
            getView()
                    .bindTitle(chat);
        } catch (Exception e) {
            CrashlyticsCore.getInstance().logException(e);
        }

    }

    public void editName(String title) {
        cs.add(client.sendRx(new TdApi.ChangeChatTitle(path.chatId, title))
                .observeOn(mainThread())
                .subscribe(new ObserverAdapter<TdApi.TLObject>() {
                    @Override
                    public void onNext(TdApi.TLObject response) {
                        Flow.get(getView())
                                .goBack();
                    }

                }));
    }



    @Override
    public void dropView(EditChatTitleView view) {
        super.dropView(view);
        cs.unsubscribe();
    }
}
