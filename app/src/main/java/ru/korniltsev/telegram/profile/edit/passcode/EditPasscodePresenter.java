package ru.korniltsev.telegram.profile.edit.passcode;

import android.os.Bundle;
import android.support.annotation.NonNull;
import flow.Flow;
import mortar.ViewPresenter;
import ru.korniltsev.telegram.attach_panel.ListChoicePopup;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.passcode.PasscodeManager;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.main.passcode.PasscodePath;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Map;

@Singleton
public class EditPasscodePresenter extends ViewPresenter<EditPasscodeView> {
    final EditPasscode path;
    final RXClient client;
    final PasscodeManager passcodeManager;
    final ActivityOwner activity;
    private ListChoicePopup popup;

    @Inject
    public EditPasscodePresenter(EditPasscode path, RXClient client, PasscodeManager passcodeManager, ActivityOwner activity) {
        this.path = path;
        this.client = client;
        this.passcodeManager = passcodeManager;
        this.activity = activity;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);

        getView()
                .bind(passcodeManager.passCodeEnabled(), false, passcodeManager.getAutoLockTime());
    }

    public void toggleClicked() {
        if (passcodeManager.passCodeEnabled()){
            passcodeManager.setPasscodeEnabled(false);
            getView().bind(false, true, passcodeManager.getAutoLockTime());
        } else {
            //open
            setPasscode();
        }
    }

    private void setPasscode() {
        Flow.get(getView())
                .set(new PasscodePath(PasscodePath.TYPE_SET, PasscodeManager.TYPE_PIN));
    }

    public void changePasscodeClicked() {
        setPasscode();
    }



    public void changeAutoLockTiming() {
        final ArrayList<ListChoicePopup.Item> data = new ArrayList<>();
        for (final Map.Entry<Long, String> e : getView().mapping()) {
            data.add(new ListChoicePopup.Item(e.getValue(), new Runnable() {
                @Override
                public void run() {
                    passcodeManager.setAutoLockTiming(e.getKey());
                    getView()
                            .bindAutoLockTiming(e.getKey());
                }
            }));
        }

        popup = ListChoicePopup.create(activity.expose(), data);
    }

    @NonNull
    private ListChoicePopup.Item t(String localizedText, final long t) {
        return new ListChoicePopup.Item(localizedText, new Runnable() {
            @Override
            public void run() {
                passcodeManager.setAutoLockTiming(t);
            }
        });
    }



    public boolean hidePopup() {
        if (popup != null && popup.isShowing()){
            popup.dismiss();
            popup = null;
            return true;
        }
        popup = null;
        return false;
    }
}
