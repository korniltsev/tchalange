package ru.korniltsev.telegram.profile.edit.name;

import android.os.Bundle;
import android.widget.Toast;
import com.crashlytics.android.core.CrashlyticsCore;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;
import javax.inject.Singleton;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

@Singleton
public class EditNamePresenter extends ViewPresenter<EditNameView> {
    final EditNamePath path;
    final RXClient client;
    private CompositeSubscription cs;

    @Inject
    public EditNamePresenter(EditNamePath path, RXClient client) {
        this.path = path;
        this.client = client;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        cs = new CompositeSubscription();
        try {
            getView().bindUser(
                    client.getMeBlocking());
        } catch (Exception e) {
            CrashlyticsCore.getInstance().logException(e);
        }
    }

    public void editName(String firstName, String lastName) {
        cs.add(client.sendRx(new TdApi.ChangeName(firstName, lastName))
                .observeOn(mainThread())
                .subscribe(new ObserverAdapter<TdApi.TLObject>() {
                    @Override
                    public void onNext(TdApi.TLObject response) {
                        Flow.get(getView())
                                .goBack();
                    }

                    @Override
                    public void onError(Throwable th) {

                        if (th instanceof RXClient.RxClientException){
                            final TdApi.Error err = ((RXClient.RxClientException) th).error;
                            if (err.text.equals("NAME_NOT_MODIFIED")) {
                                showNotModifiedError();
                                return;
                            }
                        }
                        super.onError(th);
                    }
                }));
    }

    private void showNotModifiedError() {
        Toast.makeText(getView().getContext(), R.string.name_not_modified, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void dropView(EditNameView view) {
        super.dropView(view);
        cs.unsubscribe();
    }
}
