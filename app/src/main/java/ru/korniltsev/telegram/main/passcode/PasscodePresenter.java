package ru.korniltsev.telegram.main.passcode;

import android.os.Bundle;
import android.support.annotation.NonNull;
import flow.Flow;
import mortar.ViewPresenter;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.common.FlowHistoryStripper;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.passcode.PasscodeManager;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.profile.edit.passcode.EditPasscode;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PasscodePresenter extends ViewPresenter<PasscodeView> {
    final PasscodePath path;
    final ActivityOwner owner;
    final PasscodeManager passcodeManager;


    @Inject
    public PasscodePresenter(PasscodePath path, ActivityOwner owner, PasscodeManager passcodeManager) {
        this.path = path;
        this.owner = owner;
        this.passcodeManager = passcodeManager;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);

        getView()
                .bindPasscode(path);
    }

    @Override
    public void dropView(PasscodeView view) {
        super.dropView(view);
        view.hideKeyboard();
    }

    public boolean unlock(String s) {
        if (passcodeManager.unlock(s)) {
            final Flow flow = Flow.get(getView());
            if (path.type == PasscodePath.TYPE_LOCK){
                flow.goBack();
            } else {
                AppUtils.flowPushAndRemove(getView(), new EditPasscode(), new FlowHistoryStripper() {
                    @Override
                    public boolean shouldRemovePath(Object path) {
                        return path instanceof PasscodePath;
                    }
                }, Flow.Direction.FORWARD);
//                flow.goBack();
//                flow.set(new EditPasscode());
            }

            return true;
        }
        return false;
    }

    public boolean onBackPressed() {
        if (path.type == PasscodePath.TYPE_LOCK){
            owner.expose().finish();
            return true;
        }
        return false;
    }

    public void setNewPassword(@NonNull String firstPassword) {
        passcodeManager.setPassword(firstPassword);
        passcodeManager.setPasscodeEnabled(true);
        Flow.get(getView())
                .goBack();
    }
}
