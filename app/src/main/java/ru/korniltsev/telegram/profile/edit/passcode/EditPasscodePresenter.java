package ru.korniltsev.telegram.profile.edit.passcode;

import android.os.Bundle;
import flow.Flow;
import mortar.ViewPresenter;
import ru.korniltsev.telegram.core.rx.RXClient;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EditPasscodePresenter extends ViewPresenter<EditPasscodeView> {
    final EditPasscode path;
    final RXClient client;

    @Inject
    public EditPasscodePresenter(EditPasscode path, RXClient client) {
        this.path = path;
        this.client = client;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);

    }


}
