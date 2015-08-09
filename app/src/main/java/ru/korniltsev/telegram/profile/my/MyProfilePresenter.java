package ru.korniltsev.telegram.profile.my;

import android.os.Bundle;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.passcode.PasscodeManager;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.main.passcode.PasscodePath;
import ru.korniltsev.telegram.profile.edit.name.EditNamePath;
import ru.korniltsev.telegram.profile.edit.passcode.EditPasscode;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;
import javax.inject.Singleton;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

@Singleton
public class MyProfilePresenter extends ViewPresenter<MyProfileView> {
    final MyProfilePath path;
    final RXClient client;
    final PasscodeManager passcodeManager;
    private Subscription subscribtion = Subscriptions.empty();

    @Inject
    public MyProfilePresenter(MyProfilePath path, RXClient client, PasscodeManager passcodeManager) {
        this.path = path;
        this.client = client;
        this.passcodeManager = passcodeManager;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        subscribtion = client.sendRx(new TdApi.GetMe())
                .observeOn(mainThread())
                .subscribe(new ObserverAdapter<TdApi.TLObject>() {
                    @Override
                    public void onNext(TdApi.TLObject response) {
                        final TdApi.User me = (TdApi.User) response;
                        getView().bindUser(me, passcodeManager.passCodeEnabled());
                    }
                });


    }

    @Override
    public void dropView(MyProfileView view) {
        super.dropView(view);
        subscribtion.unsubscribe();
    }

    public void logout() {
        client.logout();
    }

    public void editName() {
        getView().post(new Runnable() {
            @Override
            public void run() {
                Flow.get(getView())
                        .set(new EditNamePath());
            }
        });

    }

    public void passcodeClicked() {
        if (passcodeManager.passCodeEnabled()){
            Flow.get(getView())
                    .set(new PasscodePath(PasscodePath.TYPE_LOCK_TO_CHANGE));
        } else {
            Flow.get(getView())
                    .set(new EditPasscode());
        }
    }

    public void changePhoto() {
        AppUtils.toastUnsupported(getView().getContext());
    }
}
