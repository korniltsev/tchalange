package ru.korniltsev.telegram.profile.my;

import android.os.Bundle;
import com.crashlytics.android.core.CrashlyticsCore;
import flow.Flow;
import mortar.ViewPresenter;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.core.passcode.PasscodeManager;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.main.passcode.PasscodePath;
import ru.korniltsev.telegram.profile.edit.name.EditNamePath;
import ru.korniltsev.telegram.profile.edit.passcode.EditPasscode;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MyProfilePresenter extends ViewPresenter<MyProfileView> {
    final MyProfilePath path;
    final RXClient client;
    final PasscodeManager passcodeManager;

    @Inject
    public MyProfilePresenter(MyProfilePath path, RXClient client, PasscodeManager passcodeManager) {
        this.path = path;
        this.client = client;
        this.passcodeManager = passcodeManager;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        try {
            getView()
                    .bindUser(
                            client.getMeBlocking(), passcodeManager.passCodeEnabled());
        } catch (Exception e) {
            CrashlyticsCore.getInstance().logException(e);

        }
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
