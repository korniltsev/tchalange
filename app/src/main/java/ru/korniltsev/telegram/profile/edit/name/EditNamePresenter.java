package ru.korniltsev.telegram.profile.edit.name;

import android.os.Bundle;
import flow.Flow;
import mortar.ViewPresenter;
import ru.korniltsev.telegram.core.rx.RXClient;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EditNamePresenter extends ViewPresenter<EditNameView> {
    final EditNamePath path;
    final RXClient client;

    @Inject
    public EditNamePresenter(EditNamePath path, RXClient client) {
        this.path = path;
        this.client = client;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        getView().bindUser(path.user);

    }

    public void editName(String firstName, String lastName) {
        Flow.get(getView())
                .goBack();
    }
}
